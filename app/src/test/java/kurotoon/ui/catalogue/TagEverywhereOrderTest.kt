package kurotoon.ui.catalogue

import io.kotest.matchers.shouldBe
import kurotoon.ui.home.KuroHomeViewModel.SourceRow
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga

class TagEverywhereOrderTest {

    @Test
    fun `rows with titles come first`() {
        val rows = listOf(
            SourceRow(1, "failed", items = null, failed = true),
            SourceRow(2, "empty", items = emptyList()),
            SourceRow(3, "loading", items = null),
            SourceRow(4, "full", items = listOf(Manga.create())),
        )
        TagEverywhereViewModel.ordered(rows).map { it.name } shouldBe listOf("full", "loading", "failed", "empty")
    }
}
