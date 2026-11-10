package kurotoon.screenshots

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import eu.kanade.presentation.theme.colorscheme.KuroToonColorScheme
import kurotoon.ui.KuroBottomBar
import kurotoon.ui.KuroBottomBarItem
import kurotoon.ui.browse.BrowseChips
import kurotoon.ui.browse.BrowseSort
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

    private fun shot(name: String, content: @Composable () -> Unit) {
        captureRoboImage("build/screenshots/$name.png") {
            MaterialTheme(colorScheme = KuroToonColorScheme.getColorScheme(isDark = true, isAmoled = false, true)) {
                Surface(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                    content()
                }
            }
        }
    }
}
