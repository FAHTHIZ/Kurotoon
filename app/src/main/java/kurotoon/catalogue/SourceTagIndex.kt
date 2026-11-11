package kurotoon.catalogue

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.CatalogueSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tachiyomi.domain.source.service.SourceManager

/**
 * Every tag offered by the sources you have installed, merged into one list.
 *
 * Each source publishes its own genre/tag filters. This reads them all, folds duplicates that only
 * differ in case or spacing ("Slice of Life" / "slice of life"), and counts how many sources offer
 * each tag so the common ones come first. The list follows [watch]: install, remove, enable or
 * disable a source (or a language) and the tags update without restarting the app.
 */
object SourceTagIndex {

    data class Tag(val name: String, val sources: Int)

    private val lock = Mutex()
    private var cachedKey: Set<Long>? = null
    private var cached: List<Tag> = emptyList()

    fun watch(sourceManager: SourceManager, preferences: SourcePreferences): Flow<List<Tag>> =
        combine(
            sourceManager.sources,
            preferences.enabledLanguages.changes(),
            preferences.disabledSources.changes(),
        ) { sources, languages, disabled ->
            sources.filterIsInstance<CatalogueSource>()
                .filter { it.id != LOCAL_SOURCE_ID && it.lang in languages && it.id.toString() !in disabled }
        }
            .distinctUntilChanged { old, new -> old.map { it.id }.toSet() == new.map { it.id }.toSet() }
            .map { build(it) }
            .flowOn(Dispatchers.IO)

    suspend fun build(sources: List<CatalogueSource>): List<Tag> = lock.withLock {
        val key = sources.map { it.id }.toSet()
        if (key == cachedKey) return cached
        val found = LinkedHashMap<String, Pair<String, Int>>()
        sources.forEach { source ->
            val names = runCatching { CatalogFacets.from(source.getFilterList()) }
                .getOrDefault(emptyList())
                .filterIsInstance<CatalogFacet.Tags>()
                .flatMap { facet -> facet.options.map { it.name.trim() } }
                .filter { it.length in MIN_LENGTH..MAX_LENGTH && it.any(Char::isLetter) }
                .distinctBy(::fold)
            names.forEach { name ->
                val k = fold(name)
                val (shown, count) = found[k] ?: (name to 0)
                found[k] = shown to count + 1
            }
        }
        cached = found.values
            .map { (name, count) -> Tag(name, count) }
            .sortedWith(compareByDescending<Tag> { it.sources }.thenBy { it.name.lowercase() })
        cachedKey = key
        cached
    }

    /** Tags whose name contains [query], best matches (prefix, then most sources) first. */
    fun matching(tags: List<Tag>, query: String, limit: Int): List<Tag> {
        val q = fold(query)
        if (q.isEmpty()) return tags.take(limit)
        return tags.asSequence()
            .filter { fold(it.name).contains(q) }
            .sortedWith(compareByDescending<Tag> { fold(it.name).startsWith(q) }.thenByDescending { it.sources })
            .take(limit)
            .toList()
    }

    private fun fold(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

    private const val LOCAL_SOURCE_ID = 0L
    private const val MIN_LENGTH = 2
    private const val MAX_LENGTH = 32
}
