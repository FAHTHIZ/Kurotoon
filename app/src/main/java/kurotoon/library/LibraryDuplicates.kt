package kurotoon.library

import tachiyomi.domain.library.model.LibraryManga
import java.text.Normalizer

/** Series saved more than once, usually from different sources, found by their titles. */
object LibraryDuplicates {

    private val BRACKETS = Regex("""\([^)]*\)|\[[^\]]*\]|\{[^}]*\}""")
    private val MARKS = Regex("""\p{M}+""")
    private val NOT_WORD = Regex("""[^\p{L}\p{N}]+""")

    /** A title reduced to its letters and digits, so "Solo Leveling (Official)" matches "solo-leveling". */
    fun keyOf(title: String): String =
        Normalizer.normalize(title.replace(BRACKETS, " "), Normalizer.Form.NFKD)
            .replace(MARKS, "")
            // Recompose so Hangul and other scripts split by NFKD come back whole
            .let { Normalizer.normalize(it, Normalizer.Form.NFC) }
            .lowercase()
            .replace(NOT_WORD, "")

    /**
     * Groups of two or more entries that share a title key or a tracker entry (same tracker, same remote
     * id, mihon#84), so copies with different titles are still caught. Biggest groups first, then by title.
     *
     * [trackKeys] maps a manga id to its tracker keys, such as "2:12345".
     */
    fun groups(
        library: List<LibraryManga>,
        trackKeys: Map<Long, Collection<String>> = emptyMap(),
    ): List<List<LibraryManga>> {
        val entries = library.distinctBy { it.id }
        val parent = IntArray(entries.size) { it }
        fun find(i: Int): Int {
            var x = i
            while (parent[x] != x) {
                parent[x] = parent[parent[x]]
                x = parent[x]
            }
            return x
        }
        fun join(indices: List<Int>) = indices.drop(1).forEach { parent[find(it)] = find(indices.first()) }

        entries.indices.groupBy { keyOf(entries[it].manga.title) }
            .filterKeys { it.isNotEmpty() }
            .values.forEach(::join)
        entries.indices
            .flatMap { i -> trackKeys[entries[i].id].orEmpty().map { it to i } }
            .groupBy({ it.first }, { it.second })
            .values.forEach(::join)

        return entries.indices.groupBy(::find).values
            .filter { it.size > 1 }
            // Most-read copy first: that's the one worth keeping
            .map { group -> group.map(entries::get).sortedByDescending { it.readCount } }
            .sortedWith(
                compareByDescending<List<LibraryManga>> {
                    it.size
                }.thenBy { it.first().manga.title.lowercase() },
            )
    }
}
