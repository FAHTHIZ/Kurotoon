package kurotoon.catalogue

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList

/**
 * A browsable dimension of a source's catalogue, read straight from its filter list: a tag group
 * (genres, themes, formats, demographics…) or a single-choice list (status, type, year…).
 *
 * Every source exposes a different set, so the browse header changes with the catalogue: a manga
 * site may offer Genres, Demographic and Status, a webtoon site Genres and Day, a novel site Tags
 * and Language. [filterIndex] points at the filter in [FilterList], so facets can be applied to a
 * fresh copy of the source's filters at any time.
 */
@Immutable
sealed interface CatalogFacet {
    val filterIndex: Int
    val title: String

    /** Many-of tags. [excludable] when the source accepts tri-state include/exclude. */
    data class Tags(
        override val filterIndex: Int,
        override val title: String,
        val options: List<Option>,
        val excludable: Boolean,
    ) : CatalogFacet

    /** One-of values. [defaultIndex] is the source's own default ("Any", "All"…). */
    data class Choice(
        override val filterIndex: Int,
        override val title: String,
        val values: List<String>,
        val defaultIndex: Int,
    ) : CatalogFacet

    /** [childIndex] is the option's position inside its group. */
    data class Option(val childIndex: Int, val name: String)
}

/** What a facet chip currently looks like. */
enum class FacetState { Off, Included, Excluded }

/** One applied facet value, used for the "active filters" row. */
@Immutable
data class ActiveFacet(
    val filterIndex: Int,
    val childIndex: Int,
    val label: String,
    val excluded: Boolean,
)

object CatalogFacets {

    /** Groups with fewer tags than this are usually switches ("Completed only"), not a facet. */
    private const val MIN_TAGS = 2

    /** Single-choice lists this long are usually page sizes or the like; still useful, but capped. */
    private const val MAX_CHOICES = 60

    fun from(filters: FilterList): List<CatalogFacet> {
        val facets = mutableListOf<CatalogFacet>()
        var lastHeader: String? = null
        filters.forEachIndexed { index, filter ->
            when (filter) {
                is Filter.Header -> lastHeader = filter.name.takeIf { it.isNotBlank() }
                is Filter.Group<*> -> {
                    val options = filter.state.mapIndexedNotNull { childIndex, child ->
                        when (child) {
                            is Filter.CheckBox, is Filter.TriState -> (child as Filter<*>).name
                                .takeIf { it.isNotBlank() }
                                ?.let { CatalogFacet.Option(childIndex, it.trim()) }
                            else -> null
                        }
                    }.distinctBy { it.name }
                    if (options.size >= MIN_TAGS) {
                        facets += CatalogFacet.Tags(
                            filterIndex = index,
                            title = filter.name.ifBlank { lastHeader.orEmpty() }.trim(),
                            options = options,
                            excludable = filter.state.any { it is Filter.TriState },
                        )
                    }
                    lastHeader = null
                }
                is Filter.Select<*> -> {
                    val values = filter.values.map { it.toString().trim() }
                    if (values.size in 2..MAX_CHOICES && values.all { it.isNotEmpty() }) {
                        facets += CatalogFacet.Choice(
                            filterIndex = index,
                            title = filter.name.ifBlank { lastHeader.orEmpty() }.trim(),
                            values = values,
                            defaultIndex = filter.state.coerceIn(0, values.lastIndex),
                        )
                    }
                    lastHeader = null
                }
                is Filter.Sort -> lastHeader = null
                else -> {}
            }
        }
        // Genre-like groups first: they are what people browse by
        return facets.sortedByDescending { facet -> GenreHints.any { facet.title.contains(it, ignoreCase = true) } }
    }

    fun stateOf(filters: FilterList, filterIndex: Int, childIndex: Int): FacetState {
        return when (val filter = filters.getOrNull(filterIndex)) {
            is Filter.Group<*> -> when (val child = filter.state.getOrNull(childIndex)) {
                is Filter.TriState -> when (child.state) {
                    Filter.TriState.STATE_INCLUDE -> FacetState.Included
                    Filter.TriState.STATE_EXCLUDE -> FacetState.Excluded
                    else -> FacetState.Off
                }
                is Filter.CheckBox -> if (child.state) FacetState.Included else FacetState.Off
                else -> FacetState.Off
            }
            is Filter.Select<*> -> if (filter.state == childIndex) FacetState.Included else FacetState.Off
            else -> FacetState.Off
        }
    }

    /** The selected index of a single-choice facet. */
    fun choiceOf(filters: FilterList, filterIndex: Int): Int =
        (filters.getOrNull(filterIndex) as? Filter.Select<*>)?.state ?: 0

    /**
     * Cycles a tag: off → included → excluded (when the source supports it) → off.
     * Mutates [filters] in place, so pass a copy.
     */
    fun cycleTag(filters: FilterList, filterIndex: Int, childIndex: Int) {
        val group = filters.getOrNull(filterIndex) as? Filter.Group<*> ?: return
        when (val child = group.state.getOrNull(childIndex)) {
            is Filter.TriState -> child.state = when (child.state) {
                Filter.TriState.STATE_IGNORE -> Filter.TriState.STATE_INCLUDE
                Filter.TriState.STATE_INCLUDE -> Filter.TriState.STATE_EXCLUDE
                else -> Filter.TriState.STATE_IGNORE
            }
            is Filter.CheckBox -> child.state = !child.state
            else -> {}
        }
    }

    /** Goes straight to excluded (or back to off), for long presses. */
    fun excludeTag(filters: FilterList, filterIndex: Int, childIndex: Int) {
        val group = filters.getOrNull(filterIndex) as? Filter.Group<*> ?: return
        val child = group.state.getOrNull(childIndex) as? Filter.TriState ?: return
        child.state = if (child.isExcluded()) Filter.TriState.STATE_IGNORE else Filter.TriState.STATE_EXCLUDE
    }

    /** Picks a value; picking the selected one again goes back to [defaultIndex]. */
    fun selectChoice(filters: FilterList, filterIndex: Int, valueIndex: Int, defaultIndex: Int = 0) {
        val select = filters.getOrNull(filterIndex) as? Filter.Select<*> ?: return
        select.state = if (select.state == valueIndex) defaultIndex else valueIndex
    }

    fun clear(filters: FilterList, filterIndex: Int, childIndex: Int, defaultIndex: Int = 0) {
        when (val filter = filters.getOrNull(filterIndex)) {
            is Filter.Group<*> -> when (val child = filter.state.getOrNull(childIndex)) {
                is Filter.TriState -> child.state = Filter.TriState.STATE_IGNORE
                is Filter.CheckBox -> child.state = false
                else -> {}
            }
            is Filter.Select<*> -> filter.state = defaultIndex
            else -> {}
        }
    }

    /** Every applied facet value, in facet order. A choice left at its default is not listed. */
    fun active(facets: List<CatalogFacet>, filters: FilterList): List<ActiveFacet> = buildList {
        facets.forEach { facet ->
            when (facet) {
                is CatalogFacet.Tags -> facet.options.forEach { option ->
                    when (stateOf(filters, facet.filterIndex, option.childIndex)) {
                        FacetState.Included -> add(
                            ActiveFacet(facet.filterIndex, option.childIndex, option.name, false),
                        )
                        FacetState.Excluded -> add(ActiveFacet(facet.filterIndex, option.childIndex, option.name, true))
                        FacetState.Off -> {}
                    }
                }
                is CatalogFacet.Choice -> {
                    val selected = choiceOf(filters, facet.filterIndex)
                    if (selected != facet.defaultIndex) {
                        val label = facet.values.getOrNull(selected) ?: return@forEach
                        add(ActiveFacet(facet.filterIndex, selected, label, false))
                    }
                }
            }
        }
    }

    /** How many values are applied in one facet, for the facet tab badge. */
    fun activeCount(facet: CatalogFacet, filters: FilterList): Int = when (facet) {
        is CatalogFacet.Tags -> facet.options.count {
            stateOf(filters, facet.filterIndex, it.childIndex) !=
                FacetState.Off
        }
        is CatalogFacet.Choice -> if (choiceOf(filters, facet.filterIndex) != facet.defaultIndex) 1 else 0
    }

    private val GenreHints = listOf("genre", "género", "genero", "жанр", "tag", "categor")
}

/**
 * Copies every filter's state from [from] onto this list. Both lists must come from the same
 * source's `getFilterList()`, so filters line up by position; mismatched entries are skipped.
 */
@Suppress("UNCHECKED_CAST")
fun FilterList.copyStateFrom(from: FilterList): FilterList {
    forEachIndexed { index, target ->
        val origin = from.getOrNull(index) ?: return@forEachIndexed
        if (origin::class != target::class || origin.name != target.name) return@forEachIndexed
        when (target) {
            is Filter.Group<*> -> {
                val originChildren = (origin as Filter.Group<*>).state
                target.state.forEachIndexed { childIndex, child ->
                    val originChild = originChildren.getOrNull(childIndex)
                    if (child is Filter<*> && originChild is Filter<*> && child::class == originChild::class) {
                        (child as Filter<Any?>).state = originChild.state
                    }
                }
            }
            is Filter.Header, is Filter.Separator -> {}
            else -> (target as Filter<Any?>).state = origin.state
        }
    }
    return this
}
