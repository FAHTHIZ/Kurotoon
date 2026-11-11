package kurotoon.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import kurotoon.panels.SavedPanels
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CollectionsBookmark
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** The latest pages kept from the reader as a row of posters. Draws nothing until one is kept. */
@Composable
internal fun HomePanels(onSeeAll: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SavedPanels.get(context) }
    val all by store.panels.collectAsState()
    if (all.isEmpty()) return
    val panels = all.take(MAX)
    Column {
        HomeShelfHeader(
            title = stringResource(MR.strings.kurotoon_home_panels),
            icon = MaterialSymbols.Rounded.CollectionsBookmark,
            subtitle = stringResource(MR.strings.kurotoon_panels_summary),
            onSeeAll = onSeeAll,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(panels, key = { it.id }) { panel ->
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 168.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable {
                            context.startActivity(ReaderActivity.newIntent(context, panel.mangaId, panel.chapterId))
                        },
                ) {
                    AsyncImage(
                        model = store.fileOf(panel),
                        contentDescription = panel.mangaTitle,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Text(
                        text = panel.mangaTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.8f))))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

private const val MAX = 12
