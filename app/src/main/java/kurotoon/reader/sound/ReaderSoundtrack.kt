package kurotoon.reader.sound

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
import java.io.File
import java.security.MessageDigest

/**
 * Everything the reader plays: WEBTOON episode music that starts and stops on the right panels,
 * a synthesised ambience under any chapter, and an optional page-turn swish.
 *
 * It pauses with the reader, steps aside for calls and other apps, goes quiet when headphones are
 * unplugged, crossfades between tracks, buffers the next track before its panel arrives, retries
 * broken streams and remembers each chapter's music so downloaded chapters play it offline.
 *
 * Main thread only.
 */
@Stable
class ReaderSoundtrack(context: Context) {

    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val focus = SoundFocus(app, ::onFocusChanged)
    private val music = EpisodeMusic(app, scope, ::syncMusicState)
    private val ambience = AmbienceEngine()
    private val pageTurn = PageTurnSound()

    /** Episode music in this chapter, empty when there is none. */
    var tracks by mutableStateOf<List<BgmTrack>>(emptyList())
        private set

    /** The episode music playing right now, if any. */
    var playing by mutableStateOf<BgmTrack?>(null)
        private set

    /** Whether the current page sits inside a piece of episode music, muted or not. */
    var musicHere by mutableStateOf(false)
        private set

    /** The current track is still loading or buffering. */
    var buffering by mutableStateOf(false)
        private set

    /** Looking up this chapter's music. */
    var loadingTracks by mutableStateOf(false)
        private set

    /** A track couldn't be played after several tries; [retry] tries again. */
    var musicFailed by mutableStateOf(false)
        private set

    /** Muted from the reader's quick toggle; lasts until the reader closes. */
    var muted by mutableStateOf(false)
        private set

    /** Sound stopped because of a call, another app or unplugged headphones; tap to resume. */
    var interruption by mutableStateOf<SoundFocus.Interruption?>(null)
        private set

    /** The ambience that is actually playing (Auto resolved to a real one). */
    var activeScape by mutableStateOf(Soundscape.OFF)
        private set

    private var musicEnabled = true
    private var musicSlider = 0.8f
    private var ambienceSlider = 0.5f

    private var resumed = false
    private var chapterKey: String? = null
    private var loadJob: Job? = null
    private var currentPage = 0
    private var client: OkHttpClient? = null

    /** Lets other apps keep playing instead of pausing them. */
    fun setMixWithOthers(mix: Boolean) {
        focus.mixWithOthers = mix
        refreshOutputs()
    }

    /**
     * Loads the music for a chapter. [baseUrl] and [chapterUrl] identify it on its source, [pageUrls]
     * are the reader's loaded image urls, and [client] is the source's network client.
     */
    fun loadChapter(baseUrl: String?, chapterUrl: String, pageUrls: List<String?>, client: OkHttpClient?) {
        val key = "$baseUrl|$chapterUrl"
        if (key == chapterKey) return
        lastLoad = { loadChapter(baseUrl, chapterUrl, pageUrls, client) }
        chapterKey = key
        loadJob?.cancel()
        music.clear(immediate = false)
        tracks = emptyList()
        musicHere = false
        loadingTracks = false
        if (!musicEnabled || client == null || !WebtoonBgm.isWebtoonSource(baseUrl)) return
        val viewer = WebtoonBgm.viewerUrl(baseUrl!!, chapterUrl) ?: return
        this.client = client
        music.setClient(client)
        loadingTracks = true
        loadJob = scope.launch {
            val found = withContext(Dispatchers.IO) { BgmLists.get(app, key) }
                ?: withContext(Dispatchers.IO) {
                    runCatching {
                        val html = client.newCall(WebtoonBgm.viewerRequest(viewer)).execute().use { it.body.string() }
                        WebtoonBgm.parse(html, pageUrls).also { BgmLists.put(app, key, it) }
                    }.onFailure { logcat { "No episode music: ${it.message}" } }.getOrNull()
                }
            if (!isActive || chapterKey != key) return@launch
            loadingTracks = false
            tracks = found.orEmpty()
            evaluate()
        }
    }

    /** Turns episode music on or off; turning it on reloads the current chapter's tracks. */
    fun enableMusic(enabled: Boolean) {
        if (enabled == musicEnabled) return
        musicEnabled = enabled
        chapterKey = null
        if (!enabled) {
            loadJob?.cancel()
            music.clear(immediate = false)
            tracks = emptyList()
            musicHere = false
            loadingTracks = false
        }
    }

    /** Called as the reader moves; [index] is the zero-based page in the current chapter. */
    fun onPage(index: Int) {
        currentPage = index
        // Flinging through a strip passes many panels a second: settle before switching tracks
        main.removeCallbacks(evaluateRunnable)
        main.postDelayed(evaluateRunnable, SETTLE_MS)
        musicHere = musicEnabled && trackAt(index) != null
    }

    private val evaluateRunnable = Runnable { evaluate() }

    private fun evaluate() {
        val index = currentPage
        musicHere = musicEnabled && trackAt(index) != null
        if (!musicEnabled || muted) {
            music.hold()
        } else {
            music.play(trackAt(index))
            // Get the next track ready a couple of panels early so it starts right on its panel
            val next = tracks.firstOrNull { it.playPage > index && it.playPage - index <= ARM_AHEAD }
            if (next != null) {
                music.arm(next)
            } else {
                tracks.firstOrNull { it.playPage > index && it.playPage - index <= PREFETCH_AHEAD }?.let { soon ->
                    val c = client ?: return@let
                    scope.launch(Dispatchers.IO) { runCatching { BgmStreams.prefetch(app, c, soon.audioId) } }
                }
            }
        }
        syncMusicState()
        refreshOutputs()
    }

    /** The track that should be heard on [index]: the latest one started that hasn't reached its stop panel. */
    private fun trackAt(index: Int): BgmTrack? =
        tracks.lastOrNull { it.playPage <= index && (it.stopPage == null || index < it.stopPage) }

    /** The badge and the sheet's button: resume after an interruption, otherwise mute or unmute. */
    fun toggleMute() {
        if (interruption != null) resumeAfterInterruption() else mute(!muted)
    }

    fun mute(mute: Boolean) {
        if (!mute) focus.clearInterruption()
        if (mute && !muted) {
            // Stay audible long enough for the music and ambience to fade out instead of cutting off
            mutedAt = SystemClock.uptimeMillis()
            main.removeCallbacks(muteSettled)
            main.postDelayed(muteSettled, MUTE_FADE_MS + 50)
        }
        muted = mute
        evaluate()
    }

    private var mutedAt = 0L
    private val muteSettled = Runnable { refreshOutputs() }

    /** Takes the sound back after a call, another app or unplugged headphones. */
    fun resumeAfterInterruption() {
        focus.clearInterruption()
        if (muted) muted = false
        evaluate()
    }

    /** Tries failed tracks again. */
    fun retry() {
        if (music.retryFailed()) evaluate()
        val key = chapterKey ?: return
        if (tracks.isEmpty()) {
            BgmLists.forget(app, key)
            lastLoad?.let { load ->
                chapterKey = null
                load()
            }
        }
    }

    private var lastLoad: (() -> Unit)? = null

    /** Switches the ambience; [genres] decide what Auto plays. */
    fun setSoundscape(scape: Soundscape, genres: List<String>?) {
        activeScape = if (scape == Soundscape.AUTO) Soundscape.forGenres(genres) else scape
        refreshOutputs()
    }

    fun setVolumes(music: Float, ambience: Float) {
        musicSlider = music
        ambienceSlider = ambience
        this.music.volume = perceivedVolume(music)
        refreshOutputs()
    }

    fun playPageTurn(volume: Float) {
        if (muted || !resumed || interruption != null) return
        pageTurn.play(perceivedVolume(volume))
    }

    fun onResume() {
        resumed = true
        // Coming back to the reader counts as asking for sound again
        if (interruption == SoundFocus.Interruption.OTHER_APP) focus.clearInterruption()
        evaluate()
    }

    fun onPause() {
        resumed = false
        refreshOutputs()
        focus.abandon()
    }

    fun release() {
        resumed = false
        scope.cancel()
        main.removeCallbacksAndMessages(null)
        music.release()
        ambience.stop()
        pageTurn.release()
        focus.release()
    }

    private fun onFocusChanged() {
        interruption = focus.interrupted
        refreshOutputs()
    }

    private fun syncMusicState() {
        playing = music.playingTrack
        buffering = music.isBuffering
        musicFailed = music.hasFailure
        refreshAmbienceGain()
    }

    /** Decides what may be heard right now and starts or stops each output to match. */
    private fun refreshOutputs() {
        val fadingOut = muted && SystemClock.uptimeMillis() - mutedAt < MUTE_FADE_MS
        val wantsSound = resumed && (!muted || fadingOut) &&
            ((musicEnabled && music.playingTrack != null) || activeScape != Soundscape.OFF)
        val allowed = wantsSound && focus.interrupted == null && focus.acquire()
        // Give other apps their sound back while we have nothing to play
        if (!wantsSound) focus.abandon()
        interruption = focus.interrupted
        music.duck = focus.duck
        music.setAudible(allowed)
        if (allowed && activeScape != Soundscape.OFF) {
            ambience.start(activeScape)
        } else {
            ambience.stop()
        }
        refreshAmbienceGain()
    }

    private fun refreshAmbienceGain() {
        val base = perceivedVolume(ambienceSlider) * focus.duck
        // Sit under episode music so the two never fight
        ambience.gain = if (muted) {
            0f
        } else if (music.playingTrack != null) {
            base * 0.3f
        } else {
            base
        }
    }

    private companion object {
        const val SETTLE_MS = 180L

        // Just over EpisodeMusic's fade-out, so mute never cuts a note off
        const val MUTE_FADE_MS = 1_900L
        const val ARM_AHEAD = 2
        const val PREFETCH_AHEAD = 6
    }
}

/**
 * Remembers which episode music each chapter has, so a chapter read before plays its music without
 * fetching the viewer page again, including downloaded chapters read offline.
 */
internal object BgmLists {

    private const val FOUND_TTL_MS = 30L * 24 * 60 * 60 * 1000
    private const val NONE_TTL_MS = 3L * 24 * 60 * 60 * 1000
    private val memory = HashMap<String, List<BgmTrack>>()

    private fun file(context: Context, key: String): File {
        val hash = MessageDigest.getInstance("SHA-1").digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(File(context.cacheDir, "kurotoon-bgm-lists").apply { mkdirs() }, hash)
    }

    fun get(context: Context, key: String): List<BgmTrack>? {
        synchronized(memory) { memory[key] }?.let { return it }
        val f = file(context, key)
        if (!f.exists()) return null
        val lines = runCatching { f.readLines() }.getOrNull() ?: return null
        val tracks = lines.mapNotNull { line ->
            val p = line.split(',')
            if (p.size < 4) return@mapNotNull null
            BgmTrack(p[0], p[1].toIntOrNull() ?: 0, p[2].toIntOrNull() ?: return@mapNotNull null, p[3].toIntOrNull())
        }
        val age = System.currentTimeMillis() - f.lastModified()
        if (age > (if (tracks.isEmpty()) NONE_TTL_MS else FOUND_TTL_MS)) return null
        synchronized(memory) { memory[key] = tracks }
        return tracks
    }

    fun put(context: Context, key: String, tracks: List<BgmTrack>) {
        synchronized(memory) { memory[key] = tracks }
        runCatching {
            file(context, key).writeText(
                tracks.joinToString("\n") { "${it.audioId},${it.order},${it.playPage},${it.stopPage ?: ""}" },
            )
        }
    }

    fun forget(context: Context, key: String) {
        synchronized(memory) { memory.remove(key) }
        runCatching { file(context, key).delete() }
    }
}
