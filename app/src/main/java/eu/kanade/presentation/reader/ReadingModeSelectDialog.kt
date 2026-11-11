package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.manga.model.readingMode
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.reader.components.ModeSelectionDialog
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsViewModel
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.LabeledCheckbox
import tachiyomi.presentation.core.components.SettingsIconGrid
import tachiyomi.presentation.core.components.SettingsItemsPaddings
import tachiyomi.presentation.core.components.material.IconToggleButton
import tachiyomi.presentation.core.i18n.stringResource

private val ReadingModesWithoutDefault = ReadingMode.entries - ReadingMode.DEFAULT

@Composable
fun ReadingModeSelectDialog(
    onDismissRequest: () -> Unit,
    viewModel: ReaderSettingsViewModel,
    onChange: (StringResource) -> Unit,
) {
    val manga by viewModel.mangaFlow.collectAsState()
    val readingMode = remember(manga) { ReadingMode.fromPreference(manga?.readingMode?.toInt()) }
    val sourceMode = remember(manga) { viewModel.getSourceReadingMode() }
    val sourceName = remember(manga) { viewModel.sourceName() }

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        DialogContent(
            readingMode = readingMode,
            sourceMode = sourceMode,
            sourceName = sourceName,
            onChangeReadingMode = { mode, forSource ->
                when {
                    forSource -> viewModel.setSourceReadingMode(mode)
                    // Unticked, so the source stops deciding and this series keeps the pick
                    readingMode == ReadingMode.DEFAULT && sourceMode != null -> {
                        viewModel.setSourceReadingMode(null)
                        viewModel.setReadingMode(mode)
                    }
                    else -> viewModel.setReadingMode(mode)
                }
                onChange(mode.stringRes)
                onDismissRequest()
            },
        )
    }
}

@Composable
private fun DialogContent(
    readingMode: ReadingMode,
    onChangeReadingMode: (ReadingMode, forSource: Boolean) -> Unit,
    sourceMode: ReadingMode? = null,
    sourceName: String? = null,
) {
    // A series following its source shows the source's mode as the pick
    val followsSource = readingMode == ReadingMode.DEFAULT && sourceMode != null
    var selected by remember { mutableStateOf(if (followsSource) sourceMode!! else readingMode) }
    var forSource by remember { mutableStateOf(followsSource) }

    ModeSelectionDialog(
        onUseDefault = { onChangeReadingMode(ReadingMode.DEFAULT, false) }
            .takeIf { readingMode != ReadingMode.DEFAULT || sourceMode != null },
        onApply = { onChangeReadingMode(selected, forSource) },
    ) {
        SettingsIconGrid(MR.strings.pref_category_reading_mode) {
            items(ReadingModesWithoutDefault) { mode ->
                IconToggleButton(
                    checked = mode == selected,
                    onCheckedChange = {
                        selected = mode
                    },
                    modifier = Modifier.fillMaxWidth(),
                    imageVector = mode.icon,
                    title = stringResource(mode.stringRes),
                )
            }
        }
        if (sourceName != null) {
            LabeledCheckbox(
                label = stringResource(MR.strings.kurotoon_reader_modeForSource, sourceName),
                checked = forSource,
                onCheckedChange = { forSource = it },
                modifier = Modifier.padding(horizontal = SettingsItemsPaddings.Horizontal),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun DialogContentPreview() {
    TachiyomiPreviewTheme {
        Surface {
            Column {
                DialogContent(
                    readingMode = ReadingMode.DEFAULT,
                    onChangeReadingMode = { _, _ -> },
                )

                DialogContent(
                    readingMode = ReadingMode.LEFT_TO_RIGHT,
                    onChangeReadingMode = { _, _ -> },
                )
            }
        }
    }
}
