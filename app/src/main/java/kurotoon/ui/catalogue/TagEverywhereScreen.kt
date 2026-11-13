package kurotoon.ui.catalogue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import kurotoon.icons.materialsymbols.MaterialSymbols
import kurotoon.icons.materialsymbols.rounded.Search
import kurotoon.ui.home.HomeSourceRow
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen

/** A tag browsed on every source whose catalogue has it, one row per source. */
class TagEverywhereScreen(private val tag: String) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = assistedMetroViewModel<TagEverywhereViewModel, TagEverywhereViewModel.Factory> {
            create(tag = tag)
        }
        val rows by viewModel.rows.collectAsState()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = tag,
                    subtitle = stringResource(MR.strings.kurotoon_tagEverywhere_subtitle),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            val current = rows
            when {
                current == null -> LoadingScreen(Modifier.padding(padding))
                current.isEmpty() -> EmptyScreen(
                    message = stringResource(MR.strings.kurotoon_tagEverywhere_none, tag),
                    modifier = Modifier.padding(padding),
                    actions = listOf(
                        EmptyScreenAction(
                            MR.strings.kurotoon_tagEverywhere_searchText,
                            MaterialSymbols.Rounded.Search,
                        ) {
                            navigator.replace(GlobalSearchScreen(tag))
                        },
                    ),
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = padding.calculateBottomPadding() + 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    items(TagEverywhereViewModel.ordered(current), key = { it.key }) { row ->
                        HomeSourceRow(
                            modifier = Modifier.animateItem(),
                            row = row,
                            onSeeAll = {
                                navigator.push(
                                    BrowseSourceScreen(row.sourceId, listingQuery = null, initialGenre = tag),
                                )
                            },
                            onMangaClick = { navigator.push(MangaScreen(it.id, fromSource = true)) },
                            onRetry = { viewModel.retry(row) },
                            showListing = false,
                        )
                    }
                }
            }
        }
    }
}
