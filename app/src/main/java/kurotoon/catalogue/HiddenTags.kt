package kurotoon.catalogue

import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList

/**
 * Tags the reader never wants to see, typed once in settings. They are excluded from every
 * filtered search on sources that support excluding, and titles carrying them are dropped
 * from browse results and Home rows whenever their genres are known.
 */
object HiddenTags {

    /** Comma- or newline-separated tags, compared case-insensitively. */
    fun parse(raw: String): Set<String> =
        raw.split(',', '\n').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

    /** The tags as typed, first spelling kept, for showing as chips. */
    fun names(raw: String): List<String> =
        raw.split(',', '\n').map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }

    /** [raw] without [tag] (ignoring case), rewritten as a tidy comma list. */
    fun remove(raw: String, tag: String): String =
        names(raw).filterNot { it.equals(tag.trim(), ignoreCase = true) }.joinToString(", ")

    /** True when any of [genres] is hidden. */
    fun hides(genres: List<String>?, hidden: Set<String>): Boolean =
        hidden.isNotEmpty() && genres.orEmpty().any { it.trim().lowercase() in hidden }

    /**
     * Sets every untouched tri-state tag named in [hidden] to excluded. Tags the reader set by
     * hand are left alone. Mutates [filters] in place, so pass a copy. Returns how many changed.
     */
    fun excludeIn(filters: FilterList, hidden: Set<String>): Int {
        if (hidden.isEmpty()) return 0
        var changed = 0
        filters.forEach { filter ->
            if (filter !is Filter.Group<*>) return@forEach
            filter.state.forEach { child ->
                if (
                    child is Filter.TriState &&
                    child.state == Filter.TriState.STATE_IGNORE &&
                    child.name.trim().lowercase() in hidden
                ) {
                    child.state = Filter.TriState.STATE_EXCLUDE
                    changed++
                }
            }
        }
        return changed
    }
}
