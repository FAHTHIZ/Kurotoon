package kurotoon.library

import eu.kanade.tachiyomi.source.model.SManga
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

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

/** Series started, with chapters left, but not opened in a while: easy to forget about. */
object StalledList {

    const val LIMIT = 15
    val AFTER = 21.days

    fun of(library: List<LibraryManga>, now: Instant, limit: Int = LIMIT): List<LibraryManga> =
        library.asSequence()
            .filter { it.hasStarted && it.unreadCount > 0 }
            .filter { entry -> entry.lastRead?.let { now - it >= AFTER } == true }
            .distinctBy { it.id }
            // Most recently dropped first; the oldest are likely abandoned on purpose
            .sortedByDescending { it.lastRead }
            .take(limit)
            .toList()
}

/** Series saved in the last few weeks, newest first, so a fresh add is easy to find again (mihon#2529). */
object RecentlyAddedList {

    const val LIMIT = 15
    val WITHIN = 30.days

    fun of(library: List<LibraryManga>, now: Instant, limit: Int = LIMIT): List<LibraryManga> =
        library.asSequence()
            .filter { entry ->
                val added = entry.manga.favoriteAt ?: return@filter false
                added != Manga.UNKNOWN_FAVORITE_AT && now - added <= WITHIN
            }
            .distinctBy { it.id }
            .sortedByDescending { it.manga.favoriteAt }
            .take(limit)
            .toList()
}
