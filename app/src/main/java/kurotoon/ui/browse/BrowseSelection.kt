package kurotoon.ui.browse

import androidx.compose.runtime.compositionLocalOf

/** Ids of the titles picked in Browse while selecting several to add to the library at once. */
val LocalBrowseSelection = compositionLocalOf { emptySet<Long>() }
