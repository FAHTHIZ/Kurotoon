package kurotoon.reader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.util.Date

/** The time of day in the phone's 12/24 h style, updated on the minute. Null when [enabled] is off. */
@Composable
fun rememberReaderClock(enabled: Boolean): String? {
    val context = LocalContext.current
    var text by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(enabled) {
        if (!enabled) {
            text = null
            return@LaunchedEffect
        }
        val format = DateFormat.getTimeFormat(context)
        while (true) {
            val now = System.currentTimeMillis()
            text = format.format(Date(now))
            delay(60_000L - now % 60_000L + 50L)
        }
    }
    return text
}

/** Battery level and whether it's charging, while [enabled]. */
data class BatteryLevel(val percent: Int, val charging: Boolean)

@Composable
fun rememberBatteryLevel(enabled: Boolean): BatteryLevel? {
    val context = LocalContext.current
    var level by remember { mutableStateOf<BatteryLevel?>(null) }
    DisposableEffect(enabled) {
        if (!enabled) {
            level = null
            return@DisposableEffect onDispose {}
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                level = intent?.toBatteryLevel()
            }
        }
        val sticky = runCatching {
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }.getOrNull()
        level = sticky?.toBatteryLevel()
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return level
}

private fun Intent.toBatteryLevel(): BatteryLevel? {
    val raw = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, 100)
    if (raw < 0 || scale <= 0) return null
    val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    return BatteryLevel(raw * 100 / scale, charging)
}
