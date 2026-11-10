package kurotoon.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kurotoon.library.LibraryDuplicates
import kurotoon.ui.KuroColors
import logcat.LogPriority
import mihon.app.di.appGraph
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen

/** Series saved more than once, side by side, so the extra copies can be opened and removed. */
class DuplicatesScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val sourceManager = remember { context.appGraph.sourceManager }
        val found by remember {
            context.appGraph.getLibraryManga.subscribe()
                .map { library ->
                    val groups = LibraryDuplicates.groups(library)
                    val names = groups.flatten().map { it.manga.source }.distinct()
                        .associateWith { sourceManager.getOrStub(it).name }
                    groups to names
                }
                .catch { logcat(LogPriority.ERROR, it) }
        }.collectAsState(initial = null)

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.kurotoon_duplicates_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            val current = found?.first
            when {
                current == null -> LoadingScreen(Modifier.padding(padding))
                current.isEmpty() -> EmptyScreen(
                    message = stringResource(MR.strings.kurotoon_duplicates_none),
                    modifier = Modifier.padding(padding),
                )
                else -> DuplicatesList(
                    groups = current,
                    sourceName = { id -> found?.second?.get(id) ?: id.toString() },
                    onClick = { navigator.push(MangaScreen(it)) },
                    contentPadding = padding,
                )
            }
        }
    }
}

@Composable
internal fun DuplicatesList(
    groups: List<List<LibraryManga>>,
    sourceName: (Long) -> String,
    onClick: (Long) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "info") {
            Text(
                text = stringResource(MR.strings.kurotoon_duplicates_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
        }
        items(groups, key = { group -> group.first().id }) { group ->
            Column {
                Text(
                    text = group.first().manga.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(group, key = { it.id }) { entry ->
                        Column(modifier = Modifier.width(100.dp)) {
                            MangaCover.Book(
                                data = entry.manga.asMangaCover(),
                                shape = RoundedCornerShape(12.dp),
                                contentDescription = entry.manga.title,
                                onClick = { onClick(entry.id) },
                            )
                            Text(
                                text = sourceName(entry.manga.source),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (entry ===
                                    group.first()
                                ) {
                                    KuroColors.Crimson
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                            Text(
                                text = stringResource(
                                    MR.strings.kurotoon_duplicates_progress,
                                    entry.readCount,
                                    entry.totalChapters,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
