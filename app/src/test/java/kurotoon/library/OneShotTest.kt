package kurotoon.library

import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class OneShotTest {

    @Test
    fun `a oneshot tag is enough in any spelling`() {
        OneShot.matches(listOf("Romance", "One-shot"), SManga.ONGOING.toLong(), 0) shouldBe true
        OneShot.matches(listOf("ONESHOT"), SManga.UNKNOWN.toLong(), 3) shouldBe true
        OneShot.matches(listOf("One Shot"), SManga.UNKNOWN.toLong(), 1) shouldBe true
    }

    @Test
    fun `one chapter counts only when the series is finished`() {
        OneShot.matches(null, SManga.COMPLETED.toLong(), 1) shouldBe true
        OneShot.matches(null, SManga.PUBLISHING_FINISHED.toLong(), 1) shouldBe true
        OneShot.matches(null, SManga.ONGOING.toLong(), 1) shouldBe false
        OneShot.matches(listOf("Action"), SManga.COMPLETED.toLong(), 12) shouldBe false
    }
}
