package tachiyomi.domain.history.model

import kotlin.time.Instant

/** One chapter's history row with the series it belongs to, for reading summaries. */
data class ReadEntry(
    val readAt: Instant,
    val duration: Long,
    val mangaId: Long,
    val title: String,
    val genres: List<String>,
)
