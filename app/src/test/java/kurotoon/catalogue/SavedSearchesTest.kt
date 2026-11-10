package kurotoon.catalogue

import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SavedSearchesTest {

    private class Genre(name: String) : Filter.TriState(name)
    private class Genres : Filter.Group<Genre>("Genres", listOf(Genre("Action"), Genre("Romance"), Genre("Drama")))
    private class Status : Filter.Select<String>("Status", arrayOf("Any", "Ongoing", "Completed"))
    private class OrderBy : Filter.Sort("Sort", arrayOf("Popular", "Latest"), Selection(0, false))
    private class Author : Filter.Text("Author")
    private class Header : Filter.Header("Note")

    private fun filters() = FilterList(Header(), Genres(), Status(), OrderBy(), Author())

    @Test
    fun `round-trips every filter kind through json`() {
        val applied = filters()
        (applied[1] as Genres).state[1].state = Filter.TriState.STATE_INCLUDE
        (applied[1] as Genres).state[2].state = Filter.TriState.STATE_EXCLUDE
        (applied[2] as Status).state = 2
        (applied[3] as OrderBy).state = Filter.Sort.Selection(1, true)
        (applied[4] as Author).state = "Kim"

        val saved = SavedSearch(1, 7, "Romance", query = "love", filters = SavedSearches.snapshot(applied))
        val decoded = SavedSearches.decode(SavedSearches.encode(listOf(saved))).single()
        decoded shouldBe saved

        val restored = SavedSearches.restore(filters(), decoded.filters)
        (restored[1] as Genres).state.map { it.state } shouldBe listOf(0, 1, 2)
        (restored[2] as Status).state shouldBe 2
        (restored[3] as OrderBy).state shouldBe Filter.Sort.Selection(1, true)
        (restored[4] as Author).state shouldBe "Kim"
        SavedSearches.snapshot(restored) shouldBe saved.filters
    }

    @Test
    fun `skips filters that changed since saving`() {
        val saved = SavedSearches.snapshot(filters().also { (it[2] as Status).state = 9 })
        val restored = SavedSearches.restore(FilterList(Header(), Genres(), Author()), saved)
        (restored[2] as Author).state shouldBe ""
    }

    @Test
    fun `bad json gives no saved searches`() {
        SavedSearches.decode("") shouldBe emptyList()
        SavedSearches.decode("{nope") shouldBe emptyList()
    }

    @Test
    fun `suggests a readable name`() {
        val active = listOf(
            ActiveFacet(1, 1, "Romance", false),
            ActiveFacet(1, 2, "Drama", true),
            ActiveFacet(2, 2, "Completed", false),
        )
        SavedSearches.suggestName("love", active) shouldBe "“love” + Romance + Completed + −Drama"
    }
}
