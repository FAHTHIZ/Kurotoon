package eu.kanade.presentation.browse.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import kurotoon.ui.KuroFilterPill
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun GlobalSearchToolbar(
    searchQuery: String?,
    progress: Int,
    total: Int,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    hideSourceFilter: Boolean,
    sourceFilter: SourceFilter,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onlyShowHasResults: Boolean,
    onToggleResults: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    merged: Boolean? = null,
    onToggleMerged: (Boolean) -> Unit = {},
) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        Box {
            SearchToolbar(
                searchQuery = searchQuery,
                onChangeSearchQuery = onChangeSearchQuery,
                onSearch = onSearch,
                onClickCloseSearch = navigateUp,
                navigateUp = navigateUp,
                scrollBehavior = scrollBehavior,
            )
            if (progress in 0..<total) {
                val fraction by animateFloatAsState(progress / total.toFloat(), label = "searchProgress")
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(3.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (merged != null) {
                KuroFilterPill(
                    text = stringResource(MR.strings.kurotoon_search_merged),
                    selected = merged,
                    onClick = { onToggleMerged(true) },
                )
                KuroFilterPill(
                    text = stringResource(MR.strings.kurotoon_search_bySource),
                    selected = !merged,
                    onClick = { onToggleMerged(false) },
                )
                PillDivider()
            }
            if (!hideSourceFilter) {
                KuroFilterPill(
                    text = stringResource(MR.strings.pinned_sources),
                    selected = sourceFilter == SourceFilter.PinnedOnly,
                    onClick = { onChangeSearchFilter(SourceFilter.PinnedOnly) },
                )
                KuroFilterPill(
                    text = stringResource(MR.strings.all),
                    selected = sourceFilter == SourceFilter.All,
                    onClick = { onChangeSearchFilter(SourceFilter.All) },
                )
            }
            // Only meaningful when results are grouped by source
            if (merged != true) {
                if (!hideSourceFilter) PillDivider()
                KuroFilterPill(
                    text = stringResource(MR.strings.has_results),
                    selected = onlyShowHasResults,
                    onClick = { onToggleResults() },
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}

@Composable
private fun PillDivider() {
    Box(
        modifier = Modifier
            .size(width = 1.dp, height = 20.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}
