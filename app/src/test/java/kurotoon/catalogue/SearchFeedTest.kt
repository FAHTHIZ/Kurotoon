package kurotoon.catalogue

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SearchFeedTest {

    @Test
    fun `nothing is new on the first load`() {
        SearchFeed.fresh(emptyList(), listOf("/a", "/b")) shouldBe emptySet()
    }

    @Test
    fun `titles missing last time are new`() {
        SearchFeed.fresh(listOf("/a", "/b"), listOf("/c", "/a")) shouldBe setOf("/c")
    }

    @Test
    fun `remembers newest first within the cap`() {
        SearchFeed.remember(listOf("/a", "/b"), listOf("/c", "/a"), cap = 3) shouldBe listOf("/c", "/a", "/b")
        SearchFeed.remember(listOf("/a", "/b"), listOf("/c", "/d"), cap = 3) shouldBe listOf("/c", "/d", "/a")
    }

    @Test
    fun `round trips`() {
        SearchFeed.decode(SearchFeed.encode(listOf("/a", "/b"))) shouldBe listOf("/a", "/b")
        SearchFeed.decode("") shouldBe emptyList()
    }
}
