package kurotoon.catalogue

/** Splits the tag index into what the Browse › Tags page shows. */
object TagSections {

    data class Section(val label: String, val tags: List<SourceTagIndex.Tag>)

    /** Tags not hidden, narrowed by [query]: prefix matches first, then by how many sources offer them. */
    fun visible(tags: List<SourceTagIndex.Tag>, hidden: Set<String>, query: String): List<SourceTagIndex.Tag> {
        val shown = tags.filter { it.name.trim().lowercase() !in hidden }
        return if (query.isBlank()) shown else SourceTagIndex.matching(shown, query, Int.MAX_VALUE)
    }

    /** One section per first letter, A to Z; digits and other scripts go under "#". */
    fun alphabetical(tags: List<SourceTagIndex.Tag>): List<Section> =
        tags.sortedBy { it.name.lowercase() }
            .groupBy { tag ->
                val first = tag.name.trim().firstOrNull()?.uppercaseChar()
                if (first != null && first in 'A'..'Z') first.toString() else "#"
            }
            .toSortedMap(compareBy<String> { it == "#" }.thenBy { it })
            .map { (label, list) -> Section(label, list) }
}
