package eu.kanade.presentation.reader.appbars

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import mihon.icons.custommaterialsymbols.CustomMaterialSymbols
import mihon.icons.custommaterialsymbols.rounded.CropOff
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.FormatListBulleted
import mihon.icons.materialsymbols.rounded.Crop
import mihon.icons.materialsymbols.rounded.MusicNote
import mihon.icons.materialsymbols.rounded.PlayArrow
import mihon.icons.materialsymbols.rounded.Settings
import mihon.icons.materialsymbols.rounded.VolumeOff
import mihon.icons.materialsymbols.roundedfilled.Pause
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ReaderBottomBar(
    readingMode: ReadingMode,
    onClickReadingMode: () -> Unit,
    orientation: ReaderOrientation,
    onClickOrientation: () -> Unit,
    cropEnabled: Boolean,
    onClickCropBorder: () -> Unit,
    onClickSettings: () -> Unit,
    autoScrolling: Boolean,
    onClickAutoScroll: () -> Unit,
    modifier: Modifier = Modifier,
    onClickChapterList: () -> Unit = {},
    soundActive: Boolean = false,
    soundMuted: Boolean = false,
    onClickSound: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .pointerInput(Unit) {},
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClickChapterList) {
            Icon(
                imageVector = MaterialSymbols.AutoMirroredRounded.FormatListBulleted,
                contentDescription = stringResource(MR.strings.kurotoon_reader_chapterList),
            )
        }

        IconButton(onClick = onClickReadingMode) {
            Icon(
                imageVector = readingMode.icon,
                contentDescription = stringResource(MR.strings.viewer),
            )
        }

        IconButton(onClick = onClickOrientation) {
            Icon(
                imageVector = orientation.icon,
                contentDescription = stringResource(MR.strings.rotation_type),
            )
        }

        IconButton(onClick = onClickCropBorder) {
            Icon(
                imageVector = if (cropEnabled) MaterialSymbols.Rounded.Crop else CustomMaterialSymbols.Rounded.CropOff,
                contentDescription = stringResource(MR.strings.pref_crop_borders),
            )
        }

        IconButton(onClick = onClickAutoScroll) {
            Icon(
                imageVector = if (autoScrolling) {
                    MaterialSymbols.RoundedFilled.Pause
                } else {
                    MaterialSymbols.Rounded.PlayArrow
                },
                contentDescription = stringResource(MR.strings.kurotoon_reader_autoScroll),
                tint = if (autoScrolling) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            )
        }

        IconButton(onClick = onClickSound) {
            Icon(
                imageVector = if (soundMuted) MaterialSymbols.Rounded.VolumeOff else MaterialSymbols.Rounded.MusicNote,
                contentDescription = stringResource(MR.strings.kurotoon_sound_title),
                tint = if (soundActive && !soundMuted) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            )
        }

        IconButton(onClick = onClickSettings) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Settings,
                contentDescription = stringResource(MR.strings.action_settings),
            )
        }
    }
}
