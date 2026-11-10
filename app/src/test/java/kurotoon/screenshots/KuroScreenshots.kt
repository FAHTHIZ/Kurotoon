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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.github.takahirom.roborazzi.captureRoboImage
import eu.kanade.presentation.theme.colorscheme.KuroToonColorScheme
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import kurotoon.catalogue.CatalogFacet
import kurotoon.catalogue.CatalogFacets
import kurotoon.catalogue.SavedSearch
import kurotoon.shop.toPack
import kurotoon.ui.KuroBottomBar
import kurotoon.ui.KuroBottomBarItem
import kurotoon.ui.browse.BrowseChips
import kurotoon.ui.browse.BrowseSort
import kurotoon.ui.browse.SavedSearchRow
import kurotoon.ui.fx.drawTapFrame
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
    fun sourceFilter() = shot("source_filter") {
        androidx.compose.foundation.layout.Column {
            kurotoon.ui.browse.SourceFilterField(query = "", onQueryChange = {})
            kurotoon.ui.browse.SourceFilterField(query = "asura", onQueryChange = {})
        }
    }

    @Test
    fun browseChips() = shot("browse_chips") {
        val catalogue = remember { sampleCatalogue() }
        val facets = remember { CatalogFacets.from(catalogue) }
        val applied = remember {
            sampleCatalogue().also { filters ->
                val genres = facets.first() as CatalogFacet.Tags
                CatalogFacets.cycleTag(filters, genres.filterIndex, 1)
                CatalogFacets.cycleTag(filters, genres.filterIndex, 4)
                CatalogFacets.excludeTag(filters, genres.filterIndex, 3)
                CatalogFacets.selectChoice(filters, facets.first { it.title == "Status" }.filterIndex, 1)
            }
        }
        Column {
            BrowseChips(
                supportsLatest = true,
                sorts = listOf("Rating", "Title", "Year"),
                selectedSort = BrowseSort.Latest,
                hasFilters = true,
                onPopular = {},
                onLatest = {},
                onSort = {},
                onFilters = {},
                modifier = Modifier.padding(top = 12.dp),
                facets = facets,
            )
            BrowseChips(
                supportsLatest = false,
                sorts = emptyList(),
                selectedSort = BrowseSort.None,
                extraRow = {
                    SavedSearchRow(
                        saved = listOf(
                            SavedSearch(1, 1, "Romance + Completed"),
                            SavedSearch(2, 1, "Murim"),
                            SavedSearch(3, 1, "“regression”"),
                        ),
                        selectedId = 1,
                        canSave = true,
                        onSave = {},
                        onOpen = {},
                        onEdit = {},
                    )
                },
                hasFilters = true,
                onPopular = {},
                onLatest = {},
                onSort = {},
                onFilters = {},
                facets = facets,
                applied = applied,
                active = CatalogFacets.active(facets, applied),
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
    fun chapterComplete() = shot("chapter_complete") {
        kurotoon.reader.ChapterCompleteCard(
            chapterName = "Chapter 112 - The Monarch Awakens",
            modifier = Modifier.padding(24.dp),
            settled = true,
        )
    }

    @Test
    fun progressOrb() = shot("progress_orb") {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp),
        ) {
            listOf(3, 12, 25).forEach { page ->
                kurotoon.reader.ReaderProgressOrb(currentPage = page, totalPages = 25, onClick = {})
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
            state = KuroHomeViewModel.State(
                isLoading = false,
                continueReading = history,
                rows = rows,
                savedRows = listOf(
                    KuroHomeViewModel.SourceRow(
                        sourceId = 0L,
                        name = "Murim",
                        items = (0 until 8).map { manga(100L + it, titles[(it + 2) % titles.size]) },
                        savedSearch = kurotoon.catalogue.SavedSearch(1L, 0L, "Murim", "murim", emptyList()),
                        sourceName = "MangaDex",
                    ),
                ),
            ),
            snackbarHostState = SnackbarHostState(),
            onSearchClick = {},
            onAddSourcesClick = {},
            onMangaClick = {},
            onBrowseMangaClick = {},
            onRowClick = {},
            onRetryRow = {},
            onResumeClick = {},
            onExtensionsClick = {},
            onHistoryClick = {},
            onDownloadsClick = {},
            onStatsClick = {},
        )
    }

    @OptIn(ExperimentalCoilApi::class)
    private val packs by lazy {
        java.io.File("src/main/assets/kuro/packs").listFiles { file -> file.isDirectory }.orEmpty()
            .sortedBy { it.name }
            .mapNotNull { folder ->
                kurotoon.shop.packJson
                    .decodeFromString<kurotoon.shop.PackManifest>(java.io.File(folder, "pack.json").readText())
                    .toPack(kurotoon.shop.PackOrigin.BUILT_IN) { it }
            }
    }

    private val rewards = kurotoon.rewards.RewardsState(
        points = 1240,
        lifetimePoints = 3810,
        streak = 6,
        bestStreak = 12,
        progress = mapOf("read_3" to 3, "read_10" to 4, "search_1" to 1),
        claimed = setOf("search_1"),
        owned = setOf(
            "kuro.avatars/galaxy",
            "kuro.frames/mythic",
            "kuro.themes/aurora",
            "kuro.badges/night",
            "kuro.badges/sage",
        ),
        equipped = mapOf(
            "avatar" to "kuro.avatars/galaxy",
            "frame" to "kuro.frames/mythic",
            "sticker" to "kuro.starter/fire",
        ),
        badges = listOf("kuro.badges/night", "kuro.badges/sage"),
        achievements = mapOf("first_chapter" to 1_762_000_000_000, "first_bookmark" to 1_762_100_000_000),
    )

    private fun shopShot(name: String, section: kurotoon.ui.shop.ShopSection) = shot(name, fullScreen = true) {
        kurotoon.ui.shop.ShopContent(
            state = rewards,
            packs = packs,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(),
            onClaim = {},
            onClaimAll = {},
            onBuy = { kurotoon.rewards.PurchaseResult.Success },
            onEquip = {},
            onUnequip = {},
            onImport = {},
            onRemovePack = {},
            initialSection = section,
            animated = false,
        )
    }

    @Test
    fun shop() = shopShot("shop", kurotoon.ui.shop.ShopSection.SHOP)

    @Test
    fun shopMissions() = shopShot("shop_missions", kurotoon.ui.shop.ShopSection.MISSIONS)

    @Test
    fun shopCollection() = shopShot("shop_collection", kurotoon.ui.shop.ShopSection.COLLECTION)

    @Test
    fun shopPacks() = shopShot("shop_packs", kurotoon.ui.shop.ShopSection.PACKS)

    @Test
    fun profile() = shot("profile") {
        val items = packs.flatMap { it.items }.associateBy { it.code }
        kurotoon.ui.profile.KuroProfileHeader(
            nickname = "Kuro reader",
            onNicknameChange = {},
            stats = kurotoon.ui.profile.ProfileStats(inLibrary = 42, chaptersRead = 1288, unread = 97),
            onLibraryClick = {},
            onHistoryClick = {},
            look = kurotoon.ui.profile.ProfileLook(
                points = rewards.points,
                avatar = items["kuro.avatars/galaxy"],
                frame = items["kuro.frames/mythic"],
                sticker = items["kuro.starter/fire"],
                badges = rewards.badges.mapNotNull(items::get),
            ),
        )
    }

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

private class SampleGenre(name: String) : Filter.TriState(name)
private class SampleGenres : Filter.Group<SampleGenre>(
    "Genres",
    listOf("Action", "Adventure", "Comedy", "Drama", "Fantasy", "Horror", "Isekai", "Murim", "Romance", "Slice of Life")
        .map(::SampleGenre),
)
private class SampleStatus : Filter.Select<String>("Status", arrayOf("Any", "Ongoing", "Completed", "Hiatus"))
private class SampleType : Filter.Select<String>("Type", arrayOf("All", "Manga", "Manhwa", "Manhua", "Webtoon"))

private fun sampleCatalogue() = FilterList(SampleGenres(), SampleStatus(), SampleType())
