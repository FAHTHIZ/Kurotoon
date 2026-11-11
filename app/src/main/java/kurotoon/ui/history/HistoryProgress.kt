package kurotoon.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.app.di.appGraph
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * "Finished" or "p. 12" for a history row, so a chapter you stopped halfway through looks different
 * from one you read to the end (mihon#146). Null while loading or when there's nothing to add.
 */
@Composable
fun rememberHistoryProgress(chapterId: Long, readAt: Any?): String? {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    val state by produceState<Pair<Boolean, Int>?>(null, chapterId, readAt) {
        if (inPreview) return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching { context.appGraph.getChapter.await(chapterId) }.getOrNull()
                ?.let { it.read to it.lastPageRead }
        }
    }
    val (read, lastPage) = state ?: return null
    return when {
        read -> stringResource(MR.strings.kurotoon_history_finished)
        lastPage > 0 -> stringResource(MR.strings.kurotoon_history_page, lastPage + 1)
        else -> null
    }
}
