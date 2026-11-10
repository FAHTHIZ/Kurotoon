package kurotoon.stats

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.time.Instant

class ReadingActivityTest {

    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 11) // a Wednesday

    private fun at(date: LocalDate, hour: Int = 12) =
        Instant.fromEpochMilliseconds(date.atTime(hour, 0).toInstant(zone).toEpochMilli())

    @Test
    fun `grid is monday first and stops at today`() {
        val activity = ReadingActivity.build(emptyList(), today, zone, weeks = 2)
        activity.weeks.size shouldBe 2
        activity.weeks[0][0]!!.date shouldBe LocalDate.of(2026, 11, 2)
        activity.weeks[1][2]!!.date shouldBe today
        activity.weeks[1][3] shouldBe null
    }

    @Test
    fun `counts chapters per day and streaks`() {
        val entries = listOf(
            at(today) to 60_000L,
            at(today.minusDays(1)) to 1_000L,
            at(today.minusDays(1), 20) to 1_000L,
            at(today.minusDays(2)) to 0L,
            at(today.minusDays(5)) to 0L,
            at(today.minusDays(6)) to 0L,
        )
        val activity = ReadingActivity.build(entries, today, zone, weeks = 2)
        activity.totalChapters shouldBe 6
        activity.activeDays shouldBe 5
        activity.currentStreak shouldBe 3
        activity.longestStreak shouldBe 3
        activity.busiest shouldBe 2
    }

    @Test
    fun `streak survives a today without reading yet`() {
        val entries = listOf(at(today.minusDays(1)) to 0L, at(today.minusDays(2)) to 0L)
        ReadingActivity.build(entries, today, zone, weeks = 2).currentStreak shouldBe 2
    }
}
