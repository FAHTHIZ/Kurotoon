package kurotoon.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kurotoon.stats.ReadingActivity
import kurotoon.stats.ReadingDay
import kurotoon.ui.KuroColors
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A heatmap of the last few months of reading, one square per day, Monday at the top. Columns fade
 * in left to right the first time the card shows. Below it: chapters, active days and streaks.
 */
@Composable
fun ReadingActivityCard(activity: ReadingActivity, modifier: Modifier = Modifier) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(durationMillis = 700)) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = activity.weeks.size.coerceAtLeast(1)
            val gap = 3.dp
            val cell = (maxWidth - gap * (columns - 1)) / columns
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                activity.weeks.forEachIndexed { index, week ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier.graphicsLayer {
                            // Each column fades in over a three-column stretch of the sweep, the last one ending at 1
                            alpha = ((reveal.value * (columns + 2) - index) / 3f).coerceIn(0f, 1f)
                        },
                    ) {
                        week.forEach { day -> DayCell(day, activity.busiest, cell) }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(MR.strings.kurotoon_stats_weeks, activity.weeks.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Legend()
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Figure(activity.totalChapters, stringResource(MR.strings.kurotoon_stats_chapters), Modifier.weight(1f))
            Figure(activity.activeDays, stringResource(MR.strings.kurotoon_stats_activeDays), Modifier.weight(1f))
            Figure(activity.currentStreak, stringResource(MR.strings.kurotoon_stats_streak), Modifier.weight(1f))
            Figure(activity.longestStreak, stringResource(MR.strings.kurotoon_stats_bestStreak), Modifier.weight(1f))
        }
    }
}

@Composable
private fun DayCell(day: ReadingDay?, busiest: Int, size: androidx.compose.ui.unit.Dp) {
    val color = when {
        day == null -> Color.Transparent
        day.chapters == 0 -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> KuroColors.Crimson.copy(alpha = levelAlpha(day.chapters, busiest))
    }
    Spacer(
        modifier = Modifier
            .size(size)
            .background(color, RoundedCornerShape(3.dp)),
    )
}

@Composable
private fun Legend() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = stringResource(MR.strings.kurotoon_stats_less),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(2.dp))
        LegendSquare(MaterialTheme.colorScheme.surfaceContainerHighest)
        LEVELS.forEach { alpha -> LegendSquare(KuroColors.Crimson.copy(alpha = alpha)) }
        Spacer(Modifier.width(2.dp))
        Text(
            text = stringResource(MR.strings.kurotoon_stats_more),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LegendSquare(color: Color) {
    Spacer(Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
}

@Composable
private fun Figure(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
    }
}

private val LEVELS = listOf(0.3f, 0.55f, 0.8f, 1f)

private fun levelAlpha(chapters: Int, busiest: Int): Float {
    if (busiest <= 1) return LEVELS.last()
    val step = ((chapters - 1) * LEVELS.size / busiest).coerceIn(0, LEVELS.lastIndex)
    return LEVELS[step]
}
