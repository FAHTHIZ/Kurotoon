package kurotoon.ui.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceViewModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.stats.StatsScreen
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Home
import mihon.icons.materialsymbols.roundedfilled.Home
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

data object KuroHomeTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            return TabOptions(
                index = 0u,
                title = stringResource(MR.strings.kurotoon_tab_home),
                icon = rememberVectorPainter(
                    if (isSelected) MaterialSymbols.RoundedFilled.Home else MaterialSymbols.Rounded.Home,
                ),
            )
        }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val viewModel = metroViewModel<KuroHomeViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val hidden by remember { context.appGraph.kuroPreferences.homeHiddenSections }.collectAsState()

        KuroHomeContent(
            state = state,
            snackbarHostState = snackbarHostState,
            onSearchClick = { navigator.push(GlobalSearchScreen()) },
            onAddSourcesClick = { scope.launch { HomeScreen.openTab(HomeScreen.Tab.Browse(toExtensions = true)) } },
            onMangaClick = { navigator.push(MangaScreen(it)) },
            onBrowseMangaClick = { navigator.push(MangaScreen(it.id, fromSource = true)) },
            onRowClick = { row ->
                navigator.push(
                    BrowseSourceScreen(
                        sourceId = row.sourceId,
                        listingQuery = if (row.latest) {
                            BrowseSourceViewModel.Listing.Latest.query
                        } else {
                            BrowseSourceViewModel.Listing.Popular.query
                        },
                        savedSearchId = row.savedSearch?.id,
                        initialGenre = row.pickTag,
                    ),
                )
            },
            onRetryRow = viewModel::retry,
            onToggleLatest = viewModel::toggleLatest,
            onUpdatesClick = { scope.launch { HomeScreen.openTab(HomeScreen.Tab.Updates) } },
            onResumeClick = viewModel::resume,
            onExtensionsClick = { scope.launch { HomeScreen.openTab(HomeScreen.Tab.Browse(toExtensions = true)) } },
            onHistoryClick = { scope.launch { HomeScreen.openTab(HomeScreen.Tab.History) } },
            onDownloadsClick = { navigator.push(DownloadQueueScreen) },
            onStatsClick = { navigator.push(StatsScreen()) },
            hidden = hidden,
            onRefresh = viewModel::refresh,
        )

        LaunchedEffect(Unit) {
            viewModel.openChapter.collect { chapter ->
                if (chapter != null) {
                    context.startActivity(ReaderActivity.newIntent(context, chapter.mangaId, chapter.id))
                } else {
                    snackbarHostState.showSnackbar(context.stringResource(MR.strings.no_next_chapter))
                }
            }
        }
    }
}
