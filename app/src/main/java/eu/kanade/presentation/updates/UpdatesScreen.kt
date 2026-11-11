package eu.kanade.presentation.updates

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.presentation.manga.components.MangaBottomActionMenu
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import eu.kanade.tachiyomi.ui.updates.UpdatesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CalendarMonth
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.rounded.FlipToBack
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.icons.materialsymbols.rounded.Schedule
import mihon.icons.materialsymbols.rounded.SelectAll
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.theme.active
import tachiyomi.presentation.core.util.collectAsState
import kotlin.time.Duration.Companion.seconds

@Composable
fun UpdateScreen(
    state: UpdatesViewModel.State,
    snackbarHostState: SnackbarHostState,
    lastUpdated: Long,
    onClickCover: (UpdatesItem) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    onCalendarClicked: () -> Unit,
    onScheduleClicked: () -> Unit = {},
    onUpdateLibrary: suspend () -> Boolean,
    onDownloadChapter: (List<UpdatesItem>, ChapterDownloadAction) -> Unit,
    onMultiBookmarkClicked: (List<UpdatesItem>, bookmark: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<UpdatesItem>, read: Boolean) -> Unit,
    onMultiDeleteClicked: (List<UpdatesItem>) -> Unit,
    onUpdateSelected: (UpdatesItem, Boolean, Boolean) -> Unit,
    onOpenChapter: (UpdatesItem) -> Unit,
    onFilterClicked: () -> Unit,
    hasActiveFilters: Boolean,
    onClearUpdates: () -> Unit = {},
    onRestoreCleared: (() -> Unit)? = null,
) {
    BackHandler(enabled = state.selectionMode) {
        onSelectAll(false)
    }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { scrollBehavior ->
            UpdatesAppBar(
                onCalendarClicked = { onCalendarClicked() },
                onScheduleClicked = onScheduleClicked,
                onUpdateLibrary = { scope.launch { onUpdateLibrary() } },
                onFilterClicked = { onFilterClicked() },
                hasFilters = hasActiveFilters,
                onClearUpdates = onClearUpdates.takeIf { state.items.isNotEmpty() },
                onRestoreCleared = onRestoreCleared,
                actionModeCounter = state.selected.size,
                onSelectAll = { onSelectAll(true) },
                onInvertSelection = { onInvertSelection() },
                onCancelActionMode = { onSelectAll(false) },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            UpdatesBottomBar(
                selected = state.selected,
                onDownloadChapter = onDownloadChapter,
                onMultiBookmarkClicked = onMultiBookmarkClicked,
                onMultiMarkAsReadClicked = onMultiMarkAsReadClicked,
                onMultiDeleteClicked = onMultiDeleteClicked,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        when {
            state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
            else -> {
                var isRefreshing by remember { mutableStateOf(false) }

                PullRefresh(
                    refreshing = isRefreshing,
                    onRefresh = {
                        scope.launch {
                            if (!onUpdateLibrary()) return@launch
                            // Fake refresh status but hide it after a second as it's a long running task
                            isRefreshing = true
                            delay(1.seconds)
                            isRefreshing = false
                        }
                    },
                    enabled = !state.selectionMode,
                    indicatorPadding = contentPadding,
                ) {
                    if (state.items.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .padding(contentPadding)
                                .fillMaxSize(),
                        ) {
                            UpdatesLastUpdatedHeader(
                                lastUpdated = lastUpdated,
                                onCheckNow = { scope.launch { onUpdateLibrary() } },
                            )
                            kurotoon.ui.updates.UpdateErrorsBanner()
                            EmptyScreen(
                                stringRes = if (hasActiveFilters) {
                                    MR.strings.error_no_match
                                } else {
                                    MR.strings.information_no_recent
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        val grouped by LocalContext.current.appGraph.kuroPreferences.updatesGroupSeries
                            .collectAsState()
                        val expanded = remember { mutableStateSetOf<String>() }
                        FastScrollLazyColumn(
                            contentPadding = contentPadding,
                        ) {
                            updatesLastUpdatedItem(
                                lastUpdated = lastUpdated,
                                unread = state.items.count { !it.update.read },
                                total = state.items.size,
                                onCheckNow = { scope.launch { onUpdateLibrary() } },
                            )
                            item(key = "updates-errors") { kurotoon.ui.updates.UpdateErrorsBanner() }

                            updatesUiItems(
                                uiModels = state.getUiModel(grouped, expanded.toSet()),
                                selectionMode = state.selectionMode,
                                onUpdateSelected = onUpdateSelected,
                                onClickCover = onClickCover,
                                onClickUpdate = onOpenChapter,
                                onDownloadChapter = onDownloadChapter,
                                onToggleGroup = { if (!expanded.remove(it)) expanded.add(it) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdatesAppBar(
    onCalendarClicked: () -> Unit,
    onScheduleClicked: () -> Unit,
    onUpdateLibrary: () -> Unit,
    onFilterClicked: () -> Unit,
    hasFilters: Boolean,
    onClearUpdates: (() -> Unit)?,
    onRestoreCleared: (() -> Unit)?,
    // For action mode
    actionModeCounter: Int,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onCancelActionMode: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    AppBar(
        modifier = modifier,
        title = stringResource(MR.strings.label_recent_updates),
        actions = {
            AppBarActions(
                listOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_filter),
                        icon = MaterialSymbols.Rounded.FilterList,
                        iconTint = if (hasFilters) MaterialTheme.colorScheme.active else LocalContentColor.current,
                        onClick = onFilterClicked,
                    ),
                    AppBar.Action(
                        title = stringResource(MR.strings.kurotoon_schedule_title),
                        icon = MaterialSymbols.Rounded.Schedule,
                        onClick = onScheduleClicked,
                    ),
                    AppBar.Action(
                        title = stringResource(MR.strings.action_view_upcoming),
                        icon = MaterialSymbols.Rounded.CalendarMonth,
                        onClick = onCalendarClicked,
                    ),
                    AppBar.Action(
                        title = stringResource(MR.strings.action_update_library),
                        icon = MaterialSymbols.Rounded.Refresh,
                        onClick = onUpdateLibrary,
                    ),
                    onClearUpdates?.let {
                        AppBar.OverflowAction(
                            title = stringResource(MR.strings.kurotoon_updates_clear),
                            onClick = it,
                        )
                    },
                    onRestoreCleared?.let {
                        AppBar.OverflowAction(
                            title = stringResource(MR.strings.kurotoon_updates_restore),
                            onClick = it,
                        )
                    },
                ).filterNotNull(),
            )
        },
        actionModeCounter = actionModeCounter,
        onCancelActionMode = onCancelActionMode,
        actionModeActions = {
            AppBarActions(
                listOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_all),
                        icon = MaterialSymbols.Rounded.SelectAll,
                        onClick = onSelectAll,
                    ),
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_inverse),
                        icon = MaterialSymbols.Rounded.FlipToBack,
                        onClick = onInvertSelection,
                    ),
                ),
            )
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun UpdatesBottomBar(
    selected: List<UpdatesItem>,
    onDownloadChapter: (List<UpdatesItem>, ChapterDownloadAction) -> Unit,
    onMultiBookmarkClicked: (List<UpdatesItem>, bookmark: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<UpdatesItem>, read: Boolean) -> Unit,
    onMultiDeleteClicked: (List<UpdatesItem>) -> Unit,
) {
    MangaBottomActionMenu(
        visible = selected.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        onBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected, true)
        }.takeIf { selected.fastAny { !it.update.bookmark } },
        onRemoveBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected, false)
        }.takeIf { selected.fastAll { it.update.bookmark } },
        onMarkAsReadClicked = {
            onMultiMarkAsReadClicked(selected, true)
        }.takeIf { selected.fastAny { !it.update.read } },
        onMarkAsUnreadClicked = {
            onMultiMarkAsReadClicked(selected, false)
        }.takeIf { selected.fastAny { it.update.read || it.update.lastPageRead > 0 } },
        onDownloadClicked = {
            onDownloadChapter(selected, ChapterDownloadAction.START)
        }.takeIf {
            selected.fastAny { it.downloadStateProvider() != Download.State.DOWNLOADED }
        },
        onDeleteClicked = {
            onMultiDeleteClicked(selected)
        }.takeIf { selected.fastAny { it.downloadStateProvider() == Download.State.DOWNLOADED } },
    )
}

sealed interface UpdatesUiModel {
    data class Header(val date: LocalDate) : UpdatesUiModel
    data class Item(
        val item: UpdatesItem,
        val hidden: Int = 0,
        val groupKey: String? = null,
        val expanded: Boolean = false,
    ) : UpdatesUiModel
}
