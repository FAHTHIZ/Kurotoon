package kurotoon.reader.sound

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * Plays nicely with the rest of the phone. Asks for audio focus before anything is heard, ducks
 * under navigation prompts, steps aside for calls and other music apps, and notices when the
 * headphones come out so the soundtrack never suddenly blares from the speaker.
 *
 * [onChange] runs on the main thread whenever [duck] or [interrupted] change.
 */
class SoundFocus(context: Context, private val onChange: () -> Unit) {

    private val app = context.applicationContext
    private val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val main = Handler(Looper.getMainLooper())

    /** Loudness multiplier: 1 normally, lower while another app speaks over us. */
    var duck = 1f
        private set

    /**
     * Why we went quiet without the reader asking: a call, another app's music, or unplugged
     * headphones. Null while nothing is in the way.
     */
    var interrupted: Interruption? = null
        private set

    /** Leave other apps' audio playing and mix with it instead of asking for focus. */
    var mixWithOthers = false
        set(value) {
            if (field == value) return
            field = value
            if (value) abandon()
        }

    private var hasFocus = false

    private val request: AudioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        .setWillPauseWhenDucked(false)
        .setOnAudioFocusChangeListener(::onFocusChange, main)
        .build()

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                interrupt(Interruption.UNPLUGGED)
            }
        }
    }
    private var listeningForNoise = false

    /** Asks to be heard. Returns whether sound may play now. */
    fun acquire(): Boolean {
        if (interrupted != null) return false
        listenForNoise(true)
        if (mixWithOthers || hasFocus) return true
        val result = runCatching {
            audioManager.requestAudioFocus(request)
        }.getOrDefault(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        // A refused request (usually an ongoing call) is treated like an interruption so the
        // reader shows a paused badge instead of silently doing nothing.
        if (!hasFocus) interrupt(Interruption.OTHER_APP)
        return hasFocus
    }

    /** Lets go of focus, for example when the reader goes to the background. */
    fun abandon() {
        listenForNoise(false)
        if (!hasFocus) return
        runCatching { audioManager.abandonAudioFocusRequest(request) }
        hasFocus = false
    }

    /** The reader asked to play again after an interruption (badge tap, unmute, coming back). */
    fun clearInterruption() {
        if (interrupted == null) return
        interrupted = null
        duck = 1f
        onChange()
    }

    private fun interrupt(reason: Interruption) {
        if (interrupted == reason) return
        interrupted = reason
        // Posted: this can happen inside acquire(), while the caller is still deciding what to play
        main.post(onChange)
    }

    private fun onFocusChange(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Another app took over for good: stay quiet until the reader asks again
                hasFocus = false
                listenForNoise(false)
                interrupt(Interruption.OTHER_APP)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> interrupt(Interruption.TRANSIENT)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                duck = DUCK_LEVEL
                onChange()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasFocus = true
                duck = 1f
                // Only a short interruption (a notification, a quick call) resumes on its own
                if (interrupted == Interruption.TRANSIENT) interrupted = null
                onChange()
            }
        }
    }

    private fun listenForNoise(listen: Boolean) {
        if (listen == listeningForNoise) return
        listeningForNoise = listen
        runCatching {
            if (listen) {
                ContextCompat.registerReceiver(
                    app,
                    noisyReceiver,
                    IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )
            } else {
                app.unregisterReceiver(noisyReceiver)
            }
        }
    }

    fun release() {
        abandon()
        main.removeCallbacksAndMessages(null)
    }

    enum class Interruption {
        /** A call or a short prompt; resumes by itself afterwards. */
        TRANSIENT,

        /** Another app started playing; tap to take the sound back. */
        OTHER_APP,

        /** Headphones or a Bluetooth speaker disconnected. */
        UNPLUGGED,
    }

    private companion object {
        const val DUCK_LEVEL = 0.25f
    }
}

/** Slider position (0..1) to amplitude, so 50 % on the slider sounds about half as loud. */
fun perceivedVolume(slider: Float): Float {
    val v = slider.coerceIn(0f, 1f)
    return v * v
}
