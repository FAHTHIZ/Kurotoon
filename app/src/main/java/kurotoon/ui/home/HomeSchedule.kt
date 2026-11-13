package kurotoon.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kurotoon.icons.materialsymbols.MaterialSymbols
import kurotoon.icons.materialsymbols.rounded.Schedule
import kurotoon.schedule.ScheduledSeries
import kurotoon.ui.schedule.ScheduleCover
import kurotoon.ui.schedule.longName
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** Library series that usually update today, with a link to the full week. Draws nothing when empty. */
@Composable
internal fun HomeSchedule(
    items: List<ScheduledSeries>,
    today: LocalDate,
    onMangaClick: (Long) -> Unit,
    onSeeAll: () -> Unit,
) {
    if (items.isEmpty()) return
    Column {
        HomeShelfHeader(
            title = stringResource(MR.strings.kurotoon_schedule_homeTitle),
            icon = MaterialSymbols.Rounded.Schedule,
            subtitle = stringResource(MR.strings.kurotoon_schedule_homeSummary, today.dayOfWeek.longName()),
            onSeeAll = onSeeAll,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.item.id }) { entry ->
                Column(modifier = Modifier.width(HomeCoverWidth)) {
                    ScheduleCover(entry = entry, today = today, onClick = onMangaClick)
                }
            }
        }
    }
}
