package kurotoon.ui.manga

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.util.toDurationString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.app.di.appGraph
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.secondaryItemAlpha
import kotlin.time.Duration.Companion.milliseconds

/** "12 of 80 read · 3h 20m" under the chapter count. Hidden until something has been read. */
@Composable
fun SeriesReadSummary(mangaId: Long, readCount: Int, total: Int) {
    if (readCount == 0 || total == 0) return
    val context = LocalContext.current
    val spent by produceState(0L, mangaId, readCount) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.appGraph.historyRepository.getHistoryByMangaId(mangaId)
                    .sumOf { it.readDuration.coerceAtLeast(0) }
            }.getOrDefault(0L)
        }
    }
    val progress = stringResource(MR.strings.kurotoon_series_readCount, readCount, total)
    val text = if (spent >= 60_000) {
        val time = (spent / 60_000 * 60_000).milliseconds.toDurationString(context, fallback = "")
        "$progress · $time"
    } else {
        progress
    }
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.secondaryItemAlpha(),
    )
}
