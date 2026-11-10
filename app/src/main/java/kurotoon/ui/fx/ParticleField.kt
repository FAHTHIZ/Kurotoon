package kurotoon.ui.fx

import android.app.ActivityManager
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.getSystemService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import tachiyomi.presentation.core.util.collectAsState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val AmbientColors = listOf(Color(0xFFA855F7), Color(0xFF7C3AED), Color(0xFFFF4D4D), Color(0xFF60A5FA))
private val BurstColors =
    listOf(Color(0xFFFF4D4D), Color(0xFFF5C451), Color(0xFFA855F7), Color(0xFF60A5FA), Color.White)

/**
 * One full-screen canvas for every particle effect: slow embers drifting up behind the UI and
 * one-shot sparks or confetti fired through [KuroFx]. It never takes touches, stops drawing when
 * idle, honours the system "remove animations" setting and uses fewer particles on low-RAM phones.
 */
@Composable
fun ParticleField(ambient: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val enabled by remember { context.appGraph.kuroPreferences.particles }.collectAsState()
    val reducedMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    if (!enabled || reducedMotion) return

    val density = LocalDensity.current.density
    val lowRam = remember { context.getSystemService<ActivityManager>()?.isLowRamDevice == true }
    val engine = remember(density, lowRam) { ParticleEngine(density, lowRam) }
    val ambientOn by rememberUpdatedState(ambient)
    val wake = remember { Channel<Unit>(Channel.CONFLATED) }
    var frame by remember { mutableLongStateOf(0L) }

    LaunchedEffect(engine) {
        KuroFx.flow.collect { event ->
            when (event) {
                is FxEvent.Burst -> engine.burst(event)
                FxEvent.Celebrate -> launch {
                    engine.celebrate(0)
                    wake.trySend(Unit)
                    delay(140)
                    engine.celebrate(1)
                    wake.trySend(Unit)
                    delay(120)
                    engine.celebrate(2)
                    wake.trySend(Unit)
                }
            }
            wake.trySend(Unit)
        }
    }
    LaunchedEffect(ambient) { wake.trySend(Unit) }
    LaunchedEffect(engine) {
        while (true) {
            var last = 0L
            while (engine.isActive || ambientOn) {
                withFrameNanos { now ->
                    // Steps are tuned for 60 fps; scale them so 90/120 Hz screens move at the same speed
                    val step = if (last == 0L) 1f else ((now - last) / 16_666_667f).coerceIn(0.25f, 3f)
                    last = now
                    engine.update(now / 1_000_000, step, ambientOn)
                    frame = now
                }
            }
            frame = 0L
            wake.receive()
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer()
            .onSizeChanged { engine.size = Size(it.width.toFloat(), it.height.toFloat()) },
    ) {
        if (frame != 0L) engine.draw(this)
    }
}

private class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val max: Float,
    val size: Float,
    val color: Color,
    val gravity: Float,
    val spin: Float,
    var rotation: Float,
    val confetti: Boolean,
    val ambient: Boolean,
) {
    var life = 0f
}

private class ParticleEngine(private val density: Float, lowRam: Boolean) {

    var size = Size.Zero

    private val particles = ArrayList<Particle>(128)
    private val maxAmbient = if (lowRam) 14 else 28
    private val scale = if (lowRam) 0.5f else 1f
    private var lastAmbientMs = 0L

    val isActive: Boolean get() = particles.isNotEmpty()

    fun burst(event: FxEvent.Burst) {
        val n = (event.count * scale).roundToInt()
        val colors = event.colors ?: BurstColors
        repeat(n) {
            val angle = -PI.toFloat() / 2 + (Random.nextFloat() - 0.5f) * event.spread
            val speed = event.power * (0.35f + Random.nextFloat() * 0.75f) * density
            val confetti = event.count > 30 && Random.nextFloat() < 0.55f
            particles += Particle(
                x = event.x,
                y = event.y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                max = 50f + Random.nextFloat() * 60f,
                size = (if (confetti) 3f + Random.nextFloat() * 3f else 1.2f + Random.nextFloat() * 2.2f) * density,
                color = colors.random(),
                gravity = event.gravity * density,
                spin = (Random.nextFloat() - 0.5f) * 0.4f,
                rotation = Random.nextFloat() * PI.toFloat(),
                confetti = confetti,
                ambient = false,
            )
        }
    }

    /** Wave 0 comes from the centre, waves 1 and 2 from the left and right. */
    fun celebrate(wave: Int) {
        if (size == Size.Zero) return
        val x = size.width / 2
        val y = size.height * 0.42f
        burst(
            when (wave) {
                0 -> FxEvent.Burst(x, y, count = 90, power = 9f, gravity = 0.16f)
                1 -> FxEvent.Burst(x * 0.5f, y * 1.1f, count = 40, power = 7f, gravity = 0.18f)
                else -> FxEvent.Burst(x * 1.5f, y * 1.1f, count = 40, power = 7f, gravity = 0.18f)
            },
        )
    }

    private fun spawnAmbient() {
        particles += Particle(
            x = Random.nextFloat() * size.width,
            y = size.height + 10 * density,
            vx = (Random.nextFloat() - 0.5f) * 0.15f * density,
            vy = -(0.25f + Random.nextFloat() * 0.45f) * density,
            max = 600f + Random.nextFloat() * 500f,
            size = (0.8f + Random.nextFloat() * 1.8f) * density,
            color = AmbientColors.random(),
            gravity = 0f,
            spin = 0f,
            rotation = 0f,
            confetti = false,
            ambient = true,
        )
    }

    fun update(nowMs: Long, step: Float, ambientOn: Boolean) {
        if (size == Size.Zero) return
        if (ambientOn && nowMs - lastAmbientMs > 280 && particles.count { it.ambient } < maxAmbient) {
            lastAmbientMs = nowMs
            spawnAmbient()
        }
        val margin = 20 * density
        val drag = Math.pow(0.985, step.toDouble()).toFloat()
        for (i in particles.indices.reversed()) {
            val p = particles[i]
            p.life += step
            p.vy += p.gravity * step
            if (p.ambient) {
                p.vx += sin((p.life + i * 40) / 60f) * 0.004f * density * step
            } else {
                p.vx *= drag
                p.vy *= drag
            }
            p.x += p.vx * step
            p.y += p.vy * step
            p.rotation += p.spin * step
            val k = p.life / p.max
            val gone = k >= 1f ||
                p.y < -margin ||
                (!p.ambient && p.y > size.height + margin) ||
                (p.ambient && !ambientOn && k > 0.1f)
            if (gone) particles.removeAt(i)
        }
    }

    fun draw(scope: DrawScope) = with(scope) {
        for (p in particles) {
            val k = (p.life / p.max).coerceIn(0f, 1f)
            val alpha = if (p.ambient) sin(PI.toFloat() * k) * 0.35f else 1f - k * k
            if (alpha <= 0f) continue
            val center = Offset(p.x, p.y)
            if (p.confetti) {
                rotateRad(p.rotation, pivot = center) {
                    drawRect(
                        color = p.color,
                        topLeft = Offset(p.x - p.size / 2, p.y - p.size / 4),
                        size = Size(p.size, p.size / 2),
                        alpha = alpha,
                    )
                }
            } else {
                val radius = p.size * if (p.ambient) 3f else 2.4f
                drawCircle(
                    brush = Brush.radialGradient(listOf(p.color, Color.Transparent), center, radius),
                    radius = radius,
                    center = center,
                    alpha = alpha,
                    blendMode = BlendMode.Plus,
                )
            }
        }
    }
}
