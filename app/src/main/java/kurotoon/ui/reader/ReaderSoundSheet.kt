package kurotoon.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import kurotoon.reader.sound.ReaderSoundtrack
import kurotoon.reader.sound.Soundscape
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.MusicNote
import mihon.icons.materialsymbols.rounded.VolumeOff
import mihon.icons.materialsymbols.rounded.VolumeUp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

val Soundscape.label: StringResource
    get() = when (this) {
        Soundscape.OFF -> MR.strings.kurotoon_sound_off
        Soundscape.AUTO -> MR.strings.kurotoon_sound_auto
        Soundscape.RAIN -> MR.strings.kurotoon_sound_rain
        Soundscape.NIGHT -> MR.strings.kurotoon_sound_night
        Soundscape.FIREPLACE -> MR.strings.kurotoon_sound_fire
        Soundscape.OCEAN -> MR.strings.kurotoon_sound_ocean
        Soundscape.FOCUS -> MR.strings.kurotoon_sound_focus
    }

/** Everything about what the reader plays, one tap from the reader menu. */
@Composable
fun ReaderSoundSheet(
    preferences: ReaderPreferences,
    soundtrack: ReaderSoundtrack,
    onDismissRequest: () -> Unit,
) {
    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(MR.strings.kurotoon_sound_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = soundtrack.muted,
                    onClick = soundtrack::toggleMute,
                    leadingIcon = {
                        Icon(
                            imageVector = if (soundtrack.muted) {
                                MaterialSymbols.Rounded.VolumeOff
                            } else {
                                MaterialSymbols.Rounded.VolumeUp
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    label = {
                        val label = if (soundtrack.muted) {
                            MR.strings.kurotoon_sound_unmute
                        } else {
                            MR.strings.kurotoon_sound_mute
                        }
                        Text(stringResource(label))
                    },
                )
            }

            NowPlaying(soundtrack)

            CheckboxItem(
                label = stringResource(MR.strings.kurotoon_sound_episodeMusic),
                pref = preferences.soundEpisodeMusic,
            )
            Text(
                text = stringResource(MR.strings.kurotoon_sound_episodeMusicSummary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            val musicVolume by preferences.soundMusicVolume.collectAsState()
            SliderItem(
                label = stringResource(MR.strings.kurotoon_sound_musicVolume),
                value = musicVolume / 10,
                valueRange = 0..10,
                valueString = "${musicVolume / 10 * 10}%",
                onChange = { preferences.soundMusicVolume.set(it * 10) },
            )

            AmbienceChips(preferences)

            val ambienceVolume by preferences.soundAmbienceVolume.collectAsState()
            SliderItem(
                label = stringResource(MR.strings.kurotoon_sound_ambienceVolume),
                value = ambienceVolume / 10,
                valueRange = 0..10,
                valueString = "${ambienceVolume / 10 * 10}%",
                onChange = { preferences.soundAmbienceVolume.set(it * 10) },
            )

            CheckboxItem(
                label = stringResource(MR.strings.kurotoon_sound_pageTurn),
                pref = preferences.soundPageTurn,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AmbienceChips(preferences: ReaderPreferences) {
    val ambience by preferences.soundAmbience.collectAsState()
    Text(
        text = stringResource(MR.strings.kurotoon_sound_ambience),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp),
    )
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Soundscape.entries.forEach { scape ->
            FilterChip(
                selected = Soundscape.of(ambience) == scape,
                onClick = { preferences.soundAmbience.set(scape.key) },
                label = { Text(stringResource(scape.label)) },
            )
        }
    }
}

@Composable
private fun NowPlaying(soundtrack: ReaderSoundtrack) {
    val playing = soundtrack.playing
    val tracks = soundtrack.tracks
    val scape = soundtrack.activeScape
    AnimatedVisibility(visible = tracks.isNotEmpty() || scape != Soundscape.OFF, enter = fadeIn(), exit = fadeOut()) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(MaterialSymbols.Rounded.MusicNote, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column {
                    if (tracks.isNotEmpty()) {
                        Text(
                            text = if (playing != null) {
                                val number = tracks.indexOf(playing) + 1
                                stringResource(MR.strings.kurotoon_sound_nowPlaying, number, tracks.size)
                            } else {
                                stringResource(MR.strings.kurotoon_sound_hasMusic)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (scape != Soundscape.OFF) {
                        Text(
                            text = stringResource(MR.strings.kurotoon_sound_playingScape, stringResource(scape.label)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The little music badge WEBTOON shows while an episode's soundtrack plays. Tap it to mute or
 * unmute without opening the menu.
 */
@Composable
fun ReaderSoundBadge(
    visible: Boolean,
    muted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier,
    ) {
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.55f),
            contentColor = Color.White,
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onClick),
        ) {
            Icon(
                imageVector = if (muted) MaterialSymbols.Rounded.VolumeOff else MaterialSymbols.Rounded.MusicNote,
                contentDescription = stringResource(
                    if (muted) MR.strings.kurotoon_sound_unmute else MR.strings.kurotoon_sound_mute,
                ),
                modifier = Modifier.padding(9.dp),
            )
        }
    }
}
