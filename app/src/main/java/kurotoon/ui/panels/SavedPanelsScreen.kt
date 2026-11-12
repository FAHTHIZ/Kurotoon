package kurotoon.ui.panels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import kurotoon.panels.SavedPanel
import kurotoon.panels.SavedPanels
import kurotoon.ui.KuroAlertDialog
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.KuroTextButton
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen

/** The gallery of pages kept from the reader. Tap one to jump back into its chapter. */
class SavedPanelsScreen : Screen() {

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val store = remember { SavedPanels.get(context) }
        val panels by store.panels.collectAsState()
        var removing by remember { mutableStateOf<SavedPanel?>(null) }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.kurotoon_panels_title),
                    subtitle = if (panels.isEmpty()) null else panels.size.toString(),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            if (panels.isEmpty()) {
                EmptyScreen(
                    message = stringResource(MR.strings.kurotoon_panels_empty),
                    modifier = Modifier.padding(padding),
                )
                return@Scaffold
            }
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(160.dp),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 10.dp,
            ) {
                items(panels, key = { it.id }) { panel ->
                    PanelCard(
                        panel = panel,
                        store = store,
                        onClick = {
                            context.startActivity(ReaderActivity.newIntent(context, panel.mangaId, panel.chapterId))
                        },
                        onLongClick = { removing = panel },
                    )
                }
            }
        }

        removing?.let { panel ->
            KuroAlertDialog(
                onDismissRequest = { removing = null },
                title = { Text(stringResource(MR.strings.kurotoon_panels_remove)) },
                text = { Text("${panel.mangaTitle} · ${panel.chapterName}") },
                confirmButton = {
                    KuroTextButton(onClick = {
                        store.remove(panel)
                        removing = null
                    }) { Text(stringResource(MR.strings.action_delete)) }
                },
                dismissButton = {
                    KuroTextButton(onClick = { removing = null }) { Text(stringResource(MR.strings.action_cancel)) }
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PanelCard(panel: SavedPanel, store: SavedPanels, onClick: () -> Unit, onLongClick: () -> Unit) {
    // Tall webtoon strips are cropped to a poster shape so the grid stays readable
    val ratio = if (panel.width > 0 && panel.height > 0) {
        (panel.width.toFloat() / panel.height).coerceIn(0.45f, 1.6f)
    } else {
        0.7f
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        AsyncImage(
            model = store.fileOf(panel),
            contentDescription = panel.mangaTitle,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .heightIn(min = 120.dp),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(
                text = panel.mangaTitle,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(MR.strings.kurotoon_panels_where, panel.chapterName, panel.page + 1),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
