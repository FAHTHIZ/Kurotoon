package kurotoon.ui.manga

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.manga.ChapterList
import kotlinx.coroutines.launch
import kurotoon.icons.materialsymbols.MaterialSymbols
import kurotoon.icons.materialsymbols.rounded.History
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** Lists shorter than this fit on a couple of screens, so the button would only add noise. */
const val JUMP_MIN_CHAPTERS = 20

/**
 * A small button that scrolls the chapter list to the next chapter you'd read, shown only while that
 * row is off screen in a long list. Saves flinging through hundreds of rows to find your place.
 *
 * [headerCount] is the number of list items above the first chapter row.
 */
@Composable
fun JumpToChapterButton(
    listState: LazyListState,
    chapters: List<ChapterList>,
    targetId: Long?,
    headerCount: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val items by rememberUpdatedState(chapters)
    val target by rememberUpdatedState(targetId)
    val index by remember {
        derivedStateOf {
            val id = target ?: return@derivedStateOf -1
            if (items.size < JUMP_MIN_CHAPTERS) return@derivedStateOf -1
            val row = items.indexOfFirst { it is ChapterList.Item && it.id == id }
            if (row < 0) -1 else row + headerCount
        }
    }
    val offScreen by remember {
        derivedStateOf {
            val i = index
            i >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == i }
        }
    }
    if (!enabled || !offScreen) return
    SmallFloatingActionButton(
        onClick = { scope.launch { listState.animateScrollToItem((index - 1).coerceAtLeast(0)) } },
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Icon(
            MaterialSymbols.Rounded.History,
            contentDescription = stringResource(MR.strings.kurotoon_manga_jumpToChapter),
        )
    }
}
