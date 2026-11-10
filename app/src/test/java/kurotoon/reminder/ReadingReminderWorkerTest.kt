package kurotoon.reminder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class ReadingReminderWorkerTest {

    private val zone = ZoneId.of("Asia/Dhaka")

    @Test
    fun `later today when the hour has not passed`() {
        val now = ZonedDateTime.of(2026, 11, 10, 18, 30, 0, 0, zone)
        assertEquals(Duration.ofMinutes(90), ReadingReminderWorker.initialDelay(now, 20))
    }

    @Test
    fun `tomorrow when the hour has passed`() {
        val now = ZonedDateTime.of(2026, 11, 10, 21, 0, 0, 0, zone)
        assertEquals(Duration.ofHours(23), ReadingReminderWorker.initialDelay(now, 20))
    }

    @Test
    fun `exactly on the hour waits a full day`() {
        val now = ZonedDateTime.of(2026, 11, 10, 20, 0, 0, 0, zone)
        assertEquals(Duration.ofDays(1), ReadingReminderWorker.initialDelay(now, 20))
    }
}
