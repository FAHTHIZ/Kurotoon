package kurotoon.ui.shop

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kurotoon.ui.KuroColors

enum class KuroMood { IDLE, HAPPY, READING, SLEEPY, HYPED }

/**
 * Kuro, the KuroToon cat, drawn in code so it's crisp at any size and can be recoloured by packs.
 * [colors] are optional: body light, body dark, eyes, then aura.
 */
@Composable
fun KuroMascot(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    mood: KuroMood = KuroMood.IDLE,
    colors: List<Color> = emptyList(),
    animated: Boolean = true,
) {
    val bob: State<Float> = if (animated) {
        rememberInfiniteTransition(label = "kuroBob").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(if (mood == KuroMood.SLEEPY) 2000 else 1300), RepeatMode.Reverse),
            label = "kuroBobValue",
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0.5f) }
    }
    val bodyLight = colors.getOrNull(0) ?: Color(0xFF3A2F4D)
    val bodyDark = colors.getOrNull(1) ?: Color(0xFF15111C)
    val eye = colors.getOrNull(2) ?: KuroColors.Aurora
    val aura = colors.getOrNull(3)
    Canvas(modifier.size(size)) {
        // The artwork is laid out on a 112 unit square, like the web version
        val unit = this.size.minDimension / 112f
        scale(unit, pivot = Offset.Zero) {
            drawKuro(bob.value, mood, bodyLight, bodyDark, eye, aura)
        }
    }
}

private fun DrawScope.drawKuro(
    bob: Float,
    mood: KuroMood,
    bodyLight: Color,
    bodyDark: Color,
    eye: Color,
    aura: Color?,
) {
    val auraBrush = Brush.linearGradient(
        aura?.let { listOf(it, it.copy(alpha = 0.6f)) }
            ?: listOf(KuroColors.Crimson, KuroColors.Galaxy, KuroColors.Aurora),
        start = Offset(0f, 0f),
        end = Offset(112f, 112f),
    )
    val breathe = if (mood == KuroMood.HYPED) 1f + bob * 0.08f else 1f + bob * 0.03f
    scale(breathe, pivot = Offset(56f, 58f)) {
        drawCircle(auraBrush, radius = 46f, center = Offset(56f, 58f), alpha = 0.22f)
    }

    val lift = if (mood == KuroMood.SLEEPY) bob * 1.5f else -bob * 3f
    translate(top = lift) {
        val body = Brush.radialGradient(
            listOf(bodyLight, lerpColor(bodyLight, bodyDark, 0.6f), bodyDark),
            center = Offset(56f * 0.7f, 56f * 0.56f),
            radius = 60f,
        )
        drawPath(
            Path().apply {
                moveTo(24f, 30f)
                lineTo(34f, 8f)
                lineTo(46f, 26f)
                close()
            },
            body,
        )
        drawPath(
            Path().apply {
                moveTo(88f, 30f)
                lineTo(78f, 8f)
                lineTo(66f, 26f)
                close()
            },
            body,
        )
        drawCircle(body, radius = 38f, center = Offset(56f, 56f))
        drawCircle(auraBrush, radius = 38f, center = Offset(56f, 56f), alpha = 0.7f, style = Stroke(1.5f))

        val eyeStroke = Stroke(4f, cap = StrokeCap.Round)
        listOf(34f, 66f).forEach { x ->
            val path = Path()
            when (mood) {
                KuroMood.READING -> {
                    path.moveTo(x, 47f)
                    path.lineTo(x + 12f, 47f)
                }
                KuroMood.SLEEPY -> {
                    path.moveTo(x, 49f)
                    path.quadraticTo(x + 6f, 53f, x + 12f, 49f)
                }
                else -> {
                    val (y, lift2) = when (mood) {
                        KuroMood.HAPPY -> 48f to -10f
                        KuroMood.HYPED -> 44f to -12f
                        else -> 46f to -7f
                    }
                    path.moveTo(x, y)
                    path.quadraticTo(x + 6f, y + lift2, x + 12f, y)
                }
            }
            drawPath(path, eye, style = eyeStroke)
        }
        val mouth = Path()
        if (mood == KuroMood.HAPPY || mood == KuroMood.HYPED) {
            mouth.moveTo(48f, 68f)
            mouth.quadraticTo(56f, 76f, 64f, 68f)
        } else {
            mouth.moveTo(50f, 68f)
            mouth.quadraticTo(56f, 71f, 62f, 68f)
        }
        drawPath(mouth, KuroColors.Crimson, style = Stroke(3f, cap = StrokeCap.Round))
        drawCircle(KuroColors.Gold, radius = 4f, center = Offset(56f, 30f), alpha = 0.9f)
    }
}

private fun lerpColor(a: Color, b: Color, t: Float): Color = androidx.compose.ui.graphics.lerp(a, b, t)
