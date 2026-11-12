package kurotoon.reader

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.reader.reportPress
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Favorite
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.KuroButton
import tachiyomi.presentation.core.components.material.KuroButtonStyle
import tachiyomi.presentation.core.i18n.stringResource

/**
 * WEBTOON asks you to subscribe when an episode ends. Between chapters of a series that isn't in
 * the library yet, this offers the same in one tap; it disappears once the series is added.
 */
@Composable
fun AddToLibraryPrompt(onButtonPressChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.readerActivity() } ?: return
    val state by activity.viewModel.state.collectAsState()
    val manga = state.manga ?: return
    if (manga.favorite || manga.source == LOCAL_SOURCE_ID) return
    KuroButton(
        style = KuroButtonStyle.Tonal,
        onClick = activity.viewModel::addToLibrary,
        modifier = Modifier
            .padding(top = 20.dp)
            .reportPress(onButtonPressChange),
    ) {
        Icon(MaterialSymbols.Rounded.Favorite, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        Text(stringResource(MR.strings.kurotoon_reader_addToLibrary))
    }
}

private const val LOCAL_SOURCE_ID = 0L

private tailrec fun Context.readerActivity(): ReaderActivity? = when (this) {
    is ReaderActivity -> this
    is ContextWrapper -> baseContext.readerActivity()
    else -> null
}
