package kurotoon.library

import eu.kanade.tachiyomi.ui.library.LibraryItem
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga
import kotlin.time.Instant

class LibraryCombineTest {

    private fun item(id: Long, title: String, author: String? = null, lastRead: Long? = null) = LibraryItem(
        libraryManga = LibraryManga(
            manga = Manga.create().copy(id = id, title = title, author = author, source = id),
            categories = emptyList(),
            totalChapters = 10,
            readCount = 0,
            bookmarkCount = 0,
            latestUpload = null,
            chapterFetchedAt = null,
            lastRead = lastRead?.let { Instant.fromEpochMilliseconds(it) },
        ),
        downloadCount = 0,
        unreadCount = 10,
        isLocal = false,
        sourceName = "source $id",
        sourceLanguage = "en",
        badges = LibraryItem.Badges(downloadCount = 0, unreadCount = 10, isLocal = false, sourceLanguage = "en"),
    )

    @Test
    fun `keeps the most recently read copy with a badge`() {
        val result = LibraryCombine.combine(
            listOf(
                item(1, "Solo Leveling", lastRead = 100),
                item(2, "solo-leveling (Official)", lastRead = 500),
                item(3, "Berserk"),
            ),
        )
        result.map { it.id } shouldBe listOf(2L, 3L)
        result.first().badges.copies shouldBe 2
        result.last().badges.copies shouldBe 1
    }

    @Test
    fun `keeps same titles apart when authors differ`() {
        val result = LibraryCombine.combine(
            listOf(
                item(1, "Monster", author = "Naoki Urasawa"),
                item(2, "Monster", author = "Someone Else"),
            ),
        )
        result.map { it.id } shouldBe listOf(1L, 2L)
    }
}
