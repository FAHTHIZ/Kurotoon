package kurotoon.ui.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kurotoon.ui.KuroColors

/**
 * A thin bar along the bottom edge of a cover showing how much of the series has been read. It sits
 * inside the cover's rounded corners and grows smoothly when the read count changes.
 */
@Composable
fun CoverReadProgress(progress: Float, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(durationMillis = 600), label = "readProgress")
    Box(modifier = modifier.clip(MaterialTheme.shapes.extraSmall)) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(4.dp)
                .background(Color.Black.copy(alpha = 0.55f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .background(
                        Brush.horizontalGradient(
                            listOf(KuroColors.Crimson, lerp(KuroColors.Crimson, Color.White, 0.35f)),
                        ),
                    ),
            )
        }
    }
}

/** Read fraction for a library entry, or null when nothing has been read yet. */
fun readFraction(readCount: Long, totalChapters: Long): Float? =
    if (totalChapters <= 0 || readCount <= 0) null else readCount.toFloat() / totalChapters
