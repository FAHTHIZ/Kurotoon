package kurotoon.library

import androidx.compose.runtime.Immutable

/** A genre found in the library and how many saved series carry it. */
@Immutable
data class LibraryTag(val key: String, val name: String, val count: Int)

/** Builds the library's own tag chips from the genres of the series in it. */
object LibraryTags {

    const val LIMIT = 30

    fun keyOf(tag: String): String = tag.trim().lowercase()

    /** Most common genres first, ties alphabetical; the first spelling seen is the one shown. */
    fun top(genres: List<List<String>?>, limit: Int = LIMIT): List<LibraryTag> {
        val names = linkedMapOf<String, String>()
        val counts = mutableMapOf<String, Int>()
        genres.forEach { list ->
            list.orEmpty().map { it.trim() }.filter { it.isNotEmpty() }.distinctBy(::keyOf).forEach { tag ->
                val key = keyOf(tag)
                names.putIfAbsent(key, tag)
                counts[key] = (counts[key] ?: 0) + 1
            }
        }
        return counts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(limit)
            .map { LibraryTag(it.key, names.getValue(it.key), it.value) }
    }

    /** True when [genres] has every tag in [selected] (keys from [keyOf]). */
    fun matches(genres: List<String>?, selected: Set<String>): Boolean {
        if (selected.isEmpty()) return true
        val keys = genres.orEmpty().mapTo(HashSet(), ::keyOf)
        return selected.all { it in keys }
    }
}
