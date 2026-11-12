package kurotoon.search

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga
import kotlin.time.Instant

class UnifiedSearchTest {

    private fun manga(title: String, source: Long, favorite: Boolean = false) =
        Manga.create().copy(
            title = title,
            source = source,
            url = "/$source/$title",
            favoriteAt = if (favorite) Instant.fromEpochMilliseconds(1) else null,
        )

    @Test
    fun `same series on two sources becomes one hit`() {
        val hits = UnifiedSearch.merge(
            listOf(
                listOf(manga("Solo Leveling", 1), manga("Other", 1)),
                listOf(manga("Solo-Leveling!", 2)),
            ),
            query = "solo",
        )
        hits.size shouldBe 2
        hits.first().sourceCount shouldBe 2
    }

    @Test
    fun `library copy is the one opened`() {
        val hits = UnifiedSearch.merge(
            listOf(listOf(manga("Tower of God", 1)), listOf(manga("Tower of God", 2, favorite = true))),
            query = "tower of god",
        )
        hits.single().primary.source shouldBe 2
    }

    @Test
    fun `exact title beats a longer one`() {
        val hits = UnifiedSearch.merge(
            listOf(listOf(manga("The Beginning After the End", 1), manga("Beginning", 1))),
            query = "Beginning",
        )
        hits.map { it.primary.title } shouldBe listOf("Beginning", "The Beginning After the End")
    }

    @Test
    fun `sources are interleaved before deeper results`() {
        val hits = UnifiedSearch.merge(
            listOf(listOf(manga("A one", 1), manga("A two", 1)), listOf(manga("B one", 2))),
            query = "zzz",
        )
        hits.map { it.primary.title } shouldBe listOf("A one", "B one", "A two")
    }

    @Test
    fun `library titles that match are folded in`() {
        val hits = UnifiedSearch.merge(
            listOf(emptyList()),
            query = "omni",
            library = listOf(manga("Omniscient Reader", 9, favorite = true), manga("Nope", 9, favorite = true)),
        )
        hits.single().inLibrary shouldBe true
    }

    @Test
    fun `duplicate entries from one source count once`() {
        val hits = UnifiedSearch.merge(listOf(listOf(manga("X", 1), manga("X", 1).copy(url = "/b"))), query = "x")
        hits.single().sourceCount shouldBe 1
    }
}
