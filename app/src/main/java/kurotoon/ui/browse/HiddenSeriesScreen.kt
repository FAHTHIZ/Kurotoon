package kurotoon.ui.browse

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.app.di.appGraph
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.KuroTextButton
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.collectAsState

/** Series hidden from Browse and Search, each with a way to bring it back (mihon#2322). */
class HiddenSeriesScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val preference = remember { context.appGraph.kuroPreferences.hiddenSeries }
        val ids by preference.collectAsState()
        var titles by remember { mutableStateOf(emptyMap<Long, Manga>()) }
        var sourceNames by remember { mutableStateOf(emptyMap<Long, String>()) }

        LaunchedEffect(ids) {
            titles = withContext(Dispatchers.IO) {
                ids.mapNotNull { it.toLongOrNull() }
                    .mapNotNull { id -> runCatching { context.appGraph.mangaRepository.getMangaById(id) }.getOrNull() }
                    .associateBy { it.id }
            }
            sourceNames = titles.values.map { it.source }.distinct()
                .associateWith { context.appGraph.sourceManager.getOrStub(it).name }
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.kurotoon_browse_hiddenSeries),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            val shown = ids.mapNotNull { it.toLongOrNull() }.mapNotNull(titles::get).sortedBy { it.title.lowercase() }
            if (ids.isEmpty()) {
                EmptyScreen(
                    stringRes = MR.strings.kurotoon_browse_hiddenSeriesNone,
                    modifier = Modifier.padding(padding),
                )
                return@Scaffold
            }
            LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxWidth()) {
                items(shown, key = { it.id }) { manga ->
                    ListItem(
                        headlineContent = { Text(manga.title) },
                        supportingContent = {
                            Text(sourceNames[manga.source].orEmpty())
                        },
                        trailingContent = {
                            KuroTextButton(
                                onClick = { preference.set(preference.get() - manga.id.toString()) },
                                modifier = Modifier.padding(start = 8.dp),
                            ) {
                                Text(stringResource(MR.strings.kurotoon_browse_unhideSeries))
                            }
                        },
                    )
                }
            }
        }
    }
}
