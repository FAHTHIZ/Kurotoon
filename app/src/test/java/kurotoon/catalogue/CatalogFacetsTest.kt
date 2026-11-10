package kurotoon.catalogue

import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CatalogFacetsTest {

    private class Genre(name: String) : Filter.TriState(name)
    private class Theme(name: String) : Filter.CheckBox(name)
    private class GenreGroup(genres: List<Genre>) : Filter.Group<Genre>("Genres", genres)
    private class ThemeGroup(themes: List<Theme>) : Filter.Group<Theme>("Themes", themes)
    private class Status : Filter.Select<String>("Status", arrayOf("Any", "Ongoing", "Completed"))
    private class Type : Filter.Select<String>("Type", arrayOf("Manga", "Manhwa", "Manhua"), state = 1)
    private class OrderBy : Filter.Sort("Sort", arrayOf("Popular", "Latest", "Title"))
    private class Adult : Filter.CheckBox("Adult only")
    private class Author : Filter.Text("Author")

    private fun webtoonSite() = FilterList(
        Author(),
        Adult(),
        Status(),
        OrderBy(),
        ThemeGroup(listOf(Theme("Isekai"), Theme("Murim"), Theme("Regression"))),
        GenreGroup(listOf(Genre("Action"), Genre("Romance"), Genre("Comedy"))),
        Type(),
    )

    @Test
    fun `reads every browsable facet, genres first`() {
        val facets = CatalogFacets.from(webtoonSite())
        facets.map { it.title } shouldBe listOf("Genres", "Status", "Themes", "Type")
        (facets[0] as CatalogFacet.Tags).excludable shouldBe true
        (facets[2] as CatalogFacet.Tags).excludable shouldBe false
        (facets[3] as CatalogFacet.Choice).defaultIndex shouldBe 1
    }

    @Test
    fun `tags cycle through include, exclude and off`() {
        val filters = webtoonSite()
        val genres = CatalogFacets.from(filters).first() as CatalogFacet.Tags
        val action = genres.options.first().childIndex
        CatalogFacets.cycleTag(filters, genres.filterIndex, action)
        CatalogFacets.stateOf(filters, genres.filterIndex, action) shouldBe FacetState.Included
        CatalogFacets.cycleTag(filters, genres.filterIndex, action)
        CatalogFacets.stateOf(filters, genres.filterIndex, action) shouldBe FacetState.Excluded
        CatalogFacets.cycleTag(filters, genres.filterIndex, action)
        CatalogFacets.stateOf(filters, genres.filterIndex, action) shouldBe FacetState.Off
    }

    @Test
    fun `checkbox tags only toggle`() {
        val filters = webtoonSite()
        val themes = CatalogFacets.from(filters).first { it.title == "Themes" } as CatalogFacet.Tags
        CatalogFacets.cycleTag(filters, themes.filterIndex, 1)
        CatalogFacets.stateOf(filters, themes.filterIndex, 1) shouldBe FacetState.Included
        CatalogFacets.cycleTag(filters, themes.filterIndex, 1)
        CatalogFacets.stateOf(filters, themes.filterIndex, 1) shouldBe FacetState.Off
    }

    @Test
    fun `active lists applied values and skips choice defaults`() {
        val filters = webtoonSite()
        val facets = CatalogFacets.from(filters)
        CatalogFacets.active(facets, filters) shouldBe emptyList()

        val genres = facets.first() as CatalogFacet.Tags
        CatalogFacets.cycleTag(filters, genres.filterIndex, 1)
        CatalogFacets.excludeTag(filters, genres.filterIndex, 2)
        val status = facets.first { it.title == "Status" }
        CatalogFacets.selectChoice(filters, status.filterIndex, 2)

        CatalogFacets.active(facets, filters).map { it.label to it.excluded } shouldBe listOf(
            "Romance" to false,
            "Comedy" to true,
            "Completed" to false,
        )
        CatalogFacets.activeCount(genres, filters) shouldBe 2

        // Picking the selected choice again returns to the source's default
        CatalogFacets.selectChoice(filters, status.filterIndex, 2)
        CatalogFacets.choiceOf(filters, status.filterIndex) shouldBe 0
    }

    @Test
    fun `copies applied state onto a fresh filter list`() {
        val applied = webtoonSite()
        val genres = CatalogFacets.from(applied).first() as CatalogFacet.Tags
        CatalogFacets.cycleTag(applied, genres.filterIndex, 0)
        (applied[0] as Filter.Text).state = "Kim"

        val fresh = webtoonSite().copyStateFrom(applied)
        CatalogFacets.stateOf(fresh, genres.filterIndex, 0) shouldBe FacetState.Included
        (fresh[0] as Filter.Text).state shouldBe "Kim"
        // The original is untouched by edits to the copy
        CatalogFacets.cycleTag(fresh, genres.filterIndex, 1)
        CatalogFacets.stateOf(applied, genres.filterIndex, 1) shouldBe FacetState.Off
    }
}
