package kurotoon.screenshots

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
    fun homeRowHeaders() = shot("home_row_headers") {
        androidx.compose.foundation.layout.Column {
            listOf(false, true).forEach { latest ->
                kurotoon.ui.home.HomeSourceRow(
                    row = KuroHomeViewModel.SourceRow(
                        sourceId = 1L,
                        name = "Asura Scans",
                        items = null,
                        supportsLatest = true,
                        latest = latest,
                    ),
                    onSeeAll = {},
                    onMangaClick = {},
                    onRetry = {},
                )
            }
        }
    }

    @Test
    fun libraryTags() = shot("library_tags") {
        kurotoon.ui.library.LibraryTagRow(
            tags = listOf("Action" to 42, "Fantasy" to 31, "Murim" to 12, "Romance" to 9, "Isekai" to 7)
                .map { (name, count) -> kurotoon.library.LibraryTag(name.lowercase(), name, count) },
            selected = setOf("murim"),
            onToggle = {},
            onClear = {},
        )
    }

    @Test
    fun hiddenTags() = shot("hidden_tags") {
        kurotoon.ui.browse.HiddenTagsContent(
            hidden = listOf("Gore", "Ecchi", "Harem"),
            suggestions = listOf("Action", "Murim", "Ecchi", "Regression", "Fantasy", "Romance", "System"),
            onAdd = {},
            onRemove = {},
        )
    }

    @Test
    fun searchEmptyState() = shot("search_empty_state") {
        Column {
            kurotoon.ui.browse.RecentSearchRow(
                recent = listOf("solo leveling", "murim", "regression"),
                onSearch = {},
                onClear = {},
                contentPadding = PaddingValues(horizontal = 16.dp),
            )
            kurotoon.ui.browse.TagBrowseRow(
                tags = listOf("Action", "Murim", "Regression", "Fantasy", "Romance"),
                onTag = {},
                contentPadding = PaddingValues(horizontal = 16.dp),
            )
        }
    }

    @Test
    fun duplicates() = shot("duplicates") {
        fun entry(id: Long, title: String, source: Long, read: Long) = tachiyomi.domain.library.model.LibraryManga(
            manga = tachiyomi.domain.manga.model.Manga.create().copy(id = id, title = title, source = source),
            categories = emptyList(),
            totalChapters = 120,
            readCount = read,
            bookmarkCount = 0,
            latestUpload = null,
            chapterFetchedAt = null,
            lastRead = null,
        )
        kurotoon.ui.library.DuplicatesList(
            groups = listOf(
                listOf(entry(1, "Solo Leveling", 1, 80), entry(2, "Solo Leveling (Official)", 2, 3)),
                listOf(entry(3, "Tower of God", 2, 40), entry(4, "Tower Of God", 3, 0), entry(5, "Tower of God", 1, 0)),
            ),
            sourceName = { mapOf(1L to "Asura Scans", 2L to "MangaDex", 3L to "Webtoons")[it].orEmpty() },
            onClick = {},
        )
    }

    @Test
    fun homeStalled() = shot("home_stalled") {
        kurotoon.ui.home.HomeStalled(
            items = listOf("Omniscient Reader" to 40L, "Lookism" to 3L).mapIndexed { i, (t, n) ->
                tachiyomi.domain.library.model.LibraryManga(
                    manga = tachiyomi.domain.manga.model.Manga.create().copy(id = i.toLong(), title = t),
                    categories = emptyList(),
                    totalChapters = n + 10,
                    readCount = 10,
                    bookmarkCount = 0,
                    latestUpload = null,
                    chapterFetchedAt = null,
                    lastRead = null,
                )
            },
            onMangaClick = {},
        )
    }

    @Test
    fun homeBinge() = shot("home_binge") {
        kurotoon.ui.home.HomeBinge(
            items = listOf("Solo Leveling" to 200L, "The Breaker" to 72L, "Noblesse" to 544L).mapIndexed { i, (t, n) ->
                tachiyomi.domain.library.model.LibraryManga(
                    manga = tachiyomi.domain.manga.model.Manga.create().copy(id = i.toLong(), title = t),
                    categories = emptyList(),
                    totalChapters = n,
                    readCount = 0,
                    bookmarkCount = 0,
                    latestUpload = null,
                    chapterFetchedAt = null,
                    lastRead = null,
                )
            },
            onMangaClick = {},
        )
    }

    @Test
    fun homeSavedFeed() = shot("home_saved_feed") {
        kurotoon.ui.home.HomeSourceRow(
            row = KuroHomeViewModel.SourceRow(
                sourceId = 1L,
                name = "Murim regression",
                items = (0..3).map { i ->
                    tachiyomi.domain.manga.model.Manga.create().copy(id = i.toLong(), url = "/$i", title = "Title $i")
                },
                savedSearch = kurotoon.catalogue.SavedSearch(id = 1L, sourceId = 1L, name = "Murim regression"),
                sourceName = "Asura Scans",
                freshUrls = setOf("/0", "/2"),
            ),
            onSeeAll = {},
            onMangaClick = {},
            onRetry = {},
        )
    }

    @Test
    fun homePickRow() = shot("home_pick_row") {
        kurotoon.ui.home.HomeSourceRow(
            row = KuroHomeViewModel.SourceRow(
                sourceId = 1L,
                name = "Murim",
                items = null,
                sourceName = "Asura Scans",
                pickTag = "Murim",
            ),
            onSeeAll = {},
            onMangaClick = {},
            onRetry = {},
        )
    }

    @Test
    fun homeNewChapters() = shot("home_new_chapters") {
        kurotoon.ui.home.HomeNewChapters(
            items = listOf("Solo Leveling" to 3, "Tower of God" to 1, "Omniscient Reader" to 12).mapIndexed {
                    i,
                    (title, n),
                ->
                KuroHomeViewModel.NewChapters(
                    tachiyomi.domain.updates.model.UpdatesWithRelations(
                        mangaId = i.toLong(),
                        mangaTitle = title,
                        chapterId = i.toLong(),
                        chapterName = "Chapter ${200 + i}",
                        scanlator = null,
                        chapterUrl = "/$i",
                        read = false,
                        bookmark = false,
                        lastPageRead = 0,
                        sourceId = 1L,
                        dateFetch = kotlin.time.Instant.fromEpochMilliseconds(0),
                        coverData = MangaCover(i.toLong(), 1L, true, "https://example.org/$i.jpg", null),
                    ),
                    n,
                )
            },
            onMangaClick = {},
            onSeeAll = {},
        )
    }

    @Test
    fun sourceFilter() = shot("source_filter") {
        androidx.compose.foundation.layout.Column {
            kurotoon.ui.browse.SourceFilterField(query = "", onQueryChange = {})
            kurotoon.ui.browse.SourceFilterField(query = "asura", onQueryChange = {})
            kurotoon.ui.browse.UniversalSearchCard(
                onSearch = {},
                recent = listOf("solo leveling", "tower of god", "murim", "return of the mount hua sect"),
            )
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
                supportsLatest = true,
                latest = r == 1,
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

    @Test
    fun libraryProgress() = shot("library_progress") {
        val entries = listOf("Solo Leveling" to 0.25f, "Omniscient Reader" to 0.7f, "Tower of God" to 1f)
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            entries.forEachIndexed { index, (title, progress) ->
                androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                    eu.kanade.presentation.library.components.MangaCompactGridItem(
                        coverData = MangaCover(
                            mangaId = index.toLong(),
                            sourceId = 1L,
                            isMangaFavorite = true,
                            url = "https://example.org/$index.jpg",
                            lastModified = null,
                        ),
                        title = title,
                        onClick = {},
                        onLongClick = {},
                        onClickContinueReading = if (progress < 1f) ({}) else null,
                        readProgress = progress,
                    )
                }
            }
        }
    }

    @Test
    fun readingActivity() = shot("reading_activity") {
        val today = java.time.LocalDate.of(2026, 11, 11)
        val zone = java.time.ZoneOffset.UTC
        val entries = (0 until 119).flatMap { back ->
            val date = today.minusDays(back.toLong())
            val count = ((back * 7 + 3) % 11).let { if (it < 4) 0 else it - 3 }.coerceAtMost(if (back < 9) 6 else 4)
            (0 until count).map {
                kotlin.time.Instant.fromEpochMilliseconds(date.atTime(20, it).toInstant(zone).toEpochMilli()) to 60_000L
            }
        }
        val todays = (0 until 3).map {
            kotlin.time.Instant.fromEpochMilliseconds(today.atTime(9, it).toInstant(zone).toEpochMilli()) to 60_000L
        }
        val activity = kurotoon.stats.ReadingActivity.build(entries + todays, today, zone)
        androidx.compose.foundation.layout.Box(Modifier.padding(16.dp)) {
            kurotoon.ui.stats.ReadingActivityCard(activity, goal = 5)
        }
    }

    @Test
    fun yearInReview() = shot("year_in_review") {
        val review = kurotoon.stats.YearInReview(
            year = 2026,
            chapters = 1248,
            millis = (86L * 60 + 25) * 60_000,
            series = 37,
            topSeries = listOf(
                kurotoon.stats.TopSeries(1, "Solo Leveling", 179, 0),
                kurotoon.stats.TopSeries(2, "Omniscient Reader's Viewpoint", 142, 0),
                kurotoon.stats.TopSeries(3, "Tower of God", 96, 0),
            ),
            topGenres = listOf("Action" to 21, "Fantasy" to 17, "Martial Arts" to 9),
            busiestMonth = java.time.Month.AUGUST,
            busiestMonthChapters = 212,
        )
        kurotoon.ui.stats.YearInReviewCard(review)
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
