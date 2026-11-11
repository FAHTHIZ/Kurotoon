package eu.kanade.presentation.library.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.category.visualName
import kurotoon.ui.browse.Pill
import tachiyomi.domain.category.model.Category

/**
 * Categories as a row of pills rather than Mihon's underlined tab strip. The pill for the page on
 * screen is filled with the accent and the row keeps it in view as you swipe.
 */
@Composable
internal fun LibraryTabs(
    categories: List<Category>,
    pagerState: PagerState,
    getItemCountForCategory: (Category) -> Int?,
    onTabItemClick: (Int) -> Unit,
) {
    val currentPageIndex = pagerState.currentPage.coerceAtMost(categories.lastIndex)
    val listState = rememberLazyListState()
    LaunchedEffect(currentPageIndex) {
        if (currentPageIndex >= 0) listState.animateScrollToItem(currentPageIndex, scrollOffset = -48)
    }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(categories, key = { _, it -> it.id }) { index, category ->
            val selected = index == currentPageIndex
            val background by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                label = "categoryPillBackground",
            )
            val content by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "categoryPillContent",
            )
            Pill(
                onClick = { onTabItemClick(index) },
                height = 34.dp,
                background = background,
            ) {
                Text(
                    text = category.visualName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = content,
                    maxLines = 1,
                )
                val count = getItemCountForCategory(category)
                if (count != null) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = content.copy(alpha = 0.75f),
                        modifier = Modifier,
                    )
                }
            }
        }
    }
}
