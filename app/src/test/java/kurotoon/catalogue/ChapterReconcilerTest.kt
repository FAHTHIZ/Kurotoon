package kurotoon.catalogue

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ChapterReconcilerTest {

    @Test
    fun `unions chapter lists and retains every source variant`() {
        val result = ChapterReconciler.reconcile(
            listOf(
                snapshot("Provider A", "Example", "1", "5", "8"),
                snapshot("Provider B", "Example", "2", "3", "4", "5"),
                snapshot("Provider C", "Example", "6", "7", "5"),
            ),
        ).single()
        result.chapters.map { it.chapterNumber } shouldBe listOf("1", "2", "3", "4", "5", "6", "7", "8")
        result.chapters.single { it.chapterNumber == "5" }.variants.size shouldBe 3
        result.chapters.single { it.chapterNumber == "5" }.sourceCount shouldBe 3
        result.missingChapterNumbers shouldBe emptyList()
    }

    @Test
    fun `normalizes chapter number formatting across snapshots`() {
        val result = ChapterReconciler.reconcile(
            listOf(
                snapshotWith("A", LocalChapterRecord(title = "Chapter 005")),
                snapshotWith("B", LocalChapterRecord(title = "Ch. 5", number = "05.0")),
            ),
        ).single()
        result.chapters.size shouldBe 1
        result.chapters.single().chapterNumber shouldBe "5"
        result.chapters.single().sourceCount shouldBe 2
    }

    @Test
    fun `preserves title variations instead of choosing one silently`() {
        val result = ChapterReconciler.reconcile(
            listOf(
                snapshotWith("Provider A", LocalChapterRecord(title = "Chapter 5 - Arrival", number = "5")),
                snapshotWith("Provider B", LocalChapterRecord(title = "Chapter 5: The Arrival", number = "5")),
            ),
        ).single().chapters.single()
        result.variants.map { it.title } shouldBe listOf("Chapter 5 - Arrival", "Chapter 5: The Arrival")
        result.hasTitleVariants shouldBe true
        result.needsReview shouldBe false
    }

    @Test
    fun `flags duplicate numbered entries from one source for review`() {
        val result = ChapterReconciler.reconcile(
            listOf(
                snapshotWith(
                    "Provider A",
                    LocalChapterRecord(title = "Chapter 5 - Release A", number = "5"),
                    LocalChapterRecord(title = "Chapter 5 - Release B", number = "5"),
                ),
            ),
        ).single().chapters.single()
        result.variants.size shouldBe 2
        result.hasDuplicateEntriesWithinSource shouldBe true
        result.needsReview shouldBe true
    }

    @Test
    fun `keeps unnumbered chapters separate unless normalized titles match`() {
        val result = ChapterReconciler.reconcile(
            listOf(
                snapshotWith("Provider A", LocalChapterRecord(title = "Special: Afterword")),
                snapshotWith("Provider B", LocalChapterRecord(title = "Special Afterword")),
                snapshotWith("Provider C", LocalChapterRecord(title = "Author Notes")),
            ),
        ).single()
        result.chapters.size shouldBe 2
        result.chapters.single { it.displayTitle == "Special: Afterword" }.sourceCount shouldBe 2
    }

    @Test
    fun `reports gaps from the union rather than from any single snapshot`() {
        val result = ChapterReconciler.reconcile(
            listOf(
                snapshot("Provider A", "Example", "1", "5", "8"),
                snapshot("Provider B", "Example", "2", "3", "5"),
            ),
        ).single()
        result.missingChapterNumbers shouldBe listOf("4", "6", "7")
    }

    @Test
    fun `separates different series titles`() {
        val result = ChapterReconciler.reconcile(
            listOf(
                snapshot("Provider A", "Series One", "1"),
                snapshot("Provider B", "Series Two", "1"),
            ),
        )
        result.size shouldBe 2
    }

    private fun snapshot(source: String, series: String, vararg numbers: String) = ChapterSourceSnapshot(
        seriesTitle = series,
        sourceLabel = source,
        chapters = numbers.map { LocalChapterRecord(title = "Chapter " + it, number = it) },
    )

    private fun snapshotWith(source: String, vararg chapters: LocalChapterRecord) = ChapterSourceSnapshot(
        seriesTitle = "Example",
        sourceLabel = source,
        chapters = chapters.toList(),
    )
}
