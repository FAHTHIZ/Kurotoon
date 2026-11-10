package kurotoon.library

import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga
import kotlin.time.Instant

class BingeListTest {

    private fun entry(id: Long, status: Int, total: Long, read: Long = 0, added: Long = id) = LibraryManga(
        manga = Manga.create().copy(id = id, status = status.toLong(), favoriteAt = Instant.fromEpochSeconds(added)),
        categories = emptyList(),
        totalChapters = total,
        readCount = read,
        bookmarkCount = 0,
        latestUpload = null,
        chapterFetchedAt = null,
        lastRead = null,
    )

    @Test
    fun `keeps finished unstarted series, newest saved first`() {
        val library = listOf(
            entry(1, SManga.COMPLETED, total = 100),
            entry(2, SManga.ONGOING, total = 50),
            entry(3, SManga.COMPLETED, total = 80, read = 3),
            entry(4, SManga.PUBLISHING_FINISHED, total = 40),
            entry(5, SManga.COMPLETED, total = 0),
        )
        BingeList.of(library).map { it.id } shouldBe listOf(4L, 1L)
        BingeList.of(library, limit = 1).map { it.id } shouldBe listOf(4L)
    }
}
