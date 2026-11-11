package kurotoon.reader.sound

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import tachiyomi.core.common.util.system.logcat
import java.io.File

/**
 * Plays WEBTOON episode music with ExoPlayer, which handles Naver's HLS streams far more reliably
 * than the platform player. Tracks crossfade into each other, the next track is buffered a couple of
 * panels before it starts so it lands on the right panel, failures retry with a fresh stream token,
 * and everything heard is cached so re-reading a chapter (even offline) plays its music again.
 *
 * Main thread only.
 */
@OptIn(UnstableApi::class)
internal class EpisodeMusic(
    context: Context,
    private val scope: CoroutineScope,
    private val onStateChanged: () -> Unit,
) {

    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    /** One track with its own player, fading towards [target]. */
    private inner class Deck(val track: BgmTrack) {
        var player: ExoPlayer? = null
        var level = 0f
        var target = 0f

        /** Pause (rather than release) once silent: used for mute so unmuting continues the song. */
        var holdWhenSilent = false
        var attempts = 0
        var loadJob: Job? = null
        var buffering = true
        var released = false

        fun release() {
            if (released) return
            released = true
            loadJob?.cancel()
            player?.let { p -> runCatching { p.release() } }
            player = null
        }
    }

    private var client: OkHttpClient? = null
    private var current: Deck? = null
    private var armed: Deck? = null
    private val fading = mutableListOf<Deck>()

    /** Tracks that failed every retry in this chapter; skipped until [retryFailed]. */
    private val failed = mutableSetOf<String>()

    /** Base loudness from the music slider, already on a perceptual curve. */
    var volume = 0.64f
        set(value) {
            field = value
            applyVolumes()
        }

    /** Extra multiplier from focus ducking. */
    var duck = 1f
        set(value) {
            field = value
            applyVolumes()
        }

    /** Whether players may make sound right now (reader in front, not muted or interrupted). */
    private var audible = false

    val playingTrack: BgmTrack? get() = current?.track
    val isBuffering: Boolean get() = current?.let { it.buffering && !it.released } == true
    val hasFailure: Boolean get() = failed.isNotEmpty()

    fun setClient(client: OkHttpClient?) {
        this.client = client
    }

    /** Fades into [track], or out to silence when it's null. Reuses a deck still fading out. */
    fun play(track: BgmTrack?) {
        if (track?.audioId in failed) return play(null)
        val now = current
        if (now?.track == track) {
            if (now != null) {
                now.target = 1f
                now.holdWhenSilent = false
                if (audible) startPlayer(now)
                tick()
            }
            return
        }
        now?.let {
            it.target = 0f
            it.holdWhenSilent = false
            fading += it
        }
        current = null
        if (track != null) {
            val revived = fading.firstOrNull { it.track == track && !it.released }
                ?: armed?.takeIf { it.track == track }?.also { armed = null }
            val deck = revived?.also { fading.remove(it) } ?: Deck(track).also(::load)
            deck.target = 1f
            deck.holdWhenSilent = false
            current = deck
            if (audible) startPlayer(deck)
        }
        tick()
        onStateChanged()
    }

    /** Buffers [track] silently so it can start the instant its panel scrolls in. */
    fun arm(track: BgmTrack?) {
        if (track == null || track.audioId in failed) return
        if (current?.track == track || armed?.track == track) return
        if (fading.any { it.track == track }) return
        armed?.release()
        armed = Deck(track).also(::load)
    }

    /** Silences music without forgetting where the song was (mute, sleep timer). */
    fun hold() {
        current?.let {
            it.target = 0f
            it.holdWhenSilent = true
        }
        tick()
    }

    /** The reader can be heard again or must go quiet (pause, interruption, mute). */
    fun setAudible(audible: Boolean) {
        if (this.audible == audible) return
        this.audible = audible
        if (audible) {
            current?.let(::startPlayer)
        } else {
            // Going to the background: anything mid-fade-out simply stops
            fading.forEach { it.release() }
            fading.clear()
            current?.player?.let { runCatching { it.pause() } }
        }
        tick()
    }

    /** Forgets every track (new chapter, music turned off). */
    fun clear(immediate: Boolean) {
        armed?.release()
        armed = null
        failed.clear()
        if (immediate) {
            current?.release()
            fading.forEach { it.release() }
            fading.clear()
            current = null
        } else {
            play(null)
        }
        onStateChanged()
    }

    fun retryFailed(): Boolean {
        if (failed.isEmpty()) return false
        failed.clear()
        onStateChanged()
        return true
    }

    fun release() {
        main.removeCallbacksAndMessages(null)
        armed?.release()
        current?.release()
        fading.forEach { it.release() }
        fading.clear()
        current = null
        armed = null
    }

    private fun load(deck: Deck) {
        val c = client ?: return
        deck.loadJob?.cancel()
        deck.loadJob = scope.launch {
            val fresh = deck.attempts > 0
            val url = withContext(Dispatchers.IO) { BgmStreams.url(app, c, deck.track.audioId, fresh) }
            if (deck.released) return@launch
            if (url == null) {
                onLoadError(deck, "no stream")
                return@launch
            }
            val p = deck.player ?: buildPlayer(c).also {
                deck.player = it
                attach(deck, it)
            }
            val item = MediaItem.Builder()
                .setUri(Uri.parse(url))
                .apply { if (url.contains("m3u8") || url.contains("/hls")) setMimeType(MimeTypes.APPLICATION_M3U8) }
                .build()
            val resumeAt = p.currentPosition.takeIf { fresh }
            p.setMediaItem(item)
            resumeAt?.let { p.seekTo(it) }
            p.prepare()
            if (deck === current && audible) startPlayer(deck)
            tick()
        }
    }

    private fun attach(deck: Deck, player: ExoPlayer) {
        player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    val buffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                    if (deck.buffering != buffering) {
                        deck.buffering = buffering
                        if (deck === current) onStateChanged()
                    }
                    if (playbackState == Player.STATE_READY) {
                        deck.attempts = 0
                        tick()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    onLoadError(deck, error.errorCodeName)
                }
            },
        )
    }

    private fun onLoadError(deck: Deck, reason: String) {
        if (deck.released) return
        logcat { "Episode music ${deck.track.audioId} failed ($reason), attempt ${deck.attempts + 1}" }
        if (deck.attempts >= MAX_ATTEMPTS) {
            failed += deck.track.audioId
            if (deck === current) current = null
            if (deck === armed) armed = null
            fading.remove(deck)
            deck.release()
            onStateChanged()
            return
        }
        deck.attempts++
        // Back off, then ask for a fresh token: Naver's stream addresses expire after a while
        scope.launch {
            delay(RETRY_BASE_MS shl (deck.attempts - 1))
            if (!deck.released) load(deck)
        }
    }

    private fun startPlayer(deck: Deck) {
        val p = deck.player ?: return
        if (deck.target > 0f || deck.level > 0f) runCatching { p.play() }
    }

    private fun buildPlayer(client: OkHttpClient): ExoPlayer {
        val upstream = OkHttpDataSource.Factory(client)
            .setDefaultRequestProperties(
                mapOf("Referer" to "https://www.webtoons.com/", "User-Agent" to WebtoonBgm.DESKTOP_UA),
            )
        val cached = CacheDataSource.Factory()
            .setCache(BgmStreams.cache(app))
            .setUpstreamDataSourceFactory(upstream)
            // Segment addresses carry short-lived tokens in the query; the path alone names the audio
            // Playlists keep their full address so a fresh token always gets a fresh playlist, while
            // the last address we saved still finds its playlist in the cache when offline
            .setCacheKeyFactory { spec ->
                val uri = spec.uri
                spec.key ?: if (uri.lastPathSegment.orEmpty().endsWith(".m3u8")) {
                    uri.toString()
                } else {
                    uri.buildUpon().clearQuery().build().toString()
                }
            }
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        return ExoPlayer.Builder(app)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cached))
            .setLoadControl(
                DefaultLoadControl.Builder()
                    // Short tracks: buffer quickly, start fast, don't hoard memory
                    .setBufferDurationsMs(15_000, 60_000, 500, 1_500)
                    .build(),
            )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                // Focus is handled once for music and ambience together in SoundFocus
                false,
            )
            .setHandleAudioBecomingNoisy(false)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
            }
    }

    private val fadeTick = Runnable { tick() }

    /** Moves every deck one step towards its target and keeps ticking until all have arrived. */
    private fun tick() {
        main.removeCallbacks(fadeTick)
        var moving = false
        val decks = buildList {
            current?.let(::add)
            addAll(fading)
        }
        for (deck in decks) {
            if (deck.released) continue
            val ready = deck.player?.playbackState == Player.STATE_READY
            // Hold the fade-in until the audio is actually there, so it never starts mid-swell
            if (deck.target > deck.level && ready && audible) {
                deck.level = (deck.level + FADE_IN_STEP).coerceAtMost(deck.target)
            } else if (deck.target < deck.level) {
                deck.level = (deck.level - FADE_OUT_STEP).coerceAtLeast(deck.target)
            }
            deck.player?.let { runCatching { it.volume = deck.level * volume * duck } }
            if (deck.level != deck.target && (deck.target < deck.level || (audible && deck.player != null))) {
                moving = true
            }
            if (deck.level == 0f && deck.target == 0f) {
                if (deck.holdWhenSilent && deck === current) {
                    deck.player?.let { runCatching { it.pause() } }
                } else if (deck !== current) {
                    deck.release()
                }
            }
        }
        fading.removeAll { it.released }
        if (moving) main.postDelayed(fadeTick, FRAME_MS)
    }

    private fun applyVolumes() {
        current?.let { d -> d.player?.let { runCatching { it.volume = d.level * volume * duck } } }
        fading.forEach { d -> d.player?.let { runCatching { it.volume = d.level * volume * duck } } }
    }

    private companion object {
        const val FRAME_MS = 40L
        const val FADE_IN_STEP = 40f / 1400f
        const val FADE_OUT_STEP = 40f / 1800f
        const val MAX_ATTEMPTS = 3
        const val RETRY_BASE_MS = 1_200L
    }
}

/** Stream addresses and the audio cache shared by every reader session. */
internal object BgmStreams {

    private const val CACHE_BYTES = 96L * 1024 * 1024
    private const val TOKEN_TTL_MS = 8 * 60 * 1000L
    private const val PREFS = "kurotoon_bgm_streams"

    private val tokens = HashMap<String, Pair<String, Long>>()

    @Volatile private var cache: SimpleCache? = null

    @OptIn(UnstableApi::class)
    fun cache(context: Context): SimpleCache = cache ?: synchronized(this) {
        cache ?: SimpleCache(
            File(context.applicationContext.cacheDir, "kurotoon-bgm"),
            LeastRecentlyUsedCacheEvictor(CACHE_BYTES),
            StandaloneDatabaseProvider(context.applicationContext),
        ).also { cache = it }
    }

    /**
     * A playable address for [audioId]. Reuses a recent token unless [fresh], and falls back to the
     * last address we ever got when the API can't be reached, which still plays from the cache.
     */
    fun url(context: Context, client: OkHttpClient, audioId: String, fresh: Boolean): String? {
        val now = System.currentTimeMillis()
        if (!fresh) {
            synchronized(tokens) { tokens[audioId] }?.takeIf { now - it.second < TOKEN_TTL_MS }?.let { return it.first }
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val url = WebtoonBgm.streamUrl(client, audioId)
        if (url != null) {
            synchronized(tokens) { tokens[audioId] = url to now }
            prefs.edit().putString(audioId, url).apply()
            return url
        }
        return prefs.getString(audioId, null)
    }

    /** Warms up the token for a track that's coming soon. */
    fun prefetch(context: Context, client: OkHttpClient, audioId: String) {
        val cached = synchronized(tokens) { tokens[audioId] }
        if (cached != null && System.currentTimeMillis() - cached.second < TOKEN_TTL_MS) return
        url(context, client, audioId, fresh = false)
    }
}
