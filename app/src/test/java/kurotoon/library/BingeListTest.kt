package kurotoon.library

import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga
import kotlin.time.Instant

class BingeListTest {

    private fun entry(
        id: Long,
        status: Int,
        total: Long,
        read: Long = 0,
        added: Long = id,
        lastRead: Instant? = null,
    ) = LibraryManga(
        manga = Manga.create().copy(id = id, status = status.toLong(), favoriteAt = Instant.fromEpochSeconds(added)),
        categories = emptyList(),
        totalChapters = total,
        readCount = read,
        bookmarkCount = 0,
        latestUpload = null,
        chapterFetchedAt = null,
        lastRead = lastRead,
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

    @Test
    fun `finds started series left alone for three weeks`() {
        val now = Instant.fromEpochSeconds(100L * 86_400)
        fun daysAgo(d: Long) = Instant.fromEpochSeconds((100L - d) * 86_400)
        val library = listOf(
            entry(1, SManga.ONGOING, total = 50, read = 10, lastRead = daysAgo(30)),
            entry(2, SManga.ONGOING, total = 50, read = 10, lastRead = daysAgo(5)),
            entry(3, SManga.ONGOING, total = 50, read = 50, lastRead = daysAgo(40)),
            entry(4, SManga.COMPLETED, total = 50, read = 2, lastRead = daysAgo(22)),
            entry(5, SManga.ONGOING, total = 50, read = 0, lastRead = null),
        )
        StalledList.of(library, now).map { it.id } shouldBe listOf(4L, 1L)
    }

    @Test
    fun `lists series saved in the last month, newest first`() {
        val now = Instant.fromEpochSeconds(100L * 86_400)
        fun daysAgo(d: Long) = (100L - d) * 86_400
        val library = listOf(
            entry(1, SManga.ONGOING, total = 5, added = daysAgo(3)),
            entry(2, SManga.ONGOING, total = 5, added = daysAgo(45)),
            entry(3, SManga.COMPLETED, total = 5, added = daysAgo(1)),
            entry(4, SManga.ONGOING, total = 5, added = 0),
        )
        RecentlyAddedList.of(library, now).map { it.id } shouldBe listOf(3L, 1L)
    }
}
