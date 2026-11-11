package kurotoon.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import kurotoon.catalogue.FacetState
import kurotoon.ui.browse.TagPill
import mihon.app.di.appGraph
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.TriState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/**
 * One-tap library filters above the grid, so the common ones don't need the filter sheet.
 * A tap cycles a filter the same way the sheet does: only, then exclude, then off.
 * They write the same preferences as the sheet, so both always agree.
 */
@Composable
fun LibraryQuickFilters(modifier: Modifier = Modifier) {
    val prefs = LocalContext.current.appGraph.libraryPreferences
    val filters = listOf(
        MR.strings.kurotoon_library_quick_unread to prefs.filterUnread,
        MR.strings.kurotoon_library_quick_started to prefs.filterStarted,
        MR.strings.kurotoon_library_quick_downloaded to prefs.filterDownloaded,
        MR.strings.kurotoon_library_quick_bookmarked to prefs.filterBookmarked,
        MR.strings.kurotoon_library_quick_completed to prefs.filterCompleted,
        MR.strings.kurotoon_library_quick_oneShot to prefs.filterOneShot,
    )
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(filters) { (label, pref) ->
            QuickFilterPill(label, pref)
        }
    }
}

@Composable
private fun QuickFilterPill(label: StringResource, pref: Preference<TriState>) {
    val state by pref.collectAsState()
    TagPill(
        text = stringResource(label),
        state = when (state) {
            TriState.DISABLED -> FacetState.Off
            TriState.ENABLED_IS -> FacetState.Included
            TriState.ENABLED_NOT -> FacetState.Excluded
        },
        onClick = { pref.set(state.next()) },
        onLongClick = { pref.set(TriState.DISABLED) },
    )
}
