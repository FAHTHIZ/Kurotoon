package eu.kanade.presentation.library.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.KuroTextButton
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun GlobalSearchItem(
    searchQuery: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KuroTextButton(
        modifier = modifier,
        onClick = onClick,
    ) {
        Text(
            text = stringResource(MR.strings.action_global_search_query, searchQuery),
            modifier = Modifier.zIndex(99f),
        )
    }
}
