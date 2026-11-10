package kurotoon.catalogue

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class RecentSearchesTest {

    @Test
    fun `newest first without duplicates`() {
        val list = RecentSearches.add(RecentSearches.add(listOf("tower of god"), "solo leveling"), "Tower  of God ")
        list shouldBe listOf("Tower of God", "solo leveling")
    }

    @Test
    fun `blank queries are ignored and the list is capped`() {
        RecentSearches.add(listOf("a"), "  ") shouldBe listOf("a")
        val many = (1..20).fold(emptyList<String>()) { acc, i -> RecentSearches.add(acc, "q$i") }
        many.size shouldBe RecentSearches.LIMIT
        many.first() shouldBe "q20"
    }

    @Test
    fun `round trips and survives bad data`() {
        RecentSearches.decode(RecentSearches.encode(listOf("a", "b"))) shouldBe listOf("a", "b")
        RecentSearches.decode("not json") shouldBe emptyList()
    }
}
