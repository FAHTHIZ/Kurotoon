package kurotoon.updates

/**
 * Folds several chapters of one series fetched on the same day into one row, so a big release dump
 * doesn't push everything else off screen. Order of first appearance is kept.
 */
object UpdateGroups {

    data class Row<T>(val item: T, val key: String, val hidden: Int, val expanded: Boolean)

    fun <T> fold(items: List<T>, key: (T) -> String, expanded: Set<String>): List<Row<T>> {
        val groups = LinkedHashMap<String, MutableList<T>>()
        items.forEach { groups.getOrPut(key(it)) { mutableListOf() }.add(it) }
        return groups.flatMap { (k, list) ->
            when {
                list.size == 1 -> listOf(Row(list[0], k, 0, false))
                k in expanded -> list.mapIndexed { i, it -> Row(it, k, if (i == 0) list.size - 1 else 0, true) }
                else -> listOf(Row(list[0], k, list.size - 1, false))
            }
        }
    }
}
