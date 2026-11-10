package kurotoon.stats

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tachiyomi.domain.history.model.ReadEntry
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneOffset
import kotlin.time.Instant

class YearInReviewTest {

    private val zone = ZoneOffset.UTC

    private fun entry(id: Long, at: String, minutes: Long, vararg genres: String) = ReadEntry(
        readAt = Instant.fromEpochMilliseconds(LocalDateTime.parse(at).toInstant(zone).toEpochMilli()),
        duration = minutes * 60_000,
        mangaId = id,
        title = "Series $id",
        genres = genres.toList(),
    )

    @Test
    fun `summarises one year`() {
        val entries = listOf(
            entry(1, "2026-03-01T10:00", 30, "Action", "Fantasy"),
            entry(1, "2026-03-02T10:00", 30, "Action", "Fantasy"),
            entry(2, "2026-03-05T10:00", 10, "action", "Romance"),
            entry(3, "2026-07-01T10:00", 5, "Romance"),
            entry(4, "2025-12-31T23:00", 999, "Horror"),
        )
        val review = YearInReview.build(entries, 2026, zone)!!
        assertEquals(4, review.chapters)
        assertEquals(75 * 60_000L, review.millis)
        assertEquals(3, review.series)
        assertEquals(listOf(1L, 2L, 3L), review.topSeries.map { it.mangaId })
        assertEquals(listOf("Action" to 2, "Romance" to 2, "Fantasy" to 1), review.topGenres)
        assertEquals(Month.MARCH, review.busiestMonth)
        assertEquals(3, review.busiestMonthChapters)
    }

    @Test
    fun `empty year gives nothing`() {
        assertNull(YearInReview.build(emptyList(), 2026, zone))
    }
}
