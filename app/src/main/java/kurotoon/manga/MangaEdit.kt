package kurotoon.manga

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Your own version of a series' details, as in TachiyomiSY's "Edit info". Blank fields keep the source's value.
 * Edits are kept apart from the database so a library refresh can put them back over what the source sends.
 */
@Serializable
data class MangaEdit(
    val title: String? = null,
    val author: String? = null,
    val artist: String? = null,
    val description: String? = null,
    val genres: List<String>? = null,
) {
    val isEmpty: Boolean
        get() = title.isNullOrBlank() && author.isNullOrBlank() && artist.isNullOrBlank() &&
            description.isNullOrBlank() && genres.isNullOrEmpty()

    /** Normalised: trimmed, with blanks turned into "keep the source's value". */
    fun cleaned() = MangaEdit(
        title = title?.trim()?.takeIf { it.isNotEmpty() },
        author = author?.trim()?.takeIf { it.isNotEmpty() },
        artist = artist?.trim()?.takeIf { it.isNotEmpty() },
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        genres = genres?.map { it.trim() }?.filter { it.isNotEmpty() }?.distinct()?.takeIf { it.isNotEmpty() },
    )

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun decodeAll(raw: String): Map<Long, MangaEdit> = runCatching {
            json.decodeFromString<Map<String, MangaEdit>>(raw).mapNotNull { (k, v) ->
                k.toLongOrNull()?.let { it to v }
            }.toMap()
        }.getOrDefault(emptyMap())

        fun encodeAll(edits: Map<Long, MangaEdit>): String =
            json.encodeToString(edits.filterValues { !it.isEmpty }.mapKeys { it.key.toString() })

        /** Comma-separated tags from a text field. */
        fun parseGenres(text: String): List<String> = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }
}
