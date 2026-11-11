package kurotoon.reader.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import tachiyomi.core.common.util.system.logcat
import kotlin.concurrent.thread

/**
 * Everything the reader plays: WEBTOON episode music that starts and stops on the right panels,
 * a synthesised ambience under any chapter, and an optional page-turn swish. All of it pauses with
 * the reader, yields to calls and other apps through audio focus, and stops when the reader closes.
 */
@Stable
class ReaderSoundtrack(context: Context) {

    private val app = context.applicationContext
    private val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val main = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Episode music in this chapter, empty when there is none. */
    var tracks by mutableStateOf<List<BgmTrack>>(emptyList())
        private set

    /** The episode music playing right now, if any. */
    var playing by mutableStateOf<BgmTrack?>(null)
        private set

    /** Whether the current page sits inside a piece of episode music, muted or not. */
    var musicHere by mutableStateOf(false)
        private set

    /** Muted from the reader's quick toggle; lasts until the reader closes. */
    var muted by mutableStateOf(false)
        private set

    /** The ambience that is actually playing (Auto resolved to a real one). */
    var activeScape by mutableStateOf(Soundscape.OFF)
        private set

    private var musicEnabled = true
    var musicVolume = 0.8f
    var ambienceVolume = 0.5f

    private var resumed = false
    private var chapterKey: String? = null
    private var loadJob: Job? = null
    private var currentPage = 0

    private var player: MediaPlayer? = null
    private var playerTrack: BgmTrack? = null
    private var fadeLevel = 0f
    private var fadeTarget = 0f
    private val fadeTick = object : Runnable {
        override fun run() {
            val p = player ?: return
            val step = 0.06f
            fadeLevel = if (fadeLevel < fadeTarget) {
                (fadeLevel + step).coerceAtMost(fadeTarget)
            } else {
                (fadeLevel - step).coerceAtLeast(fadeTarget)
            }
            val v = fadeLevel * musicVolume
            runCatching { p.setVolume(v, v) }
            if (fadeLevel == 0f && fadeTarget == 0f) {
                releasePlayer()
            } else if (fadeLevel != fadeTarget) {
                main.postDelayed(this, 50)
            }
        }
    }

    @Volatile private var ambienceThread: Thread? = null

    @Volatile private var ambienceGain = 0f

    @Volatile private var ambienceRunning = false

    /** Bumped on every start and stop so an old audio thread knows to finish. */
    @Volatile private var ambienceGeneration = 0

    private val pageTurn by lazy { buildPageTurn() }

    private val focusRequest: AudioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener { change ->
            when (change) {
                AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pauseOutputs()
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> ambienceGain = ambienceVolume * 0.3f
                AudioManager.AUDIOFOCUS_GAIN -> if (resumed) resumeOutputs()
            }
        }
        .build()
    private var hasFocus = false

    /**
     * Loads the music for a chapter. [baseUrl] and [chapterUrl] identify it on its source, [pageUrls]
     * are the reader's loaded image urls, and [client] is the source's network client.
     */
    fun loadChapter(baseUrl: String?, chapterUrl: String, pageUrls: List<String?>, client: OkHttpClient?) {
        val key = "$baseUrl|$chapterUrl"
        if (key == chapterKey) return
        chapterKey = key
        loadJob?.cancel()
        stopMusic(immediate = false)
        tracks = emptyList()
        musicHere = false
        if (!musicEnabled || client == null || !WebtoonBgm.isWebtoonSource(baseUrl)) return
        val viewer = WebtoonBgm.viewerUrl(baseUrl!!, chapterUrl) ?: return
        loadJob = scope.launch {
            val found = withContext(Dispatchers.IO) {
                runCatching {
                    val html = client.newCall(WebtoonBgm.viewerRequest(viewer)).execute().use { it.body.string() }
                    WebtoonBgm.parse(html, pageUrls)
                }.onFailure { logcat { "No episode music: ${it.message}" } }.getOrDefault(emptyList())
            }
            if (!isActive || chapterKey != key) return@launch
            tracks = found
            this@ReaderSoundtrack.client = client
            onPage(currentPage)
        }
    }

    private var client: OkHttpClient? = null

    /** Turns episode music on or off; turning it on reloads the current chapter's tracks. */
    fun enableMusic(enabled: Boolean) {
        if (enabled == musicEnabled) return
        musicEnabled = enabled
        chapterKey = null
        if (!enabled) {
            loadJob?.cancel()
            stopMusic(immediate = false)
            tracks = emptyList()
        }
    }

    /** Called as the reader moves; [index] is the zero-based page in the current chapter. */
    fun onPage(index: Int) {
        currentPage = index
        musicHere = musicEnabled && tracks.any { it.playPage <= index && (it.stopPage == null || index < it.stopPage) }
        val want = trackFor(index)
        if (want == playerTrack) return
        if (want == null) {
            stopMusic(immediate = false)
        } else {
            startMusic(want)
        }
    }

    /** The track that should be heard on [index]: the latest one started that hasn't reached its stop panel. */
    private fun trackFor(index: Int): BgmTrack? {
        if (muted || !musicEnabled) return null
        return tracks.lastOrNull { it.playPage <= index && (it.stopPage == null || index < it.stopPage) }
    }

    fun toggleMute() {
        muted = !muted
        if (muted) {
            stopMusic(immediate = false)
            ambienceGain = 0f
        } else {
            ambienceGain = ambienceTargetGain()
            onPage(currentPage)
        }
    }

    /** Switches the ambience; [genres] decide what Auto plays. */
    fun setSoundscape(scape: Soundscape, genres: List<String>?) {
        val resolved = if (scape == Soundscape.AUTO) Soundscape.forGenres(genres) else scape
        if (resolved == activeScape && ambienceRunning == (resolved != Soundscape.OFF && resumed)) {
            ambienceGain = ambienceTargetGain()
            return
        }
        stopAmbience()
        activeScape = resolved
        if (resolved != Soundscape.OFF && resumed) startAmbience(resolved)
    }

    fun setVolumes(music: Float, ambience: Float) {
        musicVolume = music
        ambienceVolume = ambience
        ambienceGain = ambienceTargetGain()
        player?.let { p -> runCatching { p.setVolume(fadeLevel * music, fadeLevel * music) } }
    }

    fun playPageTurn(volume: Float) {
        if (muted || !resumed) return
        val track = pageTurn ?: return
        runCatching {
            track.stop()
            track.reloadStaticData()
            track.setVolume(volume)
            track.play()
        }
    }

    fun onResume() {
        resumed = true
        resumeOutputs()
    }

    fun onPause() {
        resumed = false
        pauseOutputs()
        abandonFocus()
    }

    fun release() {
        resumed = false
        scope.cancel()
        main.removeCallbacksAndMessages(null)
        releasePlayer()
        stopAmbience()
        runCatching { pageTurn?.release() }
        abandonFocus()
    }

    private fun ambienceTargetGain(): Float {
        if (muted) return 0f
        // Duck under episode music so the two don't fight
        return if (playerTrack != null) ambienceVolume * 0.3f else ambienceVolume
    }

    private fun pauseOutputs() {
        runCatching { player?.takeIf { it.isPlaying }?.pause() }
        stopAmbience(keepScape = true)
    }

    private fun resumeOutputs() {
        if (player != null || activeScape != Soundscape.OFF) requestFocus()
        runCatching { player?.start() }
        if (activeScape != Soundscape.OFF && !ambienceRunning) startAmbience(activeScape)
        if (player == null) onPage(currentPage)
    }

    private fun startMusic(track: BgmTrack) {
        val c = client ?: return
        releasePlayer()
        playerTrack = track
        playing = track
        ambienceGain = ambienceTargetGain()
        scope.launch {
            val url = withContext(Dispatchers.IO) { WebtoonBgm.streamUrl(c, track.audioId) }
            if (url == null || playerTrack != track) return@launch
            val p = MediaPlayer()
            player = p
            runCatching {
                p.setAudioAttributes(attributes)
                p.setDataSource(app, Uri.parse(url), mapOf("Referer" to "https://www.webtoons.com/"))
                p.isLooping = true
                p.setVolume(0f, 0f)
                p.setOnPreparedListener {
                    if (player !== it) return@setOnPreparedListener
                    if (resumed) {
                        requestFocus()
                        it.start()
                    }
                    fadeLevel = 0f
                    fadeTarget = 1f
                    main.removeCallbacks(fadeTick)
                    main.post(fadeTick)
                }
                p.setOnErrorListener { mp, what, extra ->
                    logcat { "Episode music failed: $what/$extra" }
                    if (player === mp) releasePlayer()
                    true
                }
                p.prepareAsync()
            }.onFailure {
                logcat { "Episode music failed: ${it.message}" }
                releasePlayer()
            }
        }
    }

    private fun stopMusic(immediate: Boolean) {
        playerTrack = null
        playing = null
        ambienceGain = ambienceTargetGain()
        if (player == null) return
        if (immediate) {
            releasePlayer()
        } else {
            fadeTarget = 0f
            main.removeCallbacks(fadeTick)
            main.post(fadeTick)
        }
    }

    private fun releasePlayer() {
        main.removeCallbacks(fadeTick)
        player?.let { runCatching { it.release() } }
        player = null
        fadeLevel = 0f
    }

    private fun startAmbience(scape: Soundscape) {
        if (ambienceRunning) return
        requestFocus()
        ambienceRunning = true
        val generation = ++ambienceGeneration
        ambienceGain = ambienceTargetGain()
        ambienceThread = thread(name = "kurotoon-ambience", isDaemon = true) {
            val rate = SAMPLE_RATE
            val minBuf = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(rate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(maxOf(minBuf, rate / 2))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }.getOrNull() ?: run {
                if (ambienceGeneration == generation) ambienceRunning = false
                return@thread
            }
            val synth = SoundscapeSynth(rate, scape)
            val buffer = ShortArray(rate / 20)
            var gain = 0f
            runCatching {
                track.play()
                while (ambienceGeneration == generation) {
                    // Ease towards the wanted loudness so changes never click
                    val target = ambienceGain
                    gain += (target - gain) * 0.15f
                    synth.fill(buffer, gain)
                    track.write(buffer, 0, buffer.size)
                }
                // Short fade out
                repeat(4) {
                    gain *= 0.5f
                    synth.fill(buffer, gain)
                    track.write(buffer, 0, buffer.size)
                }
            }
            runCatching {
                track.stop()
                track.release()
            }
        }
    }

    private fun stopAmbience(keepScape: Boolean = false) {
        ambienceRunning = false
        ambienceGeneration++
        ambienceThread = null
        if (!keepScape) activeScape = Soundscape.OFF
    }

    private fun requestFocus() {
        if (hasFocus) return
        hasFocus = audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        if (!hasFocus) return
        audioManager.abandonAudioFocusRequest(focusRequest)
        hasFocus = false
    }

    private fun buildPageTurn(): AudioTrack? = runCatching {
        val samples = pageTurnSamples(SAMPLE_RATE)
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
            .also { it.write(samples, 0, samples.size) }
    }.getOrNull()

    private companion object {
        const val SAMPLE_RATE = 22050
        val attributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }
}
