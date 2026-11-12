package tachiyomi.presentation.core.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** KuroToon's progress line: a rounded accent fill on a faint rounded track, no stop dot. */
@Composable
fun KuroProgressBar(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = color.copy(alpha = 0.16f),
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress().coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(color, CircleShape),
        )
    }
}

/** The progress line for work with no known end: an accent segment that glides back and forth. */
@Composable
fun KuroIndeterminateBar(
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = color.copy(alpha = 0.16f),
) {
    val transition = rememberInfiniteTransition(label = "kuroIndeterminateBar")
    val position by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "kuroIndeterminatePosition",
    )
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor),
    ) {
        val segment = maxWidth * 0.32f
        Box(
            modifier = Modifier
                .offset(x = (maxWidth - segment) * position)
                .width(segment)
                .fillMaxHeight()
                .background(color, CircleShape),
        )
    }
}
