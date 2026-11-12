package kurotoon.catalogue

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TagSectionsTest {

    private val tags = listOf(
        SourceTagIndex.Tag("Action", 5),
        SourceTagIndex.Tag("Romance", 4),
        SourceTagIndex.Tag("adventure", 3),
        SourceTagIndex.Tag("4-Koma", 2),
        SourceTagIndex.Tag("Gore", 1),
    )

    @Test
    fun `hidden tags are left out`() {
        TagSections.visible(tags, setOf("gore"), "").map { it.name } shouldBe
            listOf("Action", "Romance", "adventure", "4-Koma")
    }

    @Test
    fun `query narrows with prefix first`() {
        TagSections.visible(tags, emptySet(), "a").map { it.name }.first() shouldBe "Action"
    }

    @Test
    fun `letters in order with symbols last`() {
        TagSections.alphabetical(tags).map { it.label } shouldBe listOf("A", "G", "R", "#")
        TagSections.alphabetical(tags).first().tags.map { it.name } shouldBe listOf("Action", "adventure")
    }
}
