package kurotoon.ui.reader

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The spinner shown while a page downloads: an accent arc on a dark glass disc, with the percentage
 * in the middle once the source reports progress. It keeps turning so a slow page never looks stuck.
 */
@Composable
fun ReaderPageLoader(progress: () -> Float, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val spin = rememberInfiniteTransition(label = "pageLoaderSpin")
    val rotation by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "pageLoaderRotation",
    )
    val value = progress()
    val shown by animateFloatAsState(value, label = "pageLoaderProgress")
    Box(
        modifier = modifier
            .size(64.dp)
            .background(Color.Black.copy(alpha = 0.55f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(44.dp).rotate(rotation)) {
            val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            drawArc(Color.White.copy(alpha = 0.12f), 0f, 360f, useCenter = false, style = stroke)
            // Indeterminate: a fixed comet; determinate: the share loaded, never thinner than a dot
            val sweep = if (value <= 0f) 90f else (shown * 360f).coerceAtLeast(12f)
            drawArc(accent, -90f, sweep, useCenter = false, style = stroke)
        }
        if (value > 0f) {
            Text(
                text = "${(value * 100).toInt()}",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
