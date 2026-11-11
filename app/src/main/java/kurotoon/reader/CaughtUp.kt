package kurotoon.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.todayIn
import kurotoon.schedule.ReleaseSchedule
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.format.TextStyle
import java.util.Locale
import kotlin.time.Clock

/**
 * What the end-of-series card says once there's no next chapter: "You're all caught up", plus the
 * days new chapters usually land on when the upload history shows a pattern, the way WEBTOON
 * tells you "Up next on Tuesday".
 */
@Composable
fun caughtUpLabel(mangaId: Long): String {
    val days by produceState(emptyList<DayOfWeek>(), mangaId) {
        if (mangaId <= 0) return@produceState
        value = runCatching {
            withContext(Dispatchers.IO) {
                val uploads = Injekt.get<GetChaptersByMangaId>().await(mangaId).mapNotNull { it.dateUpload }
                val zone = TimeZone.currentSystemDefault()
                ReleaseSchedule.weekdays(uploads, Clock.System.todayIn(zone), zone)
            }
        }.getOrDefault(emptyList())
    }
    if (LocalInspectionMode.current || days.isEmpty()) return stringResource(MR.strings.kurotoon_reader_caughtUp)
    val names = days.joinToString(" & ") {
        java.time.DayOfWeek.of(it.isoDayNumber).getDisplayName(TextStyle.FULL, Locale.getDefault())
    }
    return stringResource(MR.strings.kurotoon_reader_caughtUpOn, names)
}
