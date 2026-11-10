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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.util.formatChapterNumber
import kurotoon.ui.KuroColors
import kurotoon.ui.KuroTip
import kurotoon.ui.KuroTopBar
import kurotoon.ui.kuroCard
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.Extension
import mihon.icons.materialsymbols.rounded.History
import mihon.icons.materialsymbols.rounded.QueryStats
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

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
    onResumeClick: (HistoryWithRelations) -> Unit,
    onExtensionsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onStatsClick: () -> Unit,
) {
    Scaffold(
        topBar = { KuroTopBar(onSearchClick = onSearchClick) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
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
            if (state.heroes.isNotEmpty()) {
                item(key = "hero") {
                    HomeHeroCarousel(heroes = state.heroes, onClick = onBrowseMangaClick)
                }
            }
            item(key = "shortcuts") {
                Shortcuts(
                    onExtensionsClick = onExtensionsClick,
                    onHistoryClick = onHistoryClick,
                    onDownloadsClick = onDownloadsClick,
                    onStatsClick = onStatsClick,
                )
            }
            item(key = "continue") {
                ContinueReading(
                    items = state.continueReading,
                    onMangaClick = onMangaClick,
                    onResumeClick = onResumeClick,
                )
            }
            items(state.savedRows + state.rows, key = { it.key }) { row ->
                HomeSourceRow(
                    row = row,
                    onSeeAll = { onRowClick(row) },
                    onMangaClick = onBrowseMangaClick,
                    onRetry = { onRetryRow(row) },
                )
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
            .kuroCard(shape)
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(KuroColors.Crimson.copy(alpha = 0.16f), Color.Transparent)),
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "😸", fontSize = 64.sp)
        Text(
            text = stringResource(MR.strings.kurotoon_home_welcomeTitle),
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
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
                .background(Brush.linearGradient(listOf(KuroColors.Crimson, KuroColors.CrimsonDeep)))
                .clickable(onClick = onAddSourcesClick)
                .padding(horizontal = 24.dp, vertical = 10.dp),
        ) {
            Text(
                text = stringResource(MR.strings.kurotoon_home_addSources),
                color = Color.White,
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
            .kuroCard()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(KuroColors.Crimson.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = KuroColors.Crimson, modifier = Modifier.size(18.dp))
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
    onMangaClick: (Long) -> Unit,
    onResumeClick: (HistoryWithRelations) -> Unit,
) {
    Column {
        Text(
            text = stringResource(MR.strings.kurotoon_home_continueReading),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
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
                Column(modifier = Modifier.width(112.dp)) {
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
                                .background(KuroColors.Crimson)
                                .clickable { onResumeClick(item) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = stringResource(MR.strings.action_resume),
                                color = Color.White,
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
                        Text(
                            text = stringResource(
                                MR.strings.display_mode_chapter,
                                formatChapterNumber(item.chapterNumber),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
