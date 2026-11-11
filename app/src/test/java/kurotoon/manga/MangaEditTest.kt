package kurotoon.manga

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MangaEditTest {
    @Test
    fun `blank fields mean keep the source value`() {
        val edit = MangaEdit(title = "  ", author = " Kuro ", genres = listOf(" ", "Action")).cleaned()
        assertEquals(null, edit.title)
        assertEquals("Kuro", edit.author)
        assertEquals(listOf("Action"), edit.genres)
    }

    @Test
    fun `round trips and drops empty edits`() {
        val all = mapOf(1L to MangaEdit(title = "Mine"), 2L to MangaEdit())
        val back = MangaEdit.decodeAll(MangaEdit.encodeAll(all))
        assertEquals(mapOf(1L to MangaEdit(title = "Mine")), back)
        assertTrue(MangaEdit.decodeAll("not json").isEmpty())
    }

    @Test
    fun `parses comma separated tags`() {
        assertEquals(listOf("Action", "Drama"), MangaEdit.parseGenres("Action, ,Drama,"))
    }
}
