package kurotoon.ui.history

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Instant

class DayTotalTest {

    private val zone = ZoneId.of("Asia/Dhaka")

    private fun at(text: String) =
        Instant.fromEpochMilliseconds(LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli())

    @Test
    fun `groups by local day and ignores negative durations`() {
        val totals = totalsByDay(
            listOf(
                at("2026-11-10T23:30") to 60_000L,
                at("2026-11-10T08:00") to -1L,
                at("2026-11-11T00:10") to 120_000L,
            ),
            zone,
        )
        assertEquals(DayTotal(2, 60_000L), totals["2026-11-10"])
        assertEquals(DayTotal(1, 120_000L), totals["2026-11-11"])
    }
}
