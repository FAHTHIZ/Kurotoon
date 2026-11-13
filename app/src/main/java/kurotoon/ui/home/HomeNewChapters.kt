package kurotoon.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.manga.components.MangaCover
import kurotoon.icons.materialsymbols.MaterialSymbols
import kurotoon.icons.materialsymbols.rounded.NewReleases
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** Library series with unread chapters fetched lately. Draws nothing when there are none. */
@Composable
internal fun HomeNewChapters(
    items: List<KuroHomeViewModel.NewChapters>,
    onMangaClick: (Long) -> Unit,
    onSeeAll: () -> Unit,
) {
    if (items.isEmpty()) return
    Column {
        HomeShelfHeader(
            title = stringResource(MR.strings.kurotoon_home_newChapters),
            icon = MaterialSymbols.Rounded.NewReleases,
            onSeeAll = onSeeAll,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.latest.mangaId }) { item ->
                Column(modifier = Modifier.width(HomeCoverWidth)) {
                    Box {
                        MangaCover.Book(
                            data = item.latest.coverData,
                            shape = RoundedCornerShape(12.dp),
                            contentDescription = item.latest.mangaTitle,
                            onClick = { onMangaClick(item.latest.mangaId) },
                        )
                        Text(
                            text = stringResource(MR.strings.kurotoon_home_newCount, item.count),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Text(
                        text = item.latest.mangaTitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        text = item.latest.chapterName,
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
