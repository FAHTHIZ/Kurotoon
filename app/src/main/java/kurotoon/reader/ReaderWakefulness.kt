package kurotoon.reader

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * Keeps the screen awake while someone is actually reading. Phones often time out after 30
 * seconds, which is shorter than it takes to read a dense page, so any touch, key or page turn
 * keeps the reader lit for [IDLE_MS]. Left alone longer than that, the phone sleeps as usual.
 */
@Stable
class ReaderWakefulness {
    private var lastActive by mutableLongStateOf(SystemClock.elapsedRealtime())

    var awake by mutableStateOf(true)
        private set

    fun poke() {
        lastActive = SystemClock.elapsedRealtime()
        awake = true
    }

    @Composable
    fun Track() {
        LaunchedEffect(lastActive) {
            val left = IDLE_MS - (SystemClock.elapsedRealtime() - lastActive)
            if (left > 0) delay(left)
            awake = false
        }
    }

    private companion object {
        const val IDLE_MS = 3 * 60_000L
    }
}
