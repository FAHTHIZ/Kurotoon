package kurotoon.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import eu.kanade.presentation.manga.components.MangaCover
import kurotoon.ui.KuroColors
import kurotoon.ui.kuroCard
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.AutoAwesome
import mihon.icons.materialsymbols.rounded.KeyboardArrowRight
import mihon.icons.materialsymbols.rounded.LocalFireDepartment
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** The first source's top title, presented like the web app's hero banner. */
@Composable
internal fun HomeHero(
    sourceName: String,
    manga: Manga,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(shape)
            .kuroCard(shape)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = manga.asMangaCover(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .blur(24.dp)
                .alpha(0.32f),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(KuroColors.Crimson.copy(alpha = 0.14f), MaterialTheme.colorScheme.background.copy(0.6f)),
                    ),
                ),
        )
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MangaCover.Book(
                data = manga.asMangaCover(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.width(96.dp),
                contentDescription = manga.title,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(144.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Row(
                    modifier = Modifier
                        .background(KuroColors.Crimson.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, KuroColors.Crimson.copy(alpha = 0.25f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        MaterialSymbols.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = KuroColors.Crimson,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = stringResource(MR.strings.kurotoon_home_topOn, sourceName).uppercase(),
                        color = KuroColors.Crimson,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = manga.title,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
                val tags = manga.genre.orEmpty().take(4)
                if (tags.isNotEmpty()) {
                    Text(
                        text = tags.joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(KuroColors.Crimson, KuroColors.CrimsonDeep)))
                        .clickable(onClick = onClick)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(MR.strings.kurotoon_home_readNow),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** A source's popular titles in a horizontal row. */
@Composable
internal fun HomeSourceRow(
    row: KuroHomeViewModel.SourceRow,
    onSeeAll: () -> Unit,
    onMangaClick: (Manga) -> Unit,
    onRetry: () -> Unit,
) {
    if (row.items?.isEmpty() == true) return
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(KuroColors.Crimson.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .border(1.dp, KuroColors.Crimson.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    MaterialSymbols.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = KuroColors.Crimson,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = row.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f, fill = false),
            )
            Text(
                text = " · " + stringResource(MR.strings.popular),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onSeeAll)
                    .padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(MR.strings.kurotoon_home_seeAll),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    MaterialSymbols.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        when {
            row.failed -> Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .kuroCard()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(MR.strings.kurotoon_home_sourceUnavailable, row.name),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(MR.strings.action_retry),
                    style = MaterialTheme.typography.labelMedium,
                    color = KuroColors.Crimson,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onRetry)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            row.items == null -> LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false,
            ) {
                items(6) {
                    Box(
                        Modifier
                            .width(112.dp)
                            .height(168.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp)),
                    )
                }
            }
            else -> LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(row.items, key = { it.id }) { manga ->
                    Column(modifier = Modifier.width(112.dp)) {
                        MangaCover.Book(
                            data = manga.asMangaCover(),
                            shape = RoundedCornerShape(12.dp),
                            contentDescription = manga.title,
                            onClick = { onMangaClick(manga) },
                        )
                        Text(
                            text = manga.title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
