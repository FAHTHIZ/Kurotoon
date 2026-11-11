package eu.kanade.presentation.library.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.SearchToolbar
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.rounded.FlipToBack
import mihon.icons.materialsymbols.rounded.SelectAll
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.Pill
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.theme.active
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun LibraryToolbar(
    hasActiveFilters: Boolean,
    selectedCount: Int,
    title: LibraryToolbarTitle,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
    onClickFilter: () -> Unit,
    onClickRefresh: () -> Unit,
    onClickGlobalUpdate: () -> Unit,
    onClickOpenRandomManga: () -> Unit,
    onClickDuplicates: () -> Unit = {},
    onClickGroupBy: () -> Unit = {},
    /** Null when there's no category on screen to hide. */
    onClickHideCategory: (() -> Unit)? = null,
    activeCategoryHidden: Boolean = false,
    /** Null when no category is hidden. */
    onClickRevealHidden: (() -> Unit)? = null,
    revealingHidden: Boolean = false,
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior?,
) = when {
    selectedCount > 0 -> LibrarySelectionToolbar(
        selectedCount = selectedCount,
        onClickUnselectAll = onClickUnselectAll,
        onClickSelectAll = onClickSelectAll,
        onClickInvertSelection = onClickInvertSelection,
    )
    else -> LibraryRegularToolbar(
        title = title,
        hasFilters = hasActiveFilters,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        onClickFilter = onClickFilter,
        onClickRefresh = onClickRefresh,
        onClickGlobalUpdate = onClickGlobalUpdate,
        onClickOpenRandomManga = onClickOpenRandomManga,
        onClickDuplicates = onClickDuplicates,
        onClickGroupBy = onClickGroupBy,
        onClickHideCategory = onClickHideCategory,
        activeCategoryHidden = activeCategoryHidden,
        onClickRevealHidden = onClickRevealHidden,
        revealingHidden = revealingHidden,
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun LibraryRegularToolbar(
    title: LibraryToolbarTitle,
    hasFilters: Boolean,
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    onClickFilter: () -> Unit,
    onClickRefresh: () -> Unit,
    onClickGlobalUpdate: () -> Unit,
    onClickOpenRandomManga: () -> Unit,
    onClickDuplicates: () -> Unit = {},
    onClickGroupBy: () -> Unit = {},
    onClickHideCategory: (() -> Unit)? = null,
    activeCategoryHidden: Boolean = false,
    onClickRevealHidden: (() -> Unit)? = null,
    revealingHidden: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior?,
) {
    val pillAlpha = if (isSystemInDarkTheme()) 0.12f else 0.08f
    SearchToolbar(
        titleContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title.text,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f, false),
                    overflow = TextOverflow.Ellipsis,
                )
                if (title.numberOfManga != null) {
                    Pill(
                        text = "${title.numberOfManga}",
                        color = MaterialTheme.colorScheme.primary.copy(alpha = pillAlpha + 0.08f),
                        fontSize = 14.sp,
                    )
                }
            }
        },
        searchQuery = searchQuery,
        onChangeSearchQuery = onSearchQueryChange,
        actions = {
            val filterTint = if (hasFilters) MaterialTheme.colorScheme.active else LocalContentColor.current
            // A quick on/off switch, since incognito is meant to be toggled per session (mihon#1577)
            val incognitoPref = androidx.compose.ui.platform.LocalContext.current.appGraph.basePreferences.incognitoMode
            val incognito by incognitoPref.collectAsState()
            AppBarActions(
                listOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_filter),
                        icon = MaterialSymbols.Rounded.FilterList,
                        iconTint = filterTint,
                        onClick = onClickFilter,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_update_library),
                        onClick = onClickGlobalUpdate,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_update_category),
                        onClick = onClickRefresh,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.action_open_random_manga),
                        onClick = onClickOpenRandomManga,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.kurotoon_duplicates_title),
                        onClick = onClickDuplicates,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(MR.strings.kurotoon_library_groupBy),
                        onClick = onClickGroupBy,
                    ),
                    AppBar.OverflowAction(
                        title = stringResource(
                            if (incognito) MR.strings.kurotoon_incognito_off else MR.strings.kurotoon_incognito_on,
                        ),
                        onClick = { incognitoPref.set(!incognito) },
                    ),
                ) + listOfNotNull(
                    onClickHideCategory?.let {
                        AppBar.OverflowAction(
                            title = stringResource(
                                if (activeCategoryHidden) {
                                    MR.strings.kurotoon_category_unhide
                                } else {
                                    MR.strings.kurotoon_category_hide
                                },
                            ),
                            onClick = it,
                        )
                    },
                    onClickRevealHidden?.let {
                        AppBar.OverflowAction(
                            title = stringResource(
                                if (revealingHidden) {
                                    MR.strings.kurotoon_category_concealHidden
                                } else {
                                    MR.strings.kurotoon_category_showHidden
                                },
                            ),
                            onClick = it,
                        )
                    },
                ),
            )
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun LibrarySelectionToolbar(
    selectedCount: Int,
    onClickUnselectAll: () -> Unit,
    onClickSelectAll: () -> Unit,
    onClickInvertSelection: () -> Unit,
) {
    AppBar(
        titleContent = { Text(text = "$selectedCount") },
        actions = {
            AppBarActions(
                listOf(
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_all),
                        icon = MaterialSymbols.Rounded.SelectAll,
                        onClick = onClickSelectAll,
                    ),
                    AppBar.Action(
                        title = stringResource(MR.strings.action_select_inverse),
                        icon = MaterialSymbols.Rounded.FlipToBack,
                        onClick = onClickInvertSelection,
                    ),
                ),
            )
        },
        isActionMode = true,
        onCancelActionMode = onClickUnselectAll,
    )
}

@Immutable
data class LibraryToolbarTitle(
    val text: String,
    val numberOfManga: Int? = null,
)
