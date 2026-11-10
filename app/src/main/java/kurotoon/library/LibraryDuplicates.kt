package kurotoon.library

import tachiyomi.domain.library.model.LibraryManga
import java.text.Normalizer

/** Series saved more than once, usually from different sources, found by their titles. */
object LibraryDuplicates {

    private val BRACKETS = Regex("""\([^)]*\)|\[[^]]*]|\{[^}]*}""")
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

    /** Groups of two or more entries sharing a title key; biggest groups first, then by title. */
    fun groups(library: List<LibraryManga>): List<List<LibraryManga>> =
        library.distinctBy { it.id }
            .groupBy { keyOf(it.manga.title) }
            .filterKeys { it.isNotEmpty() }
            .values
            .filter { it.size > 1 }
            // Most-read copy first: that's the one worth keeping
            .map { group -> group.sortedByDescending { it.readCount } }
            .sortedWith(
                compareByDescending<List<LibraryManga>> {
                    it.size
                }.thenBy { it.first().manga.title.lowercase() },
            )
}
