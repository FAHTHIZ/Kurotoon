package kurotoon.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toJavaDayOfWeek
import kurotoon.schedule.ScheduledSeries
import kurotoon.schedule.WeeklySchedule
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import java.time.format.TextStyle
import java.util.Locale

/** Library series laid out by the weekday they usually update on, like a webtoon daily schedule. */
class ScheduleScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = metroViewModel<ScheduleViewModel>()
        val schedule by viewModel.schedule.collectAsState()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.kurotoon_schedule_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            val current = schedule
            when {
                current == null -> LoadingScreen(Modifier.padding(padding))
                current.byDay.values.all { it.isEmpty() } -> EmptyScreen(
                    message = stringResource(MR.strings.kurotoon_schedule_empty),
                    modifier = Modifier.padding(padding),
                )
                else -> WeeklyScheduleContent(
                    schedule = current,
                    onClick = { navigator.push(MangaScreen(it)) },
                    contentPadding = padding,
                )
            }
        }
    }
}

/** Day tabs, Monday first, opening on today. */
@Composable
internal fun WeeklyScheduleContent(
    schedule: WeeklySchedule,
    onClick: (Long) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val days = DayOfWeek.entries
    val pagerState = rememberPagerState(initialPage = schedule.today.dayOfWeek.isoDayNumber - 1) { days.size }
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.padding(top = contentPadding.calculateTopPadding())) {
        PrimaryScrollableTabRow(selectedTabIndex = pagerState.currentPage, edgePadding = 8.dp) {
            days.forEachIndexed { index, day ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                    text = {
                        DayTabLabel(day, isToday = day == schedule.today.dayOfWeek, count = schedule.on(day).size)
                    },
                )
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val day = days[page]
            DayGrid(
                day = day,
                series = schedule.on(day),
                today = schedule.today,
                unscheduled = schedule.unscheduled,
                onClick = onClick,
                bottomPadding = contentPadding.calculateBottomPadding(),
            )
        }
    }
}

@Composable
private fun DayTabLabel(day: DayOfWeek, isToday: Boolean, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isToday) {
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(6.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
        Text(
            text = day.shortName(),
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
        )
        if (count > 0) {
            Text(
                text = " $count",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DayGrid(
    day: DayOfWeek,
    series: List<ScheduledSeries>,
    today: LocalDate,
    unscheduled: Int,
    onClick: (Long) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(104.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPadding + 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
            Text(
                text = if (series.isEmpty()) {
                    stringResource(MR.strings.kurotoon_schedule_none, day.longName())
                } else {
                    pluralStringResource(MR.plurals.kurotoon_schedule_count, series.size, series.size, day.longName())
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(series, key = { it.item.id }) { entry ->
            ScheduleCover(entry, today, onClick)
        }
        if (unscheduled > 0) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "footer") {
                Text(
                    text = pluralStringResource(MR.plurals.kurotoon_schedule_unscheduled, unscheduled, unscheduled),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
internal fun ScheduleCover(entry: ScheduledSeries, today: LocalDate, onClick: (Long) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box {
            MangaCover.Book(
                data = entry.item.manga.asMangaCover(),
                shape = RoundedCornerShape(12.dp),
                contentDescription = entry.item.manga.title,
                onClick = { onClick(entry.item.id) },
            )
            if (entry.item.unreadCount > 0) {
                Text(
                    text = entry.item.unreadCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
        }
        Text(
            text = entry.item.manga.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        val since = entry.daysSinceLatest(today)
        Text(
            text = when (since) {
                null -> ""
                0 -> stringResource(MR.strings.kurotoon_schedule_newToday)
                else -> pluralStringResource(MR.plurals.kurotoon_schedule_daysAgo, since, since)
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (since == 0) FontWeight.Bold else FontWeight.Normal,
            color = if (since == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

internal fun DayOfWeek.shortName(): String = toJavaDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault())

internal fun DayOfWeek.longName(): String = toJavaDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault())
