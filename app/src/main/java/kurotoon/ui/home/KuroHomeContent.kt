package kurotoon.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.util.formatChapterNumber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kurotoon.icons.materialsymbols.MaterialSymbols
import kurotoon.icons.materialsymbols.rounded.Download
import kurotoon.icons.materialsymbols.rounded.Extension
import kurotoon.icons.materialsymbols.rounded.History
import kurotoon.icons.materialsymbols.rounded.QueryStats
import kurotoon.ui.KuroSnackbarHost
import kurotoon.ui.KuroTip
import kurotoon.ui.KuroTopBar
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Duration.Companion.seconds

@Composable
fun KuroHomeContent(
    state: KuroHomeViewModel.State,
    snackbarHostState: SnackbarHostState,
    onSearchClick: () -> Unit,
    onAddSourcesClick: () -> Unit,
    onMangaClick: (Long) -> Unit,
    onBrowseMangaClick: (Manga) -> Unit,
    onRowClick: (KuroHomeViewModel.SourceRow) -> Unit,
    onRetryRow: (KuroHomeViewModel.SourceRow) -> Unit,
    onToggleLatest: (KuroHomeViewModel.SourceRow) -> Unit = {},
    onMangaLongClick: ((Manga) -> Unit)? = null,
    onUpdatesClick: () -> Unit = {},
    onScheduleClick: () -> Unit = {},
    onResumeClick: (HistoryWithRelations) -> Unit,
    onExtensionsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onStatsClick: () -> Unit,
    onSavedPanelsClick: () -> Unit = {},
    /** Keys of [HomeSection]s switched off. */
    hidden: Set<String> = emptySet(),
    onRefresh: () -> Unit = {},
) {
    fun shown(section: HomeSection) = section.key !in hidden
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { KuroTopBar(onSearchClick = onSearchClick) },
        snackbarHost = { KuroSnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        PullRefresh(
            refreshing = refreshing,
            onRefresh = {
                onRefresh()
                // Rows show their own loading state, so the spinner only acknowledges the pull
                scope.launch {
                    refreshing = true
                    delay(1.seconds)
                    refreshing = false
                }
            },
            enabled = state.hasSources,
            indicatorPadding = contentPadding,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding() + 4.dp,
                    bottom = contentPadding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (!state.isLoading && !state.hasSources) {
                    item(key = "welcome") { Welcome(onAddSourcesClick) }
                }
                if (shown(HomeSection.CONTINUE) && state.continueReading.isNotEmpty()) {
                    val upNext = state.continueReading.first()
                    item(key = "up-next") {
                        HomeUpNext(
                            item = upNext,
                            page = state.continuePages[upNext.chapterId],
                            onOpen = { onMangaClick(upNext.mangaId) },
                            onResume = { onResumeClick(upNext) },
                        )
                    }
                }
                if (shown(HomeSection.CONTINUE) && state.continueReading.size > 1) {
                    item(key = "continue") {
                        ContinueReading(
                            items = state.continueReading.drop(1),
                            pages = state.continuePages,
                            onMangaClick = onMangaClick,
                            onResumeClick = onResumeClick,
                            onSeeAll = onHistoryClick,
                        )
                    }
                }
                if (shown(HomeSection.NEW_CHAPTERS) && state.newChapters.isNotEmpty()) {
                    item(key = "new-chapters") {
                        HomeNewChapters(
                            items = state.newChapters,
                            onMangaClick = onMangaClick,
                            onSeeAll = onUpdatesClick,
                        )
                    }
                }
                if (shown(HomeSection.SCHEDULE) && state.schedule.dueToday.isNotEmpty()) {
                    item(key = "schedule") {
                        HomeSchedule(
                            items = state.schedule.dueToday,
                            today = state.schedule.today,
                            onMangaClick = onMangaClick,
                            onSeeAll = onScheduleClick,
                        )
                    }
                }
                if (shown(HomeSection.HERO) && state.heroes.isNotEmpty()) {
                    item(key = "hero") {
                        HomeHeroCarousel(heroes = state.heroes, onClick = onBrowseMangaClick)
                    }
                }
                if (shown(HomeSection.SHORTCUTS)) {
                    item(key = "shortcuts") {
                        Shortcuts(
                            onExtensionsClick = onExtensionsClick,
                            onHistoryClick = onHistoryClick,
                            onDownloadsClick = onDownloadsClick,
                            onStatsClick = onStatsClick,
                        )
                    }
                }
                if (shown(HomeSection.STALLED) && state.stalled.isNotEmpty()) {
                    item(key = "stalled") { HomeStalled(items = state.stalled, onMangaClick = onMangaClick) }
                }
                if (shown(HomeSection.BINGE) && state.binge.isNotEmpty()) {
                    item(key = "binge") { HomeBinge(items = state.binge, onMangaClick = onMangaClick) }
                }
                if (shown(HomeSection.RECENTLY_ADDED) && state.recentlyAdded.isNotEmpty()) {
                    item(key = "recently_added") {
                        HomeRecentlyAdded(items = state.recentlyAdded, onMangaClick = onMangaClick)
                    }
                }
                if (shown(HomeSection.PANELS)) {
                    item(key = "panels") { HomePanels(onSeeAll = onSavedPanelsClick) }
                }
                val rows = state.savedRows.takeIf { shown(HomeSection.SAVED) }.orEmpty() +
                    state.pickRows.takeIf { shown(HomeSection.PICKS) }.orEmpty() +
                    state.rows.takeIf { shown(HomeSection.SOURCES) }.orEmpty()
                items(rows, key = { it.key }) { row ->
                    HomeSourceRow(
                        row = row,
                        onSeeAll = { onRowClick(row) },
                        onMangaClick = onBrowseMangaClick,
                        onRetry = { onRetryRow(row) },
                        onToggleLatest = { onToggleLatest(row) },
                        onMangaLongClick = onMangaLongClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun Welcome(onAddSourcesClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        kurotoon.ui.KuroLogo(size = 56.dp)
        Text(
            text = stringResource(MR.strings.kurotoon_home_welcomeTitle),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = stringResource(MR.strings.kurotoon_home_welcomeBody),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        Box(
            modifier = Modifier
                .padding(top = 20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onAddSourcesClick)
                .padding(horizontal = 24.dp, vertical = 10.dp),
        ) {
            Text(
                text = stringResource(MR.strings.kurotoon_home_addSources),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun Shortcuts(
    onExtensionsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onStatsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Shortcut(MaterialSymbols.Rounded.History, stringResource(MR.strings.label_recent_manga), onHistoryClick)
        Shortcut(MaterialSymbols.Rounded.Download, stringResource(MR.strings.kurotoon_home_downloads), onDownloadsClick)
        Shortcut(MaterialSymbols.Rounded.Extension, stringResource(MR.strings.label_extensions), onExtensionsClick)
        Shortcut(MaterialSymbols.Rounded.QueryStats, stringResource(MR.strings.label_stats), onStatsClick)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Shortcut(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp),
        )
    }
}

@Composable
private fun ContinueReading(
    items: List<HistoryWithRelations>,
    pages: Map<Long, Int>,
    onMangaClick: (Long) -> Unit,
    onResumeClick: (HistoryWithRelations) -> Unit,
    onSeeAll: () -> Unit,
) {
    Column {
        HomeShelfHeader(
            title = stringResource(MR.strings.kurotoon_home_continueReading),
            icon = MaterialSymbols.Rounded.History,
            onSeeAll = onSeeAll,
        )
        if (items.isEmpty()) {
            KuroTip(
                text = stringResource(MR.strings.kurotoon_home_continueEmpty),
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
            )
            return
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.mangaId }) { item ->
                Column(modifier = Modifier.width(HomeCoverWidth)) {
                    Box {
                        MangaCover.Book(
                            data = item.coverData,
                            shape = RoundedCornerShape(12.dp),
                            contentDescription = item.title,
                            onClick = { onMangaClick(item.mangaId) },
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { onResumeClick(item) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = stringResource(MR.strings.action_resume),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    if (item.chapterNumber > -1) {
                        val chapter = stringResource(
                            MR.strings.display_mode_chapter,
                            formatChapterNumber(item.chapterNumber),
                        )
                        val page = pages[item.chapterId]
                        Text(
                            text = if (page != null) {
                                stringResource(MR.strings.kurotoon_home_chapterPage, chapter, page)
                            } else {
                                chapter
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
