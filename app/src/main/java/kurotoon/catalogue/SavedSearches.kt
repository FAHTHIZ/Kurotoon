package kurotoon.catalogue

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * A search kept for later: a text query plus the filter state it ran with, for one source.
 * Filters are stored by position and name, the same way they line up between two calls to the
 * source's `getFilterList()`, so a saved search survives app restarts and backups.
 */
@Immutable
@Serializable
data class SavedSearch(
    val id: Long,
    val sourceId: Long,
    val name: String,
    val query: String? = null,
    val filters: List<SavedFilter> = emptyList(),
    /** Shown as a row on Home. */
    val onHome: Boolean = true,
)

@Serializable
data class SavedFilter(val name: String, val state: JsonElement)

object SavedSearches {

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(list: List<SavedSearch>): String = json.encodeToString(list)

    fun decode(raw: String): List<SavedSearch> =
        if (raw.isBlank()) {
            emptyList()
        } else {
            runCatching {
                json.decodeFromString<List<SavedSearch>>(raw)
            }.getOrDefault(emptyList())
        }

    /** The state of every filter, by position. Headers and separators keep their slot. */
    fun snapshot(filters: FilterList): List<SavedFilter> = filters.map { filter ->
        SavedFilter(filter.name, stateOf(filter))
    }

    /** Applies a [snapshot] onto a fresh filter list from the same source. */
    fun restore(into: FilterList, saved: List<SavedFilter>): FilterList {
        into.forEachIndexed { index, filter ->
            val entry = saved.getOrNull(index)?.takeIf { it.name == filter.name } ?: return@forEachIndexed
            runCatching { apply(filter, entry.state) }
        }
        return into
    }

    private fun stateOf(filter: Filter<*>): JsonElement = when (filter) {
        is Filter.Header, is Filter.Separator -> JsonNull
        is Filter.CheckBox -> JsonPrimitive(filter.state)
        is Filter.TriState -> JsonPrimitive(filter.state)
        is Filter.Select<*> -> JsonPrimitive(filter.state)
        is Filter.Text -> JsonPrimitive(filter.state)
        is Filter.Sort -> filter.state?.let {
            JsonObject(mapOf("index" to JsonPrimitive(it.index), "ascending" to JsonPrimitive(it.ascending)))
        } ?: JsonNull
        is Filter.Group<*> -> JsonArray(filter.state.map { (it as? Filter<*>)?.let(::stateOf) ?: JsonNull })
    }

    private fun apply(filter: Filter<*>, state: JsonElement) {
        when (filter) {
            is Filter.Header, is Filter.Separator -> {}
            is Filter.CheckBox -> state.jsonPrimitive.booleanOrNull?.let { filter.state = it }
            is Filter.TriState ->
                state.jsonPrimitive.intOrNull
                    ?.takeIf { it in Filter.TriState.STATE_IGNORE..Filter.TriState.STATE_EXCLUDE }
                    ?.let { filter.state = it }
            is Filter.Select<*> ->
                state.jsonPrimitive.intOrNull
                    ?.takeIf { it in filter.values.indices }
                    ?.let { filter.state = it }
            is Filter.Text -> (state as? JsonPrimitive)?.takeIf { it.isString }?.let { filter.state = it.content }
            is Filter.Sort -> filter.state = (state as? JsonObject)?.let {
                val index = it["index"]?.jsonPrimitive?.int ?: return@let null
                Filter.Sort.Selection(index, it["ascending"]?.jsonPrimitive?.boolean ?: false)
                    .takeIf { index in filter.values.indices }
            }
            is Filter.Group<*> -> {
                val children = (state as? JsonArray) ?: return
                filter.state.forEachIndexed { i, child ->
                    val childState = children.getOrNull(i) ?: return@forEachIndexed
                    if (child is Filter<*>) runCatching { apply(child, childState) }
                }
            }
        }
    }

    /** A readable default name from what is applied: "Romance + Comedy · Completed". */
    fun suggestName(query: String?, active: List<ActiveFacet>): String {
        val parts = buildList {
            query?.takeIf { it.isNotBlank() }?.let { add("“${it.trim()}”") }
            active.filterNot { it.excluded }.take(3).forEach { add(it.label) }
            active.filter { it.excluded }.take(1).forEach { add("−${it.label}") }
        }
        return parts.joinToString(" + ").take(48)
    }
}
