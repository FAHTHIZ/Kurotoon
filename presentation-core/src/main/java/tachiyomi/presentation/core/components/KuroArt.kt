package tachiyomi.presentation.core.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** Kuro's official artwork shipped in `assets/kuro/`, one file per expression. */
enum class KuroPose(val asset: String) {
    NEUTRAL("kuro/packs/01-starter/ex_neutral.webp"),
    HAPPY("kuro/packs/01-starter/ex_happy.webp"),
    EXCITED("kuro/packs/01-starter/ex_excited.webp"),
    THINKING("kuro/packs/01-starter/ex_thinking.webp"),
    SAD("kuro/packs/01-starter/ex_sad.webp"),
    SLEEPY("kuro/packs/01-starter/ex_sleepy.webp"),
    DOZING("kuro/packs/06-stickers/sleepy.webp"),
    READING("kuro/packs/06-stickers/reading.webp"),
    WAVING("kuro/packs/06-stickers/byebye.webp"),
}

private val cache = ConcurrentHashMap<KuroPose, ImageBitmap>()

/**
 * Draws Kuro in [pose] at [size]. The artwork is decoded once off the main thread and kept in memory, so screens
 * that show him again draw him straight away.
 */
@Composable
fun KuroArt(
    pose: KuroPose,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var bitmap by remember(pose) { mutableStateOf(cache[pose]) }
    LaunchedEffect(pose) {
        if (bitmap != null) return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(pose.asset).use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
            }.getOrNull()
        }?.also { cache[pose] = it }
    }
    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = modifier.size(size),
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(modifier.size(size))
    }
}
