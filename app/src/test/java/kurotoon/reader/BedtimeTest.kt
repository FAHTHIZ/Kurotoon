package kurotoon.reader

import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BedtimeTest {
    @Test
    fun `day has no warmth`() {
        bedtimeWarmth(12 * 60) shouldBe 0f
        bedtimeWarmth(21 * 60 + 29) shouldBe 0f
        bedtimeWarmth(6 * 60 + 30) shouldBe 0f
    }

    @Test
    fun `night is fully warm`() {
        bedtimeWarmth(23 * 60) shouldBe 1f
        bedtimeWarmth(0) shouldBe 1f
        bedtimeWarmth(5 * 60) shouldBe 1f
    }

    @Test
    fun `eases at dusk and dawn`() {
        bedtimeWarmth(22 * 60) shouldBe (0.5f plusOrMinus 0.001f)
        bedtimeWarmth(6 * 60) shouldBe (0.5f plusOrMinus 0.001f)
    }
}
