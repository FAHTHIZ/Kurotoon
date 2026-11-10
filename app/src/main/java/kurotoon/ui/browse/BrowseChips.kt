package kurotoon.ui.browse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.model.FilterList
import kurotoon.catalogue.ActiveFacet
import kurotoon.catalogue.CatalogFacet
import kurotoon.catalogue.CatalogFacets
import kurotoon.catalogue.FacetState
import kurotoon.ui.KuroColors
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Block
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.icons.materialsymbols.rounded.Tune
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** Which listing chip is lit: the source's popular or latest feed, or one of its sort orders. */
sealed interface BrowseSort {
    data object Popular : BrowseSort
    data object Latest : BrowseSort
    data class Sort(val index: Int) : BrowseSort
    data object Custom : BrowseSort
    data object None : BrowseSort
}

/** Callbacks for the catalogue chips, grouped so screens and previews can pass them around. */
class FacetActions(
    val onTag: (filterIndex: Int, childIndex: Int) -> Unit,
    val onExcludeTag: (filterIndex: Int, childIndex: Int) -> Unit,
    val onChoice: (filterIndex: Int, valueIndex: Int) -> Unit,
    val onClear: (ActiveFacet) -> Unit,
    val onClearAll: () -> Unit,
) {
    companion object {
        val None = FacetActions({ _, _ -> }, { _, _ -> }, { _, _ -> }, {}, {})
    }
}

/**
 * The browse header. A row of sort pills, then the source's own catalogue: one tab per facet it
 * offers (Genres, Themes, Status, Type, Demographic…) and that facet's values as pills. Tags can
 * be stacked; tap cycles include → exclude → off where the source supports excluding, and long
 * press excludes straight away. What is applied shows up last as removable chips.
 */
@Composable
fun BrowseChips(
    supportsLatest: Boolean,
    sorts: List<String>,
    selectedSort: BrowseSort,
    hasFilters: Boolean,
    onPopular: () -> Unit,
    onLatest: () -> Unit,
    onSort: (Int) -> Unit,
    onFilters: () -> Unit,
    modifier: Modifier = Modifier,
    facets: List<CatalogFacet> = emptyList(),
    applied: FilterList? = null,
    active: List<ActiveFacet> = emptyList(),
    facetActions: FacetActions = FacetActions.None,
    extraRow: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "popular") {
                SortPill(stringResource(MR.strings.popular), selectedSort == BrowseSort.Popular, onPopular)
            }
            if (supportsLatest) {
                item(key = "latest") {
                    SortPill(stringResource(MR.strings.latest), selectedSort == BrowseSort.Latest, onLatest)
                }
            }
            sorts.forEachIndexed { index, name ->
                item(key = "sort-$index") {
                    SortPill(name, selectedSort == BrowseSort.Sort(index), { onSort(index) })
                }
            }
            if (hasFilters) {
                item(key = "filters") {
                    SortPill(
                        text = stringResource(MR.strings.action_filter),
                        selected = selectedSort == BrowseSort.Custom,
                        onClick = onFilters,
                        icon = MaterialSymbols.Rounded.Tune,
                    )
                }
            }
        }

        extraRow?.invoke()

        if (facets.isNotEmpty()) {
            CatalogFacetPanel(facets = facets, applied = applied, actions = facetActions)
        }

        AnimatedVisibility(
            visible = active.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            ActiveFacetRow(active = active, actions = facetActions)
        }
    }
}

@Composable
private fun CatalogFacetPanel(facets: List<CatalogFacet>, applied: FilterList?, actions: FacetActions) {
    // Keyed by the filter's position so it survives the source handing back new filter objects
    var selectedFilterIndex by rememberSaveable { mutableIntStateOf(facets.first().filterIndex) }
    val facet = facets.firstOrNull { it.filterIndex == selectedFilterIndex } ?: facets.first()
    var expanded by rememberSaveable { mutableStateOf(false) }

    if (facets.size > 1) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(facets, key = { "facet-${it.filterIndex}" }) { item ->
                FacetTab(
                    title = item.title.ifBlank { stringResource(MR.strings.action_filter) },
                    count = applied?.let { CatalogFacets.activeCount(item, it) } ?: 0,
                    selected = item.filterIndex == facet.filterIndex,
                    onClick = { selectedFilterIndex = item.filterIndex },
                )
            }
        }
    }

    val options = remember(facet) { facetOptions(facet) }
    val canExpand = options.size > COLLAPSED_LIMIT

    if (expanded && canExpand) {
        // Long lists (MangaDex has 70+ tags) get a box to find one by typing
        var find by rememberSaveable(facet.filterIndex) { mutableStateOf("") }
        val shown = if (find.isBlank()) options else options.filter { it.second.contains(find.trim(), true) }
        Column(modifier = Modifier.fillMaxWidth()) {
            if (options.size > FIND_LIMIT) {
                SourceFilterField(
                    query = find,
                    onQueryChange = { find = it },
                    placeholder = stringResource(MR.strings.kurotoon_browse_findTag, options.size),
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                )
            }
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                shown.forEach { (index, name) ->
                    FacetOptionPill(facet, index, name, applied, actions)
                }
            }
            ExpandToggle(expanded = true, onClick = {
                expanded = false
            }, modifier = Modifier.padding(start = 12.dp, top = 6.dp))
        }
    } else {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (canExpand) {
                item(key = "expand") { ExpandToggle(expanded = false, onClick = { expanded = true }) }
            }
            itemsIndexed(options, key = { _, it -> "opt-${facet.filterIndex}-${it.first}" }) { _, (index, name) ->
                FacetOptionPill(facet, index, name, applied, actions)
            }
        }
    }
}

/** (index, label) pairs: child indexes for tags, value indexes for choices. */
private fun facetOptions(facet: CatalogFacet): List<Pair<Int, String>> = when (facet) {
    is CatalogFacet.Tags -> facet.options.map { it.childIndex to it.name }
    is CatalogFacet.Choice -> facet.values.mapIndexed { index, value -> index to value }
}

@Composable
private fun FacetOptionPill(
    facet: CatalogFacet,
    index: Int,
    name: String,
    applied: FilterList?,
    actions: FacetActions,
) {
    when (facet) {
        is CatalogFacet.Tags -> {
            val state = applied?.let { CatalogFacets.stateOf(it, facet.filterIndex, index) } ?: FacetState.Off
            TagPill(
                text = name,
                state = state,
                onClick = { actions.onTag(facet.filterIndex, index) },
                onLongClick = if (facet.excludable) {
                    { actions.onExcludeTag(facet.filterIndex, index) }
                } else {
                    null
                },
            )
        }
        is CatalogFacet.Choice -> {
            val selected =
                (applied?.let { CatalogFacets.choiceOf(it, facet.filterIndex) } ?: facet.defaultIndex) == index
            TagPill(
                text = name,
                state = if (selected) FacetState.Included else FacetState.Off,
                onClick = { actions.onChoice(facet.filterIndex, index) },
                onLongClick = null,
                showCheck = false,
            )
        }
    }
}

@Composable
private fun ActiveFacetRow(active: List<ActiveFacet>, actions: FacetActions) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "clear-all") {
            Pill(
                onClick = actions.onClearAll,
                height = 28.dp,
                background = Color.Transparent,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    text = stringResource(MR.strings.kurotoon_browse_clearAll, active.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        items(active, key = { "active-${it.filterIndex}-${it.childIndex}" }) { item ->
            Pill(
                onClick = { actions.onClear(item) },
                height = 28.dp,
                background = if (item.excluded) {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                } else {
                    KuroColors.Crimson.copy(alpha = 0.22f)
                },
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (item.excluded) MaterialTheme.colorScheme.onSurfaceVariant else KuroColors.Crimson,
                    textDecoration = if (item.excluded) TextDecoration.LineThrough else null,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Icon(
                    imageVector = MaterialSymbols.Rounded.Close,
                    contentDescription = stringResource(MR.strings.action_remove),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun FacetTab(title: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "facetTabContent",
    )
    Column(
        modifier = Modifier
            .clip(CircleShape)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                color = content,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
            )
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(KuroColors.Crimson),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (count > 9) "9+" else count.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .size(width = if (selected) 18.dp else 0.dp, height = 2.dp)
                .clip(CircleShape)
                .background(KuroColors.Crimson),
        )
    }
}

@Composable
private fun ExpandToggle(expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Pill(
        onClick = onClick,
        height = 30.dp,
        background = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        val icon = if (expanded) MaterialSymbols.Rounded.KeyboardArrowUp else MaterialSymbols.Rounded.KeyboardArrowDown
        val label = if (expanded) MR.strings.kurotoon_browse_showLess else MR.strings.kurotoon_browse_showAll
        Icon(
            imageVector = icon,
            contentDescription = stringResource(label),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SortPill(text: String, selected: Boolean, onClick: () -> Unit, icon: ImageVector? = null) {
    val background by animateColorAsState(
        if (selected) KuroColors.Crimson else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "sortPillBackground",
    )
    val content by animateColorAsState(
        if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        label = "sortPillContent",
    )
    Pill(onClick = onClick, height = 34.dp, background = background) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        }
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
internal fun TagPill(
    text: String,
    state: FacetState,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    showCheck: Boolean = true,
) {
    val background by animateColorAsState(
        when (state) {
            FacetState.Included -> KuroColors.Crimson.copy(alpha = 0.18f)
            FacetState.Excluded -> MaterialTheme.colorScheme.surfaceContainerHighest
            FacetState.Off -> MaterialTheme.colorScheme.surfaceContainer
        },
        label = "tagPillBackground",
    )
    val content by animateColorAsState(
        when (state) {
            FacetState.Included -> KuroColors.Crimson
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "tagPillContent",
    )
    val border by animateColorAsState(
        when (state) {
            FacetState.Included -> KuroColors.Crimson.copy(alpha = 0.6f)
            FacetState.Excluded -> MaterialTheme.colorScheme.outline
            FacetState.Off -> Color.Transparent
        },
        label = "tagPillBorder",
    )
    Pill(
        onClick = onClick,
        onLongClick = onLongClick,
        height = 30.dp,
        background = background,
        border = BorderStroke(1.dp, border),
    ) {
        when {
            state == FacetState.Included && showCheck ->
                Icon(
                    MaterialSymbols.Rounded.Check,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(14.dp),
                )
            state == FacetState.Excluded ->
                Icon(
                    MaterialSymbols.Rounded.Block,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(14.dp),
                )
            else -> {}
        }
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (state != FacetState.Off) FontWeight.SemiBold else FontWeight.Normal,
            textDecoration = if (state == FacetState.Excluded) TextDecoration.LineThrough else null,
            maxLines = 1,
        )
    }
}

@Composable
internal fun Pill(
    onClick: () -> Unit,
    height: Dp,
    background: Color,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, label = "pillScale")
    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(height)
            .clip(CircleShape)
            .background(background)
            .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content()
    }
}

/** Facets with more values than this get a button to show them all at once. */
private const val COLLAPSED_LIMIT = 12

/** Facets with more values than this get a find box when expanded. */
private const val FIND_LIMIT = 24
