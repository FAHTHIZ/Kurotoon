package kurotoon.library

import tachiyomi.domain.category.model.Category

/** How the library splits into tabs. [CATEGORY] is the user's own categories, the others are built on the fly. */
enum class LibraryGroupBy { CATEGORY, SOURCE, STATUS, NONE }

/** One entry the grouping needs to know about: its id, source name and publishing status. */
data class GroupableEntry(val id: Long, val sourceName: String, val status: Long)

object LibraryGrouping {

    /**
     * Ids for the generated tabs. They sit far below zero so they never clash with real
     * categories (positive) or the default category (0).
     */
    private const val GROUP_ID_BASE = -1_000_000L

    /**
     * Builds the tabs for any mode except [LibraryGroupBy.CATEGORY]. Source tabs are alphabetical,
     * status tabs follow the order of [statusOrder], and [NONE][LibraryGroupBy.NONE] puts everything in one tab.
     */
    fun group(
        entries: List<GroupableEntry>,
        mode: LibraryGroupBy,
        sortFlags: Long,
        allLabel: String,
        statusLabel: (Long) -> String,
    ): Map<Category, List<Long>> {
        fun tab(index: Int, name: String) = Category(GROUP_ID_BASE - index, name, index.toLong(), sortFlags)
        return when (mode) {
            LibraryGroupBy.CATEGORY -> emptyMap()
            LibraryGroupBy.NONE -> mapOf(tab(0, allLabel) to entries.map { it.id })
            LibraryGroupBy.SOURCE -> entries.groupBy { it.sourceName }
                .toSortedMap(String.CASE_INSENSITIVE_ORDER)
                .entries
                .withIndex()
                .associate { (index, group) -> tab(index, group.key) to group.value.map { it.id } }
            LibraryGroupBy.STATUS -> entries.groupBy { it.status }
                .entries
                .sortedBy { statusOrder.indexOf(it.key).let { order -> if (order < 0) Int.MAX_VALUE else order } }
                .withIndex()
                .associate { (index, group) -> tab(index, statusLabel(group.key)) to group.value.map { it.id } }
        }
    }

    /** Ongoing first since those are the ones still getting chapters, unknown last. */
    val statusOrder = listOf(1L, 6L, 4L, 2L, 5L, 3L, 0L)

    fun parse(value: String): LibraryGroupBy =
        LibraryGroupBy.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: LibraryGroupBy.CATEGORY
}
