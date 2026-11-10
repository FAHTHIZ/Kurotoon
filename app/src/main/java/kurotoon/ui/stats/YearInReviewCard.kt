package kurotoon.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.util.toDurationString
import kurotoon.stats.YearInReview
import kurotoon.ui.KuroColors
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.time.format.TextStyle
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

/** "2026 in reading": time, chapters, the most-read series, favourite genres and the busiest month. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YearInReviewCard(review: YearInReview, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(28.dp)
    val time = remember(review.millis) {
        (review.millis / 60_000 * 60_000).milliseconds.toDurationString(context, fallback = "–")
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(KuroColors.CrimsonDeep.copy(alpha = 0.55f), Color(0xFF16090C), Color(0xFF0E0E10)),
                ),
            )
            .border(1.dp, KuroColors.Crimson.copy(alpha = 0.35f), shape)
            .padding(20.dp),
    ) {
        Column {
            Text(
                text = stringResource(MR.strings.kurotoon_year_title, review.year),
                style = MaterialTheme.typography.labelLarge,
                color = KuroColors.Crimson,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = time,
                fontSize = 40.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
            Text(
                text = stringResource(MR.strings.kurotoon_year_summary, review.chapters, review.series),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
            )
        }

        if (review.topSeries.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label(stringResource(MR.strings.kurotoon_year_mostRead))
                review.topSeries.forEachIndexed { index, series ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = if (index == 0) KuroColors.Gold else Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.width(28.dp),
                        )
                        Text(
                            text = series.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(MR.strings.kurotoon_year_chapters, series.chapters),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }

        if (review.topGenres.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label(stringResource(MR.strings.kurotoon_year_genres))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    review.topGenres.forEach { (genre, _) ->
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier
                                .background(KuroColors.Crimson.copy(alpha = 0.22f), CircleShape)
                                .border(1.dp, KuroColors.Crimson.copy(alpha = 0.5f), CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        review.busiestMonth?.let { month ->
            val name = month.getDisplayName(TextStyle.FULL, Locale.getDefault())
            Text(
                text = stringResource(MR.strings.kurotoon_year_busiest, name, review.busiestMonthChapters),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 1.5.sp,
        color = Color.White.copy(alpha = 0.5f),
        fontWeight = FontWeight.Bold,
    )
}
