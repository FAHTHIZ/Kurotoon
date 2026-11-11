package kurotoon.library

import eu.kanade.tachiyomi.ui.library.LibraryItem
import kurotoon.manga.MergedChapters

/**
 * Shows a series once in the library even when it's saved from several sources.
 *
 * Copies are the same series when their titles match (see [LibraryDuplicates.keyOf]) and their
 * authors agree. The copy you read most recently stands for the group, carrying a ×N badge; its
 * series page then offers the merged chapter list of every copy.
 */
object LibraryCombine {

    fun combine(items: List<LibraryItem>): List<LibraryItem> {
        val byTitle = items.groupBy { LibraryDuplicates.keyOf(it.libraryManga.manga.title) }
        val keep = HashMap<Long, Int>()
        byTitle.forEach { (key, group) ->
            if (key.isEmpty() || group.size == 1) {
                group.forEach { keep[it.id] = 1 }
                return@forEach
            }
            clusters(group).forEach { cluster ->
                val lead = cluster.maxWith(
                    compareBy<LibraryItem> { it.libraryManga.lastRead }
                        .thenBy { it.libraryManga.readCount }
                        .thenBy { it.libraryManga.totalChapters },
                )
                keep[lead.id] = cluster.size
            }
        }
        return items.mapNotNull { item ->
            val copies = keep[item.id] ?: return@mapNotNull null
            if (copies == 1) item else item.copy(badges = item.badges.copy(copies = copies))
        }
    }

    /** Splits same-title entries whose authors are known and differ ("Solo Leveling" by two people). */
    private fun clusters(group: List<LibraryItem>): List<List<LibraryItem>> {
        val result = mutableListOf<MutableList<LibraryItem>>()
        group.forEach { item ->
            val author = item.libraryManga.manga.author
            val home = result.firstOrNull { cluster ->
                cluster.all { MergedChapters.authorsAgree(it.libraryManga.manga.author, author) }
            }
            if (home != null) home += item else result += mutableListOf(item)
        }
        return result
    }
}
