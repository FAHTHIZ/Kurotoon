package kurotoon.track

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kurotoon.app.di.appGraph
import kurotoon.ui.KuroAlertDialog
import logcat.LogPriority
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.KuroTextButton
import tachiyomi.presentation.core.i18n.stringResource
import java.util.Locale

/** Links many library series to one tracker using its closest search match. */
object MassTrack {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Trackers that can be searched by title; enhanced trackers link on their own. */
    fun searchableTrackers(context: Context): List<Tracker> =
        context.appGraph.trackerManager.loggedInTrackers().filterNot { it is EnhancedTracker }

    fun start(context: Context, manga: List<Manga>, tracker: Tracker) {
        val app = context.applicationContext
        app.toast(app.pluralStringResource(MR.plurals.kurotoon_track_massStarted, manga.size, manga.size, tracker.name))
        scope.launch {
            var linked = 0
            var skipped = 0
            var unmatched = 0
            val graph = app.appGraph
            for (series in manga) {
                try {
                    val existing = graph.trackRepository.getTracksByMangaId(series.id)
                    if (existing.any { it.trackerId == tracker.id }) {
                        skipped++
                        continue
                    }
                    val match = bestMatch(series.title, tracker.search(series.title))
                    if (match == null) {
                        unmatched++
                    } else {
                        match.manga_id = series.id
                        graph.addTracks.bind(tracker, match, series.id)
                        linked++
                    }
                } catch (e: Exception) {
                    unmatched++
                    logcat(LogPriority.WARN, e) { "Mass track failed for ${series.title} on ${tracker.name}" }
                }
                // Stay polite with tracker rate limits.
                delay(SEARCH_SPACING_MS)
            }
            withContext(Dispatchers.Main) {
                app.toast(
                    app.stringResource(MR.strings.kurotoon_track_massDone, linked, unmatched, skipped),
                    android.widget.Toast.LENGTH_LONG,
                )
            }
        }
    }

    /** Exact (normalised) title first, then a title that contains the query, then the top result. */
    internal fun <T : eu.kanade.tachiyomi.data.database.models.Track> bestMatch(title: String, results: List<T>): T? {
        if (results.isEmpty()) return null
        val wanted = normalise(title)
        return results.firstOrNull { normalise(it.title) == wanted }
            ?: results.firstOrNull { normalise(it.title).contains(wanted) || wanted.contains(normalise(it.title)) }
            ?: results.first()
    }

    internal fun normalise(text: String): String =
        text.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

    private const val SEARCH_SPACING_MS = 700L
}

@Composable
fun MassTrackDialog(
    trackers: List<Tracker>,
    onPick: (Tracker) -> Unit,
    onDismiss: () -> Unit,
) {
    KuroAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(MR.strings.kurotoon_track_massTitle)) },
        text = {
            Column {
                Text(stringResource(MR.strings.kurotoon_track_massSummary))
                trackers.forEach { tracker ->
                    Text(
                        text = tracker.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(tracker) }
                            .padding(vertical = 14.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            KuroTextButton(onClick = onDismiss) { Text(stringResource(MR.strings.action_cancel)) }
        },
    )
}
