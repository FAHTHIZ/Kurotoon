package kurotoon.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import java.util.Calendar

private const val EVENING_START = 21 * 60 + 30 // 21:30, warming begins
private const val EVENING_FULL = 22 * 60 + 30 // 22:30, fully warm
private const val MORNING_START = 5 * 60 + 30 // 05:30, cooling begins
private const val MORNING_END = 6 * 60 + 30 // 06:30, back to normal

/**
 * How warm the bedtime filter should be right now, from 0 (day) to 1 (night). It eases in over an
 * hour in the evening and out at dawn so the page never visibly snaps.
 */
internal fun bedtimeWarmth(minuteOfDay: Int): Float = when {
    minuteOfDay >= EVENING_FULL || minuteOfDay < MORNING_START -> 1f
    minuteOfDay >= EVENING_START -> (minuteOfDay - EVENING_START).toFloat() / (EVENING_FULL - EVENING_START)
    minuteOfDay < MORNING_END -> 1f - (minuteOfDay - MORNING_START).toFloat() / (MORNING_END - MORNING_START)
    else -> 0f
}

@Composable
fun rememberBedtimeWarmth(enabled: Boolean): Float {
    var warmth by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(enabled) {
        if (!enabled) {
            warmth = 0f
            return@LaunchedEffect
        }
        while (true) {
            val now = Calendar.getInstance()
            warmth = bedtimeWarmth(now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE))
            delay(60_000L)
        }
    }
    return warmth
}

/** A warm amber wash with a touch of dimming, scaled by [warmth]. Draws nothing in the day. */
@Composable
fun BedtimeOverlay(warmth: Float, modifier: Modifier = Modifier) {
    if (warmth <= 0f) return
    Canvas(modifier.fillMaxSize()) {
        drawRect(Color(0xFFFFB066).copy(alpha = 0.55f * warmth), blendMode = BlendMode.Multiply)
        drawRect(Color.Black.copy(alpha = 0.12f * warmth))
    }
}
