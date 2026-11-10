package kurotoon.reader

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WebtoonDetectorTest {

    @Test
    fun `genre tags mark long strips`() {
        assertTrue(WebtoonDetector.isLongStrip(listOf("Action", "Manhwa"), "MangaDex"))
        assertTrue(WebtoonDetector.isLongStrip(listOf(" Long Strip "), null))
        assertTrue(WebtoonDetector.isLongStrip(listOf("Full Color"), null))
    }

    @Test
    fun `source name marks long strips`() {
        assertTrue(WebtoonDetector.isLongStrip(emptyList(), "Asura Scans"))
        assertTrue(WebtoonDetector.isLongStrip(null, "Webtoons.com"))
    }

    @Test
    fun `regular manga stays paged`() {
        assertFalse(WebtoonDetector.isLongStrip(listOf("Shounen", "Action"), "MangaDex"))
        assertFalse(WebtoonDetector.isLongStrip(null, null))
    }
}
