package kurotoon.ui.fx

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.PI

/** Something the particle field should draw once. Positions are window pixels. */
sealed interface FxEvent {

    data class Burst(
        val x: Float,
        val y: Float,
        val count: Int = 18,
        val spread: Float = (PI * 2).toFloat(),
        val power: Float = 4f,
        val gravity: Float = 0.12f,
        val colors: List<Color>? = null,
    ) : FxEvent

    /** Confetti from the middle of the screen, then from both sides. */
    data object Celebrate : FxEvent

    /** The small aura, ring and sparks under a finger. */
    data class Tap(val x: Float, val y: Float) : FxEvent
}

/** Fires one-shot effects drawn by [ParticleField]. Safe to call from anywhere. */
object KuroFx {

    private val events = MutableSharedFlow<FxEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val flow: SharedFlow<FxEvent> = events.asSharedFlow()

    fun burst(at: Offset, count: Int = 18, power: Float = 4f, colors: List<Color>? = null) {
        if (at == Offset.Unspecified) return
        events.tryEmit(FxEvent.Burst(at.x, at.y, count = count, power = power, colors = colors))
    }

    fun celebrate() {
        events.tryEmit(FxEvent.Celebrate)
    }

    fun tap(at: Offset) {
        events.tryEmit(FxEvent.Tap(at.x, at.y))
    }
}

/** Shows a tap burst wherever a finger lands inside this layout, without taking the touch. */
fun Modifier.tapBursts(): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (down.type == PointerType.Touch || down.type == PointerType.Stylus) {
            KuroFx.tap(down.position)
        }
    }
}

/** Remembers where a composable sits on screen so effects can start from it. */
@Stable
class FxAnchor {
    var center: Offset = Offset.Unspecified
        internal set

    fun burst(count: Int = 18, power: Float = 4f, colors: List<Color>? = null) =
        KuroFx.burst(center, count, power, colors)
}

@Composable
fun rememberFxAnchor(): FxAnchor = remember { FxAnchor() }

fun Modifier.fxAnchor(anchor: FxAnchor): Modifier = onGloballyPositioned {
    anchor.center = it.boundsInWindow().center
}
