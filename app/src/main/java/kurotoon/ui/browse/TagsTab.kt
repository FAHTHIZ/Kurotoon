package kurotoon.ui.browse

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kurotoon.catalogue.HiddenTags
import kurotoon.catalogue.SourceTagIndex
import kurotoon.catalogue.TagSections
import kurotoon.catalogue.addHiddenTag
import kurotoon.library.LibraryTags
import kurotoon.ui.KuroFilterPill
import kurotoon.ui.KuroSectionTitle
import kurotoon.ui.catalogue.TagEverywhereScreen
import kurotoon.ui.kuroBounceClickable
import kurotoon.ui.kuroCard
import kurotoon.ui.kuroShimmer
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Label
import mihon.icons.materialsymbols.rounded.Block
import mihon.icons.materialsymbols.rounded.Search
import mihon.icons.materialsymbols.rounded.TravelExplore
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Browse › Tags: every tag offered by every installed source in one place, searchable, by
 * popularity or A–Z. Tap a tag to browse it on every source; long-press for more.
 */
@Composable
fun Screen.tagsTab(): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    return TabContent(
        titleRes = MR.strings.kurotoon_tags_tab,
        actions = listOf(
            AppBar.Action(
                title = stringResource(MR.strings.kurotoon_browse_hiddenTags),
                icon = MaterialSymbols.Rounded.Block,
                onClick = { navigator.push(HiddenTagsScreen()) },
            ),
        ),
        content = { contentPadding, _ ->
            TagsPage(
                contentPadding = contentPadding,
                onTag = { navigator.push(TagEverywhereScreen(it)) },
                onSearchText = { navigator.push(GlobalSearchScreen(it)) },
            )
        },
    )
}

private enum class TagOrder { Popular, Alphabetical }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagsPage(
    contentPadding: PaddingValues,
    onTag: (String) -> Unit,
    onSearchText: (String) -> Unit,
) {
    val context = LocalContext.current
    val graph = context.appGraph
    val all by remember { SourceTagIndex.watch(graph.sourceManager, graph.sourcePreferences) }
        .collectAsState(null)
    val hiddenPref = remember { graph.kuroPreferences.hiddenTags }
    val hiddenRaw by hiddenPref.changes().collectAsState(hiddenPref.get())
    val hidden = remember(hiddenRaw) { HiddenTags.parse(hiddenRaw) }
    val sourceCount by remember {
        graph.sourceManager.sources.map { list -> list.count { it is CatalogueSource && it.id != 0L } }
    }.collectAsState(0)
    val libraryTags by produceState(emptyList<String>()) {
        value = runCatching {
            val library = withContext(Dispatchers.IO) { graph.getLibraryManga.await() }
            LibraryTags.top(library.map { it.manga.genre }, LIBRARY_TAGS).map { it.name }
        }.getOrDefault(emptyList())
    }
    TagsContent(
        tags = all,
        hidden = hidden,
        libraryTags = libraryTags,
        sourceCount = sourceCount,
        contentPadding = contentPadding,
        onTag = onTag,
        onSearchText = onSearchText,
        onHide = { tag ->
            hiddenPref.getAndSet { addHiddenTag(it, tag.name) }
            context.toast(context.stringResource(MR.strings.kurotoon_tags_hiddenNow, tag.name))
        },
    )
}

/** The Tags page itself, given the index; split out so it can be previewed without the app graph. */
@Composable
internal fun TagsContent(
    tags: List<SourceTagIndex.Tag>?,
    hidden: Set<String>,
    libraryTags: List<String>,
    sourceCount: Int,
    contentPadding: PaddingValues,
    onTag: (String) -> Unit,
    onSearchText: (String) -> Unit,
    onHide: (SourceTagIndex.Tag) -> Unit,
    initialOrder: Boolean = false,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var order by rememberSaveable { mutableStateOf(if (initialOrder) TagOrder.Alphabetical else TagOrder.Popular) }
    var sheetFor by remember { mutableStateOf<SourceTagIndex.Tag?>(null) }

    val shown = remember(tags, hidden, query) { TagSections.visible(tags.orEmpty(), hidden, query) }
    val known = remember(tags) { tags.orEmpty().associateBy { it.name.lowercase() } }
    val yours = remember(libraryTags, known, hidden) {
        libraryTags.mapNotNull { known[it.lowercase()] }.filter { it.name.lowercase() !in hidden }
    }

    LazyColumn(
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
    ) {
        item(key = "search") {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SourceFilterField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = stringResource(
                        MR.strings.kurotoon_tags_search,
                        tags.orEmpty().count {
                            it.name.lowercase() !in
                                hidden
                        },
                    ),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KuroFilterPill(
                        text = stringResource(MR.strings.kurotoon_tags_popular),
                        selected = order == TagOrder.Popular,
                        onClick = { order = TagOrder.Popular },
                    )
                    KuroFilterPill(
                        text = stringResource(MR.strings.kurotoon_tags_az),
                        selected = order == TagOrder.Alphabetical,
                        onClick = { order = TagOrder.Alphabetical },
                    )
                    Box(Modifier.weight(1f))
                    if (tags != null) {
                        Text(
                            text = stringResource(MR.strings.kurotoon_tags_summary, shown.size, sourceCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        when {
            tags == null -> item(key = "loading") { TagPlaceholders() }
            tags.isEmpty() -> item(key = "empty") {
                TagsMessage(MaterialSymbols.Rounded.TravelExplore, stringResource(MR.strings.kurotoon_tags_none))
            }
            shown.isEmpty() -> item(key = "nomatch") {
                Column(modifier = Modifier.padding(16.dp)) {
                    TagsMessage(
                        MaterialSymbols.Rounded.Search,
                        stringResource(MR.strings.kurotoon_tags_noMatch, query.trim()),
                    )
                    Row(modifier = Modifier.padding(top = 12.dp)) {
                        KuroFilterPill(
                            text = stringResource(MR.strings.kurotoon_tags_searchTitles),
                            selected = true,
                            onClick = { onSearchText(query.trim()) },
                        )
                    }
                }
            }
            else -> {
                if (query.isBlank() && yours.isNotEmpty()) {
                    item(key = "yours") {
                        TagGroup(
                            title = stringResource(MR.strings.kurotoon_tags_yours),
                            tags = yours,
                            onTag = onTag,
                            onLongTag = { sheetFor = it },
                            highlight = true,
                        )
                    }
                }
                when (order) {
                    TagOrder.Popular -> item(key = "popular") {
                        TagGroup(
                            title = if (query.isBlank()) stringResource(MR.strings.kurotoon_tags_popular) else null,
                            tags = shown,
                            onTag = onTag,
                            onLongTag = { sheetFor = it },
                        )
                    }
                    TagOrder.Alphabetical -> TagSections.alphabetical(shown).forEach { section ->
                        item(key = "letter-${section.label}") {
                            TagGroup(
                                title = section.label,
                                tags = section.tags,
                                onTag = onTag,
                                onLongTag = { sheetFor = it },
                                count = section.tags.size,
                            )
                        }
                    }
                }
            }
        }
    }

    sheetFor?.let { tag ->
        TagActionSheet(
            tag = tag,
            onBrowse = {
                sheetFor = null
                onTag(tag.name)
            },
            onSearch = {
                sheetFor = null
                onSearchText(tag.name)
            },
            onHide = {
                sheetFor = null
                onHide(tag)
            },
            onDismiss = { sheetFor = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagGroup(
    title: String?,
    tags: List<SourceTagIndex.Tag>,
    onTag: (String) -> Unit,
    onLongTag: (SourceTagIndex.Tag) -> Unit,
    highlight: Boolean = false,
    count: Int? = null,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (title != null) {
            Row(
                modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KuroSectionTitle(text = title)
                if (count != null) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        } else {
            Box(Modifier.height(16.dp))
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tags.forEach { tag ->
                TagChip(tag = tag, highlight = highlight, onClick = {
                    onTag(tag.name)
                }, onLongClick = { onLongTag(tag) })
            }
        }
    }
}

@Composable
private fun TagChip(
    tag: SourceTagIndex.Tag,
    highlight: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .kuroBounceClickable(onClick = onClick, onLongClick = onLongClick)
            .height(34.dp)
            .background(
                if (highlight) primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                CircleShape,
            )
            .padding(start = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            MaterialSymbols.AutoMirroredRounded.Label,
            contentDescription = null,
            tint = primary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = tag.name,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // How many sources offer it
        Text(
            text = tag.sources.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                .padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun TagPlaceholders() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(5) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { col ->
                    Box(
                        modifier = Modifier
                            .width((64 + ((row * 3 + col) % 4) * 18).dp)
                            .height(34.dp)
                            .kuroShimmer(CircleShape, MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                }
            }
        }
    }
}

@Composable
private fun TagsMessage(icon: ImageVector, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .kuroCard(RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TagActionSheet(
    tag: SourceTagIndex.Tag,
    onBrowse: () -> Unit,
    onSearch: () -> Unit,
    onHide: () -> Unit,
    onDismiss: () -> Unit,
) {
    AdaptiveSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = tag.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = stringResource(MR.strings.kurotoon_tags_onSources, tag.sources),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            SheetRow(MaterialSymbols.Rounded.TravelExplore, stringResource(MR.strings.kurotoon_tags_browse), onBrowse)
            SheetRow(MaterialSymbols.Rounded.Search, stringResource(MR.strings.kurotoon_tags_searchTitles), onSearch)
            SheetRow(MaterialSymbols.Rounded.Block, stringResource(MR.strings.kurotoon_tags_hide), onHide)
        }
    }
}

@Composable
private fun SheetRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .kuroBounceClickable(onClick = onClick)
            .kuroCard(RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(text = text, style = MaterialTheme.typography.titleSmall)
    }
}

private const val LIBRARY_TAGS = 16
