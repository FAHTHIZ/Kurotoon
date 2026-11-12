package eu.kanade.presentation.reader

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The page counter shown while the menu is hidden: a small dark glass capsule with the page in the
 * accent colour, optional time left, clock and battery, and a hairline progress bar along its foot.
 * Readable over white and black pages alike without the old outlined-text look.
 */
@Composable
fun ReaderPageIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier,
    minutesLeft: Int? = null,
    clock: String? = null,
    batteryPercent: Int? = null,
    charging: Boolean = false,
) {
    if (currentPage <= 0 || totalPages <= 0) return

    val timeLeft = minutesLeft?.let { stringResource(MR.strings.kurotoon_reader_minutesLeft, it) }
    val battery = batteryPercent?.let { if (charging) "$it%+" else "$it%" }
    val extras = listOfNotNull(timeLeft, clock, battery)
    val progress by animateFloatAsState(
        targetValue = (currentPage.toFloat() / totalPages).coerceIn(0f, 1f),
        label = "pageIndicatorProgress",
    )
    val accent = MaterialTheme.colorScheme.primary

    Box(modifier = modifier.padding(bottom = 6.dp)) {
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.58f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = currentPage.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent,
                )
                Text(
                    text = "/ $totalPages",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f),
                )
                extras.forEach {
                    Box(
                        Modifier
                            .padding(horizontal = 1.dp)
                            .width(3.dp)
                            .height(3.dp)
                            .background(Color.White.copy(alpha = 0.4f), CircleShape),
                    )
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 4.dp)
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(2.dp)
                        .background(accent, CircleShape),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ReaderPageIndicatorPreview() {
    TachiyomiPreviewTheme {
        Surface {
            ReaderPageIndicator(currentPage = 10, totalPages = 69, minutesLeft = 4, clock = "21:40")
        }
    }
}
