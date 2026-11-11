package eu.kanade.presentation.more.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import eu.kanade.presentation.more.settings.screen.SearchableSettings
import eu.kanade.presentation.more.settings.widget.PreferenceGroupHeader
import kotlinx.coroutines.delay
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import kotlin.time.Duration.Companion.seconds

/**
 * Preference Screen composable which contains a list of [Preference] items
 * @param items [Preference] items which should be displayed on the preference screen. An item can be a single [PreferenceItem] or a group ([Preference.PreferenceGroup])
 * @param modifier [Modifier] to be applied to the preferenceScreen layout
 */
@Composable
fun PreferenceScreen(
    items: List<Preference>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val state = rememberLazyListState()
    val highlightKey = SearchableSettings.highlightKey
    if (highlightKey != null) {
        LaunchedEffect(Unit) {
            val i = items.findHighlightedIndex(highlightKey)
            if (i >= 0) {
                delay(0.5.seconds)
                state.animateScrollToItem(i)
            }
            SearchableSettings.highlightKey = null
        }
    }

    ScrollbarLazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
    ) {
        items.fastForEachIndexed { i, preference ->
            when (preference) {
                // Create Preference Group
                is Preference.PreferenceGroup -> {
                    if (!preference.visible) return@fastForEachIndexed

                    item {
                        Column {
                            PreferenceGroupHeader(
                                title = preference.title,
                                highlighted = preference.title == highlightKey,
                            )
                        }
                    }
                    // Each group reads as one rounded card, like iOS-style grouped settings
                    val shown = preference.preferenceItems.filter { it.visible }
                    itemsIndexed(shown) { index, item ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 2.dp)
                                .clip(groupItemShape(index, shown.size))
                                .background(MaterialTheme.colorScheme.surfaceContainer),
                        ) {
                            PreferenceItem(
                                item = item,
                                highlightKey = highlightKey,
                            )
                        }
                    }
                    item {
                        if (i < items.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                // Create Preference Item
                is Preference.PreferenceItem<*, *> -> item {
                    PreferenceItem(
                        item = preference,
                        highlightKey = highlightKey,
                    )
                }
            }
        }
    }
}

private fun List<Preference>.findHighlightedIndex(highlightKey: String): Int {
    return flatMap {
        if (it is Preference.PreferenceGroup) {
            buildList<String?> {
                add(it.title) // Header
                addAll(it.preferenceItems.map { groupItem -> groupItem.title })
                add(null) // Spacer
            }
        } else {
            listOf(it.title)
        }
    }.indexOfFirst { it == highlightKey }
}

private val GroupCorner = 22.dp
private val GroupInnerCorner = 6.dp

private fun groupItemShape(index: Int, count: Int) = RoundedCornerShape(
    topStart = if (index == 0) GroupCorner else GroupInnerCorner,
    topEnd = if (index == 0) GroupCorner else GroupInnerCorner,
    bottomStart = if (index == count - 1) GroupCorner else GroupInnerCorner,
    bottomEnd = if (index == count - 1) GroupCorner else GroupInnerCorner,
)
