package kurotoon.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A bedtime timer for the reader, the way audiobook and classic comic apps have one. When it runs
 * out the reader stops auto-scroll, fades the sound and dims to a quiet "time to rest" card.
 */
@Stable
class SleepTimerState {
    /** Wall-clock time the timer ends, or null when it isn't counting down. */
    var endsAt by mutableStateOf<Long?>(null)
        private set

    /** Stop at the end of the current chapter instead of after a time. */
    var endOfChapter by mutableStateOf(false)
        private set

    /** Minutes picked, for showing which chip is on. 0 is off, -1 is end of chapter. */
    var choice by mutableStateOf(0)
        private set

    var fired by mutableStateOf(false)
        private set

    fun set(minutes: Int) {
        choice = minutes
        fired = false
        endOfChapter = minutes == END_OF_CHAPTER
        endsAt = if (minutes > 0) System.currentTimeMillis() + minutes * 60_000L else null
    }

    fun fire() {
        endsAt = null
        endOfChapter = false
        choice = 0
        fired = true
    }

    fun dismiss() {
        fired = false
    }

    /** Saves the countdown so rotating the reader (which recreates it) doesn't cancel the timer. */
    fun save(out: android.os.Bundle) {
        out.putInt(KEY_CHOICE, choice)
        out.putLong(KEY_ENDS_AT, endsAt ?: 0L)
        out.putBoolean(KEY_FIRED, fired)
    }

    fun restore(saved: android.os.Bundle) {
        choice = saved.getInt(KEY_CHOICE, 0)
        endOfChapter = choice == END_OF_CHAPTER
        // A timer that ran out while recreating fires straight away from the driver
        endsAt = saved.getLong(KEY_ENDS_AT, 0L).takeIf { it > 0 }
        fired = saved.getBoolean(KEY_FIRED, false)
    }

    companion object {
        const val END_OF_CHAPTER = -1
        private const val KEY_CHOICE = "kurotoon_sleep_choice"
        private const val KEY_ENDS_AT = "kurotoon_sleep_ends_at"
        private const val KEY_FIRED = "kurotoon_sleep_fired"
        val CHOICES = listOf(0, 15, 30, 45, 60, END_OF_CHAPTER)
    }
}

/** Counts down [state] and calls [onFire] when it runs out. */
@Composable
fun SleepTimerDriver(state: SleepTimerState, onFire: () -> Unit) {
    val fire by rememberUpdatedState(onFire)
    LaunchedEffect(state.endsAt) {
        val end = state.endsAt ?: return@LaunchedEffect
        delay((end - System.currentTimeMillis()).coerceAtLeast(0))
        state.fire()
        fire()
    }
}

/** The dimmed card shown once the timer has run out. */
@Composable
fun SleepTimerCurtain(state: SleepTimerState, onClose: () -> Unit, onKeepReading: () -> Unit = {}) {
    AnimatedVisibility(visible = state.fired, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp),
            ) {
                Text(text = "\uD83C\uDF19", style = MaterialTheme.typography.displayMedium)
                Text(
                    text = stringResource(MR.strings.kurotoon_sleep_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = stringResource(MR.strings.kurotoon_sleep_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onClose) { Text(stringResource(MR.strings.kurotoon_sleep_close)) }
                OutlinedButton(
                    onClick = {
                        state.dismiss()
                        onKeepReading()
                    },
                ) { Text(stringResource(MR.strings.kurotoon_sleep_keepReading)) }
            }
        }
    }
}
