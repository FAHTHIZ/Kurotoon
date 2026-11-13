package kurotoon.reader

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.todayIn
import kurotoon.app.di.appGraph
import kurotoon.schedule.ReleaseSchedule
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.time.format.TextStyle
import java.util.Locale
import kotlin.time.Clock

/** The weekdays [mangaId] usually gets chapters on, from its upload history; empty while unknown. */
@Composable
fun rememberUpdateDays(mangaId: Long, refreshKey: Any? = null): List<DayOfWeek> {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    val days by produceState(emptyList<DayOfWeek>(), mangaId, refreshKey) {
        if (mangaId <= 0 || inPreview) return@produceState
        value = withContext(Dispatchers.IO) { updateDays(context, mangaId) }
    }
    return days
}

private suspend fun updateDays(context: Context, mangaId: Long): List<DayOfWeek> = runCatching {
    val uploads = context.appGraph.getChaptersByMangaId.await(mangaId).mapNotNull { it.dateUpload }
    val zone = TimeZone.currentSystemDefault()
    ReleaseSchedule.weekdays(uploads, Clock.System.todayIn(zone), zone)
}.getOrDefault(emptyList())

/** "Tuesday" or "Tuesday & Friday". */
fun dayNames(days: List<DayOfWeek>): String = days.joinToString(" & ") {
    java.time.DayOfWeek.of(it.isoDayNumber).getDisplayName(TextStyle.FULL, Locale.getDefault())
}

/**
 * What the end-of-series card says once there's no next chapter: "You're all caught up", plus the
 * days new chapters usually land on when the upload history shows a pattern, the way WEBTOON
 * tells you "Up next on Tuesday".
 */
@Composable
fun caughtUpLabel(mangaId: Long): String {
    val days = rememberUpdateDays(mangaId)
    if (days.isEmpty()) return stringResource(MR.strings.kurotoon_reader_caughtUp)
    return stringResource(MR.strings.kurotoon_reader_caughtUpOn, dayNames(days))
}
