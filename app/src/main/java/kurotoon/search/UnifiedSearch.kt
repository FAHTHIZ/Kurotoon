package kurotoon.search

import kurotoon.library.LibraryDuplicates
import tachiyomi.domain.manga.model.Manga

/**
 * Folds every source's answer to one query into a single list: the same series found on several
 * sources becomes one hit, best match first.
 */
object UnifiedSearch {

    /**
     * One series, however many sources carry it. [primary] is the copy a tap opens: the one in the
     * library if there is one, otherwise the first source to list it. [copies] has one entry per source.
     */
    data class Hit(val key: String, val primary: Manga, val copies: List<Manga>) {
        val sourceCount: Int get() = copies.size
        val inLibrary: Boolean get() = primary.favorite
    }

    /**
     * [results] is each source's list in the order the sources are shown; [library] is checked too so
     * series you already follow surface even when no source answered with them.
     */
    fun merge(results: List<List<Manga>>, query: String, library: List<Manga> = emptyList()): List<Hit> {
        val wanted = LibraryDuplicates.keyOf(query)
        val groups = LinkedHashMap<String, MutableList<Manga>>()
        val order = HashMap<String, Int>()
        var position = 0
        // Round-robin so the first source's tenth answer doesn't outrank the second source's first
        val depth = results.maxOfOrNull { it.size } ?: 0
        for (rank in 0..<depth) {
            for (list in results) {
                val manga = list.getOrNull(rank) ?: continue
                add(groups, order, manga, position++)
            }
        }
        if (wanted.isNotEmpty()) {
            library.filter { LibraryDuplicates.keyOf(it.title).contains(wanted) }
                .forEach { add(groups, order, it, position++) }
        }
        return groups.map { (key, copies) ->
            val distinct = copies.distinctBy { it.source }
            Hit(key, distinct.firstOrNull { it.favorite } ?: distinct.first(), distinct)
        }
            .sortedWith(
                compareByDescending<Hit> { relevance(it.key, wanted) }
                    .thenByDescending { it.inLibrary && relevance(it.key, wanted) > 0 }
                    .thenByDescending { it.sourceCount }
                    .thenBy { order[it.key] ?: Int.MAX_VALUE },
            )
    }

    /** 3 for the exact title, 2 when it starts with the query, 1 when it contains it, 0 otherwise. */
    fun relevance(key: String, wanted: String): Int = when {
        wanted.isEmpty() -> 0
        key == wanted -> 3
        key.startsWith(wanted) -> 2
        key.contains(wanted) -> 1
        else -> 0
    }

    private fun add(
        groups: MutableMap<String, MutableList<Manga>>,
        order: MutableMap<String, Int>,
        manga: Manga,
        at: Int,
    ) {
        // Titles with nothing left after normalising (all symbols) can't be matched, so keep them apart
        val key = LibraryDuplicates.keyOf(manga.title).ifEmpty { "#${manga.source}:${manga.url}" }
        groups.getOrPut(key) { mutableListOf() }.add(manga)
        order.putIfAbsent(key, at)
    }
}
