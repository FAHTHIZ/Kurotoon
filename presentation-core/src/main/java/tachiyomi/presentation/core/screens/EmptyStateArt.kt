package tachiyomi.presentation.core.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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

/**
 * KuroToon's empty-state illustration: Kuro the cat dozing on a closed book, with drifting "z"s.
 * Drawn in code so it stays crisp, follows the active colour scheme and costs no assets.
 */
@Composable
fun EmptyStateArt(
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surface = MaterialTheme.colorScheme.surfaceContainerHighest
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant
    val transition = rememberInfiniteTransition(label = "emptyArt")
    val breathe = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "emptyArtBreathe",
    )
    val drift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing)),
        label = "emptyArtDrift",
    )
    Canvas(modifier.size(size)) {
        val unit = this.size.minDimension / 120f
        scale(unit, pivot = Offset.Zero) {
            drawSleepingKuro(breathe.value, drift.value, primary, tertiary, surface, onSurface)
        }
    }
}

private fun DrawScope.drawSleepingKuro(
    breathe: Float,
    drift: Float,
    primary: Color,
    tertiary: Color,
    surface: Color,
    ink: Color,
) {
    // Soft halo
    drawCircle(
        Brush.radialGradient(
            listOf(primary.copy(alpha = 0.28f), tertiary.copy(alpha = 0.10f), Color.Transparent),
            center = Offset(60f, 64f),
            radius = 58f,
        ),
        radius = 58f,
        center = Offset(60f, 64f),
    )

    // Closed book the cat is resting on
    val book = Path().apply {
        moveTo(22f, 92f)
        lineTo(98f, 92f)
        lineTo(102f, 104f)
        lineTo(18f, 104f)
        close()
    }
    drawPath(book, primary.copy(alpha = 0.85f))
    drawLine(surface, Offset(24f, 98f), Offset(96f, 98f), strokeWidth = 2f, cap = StrokeCap.Round)

    // Body, rising gently as it breathes
    val rise = breathe * 1.6f
    translate(top = -rise) {
        val body = Brush.verticalGradient(listOf(Color(0xFF3A2F4D), Color(0xFF15111C)), startY = 40f, endY = 94f)
        // Curled body
        drawOval(body, topLeft = Offset(30f, 62f), size = androidx.compose.ui.geometry.Size(62f, 32f))
        // Tail wrapping round the front
        drawPath(
            Path().apply {
                moveTo(88f, 84f)
                quadraticTo(104f, 80f, 96f, 90f)
                quadraticTo(76f, 98f, 52f, 92f)
            },
            Color(0xFF241C30),
            style = Stroke(6f, cap = StrokeCap.Round),
        )
        // Head with ears
        drawPath(
            Path().apply {
                moveTo(30f, 60f)
                lineTo(34f, 42f)
                lineTo(44f, 54f)
                close()
            },
            body,
        )
        drawPath(
            Path().apply {
                moveTo(62f, 60f)
                lineTo(58f, 42f)
                lineTo(48f, 54f)
                close()
            },
            body,
        )
        drawCircle(body, radius = 19f, center = Offset(46f, 66f))
        // Closed eyes
        val eye = Stroke(2.4f, cap = StrokeCap.Round)
        listOf(37f, 50f).forEach { x ->
            drawPath(
                Path().apply {
                    moveTo(x, 66f)
                    quadraticTo(x + 4f, 69f, x + 8f, 66f)
                },
                tertiary,
                style = eye,
            )
        }
        drawCircle(primary, radius = 1.6f, center = Offset(46f, 73f))
    }

    // Drifting z's
    listOf(0f, 0.33f, 0.66f).forEachIndexed { i, offset ->
        val t = (drift + offset) % 1f
        val alpha = (1f - t) * 0.9f
        val cx = 70f + t * 26f + i * 2f
        val cy = 54f - t * 34f
        val s = 4f + i * 1.6f + t * 2f
        drawPath(
            Path().apply {
                moveTo(cx - s, cy - s)
                lineTo(cx + s, cy - s)
                lineTo(cx - s, cy + s)
                lineTo(cx + s, cy + s)
            },
            ink.copy(alpha = alpha),
            style = Stroke(1.8f, cap = StrokeCap.Round),
        )
    }
}
