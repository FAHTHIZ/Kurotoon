package kurotoon.manga

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

class MergedChaptersTest {

    private fun manga(id: Long, title: String, author: String? = null) =
        Manga.create().copy(id = id, title = title, author = author)

    private fun chapters(mangaId: Long, vararg numbers: Double, read: Set<Double> = emptySet()) =
        numbers.map { n ->
            Chapter.create().copy(
                id = mangaId * 1000 + (n * 10).toLong(),
                mangaId = mangaId,
                chapterNumber = n,
                read = n in read,
            )
        }

    @Test
    fun `fills gaps from the other copy`() {
        val a = MergedChapters.Copy(manga(1, "Solo Leveling"), "A", chapters(1, 1.0, 2.0, 3.0, 4.0, 9.0))
        val bChapters = chapters(2, 5.0, 6.0, 7.0, 10.0, 3.0, read = setOf(3.0))
        val b = MergedChapters.Copy(manga(2, "Solo Leveling"), "B", bChapters)
        val merged = MergedChapters.merge(a, listOf(b))
        merged.map { it.number } shouldBe listOf(10.0, 9.0, 7.0, 6.0, 5.0, 4.0, 3.0, 2.0, 1.0)
        merged.first { it.number == 3.0 }.let {
            it.here shouldBe true
            it.read shouldBe true
            it.copies shouldBe 2
        }
        merged.first { it.number == 10.0 }.sourceName shouldBe "B"
    }

    @Test
    fun `matches title loosely but respects different authors`() {
        val here = manga(1, "Solo Leveling", "Chugong")
        val others = listOf(
            manga(2, "solo-leveling (Official)", "CHUGONG"),
            manga(3, "Solo Leveling", "Someone Else"),
            manga(4, "Solo Leveling"),
        )
        MergedChapters.sameSeries(here, others).map { it.id } shouldBe listOf(2L, 4L)
    }

    @Test
    fun `finds the next chapter in another copy`() {
        val b = MergedChapters.Copy(manga(2, "Solo Leveling"), "B", chapters(2, 5.0, 6.0, 10.0, read = setOf(5.0)))
        val c = MergedChapters.Copy(manga(3, "Solo Leveling"), "C", chapters(3, 5.0, 11.0))
        MergedChapters.nextAfter(4.0, listOf(b, c))!!.let { (chapter, copy) ->
            chapter.chapterNumber shouldBe 5.0
            copy.sourceName shouldBe "C"
        }
        MergedChapters.nextAfter(11.0, listOf(b, c)) shouldBe null
    }
}
