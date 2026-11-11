package kurotoon.reader

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A small floating ring that fills as the chapter goes on and turns gold at the end, with a core
 * that breathes softly. Tapping it brings up the reader menu.
 */
@Composable
fun ReaderProgressOrb(
    currentPage: Int,
    totalPages: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (totalPages <= 0) return
    val target = (currentPage.toFloat() / totalPages).coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow),
        label = "orbProgress",
    )
    val complete = currentPage >= totalPages
    val accent by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.run { if (complete) tertiary else primary },
        animationSpec = tween(400),
        label = "orbAccent",
    )
    // Read only while drawing, so the breathing never recomposes the reader
    val breatheState = rememberInfiniteTransition(label = "orbBreathe").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "orbBreatheValue",
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(dampingRatio = 0.5f), label = "orbPress")

    Box(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = "$currentPage / $totalPages" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val breathe = breatheState.value
            val stroke = 3.dp.toPx()
            // Room for the wider glow stroke so it never touches the clip
            val inset = stroke * 1.3f + 2.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            // Soft core
            drawCircle(
                Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.22f + breathe * 0.16f), Color.Transparent),
                    radius = size.minDimension * (0.34f + breathe * 0.04f),
                ),
                radius = size.minDimension * (0.34f + breathe * 0.04f),
            )
            drawArc(Color.White.copy(alpha = 0.14f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            // Glow under the ring, then the ring itself
            drawArc(
                accent.copy(alpha = 0.35f),
                -90f,
                360f * progress,
                false,
                topLeft,
                arcSize,
                style = Stroke(stroke * 2.6f, cap = StrokeCap.Round),
            )
            drawArc(
                accent,
                -90f,
                360f * progress,
                false,
                topLeft,
                arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            text = "${(target * 100).toInt()}",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
