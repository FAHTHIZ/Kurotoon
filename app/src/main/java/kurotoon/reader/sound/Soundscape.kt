package kurotoon.reader.sound

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Background ambience the reader can play under a chapter. Every sound is synthesised on the phone. */
enum class Soundscape(val key: String) {
    OFF("off"),

    /** Picks one from the series' genres. */
    AUTO("auto"),
    RAIN("rain"),
    NIGHT("night"),
    FIREPLACE("fire"),
    OCEAN("ocean"),
    FOCUS("focus"),
    ;

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key } ?: OFF

        /** A soundscape that suits [genres], so a horror series reads at night and a romance in the rain. */
        fun forGenres(genres: List<String>?): Soundscape {
            val tags = genres.orEmpty().map { it.lowercase() }
            fun has(vararg words: String) = tags.any { tag ->
                words.any { if (it.length <= 3) tag == it else it in tag }
            }
            return when {
                has("horror", "thriller", "mystery", "psycholog", "supernatural", "tragedy", "zombie") -> NIGHT
                has("romance", "drama", "slice of life", "josei", "shoujo", "bl", "gl", "yaoi", "yuri") -> RAIN
                has("fantasy", "historical", "isekai", "adventure", "murim", "wuxia", "magic") -> FIREPLACE
                has("comedy", "sports", "school", "music", "healing") -> OCEAN
                else -> FOCUS
            }
        }
    }
}

/**
 * Generates [Soundscape] audio sample by sample. Cheap enough to run on a background thread at
 * 22 kHz mono: a handful of one-pole filters, a noise source and a few random events.
 */
class SoundscapeSynth(private val sampleRate: Int, private val scape: Soundscape) {

    private val rnd = Random(System.nanoTime())
    private var t = 0L

    // Filter memories
    private var brown = 0f
    private var pinkA = 0f
    private var pinkB = 0f
    private var pinkC = 0f
    private var lp = 0f
    private var hp = 0f
    private var hpIn = 0f

    // Short events (rain drops, embers, crickets)
    private var eventEnv = 0f
    private var eventFreq = 0f
    private var eventPhase = 0f
    private var eventDecay = 0.999f
    private var chirpLeft = 0
    private var chirpGap = 0

    private fun white() = rnd.nextFloat() * 2f - 1f

    private fun brownNoise(): Float {
        brown = (brown + white() * 0.02f) / 1.02f
        return brown * 3.5f
    }

    private fun pinkNoise(): Float {
        val w = white()
        pinkA = 0.99765f * pinkA + w * 0.0990460f
        pinkB = 0.96300f * pinkB + w * 0.2965164f
        pinkC = 0.57000f * pinkC + w * 1.0526913f
        return (pinkA + pinkB + pinkC + w * 0.1848f) * 0.11f
    }

    private fun lowPass(x: Float, a: Float): Float {
        lp += a * (x - lp)
        return lp
    }

    private fun highPass(x: Float, a: Float): Float {
        hp = a * (hp + x - hpIn)
        hpIn = x
        return hp
    }

    /** Seconds since start, for slow swells. */
    private val seconds get() = t.toFloat() / sampleRate

    fun fill(out: ShortArray, gain: Float) {
        for (i in out.indices) {
            val s = (next() * gain).coerceIn(-1f, 1f)
            out[i] = (s * Short.MAX_VALUE).toInt().toShort()
            t++
        }
    }

    private fun next(): Float = when (scape) {
        Soundscape.RAIN -> rain()
        Soundscape.NIGHT -> night()
        Soundscape.FIREPLACE -> fire()
        Soundscape.OCEAN -> ocean()
        Soundscape.FOCUS, Soundscape.AUTO -> brownNoise() * 0.55f
        Soundscape.OFF -> 0f
    }

    private fun rain(): Float {
        // Steady hiss of distant rain plus close drops that ping and decay
        val bed = lowPass(pinkNoise(), 0.35f) * 0.55f
        if (eventEnv < 0.001f && rnd.nextInt(sampleRate / 9) == 0) {
            eventEnv = 0.25f + rnd.nextFloat() * 0.35f
            eventFreq = 1800f + rnd.nextFloat() * 2600f
            eventDecay = 0.9965f
        }
        eventPhase += (2 * PI * eventFreq / sampleRate).toFloat()
        val drop = sin(eventPhase) * eventEnv * 0.25f
        eventEnv *= eventDecay
        return bed + drop
    }

    private fun night(): Float {
        // Wind that rises and falls, with a cricket chirping in pulses
        val swell = 0.55f + 0.45f * sin(2 * PI * seconds / 11.0).toFloat()
        val wind = lowPass(brownNoise(), 0.08f) * 1.6f * swell
        var cricket = 0f
        if (chirpLeft > 0) {
            eventPhase += (2 * PI * 4700 / sampleRate).toFloat()
            val pulse = if ((chirpLeft / (sampleRate / 60)) % 2 == 0) 1f else 0f
            cricket = sin(eventPhase) * 0.05f * pulse
            chirpLeft--
            if (chirpLeft == 0) chirpGap = sampleRate / 3 + rnd.nextInt(sampleRate)
        } else if (chirpGap > 0) {
            chirpGap--
        } else {
            chirpLeft = sampleRate / 6
        }
        return wind + cricket
    }

    private fun fire(): Float {
        // Low roar of the flames plus sharp crackles from embers
        val roar = lowPass(brownNoise(), 0.05f) * 1.4f
        if (eventEnv < 0.002f && rnd.nextInt(sampleRate / 6) == 0) {
            eventEnv = 0.3f + rnd.nextFloat() * 0.7f
            eventDecay = 0.985f + rnd.nextFloat() * 0.01f
        }
        val crackle = highPass(white(), 0.6f) * eventEnv * 0.45f
        eventEnv *= eventDecay
        return roar + crackle
    }

    private fun ocean(): Float {
        // Waves: noise whose loudness and brightness follow a slow swell, about one every eight seconds
        val phase = (seconds % 8f) / 8f
        val swell = (exp(-abs(phase - 0.35f) * 6f) * 0.9f + 0.12f)
        val wave = lowPass(pinkNoise(), 0.04f + 0.25f * swell) * 2.1f * swell
        return wave
    }
}

/** A short synthesised paper swish for page turns. */
fun pageTurnSamples(sampleRate: Int): ShortArray {
    val length = sampleRate * 110 / 1000
    val rnd = Random(7)
    var lp = 0f
    return ShortArray(length) { i ->
        val x = i.toFloat() / length
        val env = (x * 9f).coerceAtMost(1f) * (1f - x) * (1f - x)
        lp += (0.15f + 0.6f * x) * ((rnd.nextFloat() * 2f - 1f) - lp)
        (lp * env * 0.35f * Short.MAX_VALUE).toInt().toShort()
    }
}
