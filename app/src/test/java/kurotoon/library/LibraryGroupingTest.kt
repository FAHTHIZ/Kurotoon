package kurotoon.library

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LibraryGroupingTest {

    private val entries = listOf(
        GroupableEntry(1, "MangaDex", 2),
        GroupableEntry(2, "asura", 1),
        GroupableEntry(3, "MangaDex", 1),
        GroupableEntry(4, "Local", 0),
    )

    private fun group(mode: LibraryGroupBy) =
        LibraryGrouping.group(entries, mode, sortFlags = 0, allLabel = "All") { "s$it" }

    @Test
    fun `sources are grouped alphabetically ignoring case`() {
        val tabs = group(LibraryGroupBy.SOURCE)
        assertEquals(listOf("asura", "Local", "MangaDex"), tabs.keys.map { it.name })
        assertEquals(listOf(1L, 3L), tabs.entries.last().value)
    }

    @Test
    fun `statuses put ongoing first and unknown last`() {
        val tabs = group(LibraryGroupBy.STATUS)
        assertEquals(listOf("s1", "s2", "s0"), tabs.keys.map { it.name })
        assertEquals(listOf(2L, 3L), tabs.values.first())
    }

    @Test
    fun `none puts everything in one tab`() {
        val tabs = group(LibraryGroupBy.NONE)
        assertEquals(1, tabs.size)
        assertEquals(4, tabs.values.single().size)
    }

    @Test
    fun `generated tabs never clash with real categories`() {
        val ids = group(LibraryGroupBy.SOURCE).keys.map { it.id }
        assertTrue(ids.all { it < 0 })
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `unknown values fall back to categories`() {
        assertEquals(LibraryGroupBy.CATEGORY, LibraryGrouping.parse("nope"))
        assertEquals(LibraryGroupBy.SOURCE, LibraryGrouping.parse("source"))
    }
}
