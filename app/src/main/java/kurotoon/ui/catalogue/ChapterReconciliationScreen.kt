package kurotoon.ui.catalogue

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import kurotoon.catalogue.ChapterReconciler
import kurotoon.catalogue.ChapterSourceSnapshot
import kurotoon.catalogue.LocalChapterSnapshotStore
import kurotoon.catalogue.ReconciledChapter
import kurotoon.catalogue.ReconciledSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChapterReconciliationScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val store = remember { LocalChapterSnapshotStore(context.applicationContext) }
        var snapshots by remember { mutableStateOf<List<ChapterSourceSnapshot>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var isImporting by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf<String?>(null) }

        val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    isImporting = true
                    message = null
                    try {
                        val contents = withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                                it.readText()
                            } ?: error("The selected file could not be opened.")
                        }
                        val imported = store.importSnapshot(contents)
                        snapshots = store.loadSnapshots()
                        message = "Imported " + imported.chapters.size + " chapters from " + imported.sourceLabel + "."
                    } catch (error: Exception) {
                        message = error.message ?: "Import failed."
                    } finally {
                        isImporting = false
                    }
                }
            }
        }

        LaunchedEffect(store) {
            try {
                snapshots = store.loadSnapshots()
            } catch (error: Exception) {
                message = error.message ?: "Could not read the local chapter store."
            } finally {
                isLoading = false
            }
        }

        val results = remember(snapshots) { ChapterReconciler.reconcile(snapshots) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Local chapter reconciliation") },
                    navigationIcon = { TextButton(onClick = navigator::pop) { Text("Back") } },
                )
            },
        ) { contentPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = contentPadding.calculateTopPadding() + 12.dp,
                    bottom = contentPadding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Merge chapter lists you already have", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Import one JSON snapshot per local catalogue. KuroToon combines matching chapter " +
                                    "numbers, keeps every original title and source label, and reports numbering gaps. " +
                                    "This screen reads imported files only; it does not contact extensions or providers.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                                enabled = !isImporting,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (isImporting) "Importing…" else "Import chapter snapshot")
                            }
                            message?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
                item {
                    Text("Local snapshots (" + snapshots.size + ")", style = MaterialTheme.typography.titleMedium)
                }
                if (isLoading) {
                    item { Text("Loading local snapshots…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else if (snapshots.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("Nothing imported yet", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Choose a JSON file using the format below. Imported snapshots are stored in " +
                                        "KuroToon's private app storage, separate from the inherited reader database.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(SNAPSHOT_EXAMPLE, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    items(snapshots, key = { it.snapshotId }) { snapshot ->
                        SnapshotCard(snapshot) {
                            scope.launch {
                                try {
                                    store.deleteSnapshot(snapshot.snapshotId)
                                    snapshots = store.loadSnapshots()
                                    message = "Removed " + snapshot.sourceLabel + "."
                                } catch (error: Exception) {
                                    message = error.message ?: "Could not remove this snapshot."
                                }
                            }
                        }
                    }
                }
                if (results.isNotEmpty()) {
                    item {
                        Text(
                            "Unified chapter lists",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(results, key = { it.seriesTitle }) { series -> SeriesReconciliationCard(series) }
                }
            }
        }
    }
}

@Composable
private fun SnapshotCard(snapshot: ChapterSourceSnapshot, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(snapshot.sourceLabel, style = MaterialTheme.typography.titleSmall)
                Text(snapshot.seriesTitle, style = MaterialTheme.typography.bodyMedium)
                Text(
                    snapshot.chapters.size.toString() + " chapter records",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onDelete) { Text("Remove") }
        }
    }
}

@Composable
private fun SeriesReconciliationCard(series: ReconciledSeries) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(series.seriesTitle, style = MaterialTheme.typography.titleMedium)
            Text(
                series.chapters.size.toString() + " unified chapters · " +
                    series.importedRecordCount + " records · " + series.sourceCount + " sources",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (series.missingChapterNumbers.isNotEmpty()) {
                Text(
                    "Numbering gaps: " + series.missingChapterNumbers.joinToString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Text(
                    "No gaps detected in the integer chapter sequence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            series.chapters.forEach { ReconciledChapterRow(it) }
        }
    }
}

@Composable
private fun ReconciledChapterRow(chapter: ReconciledChapter) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = (chapter.chapterNumber?.let { "Chapter " + it + " · " } ?: "") + chapter.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (chapter.needsReview) {
                Text("Review", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            } else if (chapter.hasTitleVariants) {
                Text(
                    "Title variants",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        chapter.variants.forEach { variant ->
            Text(
                text = variant.sourceLabel + " — " + variant.title +
                    (variant.releaseLabel?.let { " · " + it } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val SNAPSHOT_EXAMPLE = """{
  "schemaVersion": 1,
  "seriesTitle": "Example Series",
  "sourceLabel": "Local catalogue A",
  "chapters": [
    { "recordId": "a-1", "title": "Chapter 1", "number": "1" },
    { "recordId": "a-5", "title": "Chapter 5", "number": "5" },
    { "recordId": "a-8", "title": "Chapter 8", "number": "8" }
  ]
}"""
