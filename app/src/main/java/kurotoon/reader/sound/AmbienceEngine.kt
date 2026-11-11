package kurotoon.reader.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import tachiyomi.core.common.util.system.logcat
import kotlin.concurrent.thread

/**
 * Streams a [Soundscape] from [SoundscapeSynth] on its own audio thread. Changes in loudness are
 * eased so they never click, switching scapes crossfades, and when the audio output dies (Bluetooth
 * reconnecting, the audio server restarting) a new output is opened and the sound carries on.
 */
internal class AmbienceEngine {

    @Volatile var gain = 0f

    /** The scape currently streaming, or OFF. */
    @Volatile var playing = Soundscape.OFF
        private set

    @Volatile private var generation = 0

    fun start(scape: Soundscape) {
        if (scape == Soundscape.OFF) return stop()
        if (playing == scape) return
        val gen = ++generation
        playing = scape
        thread(name = "kurotoon-ambience", isDaemon = true) { run(gen, scape) }
    }

    fun stop() {
        generation++
        playing = Soundscape.OFF
    }

    private fun run(gen: Int, scape: Soundscape) {
        runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO) }
        val synth = SoundscapeSynth(SAMPLE_RATE, scape)
        val buffer = ShortArray(SAMPLE_RATE / 25)
        var level = 0f
        var failures = 0
        while (generation == gen && failures < MAX_FAILURES) {
            val track = open() ?: run {
                failures++
                Thread.sleep(RECOVER_MS)
                null
            } ?: continue
            var broken = false
            runCatching {
                track.play()
                while (generation == gen) {
                    level += (gain - level) * 0.12f
                    synth.fill(buffer, level)
                    if (track.write(buffer, 0, buffer.size) < 0) {
                        broken = true
                        break
                    }
                }
                if (!broken) {
                    // Fade out on the way out so stopping never pops
                    repeat(6) {
                        level *= 0.55f
                        synth.fill(buffer, level)
                        track.write(buffer, 0, buffer.size)
                    }
                }
            }.onFailure { broken = true }
            runCatching {
                track.pause()
                track.flush()
                track.release()
            }
            if (broken) {
                failures++
                logcat { "Ambience output lost, reopening ($failures)" }
                Thread.sleep(RECOVER_MS)
                // Come back gently rather than at full volume
                level = 0f
            }
        }
        if (generation == gen) playing = Soundscape.OFF
    }

    private fun open(): AudioTrack? = runCatching {
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(minBuf, SAMPLE_RATE / 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .takeIf { it.state == AudioTrack.STATE_INITIALIZED }
    }.getOrNull()

    companion object {
        const val SAMPLE_RATE = 22050
        private const val MAX_FAILURES = 6
        private const val RECOVER_MS = 400L
    }
}

/** A static one-shot player for the page-turn swish, rebuilt if the output goes away. */
internal class PageTurnSound {

    private var track: AudioTrack? = null
    private val samples by lazy { pageTurnSamples(AmbienceEngine.SAMPLE_RATE) }

    fun play(volume: Float) {
        if (volume <= 0f) return
        val t = track ?: build()?.also { track = it } ?: return
        val ok = runCatching {
            if (t.playState == AudioTrack.PLAYSTATE_PLAYING) t.stop()
            t.reloadStaticData()
            t.setVolume(volume)
            t.play()
        }.isSuccess
        if (!ok) {
            release()
        }
    }

    fun release() {
        track?.let { runCatching { it.release() } }
        track = null
    }

    private fun build(): AudioTrack? = runCatching {
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(AmbienceEngine.SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
            .also { it.write(samples, 0, samples.size) }
            .takeIf { it.state == AudioTrack.STATE_INITIALIZED }
    }.getOrNull()
}
