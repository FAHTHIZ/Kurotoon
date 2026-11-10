package kurotoon.screenshots

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.github.takahirom.roborazzi.captureRoboImage
import eu.kanade.presentation.theme.colorscheme.KuroToonColorScheme
import androidx.compose.foundation.layout.height
import androidx.compose.ui.geometry.Offset
import kurotoon.ui.KuroBottomBar
import kurotoon.ui.fx.drawTapFrame
import kurotoon.ui.KuroBottomBarItem
import kurotoon.ui.browse.BrowseChips
import kurotoon.ui.browse.BrowseSort
import kurotoon.ui.home.KuroHomeContent
import kurotoon.ui.home.KuroHomeViewModel
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CollectionsBookmark
import mihon.icons.materialsymbols.rounded.Explore
import mihon.icons.materialsymbols.rounded.Home
import mihon.icons.materialsymbols.rounded.NewReleases
import mihon.icons.materialsymbols.rounded.Person
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover

/**
 * Renders KuroToon's own components to PNGs under `app/build/screenshots/` for visual review.
 * Skipped unless Gradle runs with `-Pscreenshots`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "w411dp-h891dp-night-xxhdpi")
class KuroScreenshots {

    @Before
    fun onlyWhenRequested() {
        assumeTrue(System.getProperty("kurotoon.screenshots") == "true")
    }

    @Test
    fun browseChips() = shot("browse_chips") {
        Column {
            BrowseChips(
                supportsLatest = true,
                sorts = listOf("Rating", "Title", "Year"),
                genres = listOf("Action", "Adventure", "Boys' Love", "Comedy", "Crime", "Drama", "Fantasy"),
                selectedSort = BrowseSort.Latest,
                selectedGenre = null,
                hasFilters = true,
                onPopular = {},
                onLatest = {},
                onSort = {},
                onGenre = {},
                onFilters = {},
                modifier = Modifier.padding(top = 12.dp),
            )
            BrowseChips(
                supportsLatest = false,
                sorts = emptyList(),
                genres = listOf("Action", "Adventure", "Comedy", "Drama"),
                selectedSort = BrowseSort.None,
                selectedGenre = "Adventure",
                hasFilters = true,
                onPopular = {},
                onLatest = {},
                onSort = {},
                onGenre = {},
                onFilters = {},
            )
        }
    }

    @Test
    fun bottomBar() = shot("bottom_bar") {
        val items = listOf(
            "Home" to MaterialSymbols.Rounded.Home,
            "Browse" to MaterialSymbols.Rounded.Explore,
            "Library" to MaterialSymbols.Rounded.CollectionsBookmark,
            "Updates" to MaterialSymbols.Rounded.NewReleases,
            "Me" to MaterialSymbols.Rounded.Person,
        ).map { (title, icon) -> KuroBottomBarItem(title, title, rememberVectorPainter(icon), null) }
        KuroBottomBar(items = items, selectedKey = "Browse", onClick = {})
    }

    @Test
    fun tapBurst() = shot("tap_burst") {
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            listOf(0.15f, 0.3f, 0.5f, 0.75f).forEachIndexed { i, t ->
                drawTapFrame(Offset(size.width * (i + 0.5f) / 4, size.height / 2), t)
            }
        }
    }

    @Test
    fun home() = shot("home", fullScreen = true) {
        val titles = listOf("Solo Leveling", "Omniscient Reader", "Tower of God", "The Beginning After the End")
        fun manga(id: Long, title: String) = Manga.create().copy(
            id = id,
            title = title,
            source = 1L,
            url = "/$id",
            thumbnailUrl = "https://example.org/$id.jpg",
            description = "A hunter who was once the weakest rises through dungeons nobody else survives.",
        )
        val rows = listOf("MangaDex", "Asura Scans").mapIndexed { r, name ->
            KuroHomeViewModel.SourceRow(
                sourceId = r.toLong(),
                name = name,
                items = (0 until 8).map { manga(r * 10L + it, titles[it % titles.size]) },
            )
        }
        val history = (0 until 4).map {
            HistoryWithRelations(
                id = it.toLong(),
                chapterId = it.toLong(),
                mangaId = it.toLong(),
                title = titles[it],
                chapterNumber = 12.0 + it,
                readAt = null,
                readDuration = 0,
                coverData = MangaCover(it.toLong(), 1L, false, "https://example.org/$it.jpg", null),
            )
        }
        KuroHomeContent(
            state = KuroHomeViewModel.State(isLoading = false, continueReading = history, rows = rows),
            snackbarHostState = SnackbarHostState(),
            onSearchClick = {},
            onAddSourcesClick = {},
            onMangaClick = {},
            onBrowseMangaClick = {},
            onSourceClick = {},
            onRetrySource = {},
            onResumeClick = {},
            onExtensionsClick = {},
            onHistoryClick = {},
            onDownloadsClick = {},
            onStatsClick = {},
        )
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun shot(name: String, fullScreen: Boolean = false, content: @Composable () -> Unit) {
        captureRoboImage("build/screenshots/$name.png") {
            val covers = AsyncImagePreviewHandler { request -> fakeCover(request.data.hashCode()) }
            CompositionLocalProvider(
                LocalInspectionMode provides true,
                LocalAsyncImagePreviewHandler provides covers,
            ) {
                MaterialTheme(colorScheme = KuroToonColorScheme.getColorScheme(isDark = true, isAmoled = false, true)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (fullScreen) Modifier.fillMaxSize() else Modifier)
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        content()
                    }
                }
            }
        }
    }

    /** A gradient stand-in for a cover image, different for every title. */
    private fun fakeCover(seed: Int): coil3.Image {
        val bitmap = Bitmap.createBitmap(300, 450, Bitmap.Config.ARGB_8888)
        val hue = (seed and 0x7fffffff) % 360f
        val top = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.55f, 0.85f))
        val bottom = android.graphics.Color.HSVToColor(floatArrayOf((hue + 40) % 360, 0.8f, 0.3f))
        Canvas(bitmap).drawPaint(
            Paint().apply { shader = LinearGradient(0f, 0f, 0f, 450f, top, bottom, Shader.TileMode.CLAMP) },
        )
        return bitmap.asImage()
    }
}
