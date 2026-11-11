package kurotoon.ui.manga

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.tachiyomi.source.model.SManga
import kurotoon.reader.dayNames
import kurotoon.reader.rememberUpdateDays
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private val FINISHED = setOf(
    SManga.COMPLETED.toLong(),
    SManga.PUBLISHING_FINISHED.toLong(),
    SManga.CANCELLED.toLong(),
)

/** WEBTOON's "UP every TUE" under the chapter count, for ongoing series with a clear update day. */
@Composable
fun UpdateDaysLine(mangaId: Long, status: Long, chapterCount: Int) {
    if (status in FINISHED) return
    val days = rememberUpdateDays(mangaId, chapterCount)
    if (days.isEmpty()) return
    Text(
        text = stringResource(MR.strings.kurotoon_manga_updatesEvery, dayNames(days)),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}
