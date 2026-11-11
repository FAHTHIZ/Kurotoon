package kurotoon.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.pow

/** Whether auto-scroll is switched on, and whether a finger is currently on the page. */
@Stable
class AutoScrollState {
    var enabled by mutableStateOf(false)
    var holding by mutableStateOf(false)
}

/**
 * Moves the reader forward on its own while [state] is enabled and nothing is in the way. Long
 * strips glide continuously at a speed set by [speed] (1–10); paged modes turn a page on a timer.
 * It pauses while [paused] (menus or dialogs open) or a finger is down, and waits a moment after
 * the finger lifts so a fling can settle.
 */
@Composable
fun AutoScrollDriver(
    state: AutoScrollState,
    viewer: Viewer?,
    speed: Int,
    paused: Boolean,
    onReachedEnd: () -> Unit,
) {
    val density = LocalDensity.current.density
    val reachedEnd by rememberUpdatedState(onReachedEnd)
    // The paged timer isn't tied to frames, so without this it kept turning pages (and marking
    // chapters read) with the app in the background or the screen off
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val visible = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    LaunchedEffect(viewer, state.enabled, paused, state.holding, speed, visible) {
        if (viewer == null || !state.enabled || paused || state.holding || !visible) return@LaunchedEffect
        delay(RESUME_DELAY_MS)

        if (viewer.autoScrollBy(0)) {
            val pxPerSecond = stripDpPerSecond(speed) * density
            var carry = 0f
            var last = 0L
            var ended = false
            while (isActive && !ended) {
                withFrameNanos { now ->
                    if (last != 0L) {
                        carry += pxPerSecond * ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
                        val distance = carry.toInt()
                        carry -= distance
                        ended = !viewer.autoScrollBy(distance)
                    }
                    last = now
                }
            }
            if (ended) {
                state.enabled = false
                reachedEnd()
            }
        } else {
            while (isActive) {
                delay(pageIntervalMs(speed))
                viewer.moveForward()
            }
        }
    }
}

private const val RESUME_DELAY_MS = 450L

/** 18 dp/s at speed 1 up to roughly 500 dp/s at speed 10, growing evenly to the eye. */
private fun stripDpPerSecond(speed: Int): Float = 18f * 1.45f.pow(speed.coerceIn(1, 10) - 1)

/** 14 seconds per page at speed 1 down to 2 seconds at speed 10. */
private fun pageIntervalMs(speed: Int): Long = 14_000L - (speed.coerceIn(1, 10) - 1) * 1_333L
