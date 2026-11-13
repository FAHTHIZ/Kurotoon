package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.browse.BrowseSourceContent
import eu.kanade.presentation.browse.MissingSourceScreen
import eu.kanade.presentation.browse.components.BrowseSourceToolbar
import eu.kanade.presentation.browse.components.RemoveMangaDialog
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.manga.DuplicateMangaDialog
import eu.kanade.presentation.util.AssistContentScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.extension.details.SourcePreferencesScreen
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceViewModel.Listing
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kurotoon.catalogue.SavedSearches
import kurotoon.feature.migration.dialog.MigrateMangaDialog
import kurotoon.icons.materialsymbols.MaterialSymbols
import kurotoon.icons.materialsymbols.rounded.FilterList
import kurotoon.icons.materialsymbols.rounded.Refresh
import kurotoon.icons.materialsymbols.rounded.Verified
import kurotoon.icons.materialsymbols.roundedfilled.Favorite
import kurotoon.presentation.core.util.collectAsLazyPagingItems
import kurotoon.ui.KuroSnackbarHost
import kurotoon.ui.browse.BrowseChips
import kurotoon.ui.browse.BrowseSort
import kurotoon.ui.browse.FacetActions
import kurotoon.ui.browse.SavedSearchDialog
import kurotoon.ui.browse.SavedSearchRow
import tachiyomi.core.common.Constants
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.source.model.StubSource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.source.local.LocalSource

data class BrowseSourceScreen(
    val sourceId: Long,
    private val listingQuery: String?,
    /** A tag to filter by as soon as the source is ready, e.g. one tapped on a series page. */
    private val initialGenre: String? = null,
    /** Opens this saved search once the source is ready, e.g. from a Home row. */
    private val savedSearchId: Long? = null,
) : Screen(), AssistContentScreen {

    private var assistUrl: String? = null

    override fun onProvideAssistUrl() = assistUrl

    @Composable
    override fun Content() {
        val viewModel =
            assistedMetroViewModel<BrowseSourceViewModel, BrowseSourceViewModel.Factory> {
                create(sourceId = sourceId, listingQuery = listingQuery)
            }
        val state by viewModel.state.collectAsState()

        var savedSearchOpened by rememberSaveable { mutableStateOf(savedSearchId == null) }
        LaunchedEffect(state.source, state.savedSearches) {
            if (savedSearchOpened || state.source == null) return@LaunchedEffect
            state.savedSearches.firstOrNull { it.id == savedSearchId }?.let {
                viewModel.openSavedSearch(it)
                savedSearchOpened = true
            }
        }

        val navigator = LocalNavigator.currentOrThrow
        val navigateUp: () -> Unit = {
            when {
                !state.isUserQuery && state.toolbarQuery != null -> viewModel.setToolbarQuery(null)
                else -> navigator.pop()
            }
        }

        val source = state.source
        if (source == null) {
            LoadingScreen()
            return
        }

        if (source is StubSource) {
            MissingSourceScreen(
                source = source,
                navigateUp = navigateUp,
            )
            return
        }

        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        val uriHandler = LocalUriHandler.current
        val snackbarHostState = remember { SnackbarHostState() }

        val onHelpClick = { uriHandler.openUri(LocalSource.HELP_URL) }
        val onWebViewClick = f@{
            val httpSource = source as? HttpSource ?: return@f
            navigator.push(
                WebViewScreen(
                    url = httpSource.getHomeUrl(),
                    initialTitle = httpSource.name,
                    sourceId = httpSource.id,
                ),
            )
        }

        LaunchedEffect(source) {
            assistUrl = (source as? HttpSource)?.getHomeUrl()
        }

        val context = androidx.compose.ui.platform.LocalContext.current
        var selecting by remember { mutableStateOf(false) }
        var pickingDefaultCategory by remember { mutableStateOf(false) }
        var selected by remember { mutableStateOf(emptyMap<Long, tachiyomi.domain.manga.model.Manga>()) }
        val toggleSelected = { manga: tachiyomi.domain.manga.model.Manga ->
            selected = if (manga.id in selected) selected - manga.id else selected + (manga.id to manga)
        }
        val stopSelecting = {
            selecting = false
            selected = emptyMap()
        }
        androidx.activity.compose.BackHandler(enabled = selecting, onBack = stopSelecting)

        Scaffold(
            topBar = topBar@{
                if (selecting) {
                    eu.kanade.presentation.components.AppBar(
                        titleContent = {
                            Text(stringResource(MR.strings.kurotoon_browse_selected, selected.size))
                        },
                        isActionMode = true,
                        onCancelActionMode = stopSelecting,
                        actions = {
                            // Library series picked here can be checked for new chapters on their own
                            if (selected.values.any { it.favorite }) {
                                androidx.compose.material3.IconButton(
                                    onClick = {
                                        val ids = selected.values.filter { it.favorite }.map { it.id }
                                        stopSelecting()
                                        scope.launch {
                                            val started = eu.kanade.tachiyomi.data.library.LibraryUpdateWorker.startNow(
                                                androidx.work.WorkManager.getInstance(context),
                                                mangaIds = ids,
                                            )
                                            val message = if (started) {
                                                val res = MR.plurals.kurotoon_library_updatingSelected
                                                context.pluralStringResource(res, ids.size, ids.size)
                                            } else {
                                                context.stringResource(MR.strings.update_already_running)
                                            }
                                            snackbarHostState.showSnackbar(message)
                                        }
                                    },
                                ) {
                                    androidx.compose.material3.Icon(
                                        imageVector = kurotoon.icons.materialsymbols.MaterialSymbols.Rounded.Refresh,
                                        contentDescription = stringResource(MR.strings.kurotoon_library_updateSelected),
                                    )
                                }
                            }
                            tachiyomi.presentation.core.components.material.KuroTextButton(
                                enabled = selected.values.any { !it.favorite },
                                onClick = {
                                    val toHide = selected.values.filterNot { it.favorite }
                                    viewModel.hideSeries(toHide)
                                    stopSelecting()
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = context.pluralStringResource(
                                                MR.plurals.kurotoon_browse_hiddenMany,
                                                toHide.size,
                                                toHide.size,
                                            ),
                                            actionLabel = context.stringResource(MR.strings.kurotoon_action_undo),
                                            duration = androidx.compose.material3.SnackbarDuration.Long,
                                        )
                                        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                            viewModel.unhideSeries(toHide)
                                        }
                                    }
                                },
                            ) {
                                Text(stringResource(MR.strings.kurotoon_browse_hideSeries))
                            }
                            tachiyomi.presentation.core.components.material.KuroTextButton(
                                enabled = selected.values.any { !it.favorite },
                                onClick = {
                                    val toAdd = selected.values.filterNot { it.favorite }
                                    viewModel.addFavorites(toAdd)
                                    stopSelecting()
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.pluralStringResource(
                                                MR.plurals.kurotoon_browse_addedMany,
                                                toAdd.size,
                                                toAdd.size,
                                            ),
                                        )
                                    }
                                },
                            ) {
                                Text(stringResource(MR.strings.add_to_library))
                            }
                        },
                    )
                    return@topBar
                }
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .pointerInput(Unit) {},
                ) {
                    BrowseSourceToolbar(
                        searchQuery = state.toolbarQuery,
                        onSearchQueryChange = viewModel::setToolbarQuery,
                        source = source,
                        displayMode = viewModel.displayMode,
                        onDisplayModeChange = { viewModel.displayMode = it },
                        navigateUp = navigateUp,
                        onWebViewClick = onWebViewClick,
                        onHelpClick = onHelpClick,
                        onSettingsClick = { navigator.push(SourcePreferencesScreen(sourceId)) },
                        onSearch = viewModel::search,
                        onSelectClick = { selecting = true },
                        onDefaultCategoryClick = { pickingDefaultCategory = true },
                    )
                    if (state.toolbarQuery?.isEmpty() == true) {
                        val recent by remember(viewModel) { viewModel.recentSearches.changes() }
                            .collectAsState(emptyList())
                        kurotoon.ui.browse.RecentSearchRow(
                            recent = recent,
                            onSearch = viewModel::search,
                            onClear = viewModel::clearRecentSearches,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    if (pickingDefaultCategory) {
                        kurotoon.ui.browse.SourceDefaultCategoryDialog(sourceId) { pickingDefaultCategory = false }
                    }

                    BrowseChips(
                        supportsLatest = source.supportsLatest,
                        sorts = state.quickSorts,
                        selectedSort = state.selectedSort?.let(BrowseSort::Sort) ?: when {
                            state.listing == Listing.Popular -> BrowseSort.Popular
                            state.listing == Listing.Latest -> BrowseSort.Latest
                            state.activeFacets.isNotEmpty() -> BrowseSort.None
                            else -> BrowseSort.Custom
                        },
                        hasFilters = state.filters.isNotEmpty(),
                        onPopular = {
                            viewModel.resetFilters()
                            viewModel.setListing(Listing.Popular)
                        },
                        onLatest = {
                            viewModel.resetFilters()
                            viewModel.setListing(Listing.Latest)
                        },
                        onSort = { viewModel.applySort(it.takeUnless { index -> index == state.selectedSort }) },
                        onFilters = viewModel::openFilterSheet,
                        extraRow = {
                            SavedSearchRow(
                                saved = state.savedSearches,
                                selectedId = state.currentSavedSearchId,
                                canSave = state.canSaveSearch && state.currentSavedSearchId == null,
                                onSave = { viewModel.setDialog(BrowseSourceViewModel.Dialog.SaveSearch) },
                                onOpen = viewModel::openSavedSearch,
                                onEdit = { viewModel.setDialog(BrowseSourceViewModel.Dialog.EditSavedSearch(it)) },
                            )
                        },
                        facets = state.facets,
                        applied = state.appliedFilters,
                        active = state.activeFacets,
                        facetActions = remember(viewModel) {
                            FacetActions(
                                onTag = viewModel::cycleTag,
                                onExcludeTag = viewModel::excludeTag,
                                onChoice = viewModel::selectChoice,
                                onClear = viewModel::clearFacet,
                                onClearAll = viewModel::clearFacets,
                            )
                        },
                    )
                }
            },
            snackbarHost = { KuroSnackbarHost(snackbarHostState) },
        ) { paddingValues ->
            androidx.compose.runtime.CompositionLocalProvider(
                kurotoon.ui.browse.LocalBrowseSelection provides selected.keys,
            ) {
                BrowseSourceContent(
                    source = source,
                    mangaList = viewModel.mangaPagerFlowFlow.collectAsLazyPagingItems(),
                    columns = viewModel.getColumnsPreference(LocalConfiguration.current.orientation),
                    displayMode = viewModel.displayMode,
                    snackbarHostState = snackbarHostState,
                    contentPadding = paddingValues,
                    onWebViewClick = onWebViewClick,
                    onHelpClick = { uriHandler.openUri(Constants.URL_HELP) },
                    onLocalSourceHelpClick = onHelpClick,
                    onMangaClick = { manga ->
                        if (selecting) {
                            toggleSelected(manga)
                        } else {
                            navigator.push(MangaScreen(manga.id, true))
                        }
                    },
                    onMangaLongClick = { manga ->
                        if (selecting) {
                            toggleSelected(manga)
                        } else {
                            scope.launch(Dispatchers.IO) {
                                val duplicates = viewModel.getDuplicateLibraryManga(manga)
                                when {
                                    manga.favorite -> viewModel.setDialog(
                                        BrowseSourceViewModel.Dialog.RemoveManga(manga),
                                    )
                                    duplicates.isNotEmpty() -> viewModel.setDialog(
                                        BrowseSourceViewModel.Dialog.AddDuplicateManga(manga, duplicates),
                                    )
                                    else -> viewModel.addFavorite(manga)
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    },
                )
            }
        }

        val onDismissRequest = { viewModel.setDialog(null) }
        when (val dialog = state.dialog) {
            is BrowseSourceViewModel.Dialog.Filter -> {
                SourceFilterDialog(
                    onDismissRequest = onDismissRequest,
                    filters = state.filters,
                    onReset = viewModel::resetFilters,
                    onFilter = { viewModel.search(filters = state.filters) },
                    onUpdate = viewModel::setFilters,
                )
            }
            BrowseSourceViewModel.Dialog.SaveSearch -> {
                SavedSearchDialog(
                    initialName = SavedSearches.suggestName(state.listing.query, state.activeFacets),
                    initialOnHome = true,
                    editing = false,
                    onDismiss = onDismissRequest,
                    onConfirm = { name, onHome ->
                        viewModel.saveCurrentSearch(name, onHome)
                        onDismissRequest()
                    },
                )
            }
            is BrowseSourceViewModel.Dialog.EditSavedSearch -> {
                SavedSearchDialog(
                    initialName = dialog.search.name,
                    initialOnHome = dialog.search.onHome,
                    editing = true,
                    onDismiss = onDismissRequest,
                    onConfirm = { name, onHome ->
                        viewModel.updateSavedSearch(dialog.search.id, name, onHome)
                        onDismissRequest()
                    },
                    onDelete = {
                        viewModel.deleteSavedSearch(dialog.search.id)
                        onDismissRequest()
                    },
                )
            }
            is BrowseSourceViewModel.Dialog.AddDuplicateManga -> {
                DuplicateMangaDialog(
                    duplicates = dialog.duplicates,
                    onDismissRequest = onDismissRequest,
                    onConfirm = { viewModel.addFavorite(dialog.manga) },
                    onOpenManga = { navigator.push(MangaScreen(it.id)) },
                    onMigrate = { viewModel.setDialog(BrowseSourceViewModel.Dialog.Migrate(dialog.manga, it)) },
                )
            }

            is BrowseSourceViewModel.Dialog.Migrate -> {
                MigrateMangaDialog(
                    current = dialog.current,
                    target = dialog.target,
                    // Initiated from the context of [dialog.target] so we show [dialog.current].
                    onClickTitle = { navigator.push(MangaScreen(dialog.current.id)) },
                    onDismissRequest = onDismissRequest,
                )
            }
            is BrowseSourceViewModel.Dialog.RemoveManga -> {
                RemoveMangaDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = {
                        viewModel.changeMangaFavorite(dialog.manga)
                    },
                    mangaToRemove = dialog.manga,
                )
            }
            is BrowseSourceViewModel.Dialog.ChangeMangaCategory -> {
                ChangeCategoryDialog(
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = { navigator.push(CategoryScreen()) },
                    onConfirm = { include, _ ->
                        viewModel.changeMangaFavorite(dialog.manga)
                        viewModel.moveMangaToCategories(dialog.manga, include)
                    },
                )
            }
            else -> {}
        }

        var initialGenreApplied by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (initialGenre != null && !initialGenreApplied) {
                initialGenreApplied = true
                viewModel.searchGenre(initialGenre)
            }
        }

        LaunchedEffect(Unit) {
            queryEvent.receiveAsFlow()
                .collectLatest {
                    when (it) {
                        is SearchType.Genre -> viewModel.searchGenre(it.txt)
                        is SearchType.Text -> viewModel.search(it.txt)
                    }
                }
        }
    }

    suspend fun search(query: String) = queryEvent.send(SearchType.Text(query))
    suspend fun searchGenre(name: String) = queryEvent.send(SearchType.Genre(name))

    companion object {
        private val queryEvent = Channel<SearchType>()
    }

    sealed class SearchType(val txt: String) {
        class Text(txt: String) : SearchType(txt)
        class Genre(txt: String) : SearchType(txt)
    }
}
