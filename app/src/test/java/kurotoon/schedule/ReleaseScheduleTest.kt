package kurotoon.schedule

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class ReleaseScheduleTest {

    private val zone = TimeZone.UTC

    // A Wednesday
    private val today = LocalDate(2026, 11, 11)

    private fun daysAgo(vararg days: Int): List<Instant> =
        days.map { today.minus(DatePeriod(days = it)).atStartOfDayIn(zone) + 10.hours }

    @Test
    fun `weekly series lands on its day`() {
        // Every Monday for six weeks
        val uploads = daysAgo(2, 9, 16, 23, 30, 37)
        assertEquals(listOf(DayOfWeek.MONDAY), ReleaseSchedule.weekdays(uploads, today, zone))
    }

    @Test
    fun `twice a week keeps both days`() {
        // Mondays and Thursdays
        val uploads = daysAgo(2, 6, 9, 13, 16, 20)
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), ReleaseSchedule.weekdays(uploads, today, zone))
    }

    @Test
    fun `chapters dropped together count once`() {
        // Three chapters on one Friday shouldn't outweigh the Monday routine
        val uploads = daysAgo(2, 9, 16, 23) + daysAgo(5, 5, 5)
        assertEquals(listOf(DayOfWeek.MONDAY), ReleaseSchedule.weekdays(uploads, today, zone))
    }

    @Test
    fun `too few chapters or no pattern gives nothing`() {
        assertEquals(emptyList<DayOfWeek>(), ReleaseSchedule.weekdays(daysAgo(2, 9), today, zone))
        // A different weekday each time
        assertEquals(emptyList<DayOfWeek>(), ReleaseSchedule.weekdays(daysAgo(1, 9, 17, 25, 33), today, zone))
    }

    @Test
    fun `daily series is not a schedule`() {
        assertEquals(emptyList<DayOfWeek>(), ReleaseSchedule.weekdays(daysAgo(*IntArray(14) { it }), today, zone))
    }

    @Test
    fun `series on break drops off`() {
        val uploads = daysAgo(51, 58, 65, 72)
        assertEquals(emptyList<DayOfWeek>(), ReleaseSchedule.weekdays(uploads, today, zone))
    }

    @Test
    fun `follows a change of day`() {
        // Moved from Saturday to Monday five weeks ago; only the last ten dates count
        val uploads = daysAgo(2, 9, 16, 23, 30, 39, 46, 53, 60, 67, 74, 81, 88)
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.SATURDAY), ReleaseSchedule.weekdays(uploads, today, zone))
        assertEquals(listOf(DayOfWeek.MONDAY), ReleaseSchedule.weekdays(uploads.take(6), today, zone))
    }

    @Test
    fun `undated chapters are ignored`() {
        val uploads = daysAgo(2, 9, 16) + Instant.fromEpochMilliseconds(0)
        assertEquals(listOf(DayOfWeek.MONDAY), ReleaseSchedule.weekdays(uploads, today, zone))
    }
}
