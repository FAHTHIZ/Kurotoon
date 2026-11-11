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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import kurotoon.reader.SleepTimerState
import kurotoon.reader.sound.ReaderSoundtrack
import kurotoon.reader.sound.SoundFocus
import kurotoon.reader.sound.Soundscape
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Error
import mihon.icons.materialsymbols.rounded.MusicNote
import mihon.icons.materialsymbols.rounded.PlayArrow
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
    sleepTimer: SleepTimerState,
    onDismissRequest: () -> Unit,
    onAmbiencePicked: (key: String) -> Unit = {},
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
                val interrupted = soundtrack.interruption != null
                FilterChip(
                    selected = soundtrack.muted || interrupted,
                    onClick = soundtrack::toggleMute,
                    leadingIcon = {
                        Icon(
                            imageVector = when {
                                interrupted -> MaterialSymbols.Rounded.PlayArrow
                                soundtrack.muted -> MaterialSymbols.Rounded.VolumeOff
                                else -> MaterialSymbols.Rounded.VolumeUp
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    label = {
                        val label = when {
                            interrupted -> MR.strings.kurotoon_sound_resume
                            soundtrack.muted -> MR.strings.kurotoon_sound_unmute
                            else -> MR.strings.kurotoon_sound_mute
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

            AmbienceChips(preferences, onPicked = onAmbiencePicked)

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
            val pageTurn by preferences.soundPageTurn.collectAsState()
            AnimatedVisibility(visible = pageTurn) {
                val effectsVolume by preferences.soundEffectsVolume.collectAsState()
                SliderItem(
                    label = stringResource(MR.strings.kurotoon_sound_effectsVolume),
                    value = effectsVolume / 10,
                    valueRange = 1..10,
                    valueString = "${effectsVolume / 10 * 10}%",
                    onChange = { preferences.soundEffectsVolume.set(it * 10) },
                )
            }

            CheckboxItem(
                label = stringResource(MR.strings.kurotoon_sound_mixWithOthers),
                pref = preferences.soundMixWithOthers,
            )
            Text(
                text = stringResource(MR.strings.kurotoon_sound_mixWithOthersSummary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            SleepTimerChips(sleepTimer)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AmbienceChips(preferences: ReaderPreferences, onPicked: (key: String) -> Unit = {}) {
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
                onClick = {
                    preferences.soundAmbience.set(scape.key)
                    onPicked(scape.key)
                },
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
    val interruption = soundtrack.interruption
    val visible = tracks.isNotEmpty() || scape != Soundscape.OFF || soundtrack.loadingTracks ||
        soundtrack.musicFailed || interruption != null
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (soundtrack.musicFailed) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                when {
                    soundtrack.loadingTracks || soundtrack.buffering -> CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                    soundtrack.musicFailed -> Icon(MaterialSymbols.Rounded.Error, contentDescription = null)
                    else -> Icon(MaterialSymbols.Rounded.MusicNote, contentDescription = null)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val line = when {
                        soundtrack.loadingTracks -> stringResource(MR.strings.kurotoon_sound_loading)
                        soundtrack.musicFailed -> stringResource(MR.strings.kurotoon_sound_failed)
                        playing != null && soundtrack.buffering -> stringResource(MR.strings.kurotoon_sound_buffering)
                        playing != null -> stringResource(
                            MR.strings.kurotoon_sound_nowPlaying,
                            tracks.indexOf(playing) + 1,
                            tracks.size,
                        )
                        tracks.isNotEmpty() -> stringResource(MR.strings.kurotoon_sound_hasMusic)
                        else -> null
                    }
                    line?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
                    if (scape != Soundscape.OFF) {
                        Text(
                            text = stringResource(MR.strings.kurotoon_sound_playingScape, stringResource(scape.label)),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    interruption?.let {
                        Text(
                            text = stringResource(
                                when (it) {
                                    SoundFocus.Interruption.TRANSIENT -> MR.strings.kurotoon_sound_pausedCall
                                    SoundFocus.Interruption.OTHER_APP -> MR.strings.kurotoon_sound_pausedOtherApp
                                    SoundFocus.Interruption.UNPLUGGED -> MR.strings.kurotoon_sound_pausedUnplugged
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (tracks.isNotEmpty() && !soundtrack.musicFailed) {
                        Text(
                            text = stringResource(MR.strings.kurotoon_sound_offlineHint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        )
                    }
                }
                when {
                    soundtrack.musicFailed -> TextButton(onClick = soundtrack::retry) {
                        Text(stringResource(MR.strings.kurotoon_sound_retry))
                    }
                    interruption != null -> TextButton(onClick = soundtrack::resumeAfterInterruption) {
                        Text(stringResource(MR.strings.kurotoon_sound_resume))
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
    buffering: Boolean = false,
    interrupted: Boolean = false,
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
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                if (buffering && !muted && !interrupted) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 2.dp,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
                Icon(
                    imageVector = when {
                        interrupted -> MaterialSymbols.Rounded.PlayArrow
                        muted -> MaterialSymbols.Rounded.VolumeOff
                        else -> MaterialSymbols.Rounded.MusicNote
                    },
                    contentDescription = stringResource(
                        when {
                            interrupted -> MR.strings.kurotoon_sound_resume
                            muted -> MR.strings.kurotoon_sound_unmute
                            else -> MR.strings.kurotoon_sound_mute
                        },
                    ),
                    modifier = Modifier.padding(9.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SleepTimerChips(timer: SleepTimerState) {
    Text(
        text = stringResource(MR.strings.kurotoon_sleep_timer),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp),
    )
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SleepTimerState.CHOICES.forEach { minutes ->
            FilterChip(
                selected = timer.choice == minutes,
                onClick = { timer.set(minutes) },
                label = {
                    Text(
                        when (minutes) {
                            0 -> stringResource(MR.strings.kurotoon_sound_off)
                            SleepTimerState.END_OF_CHAPTER -> stringResource(MR.strings.kurotoon_sleep_endOfChapter)
                            else -> stringResource(MR.strings.kurotoon_sleep_minutes, minutes)
                        },
                    )
                },
            )
        }
    }
}
