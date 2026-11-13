package kurotoon.ui.manga

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kurotoon.icons.materialsymbols.MaterialSymbols
import kurotoon.icons.materialsymbols.rounded.Check

/**
 * The square at the start of an episode row, like the thumbnail column of an episode list in WEBTOON. Sources
 * rarely give per-chapter art, so the tile carries the episode number instead: bold on a tinted square while
 * unread, ringed with the accent while you're part-way in, and a quiet check once it's read.
 */
@Composable
fun EpisodeTile(
    number: String?,
    read: Boolean,
    inProgress: Boolean,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val shape = RoundedCornerShape(12.dp)
    val primary = MaterialTheme.colorScheme.primary
    val background = when {
        selected -> primary
        read -> MaterialTheme.colorScheme.surfaceContainer
        inProgress -> primary.copy(alpha = 0.14f)
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(shape)
            .background(background)
            .then(if (inProgress && !read && !selected) Modifier.border(1.5.dp, primary, shape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        when {
            selected -> Icon(
                imageVector = MaterialSymbols.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp),
            )
            read -> Icon(
                imageVector = MaterialSymbols.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp),
            )
            number != null -> {
                val size = when {
                    number.length <= 2 -> 18.sp
                    number.length <= 4 -> 15.sp
                    else -> 12.sp
                }
                Text(
                    text = number,
                    color = if (inProgress) primary else MaterialTheme.colorScheme.onSurface,
                    fontSize = size,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    letterSpacing = (-0.5).sp,
                )
            }
            else -> Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (inProgress) primary else MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}
