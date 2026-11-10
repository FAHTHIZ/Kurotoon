package kurotoon.library

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LibraryTagsTest {

    private val library = listOf(
        listOf("Action", "Fantasy"),
        listOf("action", "Romance"),
        listOf("Fantasy", "Action ", "Fantasy"),
        null,
    )

    @Test
    fun `counts genres case-insensitively, most common first`() {
        LibraryTags.top(library) shouldBe listOf(
            LibraryTag("action", "Action", 3),
            LibraryTag("fantasy", "Fantasy", 2),
            LibraryTag("romance", "Romance", 1),
        )
        LibraryTags.top(library, limit = 1).map { it.key } shouldBe listOf("action")
    }

    @Test
    fun `matching needs every selected tag`() {
        LibraryTags.matches(listOf("Action", "Fantasy"), setOf("action", "fantasy")) shouldBe true
        LibraryTags.matches(listOf("Action"), setOf("action", "fantasy")) shouldBe false
        LibraryTags.matches(null, emptySet()) shouldBe true
        LibraryTags.matches(null, setOf("action")) shouldBe false
    }
}
