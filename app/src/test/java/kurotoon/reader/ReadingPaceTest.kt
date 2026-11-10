package kurotoon.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReadingPaceTest {

    @Test
    fun `needs a few turns before estimating`() {
        val pace = ReadingPace()
        pace.onPageTurn(0)
        pace.onPageTurn(10_000)
        pace.onPageTurn(20_000)
        assertNull(pace.minutesLeft(30))
        pace.onPageTurn(30_000)
        // 10s a page, 30 pages left
        assertEquals(5, pace.minutesLeft(30))
    }

    @Test
    fun `ignores pauses and skims`() {
        val pace = ReadingPace()
        var t = 0L
        pace.onPageTurn(t)
        repeat(4) {
            t += 6_000
            pace.onPageTurn(t)
        }
        t += 600_000 // phone down
        pace.onPageTurn(t)
        t += 100 // skim
        pace.onPageTurn(t)
        assertEquals(1, pace.minutesLeft(10))
        assertNull(pace.minutesLeft(0))
    }
}
