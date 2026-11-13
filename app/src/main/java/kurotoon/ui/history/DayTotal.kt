package kurotoon.ui.history

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.util.toDurationString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kurotoon.app.di.appGraph
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.i18n.MR
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/** Chapters opened and time read on one day. */
data class DayTotal(val chapters: Int, val millis: Long)

/** Per-day totals for the last [days] days, keyed by ISO date. Reloads when [key] changes. */
@Composable
fun rememberDayTotals(key: Any?, days: Int = 120): State<Map<String, DayTotal>> {
    val context = LocalContext.current
    return produceState(emptyMap(), key) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val since = Instant.fromEpochMilliseconds(System.currentTimeMillis()) - days.days
                totalsByDay(context.appGraph.historyRepository.getReadActivity(since), ZoneId.systemDefault())
            }.getOrDefault(emptyMap())
        }
    }
}

fun totalsByDay(entries: List<Pair<Instant, Long>>, zone: ZoneId): Map<String, DayTotal> =
    entries
        .groupBy { (at, _) -> java.time.Instant.ofEpochMilli(at.toEpochMilliseconds()).atZone(zone).toLocalDate() }
        .map { (date: LocalDate, list) ->
            date.toString() to DayTotal(list.size, list.sumOf { it.second.coerceAtLeast(0) })
        }
        .toMap()

/** "5 chapters · 42m", or just the chapters when under a minute was tracked. */
fun DayTotal.label(context: Context): String {
    val chapters = context.pluralStringResource(MR.plurals.kurotoon_history_chapters, chapters, chapters)
    if (millis < 60_000) return chapters
    val time = (millis / 60_000 * 60_000).milliseconds.toDurationString(context, fallback = "")
    return "$chapters · $time"
}
