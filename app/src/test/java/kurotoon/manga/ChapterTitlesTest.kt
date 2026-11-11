package kurotoon.manga

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ChapterTitlesTest {

    @Test
    fun `drops a repeated chapter number`() {
        ChapterTitles.tidy("Episode 42 (ch. 42)") shouldBe "Episode 42"
        ChapterTitles.tidy("Ep. 7.5 (Chapter 7.5)") shouldBe "Ep. 7.5"
    }

    @Test
    fun `keeps names where the number adds information`() {
        ChapterTitles.tidy("Season 2 Episode 1 (ch. 51)") shouldBe "Season 2 Episode 1 (ch. 51)"
        ChapterTitles.tidy("Chapter 3") shouldBe "Chapter 3"
        ChapterTitles.tidy("(ch. 4)") shouldBe "(ch. 4)"
    }
}
