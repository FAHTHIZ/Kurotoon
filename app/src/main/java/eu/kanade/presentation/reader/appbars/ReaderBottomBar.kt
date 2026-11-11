package eu.kanade.presentation.reader.appbars

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import kurotoon.ui.kuroBounceClickable
import kurotoon.ui.kuroPopOn
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
        DockButton(onClick = onClickChapterList) {
            Icon(
                imageVector = MaterialSymbols.AutoMirroredRounded.FormatListBulleted,
                contentDescription = stringResource(MR.strings.kurotoon_reader_chapterList),
            )
        }

        DockButton(onClick = onClickReadingMode) {
            Icon(
                imageVector = readingMode.icon,
                contentDescription = stringResource(MR.strings.viewer),
            )
        }

        DockButton(onClick = onClickOrientation) {
            Icon(
                imageVector = orientation.icon,
                contentDescription = stringResource(MR.strings.rotation_type),
            )
        }

        DockButton(onClick = onClickCropBorder, active = cropEnabled) {
            Icon(
                imageVector = if (cropEnabled) MaterialSymbols.Rounded.Crop else CustomMaterialSymbols.Rounded.CropOff,
                contentDescription = stringResource(MR.strings.pref_crop_borders),
            )
        }

        DockButton(onClick = onClickAutoScroll, active = autoScrolling) {
            Icon(
                imageVector = if (autoScrolling) {
                    MaterialSymbols.RoundedFilled.Pause
                } else {
                    MaterialSymbols.Rounded.PlayArrow
                },
                contentDescription = stringResource(MR.strings.kurotoon_reader_autoScroll),
            )
        }

        DockButton(onClick = onClickSound, active = soundActive && !soundMuted) {
            Icon(
                imageVector = if (soundMuted) MaterialSymbols.Rounded.VolumeOff else MaterialSymbols.Rounded.MusicNote,
                contentDescription = stringResource(MR.strings.kurotoon_sound_title),
            )
        }

        DockButton(onClick = onClickSettings) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Settings,
                contentDescription = stringResource(MR.strings.action_settings),
            )
        }
    }
}

/**
 * A reader dock button. Switched-on tools (auto-scroll, sound, crop) sit in a filled accent circle so
 * you can tell what's running without opening anything.
 */
@Composable
private fun DockButton(
    onClick: () -> Unit,
    active: Boolean = false,
    content: @Composable () -> Unit,
) {
    val background by animateColorAsState(
        if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "dockButtonBackground",
    )
    val tint by animateColorAsState(
        if (active) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current,
        label = "dockButtonTint",
    )
    // Squishes on press; a tool that switches on pops with a small tilt
    Box(
        modifier = Modifier
            .size(44.dp)
            .kuroBounceClickable(onClick = onClick, pressed = 0.88f)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.kuroPopOn(active), contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalContentColor provides tint, content = content)
        }
    }
}
