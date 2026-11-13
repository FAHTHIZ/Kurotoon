package kurotoon.ui.updates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import kurotoon.feature.migration.config.MigrationConfigScreen
import kurotoon.icons.materialsymbols.MaterialSymbols
import kurotoon.icons.materialsymbols.rounded.Close
import kurotoon.icons.materialsymbols.rounded.Error
import kurotoon.updates.UpdateError
import kurotoon.updates.UpdateErrors
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.KuroTextButton
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen

/** Every series that failed to update, grouped by what went wrong, with a fix one tap away. */
class UpdateErrorsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val store = remember { UpdateErrors.get(context) }
        val errors by store.errors.collectAsState()
        val unknown = stringResource(MR.strings.unknown_error)
        val groups = remember(errors, unknown) {
            errors.groupBy { it.error.ifBlank { unknown } }.toList().sortedByDescending { it.second.size }
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.kurotoon_updateErrors_title),
                    subtitle = if (errors.isEmpty()) null else errors.size.toString(),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                    actions = {
                        if (errors.isNotEmpty()) {
                            KuroTextButton(onClick = store::clear) {
                                Text(stringResource(MR.strings.kurotoon_updateErrors_clear))
                            }
                        }
                    },
                )
            },
        ) { padding ->
            if (errors.isEmpty()) {
                EmptyScreen(
                    message = stringResource(MR.strings.kurotoon_updateErrors_empty),
                    modifier = Modifier.padding(padding),
                )
                return@Scaffold
            }
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
            ) {
                groups.forEach { (reason, items) ->
                    item(key = "h-$reason") {
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(items, key = { "m-${it.mangaId}" }) { error ->
                        ErrorRow(
                            error = error,
                            onOpen = { navigator.push(MangaScreen(error.mangaId)) },
                            onMigrate = { navigator.push(MigrationConfigScreen(error.mangaId)) },
                            onDismiss = { store.dismiss(error.mangaId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorRow(
    error: UpdateError,
    onOpen: () -> Unit,
    onMigrate: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = error.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = error.source,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        KuroTextButton(onClick = onMigrate) { Text(stringResource(MR.strings.action_migrate)) }
        IconButton(onClick = onDismiss) {
            Icon(
                MaterialSymbols.Rounded.Close,
                contentDescription = stringResource(MR.strings.kurotoon_updateErrors_dismiss),
            )
        }
    }
}

/** A slim card at the top of Updates while the last update left failures behind. */
@Composable
fun UpdateErrorsBanner(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current ?: return
    val context = LocalContext.current
    val store = remember { UpdateErrors.get(context) }
    val errors by store.errors.collectAsState()
    if (errors.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = { navigator.push(UpdateErrorsScreen()) },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Icon(MaterialSymbols.Rounded.Error, contentDescription = null)
            Text(
                text = pluralStringResource(MR.plurals.kurotoon_updateErrors_banner, errors.size, errors.size),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(MR.strings.kurotoon_updateErrors_review),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
