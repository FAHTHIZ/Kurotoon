package kurotoon.ui.manga

import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.materialkolor.ktx.animateColorScheme
import com.materialkolor.ktx.themeColorOrNull
import eu.kanade.presentation.theme.colorscheme.MonetCompatColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kurotoon.app.di.appGraph
import tachiyomi.domain.manga.model.Manga
import tachiyomi.presentation.core.util.collectAsState

/**
 * Recolours the accent roles of the current theme with the most prominent colour of [manga]'s cover.
 * Surfaces and backgrounds stay as they are so the screen keeps the app's look, only the buttons,
 * chips and highlights pick up the cover's mood. The change fades in once the cover is decoded.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CoverTheme(manga: Manga, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val enabled by remember { context.appGraph.kuroPreferences.coverColors }.collectAsState()
    val isDark = isSystemInDarkTheme()
    var seed by remember(manga.id) { mutableStateOf<Color?>(null) }

    LaunchedEffect(manga.id, manga.thumbnailUrl, manga.coverLastModified, enabled) {
        if (!enabled) {
            seed = null
            return@LaunchedEffect
        }
        seed = withContext(Dispatchers.Default) { coverSeed(context, manga) }
    }

    val base = MaterialTheme.colorScheme
    val target = remember(base, seed, isDark) {
        seed?.let { base.withAccentsFrom(MonetCompatColorScheme.generateColorSchemeFromSeed(it, isDark)) } ?: base
    }
    MaterialExpressiveTheme(
        colorScheme = animateColorScheme(target, animationSpec = { tween(durationMillis = 450) }),
        motionScheme = MaterialTheme.motionScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content,
    )
}

private suspend fun coverSeed(context: android.content.Context, manga: Manga): Color? {
    return try {
        val request = ImageRequest.Builder(context)
            .data(manga)
            .size(COVER_SAMPLE_SIZE)
            .allowHardware(false)
            .build()
        val drawable = context.imageLoader.execute(request).image?.asDrawable(context.resources) ?: return null
        val bitmap = drawable.toBitmap(COVER_SAMPLE_SIZE, COVER_SAMPLE_SIZE).asImageBitmap()
        bitmap.themeColorOrNull()
    } catch (_: Exception) {
        null
    }
}

private fun ColorScheme.withAccentsFrom(cover: ColorScheme): ColorScheme = copy(
    primary = cover.primary,
    onPrimary = cover.onPrimary,
    primaryContainer = cover.primaryContainer,
    onPrimaryContainer = cover.onPrimaryContainer,
    inversePrimary = cover.inversePrimary,
    secondary = cover.secondary,
    onSecondary = cover.onSecondary,
    secondaryContainer = cover.secondaryContainer,
    onSecondaryContainer = cover.onSecondaryContainer,
    tertiary = cover.tertiary,
    onTertiary = cover.onTertiary,
    tertiaryContainer = cover.tertiaryContainer,
    onTertiaryContainer = cover.onTertiaryContainer,
    surfaceTint = cover.surfaceTint,
)

private const val COVER_SAMPLE_SIZE = 96
