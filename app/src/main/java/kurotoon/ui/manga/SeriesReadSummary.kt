package kurotoon.ui.manga

import android.text.format.DateUtils
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
import kurotoon.app.di.appGraph
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.secondaryItemAlpha
import kotlin.time.Duration.Companion.milliseconds

/** "12 of 80 read · 3h 20m · last read 2 weeks ago" under the chapter count. Hidden until something has been read. */
@Composable
fun SeriesReadSummary(mangaId: Long, readCount: Int, total: Int) {
    if (readCount == 0 || total == 0) return
    val context = LocalContext.current
    val history by produceState(0L to 0L, mangaId, readCount) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val entries = context.appGraph.historyRepository.getHistoryByMangaId(mangaId)
                val spent = entries.sumOf { it.readDuration.coerceAtLeast(0) }
                val last = entries.maxOfOrNull { it.readAt?.toEpochMilliseconds() ?: 0L } ?: 0L
                spent to last
            }.getOrDefault(0L to 0L)
        }
    }
    val (spent, lastRead) = history
    val progress = stringResource(MR.strings.kurotoon_series_readCount, readCount, total)
    val text = if (spent >= 60_000) {
        val time = (spent / 60_000 * 60_000).milliseconds.toDurationString(context, fallback = "")
        "$progress · $time"
    } else {
        progress
    }
    // "Last read 3 weeks ago" for picking a series back up; skipped if read today
    val now = System.currentTimeMillis()
    val full = if (lastRead > 0 && now - lastRead >= DateUtils.DAY_IN_MILLIS) {
        val ago = DateUtils.getRelativeTimeSpanString(lastRead, now, DateUtils.DAY_IN_MILLIS).toString()
        "$text · " + stringResource(MR.strings.kurotoon_series_lastRead, ago)
    } else {
        text
    }
    Text(
        text = full,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.secondaryItemAlpha(),
    )
}
