package kurotoon.library

import eu.kanade.tachiyomi.source.model.SManga
import tachiyomi.domain.library.model.LibraryManga

/** Finished series sitting in the library unread: the ones worth a weekend. */
object BingeList {

    const val LIMIT = 15

    private val FINISHED = setOf(SManga.COMPLETED.toLong(), SManga.PUBLISHING_FINISHED.toLong())

    fun of(library: List<LibraryManga>, limit: Int = LIMIT): List<LibraryManga> =
        library.asSequence()
            .filter { it.manga.status in FINISHED && !it.hasStarted && it.totalChapters > 0 }
            .distinctBy { it.id }
            // Most recently saved first, as they're freshest in mind
            .sortedByDescending { it.manga.favoriteAt }
            .take(limit)
            .toList()
}
