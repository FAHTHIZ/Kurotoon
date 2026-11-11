package kurotoon.library

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga

class LibraryDuplicatesTest {

    private fun entry(id: Long, title: String, read: Long = 0) = LibraryManga(
        manga = Manga.create().copy(id = id, title = title, source = id),
        categories = emptyList(),
        totalChapters = 10,
        readCount = read,
        bookmarkCount = 0,
        latestUpload = null,
        chapterFetchedAt = null,
        lastRead = null,
    )

    @Test
    fun `normalises titles`() {
        LibraryDuplicates.keyOf("Solo Leveling (Official)") shouldBe "sololeveling"
        LibraryDuplicates.keyOf("solo-leveling") shouldBe "sololeveling"
        LibraryDuplicates.keyOf("Pokémon [Colored]") shouldBe "pokemon"
        LibraryDuplicates.keyOf("나 혼자만 레벨업") shouldBe "나혼자만레벨업"
    }

    @Test
    fun `groups copies with the most read first`() {
        val library = listOf(
            entry(1, "Solo Leveling"),
            entry(2, "Tower of God"),
            entry(3, "solo leveling (Official)", read = 5),
            entry(4, "Tower Of God"),
            entry(5, "Tower of God!"),
            entry(6, "Lookism"),
            entry(7, "???"),
            entry(8, "!!!"),
        )
        LibraryDuplicates.groups(library).map { group -> group.map { it.id } } shouldBe listOf(
            listOf(2L, 4L, 5L),
            listOf(3L, 1L),
        )
    }

    @Test
    fun `groups copies linked to the same tracker entry`() {
        val library = listOf(
            entry(1, "Solo Leveling"),
            entry(2, "Na Honjaman Level Up"),
            entry(3, "Lookism"),
            entry(4, "Oegeum"),
        )
        val tracks = mapOf(
            1L to listOf("2:100"),
            2L to listOf("2:100", "1:7"),
            3L to listOf("2:200"),
            4L to listOf("1:8"),
        )
        LibraryDuplicates.groups(library, tracks).map { group -> group.map { it.id } } shouldBe listOf(
            listOf(1L, 2L),
        )
    }
}
