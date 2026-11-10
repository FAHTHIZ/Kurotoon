package kurotoon.catalogue

import kotlinx.serialization.json.Json

/** The last few queries typed into Universal Search, newest first. */
object RecentSearches {

    const val LIMIT = 10

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(list: List<String>): String = json.encodeToString(list)

    fun decode(raw: String): List<String> = runCatching {
        json.decodeFromString<List<String>>(raw)
    }.getOrDefault(emptyList())

    /** Moves [query] to the front, dropping case-insensitive duplicates and anything past [LIMIT]. */
    fun add(list: List<String>, query: String): List<String> {
        val trimmed = query.trim().replace(Regex("\\s+"), " ")
        if (trimmed.isEmpty()) return list
        return (listOf(trimmed) + list.filterNot { it.equals(trimmed, ignoreCase = true) }).take(LIMIT)
    }
}
