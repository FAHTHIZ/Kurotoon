package kurotoon.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kurotoon.catalogue.HiddenTags
import kurotoon.catalogue.addHiddenTag
import kurotoon.library.LibraryTags
import logcat.LogPriority
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Add
import mihon.icons.materialsymbols.rounded.Close
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.KuroFieldShape
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.kuroFieldColors
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/** Hidden tags as removable chips, with the library's own tags offered as one-tap additions. */
class HiddenTagsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val preference = remember { context.appGraph.kuroPreferences.hiddenTags }
        val raw by preference.collectAsState()
        var libraryTags by remember { mutableStateOf(emptyList<String>()) }

        LaunchedEffect(Unit) {
            libraryTags = runCatching {
                val library = withContext(Dispatchers.IO) { context.appGraph.getLibraryManga.await() }
                LibraryTags.top(library.map { it.manga.genre }, SUGGESTION_POOL).map { it.name }
            }
                .onFailure { logcat(LogPriority.WARN, it) }
                .getOrDefault(emptyList())
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.kurotoon_browse_hiddenTags),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            HiddenTagsContent(
                hidden = HiddenTags.names(raw),
                suggestions = libraryTags,
                onAdd = { tag -> preference.getAndSet { addHiddenTag(it, tag) } },
                onRemove = { tag -> preference.getAndSet { HiddenTags.remove(it, tag) } },
                modifier = Modifier.padding(padding),
            )
        }
    }

    private companion object {
        const val SUGGESTION_POOL = 40
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HiddenTagsContent(
    hidden: List<String>,
    suggestions: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by rememberSaveable { mutableStateOf("") }
    val submit = {
        // Pasting "a, b, c" adds each one
        HiddenTags.names(input).forEach(onAdd)
        input = ""
    }
    val hiddenKeys = hidden.map { it.lowercase() }.toSet()
    val offers = suggestions.filter { it.lowercase() !in hiddenKeys }.take(SUGGESTIONS)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(MR.strings.kurotoon_hiddenTags_info),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                shape = KuroFieldShape,
                colors = kuroFieldColors(),
                value = input,
                onValueChange = { input = it },
                singleLine = true,
                placeholder = { Text(stringResource(MR.strings.kurotoon_hiddenTags_add)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = submit, enabled = input.isNotBlank()) {
                Icon(MaterialSymbols.Rounded.Add, contentDescription = stringResource(MR.strings.action_add))
            }
        }
        if (hidden.isEmpty()) {
            Text(
                text = stringResource(MR.strings.kurotoon_hiddenTags_none),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                hidden.forEach { tag ->
                    val removeLabel = stringResource(MR.strings.action_remove)
                    kurotoon.ui.KuroFilterPill(
                        text = tag,
                        selected = true,
                        onClick = { onRemove(tag) },
                        icon = MaterialSymbols.Rounded.Close,
                        modifier = Modifier.semantics { onClick(label = removeLabel, action = null) },
                    )
                }
            }
        }
        if (offers.isNotEmpty()) {
            Text(
                text = stringResource(MR.strings.kurotoon_hiddenTags_fromLibrary),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                offers.forEach { tag ->
                    kurotoon.ui.KuroFilterPill(
                        text = tag,
                        selected = false,
                        onClick = { onAdd(tag) },
                        icon = MaterialSymbols.Rounded.Add,
                    )
                }
            }
        }
    }
}

private const val SUGGESTIONS = 24
