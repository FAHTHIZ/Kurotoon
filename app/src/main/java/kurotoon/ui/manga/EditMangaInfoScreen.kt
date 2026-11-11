package kurotoon.ui.manga

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kurotoon.manga.MangaEdit
import mihon.app.di.appGraph
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaRemoteUpdate
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Edit a series' title, people, description and tags, the way TachiyomiSY lets you. The edits are applied at once
 * and put back after every library refresh, so the source can't undo them.
 */
class EditMangaInfoScreen(private val mangaId: Long) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val graph = remember { context.appGraph }
        val scope = rememberCoroutineScope()
        var manga by remember { mutableStateOf<Manga?>(null) }

        var title by rememberSaveable { mutableStateOf("") }
        var author by rememberSaveable { mutableStateOf("") }
        var artist by rememberSaveable { mutableStateOf("") }
        var description by rememberSaveable { mutableStateOf("") }
        var tags by rememberSaveable { mutableStateOf("") }
        var loaded by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(mangaId) {
            val m = withContext(Dispatchers.IO) { graph.mangaRepository.getMangaById(mangaId) }
            manga = m
            if (!loaded) {
                title = m.title
                author = m.author.orEmpty()
                artist = m.artist.orEmpty()
                description = m.description.orEmpty()
                tags = m.genre.orEmpty().joinToString(", ")
                loaded = true
            }
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.kurotoon_editInfo_title),
                    subtitle = manga?.title,
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Field(MR.strings.kurotoon_editInfo_name, title) { title = it }
                Field(MR.strings.kurotoon_editInfo_author, author) { author = it }
                Field(MR.strings.kurotoon_editInfo_artist, artist) { artist = it }
                Field(MR.strings.kurotoon_editInfo_tags, tags) { tags = it }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(MR.strings.kurotoon_editInfo_description)) },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(MR.strings.kurotoon_editInfo_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            graph.kuroPreferences.setMangaEdit(mangaId, null)
                            context.toast(MR.strings.kurotoon_editInfo_resetDone)
                            navigator.pop()
                        },
                    ) { Text(stringResource(MR.strings.kurotoon_editInfo_reset)) }
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = manga != null && title.isNotBlank(),
                        onClick = {
                            val m = manga ?: return@Button
                            val edit = MangaEdit(
                                title = title.takeIf { it.trim() != m.title },
                                author = author.takeIf { it.trim() != m.author.orEmpty() },
                                artist = artist.takeIf { it.trim() != m.artist.orEmpty() },
                                description = description.takeIf { it.trim() != m.description.orEmpty() },
                                genres = MangaEdit.parseGenres(tags).takeIf { it != m.genre.orEmpty() },
                            ).cleaned()
                            scope.launch {
                                val previous = graph.kuroPreferences.mangaEdit(mangaId) ?: MangaEdit()
                                val merged = MangaEdit(
                                    title = edit.title ?: previous.title,
                                    author = edit.author ?: previous.author,
                                    artist = edit.artist ?: previous.artist,
                                    description = edit.description ?: previous.description,
                                    genres = edit.genres ?: previous.genres,
                                )
                                graph.kuroPreferences.setMangaEdit(mangaId, merged)
                                withContext(Dispatchers.IO) { graph.mangaRepository.updateRemote(m.applying(edit)) }
                                context.toast(MR.strings.kurotoon_editInfo_saved)
                                navigator.pop()
                            }
                        },
                    ) { Text(stringResource(MR.strings.action_save)) }
                }
            }
        }
    }
}

@Composable
private fun Field(label: dev.icerock.moko.resources.StringResource, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun Manga.applying(edit: MangaEdit) = MangaRemoteUpdate(
    id = id,
    title = edit.title,
    author = edit.author ?: author,
    artist = edit.artist ?: artist,
    description = edit.description ?: description,
    genre = edit.genres ?: genre,
    status = status,
    thumbnailUrl = thumbnailUrl,
    updateStrategy = updateStrategy,
    memo = memo,
    initialized = initialized,
    coverLastModified = null,
)
