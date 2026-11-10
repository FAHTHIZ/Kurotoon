package kurotoon.ui.browse

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kurotoon.ui.KuroColors
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Tune
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** Which listing chip is lit: the source's popular or latest feed, or one of its sort orders. */
sealed interface BrowseSort {
    data object Popular : BrowseSort
    data object Latest : BrowseSort
    data class Sort(val index: Int) : BrowseSort
    data object Custom : BrowseSort
    data object None : BrowseSort
}

/**
 * The browse header from the KuroToon web app: a row of solid sort pills followed by a row of
 * genre pills, both scrolling sideways. Genres come from the source's own filters, so sources
 * without a genre list simply show the sort row.
 */
@Composable
fun BrowseChips(
    supportsLatest: Boolean,
    sorts: List<String>,
    genres: List<String>,
    selectedSort: BrowseSort,
    selectedGenre: String?,
    hasFilters: Boolean,
    onPopular: () -> Unit,
    onLatest: () -> Unit,
    onSort: (Int) -> Unit,
    onGenre: (String?) -> Unit,
    onFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "popular") {
                SortPill(stringResource(MR.strings.popular), selectedSort == BrowseSort.Popular, onPopular)
            }
            if (supportsLatest) {
                item(key = "latest") {
                    SortPill(stringResource(MR.strings.latest), selectedSort == BrowseSort.Latest, onLatest)
                }
            }
            sorts.forEachIndexed { index, name ->
                item(key = "sort-$index") {
                    SortPill(name, selectedSort == BrowseSort.Sort(index), { onSort(index) })
                }
            }
            if (hasFilters) {
                item(key = "filters") {
                    SortPill(
                        text = stringResource(MR.strings.action_filter),
                        selected = selectedSort == BrowseSort.Custom,
                        onClick = onFilters,
                        icon = MaterialSymbols.Rounded.Tune,
                    )
                }
            }
        }

        if (genres.isNotEmpty()) {
            val listState = rememberLazyListState()
            // Keep the picked genre in view when it is set from elsewhere, like a manga's tag
            LaunchedEffect(selectedGenre) {
                val index = selectedGenre?.let(genres::indexOf) ?: -1
                if (index >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == index + 1 }) {
                    listState.animateScrollToItem(index + 1)
                }
            }
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item(key = "all") {
                    GenrePill(stringResource(MR.strings.all), selectedGenre == null) { onGenre(null) }
                }
                items(genres, key = { "genre-$it" }) { genre ->
                    GenrePill(genre, selectedGenre == genre) { onGenre(if (selectedGenre == genre) null else genre) }
                }
            }
        }
    }
}

@Composable
private fun SortPill(text: String, selected: Boolean, onClick: () -> Unit, icon: ImageVector? = null) {
    val background by animateColorAsState(
        if (selected) KuroColors.Crimson else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "sortPillBackground",
    )
    val content by animateColorAsState(
        if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        label = "sortPillContent",
    )
    Pill(onClick = onClick, height = 34.dp, background = background) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        }
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun GenrePill(text: String, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(
        if (selected) KuroColors.Crimson.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainer,
        label = "genrePillBackground",
    )
    val content by animateColorAsState(
        if (selected) KuroColors.Crimson else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "genrePillContent",
    )
    val border by animateColorAsState(
        if (selected) KuroColors.Crimson.copy(alpha = 0.6f) else Color.Transparent,
        label = "genrePillBorder",
    )
    Pill(onClick = onClick, height = 30.dp, background = background, border = BorderStroke(1.dp, border)) {
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@Composable
private fun Pill(
    onClick: () -> Unit,
    height: Dp,
    background: Color,
    border: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, label = "pillScale")
    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(height)
            .clip(CircleShape)
            .background(background)
            .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content()
    }
}
