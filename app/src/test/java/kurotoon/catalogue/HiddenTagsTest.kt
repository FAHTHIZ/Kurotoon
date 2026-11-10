package kurotoon.catalogue

import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class HiddenTagsTest {

    private class Tag(name: String, state: Int = STATE_IGNORE) : Filter.TriState(name, state)
    private class Genres(tags: List<Tag>) : Filter.Group<Tag>("Genres", tags)

    @Test
    fun `parses a loose list`() {
        HiddenTags.parse(" Ecchi, Harem\nGORE ,, ") shouldBe setOf("ecchi", "harem", "gore")
        HiddenTags.parse("") shouldBe emptySet()
    }

    @Test
    fun `hides titles by genre ignoring case`() {
        val hidden = setOf("ecchi")
        HiddenTags.hides(listOf("Action", "Ecchi"), hidden) shouldBe true
        HiddenTags.hides(listOf("Action"), hidden) shouldBe false
        HiddenTags.hides(null, hidden) shouldBe false
        HiddenTags.hides(listOf("Ecchi"), emptySet()) shouldBe false
    }

    @Test
    fun `excludes untouched tags only`() {
        val ecchi = Tag("Ecchi")
        val harem = Tag("Harem", Filter.TriState.STATE_INCLUDE)
        val action = Tag("Action")
        val filters = FilterList(Genres(listOf(ecchi, harem, action)))

        HiddenTags.excludeIn(filters, setOf("ecchi", "harem")) shouldBe 1
        ecchi.state shouldBe Filter.TriState.STATE_EXCLUDE
        harem.state shouldBe Filter.TriState.STATE_INCLUDE
        action.state shouldBe Filter.TriState.STATE_IGNORE
    }

    @Test
    fun `adds a hidden tag once`() {
        addHiddenTag("", "Gore") shouldBe "Gore"
        addHiddenTag("Ecchi,", "Gore") shouldBe "Ecchi, Gore"
        addHiddenTag("Ecchi, gore", "Gore") shouldBe "Ecchi, gore"
    }
}
