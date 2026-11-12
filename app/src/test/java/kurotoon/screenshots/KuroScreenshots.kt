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
import kotlinx.datetime.minus
import kurotoon.catalogue.CatalogFacet
import kurotoon.catalogue.CatalogFacets
import kurotoon.catalogue.SavedSearch
import kurotoon.shop.toPack
import kurotoon.ui.KuroBottomBar
import kurotoon.ui.KuroBottomBarItem
import kurotoon.ui.browse.BrowseChips
import kurotoon.ui.browse.BrowseSort
import kurotoon.ui.browse.SavedSearchRow
import kurotoon.ui.home.KuroHomeContent
import kurotoon.ui.home.KuroHomeViewModel
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CollectionsBookmark
import mihon.icons.materialsymbols.rounded.Explore
import mihon.icons.materialsymbols.rounded.Home
import mihon.icons.materialsymbols.rounded.NewReleases
import mihon.icons.materialsymbols.rounded.Person
import mihon.icons.materialsymbols.rounded.Refresh
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.library.service.LibraryPreferences.ChapterSwipeAction as SwipeAction

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
                    tachiyomi.domain.manga.model.Manga.create()
                        .copy(
                            id = i.toLong(),
                            url = "/$i",
                            title = "Title $i",
                            favoriteAt = kotlin.time.Instant.fromEpochSeconds(1).takeIf {
                                i ==
                                    1
                            },
                        )
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
            "kuro.badges/hood",
            "kuro.badges/cape",
        ),
        equipped = mapOf(
            "avatar" to "kuro.avatars/galaxy",
            "frame" to "kuro.frames/mythic",
            "sticker" to "kuro.starter/ex_happy",
        ),
        badges = listOf("kuro.badges/hood", "kuro.badges/cape"),
        achievements = mapOf("first_chapter" to 1_762_000_000_000, "first_bookmark" to 1_762_100_000_000),
    )

    @Test
    fun collection() = shot("collection", fullScreen = true) {
        kurotoon.ui.shop.CollectionContent(
            state = rewards,
            packs = packs,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(),
            onUse = {},
            onStopUsing = {},
            onShare = {},
            onRemovePack = {},
        )
    }

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
                avatar = items["kuro.avatars/galaxy"],
                frame = items["kuro.frames/mythic"],
                sticker = items["kuro.starter/ex_happy"],
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

    private fun sampleSchedule(): kurotoon.schedule.WeeklySchedule {
        val today = kotlinx.datetime.LocalDate(2026, 11, 11)
        fun series(id: Long, title: String, unread: Long, ago: Int, vararg days: kotlinx.datetime.DayOfWeek) =
            kurotoon.schedule.ScheduledSeries(
                item = tachiyomi.domain.library.model.LibraryManga(
                    manga = Manga.create().copy(id = id, title = title, thumbnailUrl = "cover://$id"),
                    categories = emptyList(),
                    totalChapters = 120,
                    readCount = 120 - unread,
                    bookmarkCount = 0,
                    latestUpload = null,
                    chapterFetchedAt = null,
                    lastRead = null,
                ),
                days = days.toList(),
                latest = today.minus(kotlinx.datetime.DatePeriod(days = ago)),
            )
        val wed = kotlinx.datetime.DayOfWeek.WEDNESDAY
        val mon = kotlinx.datetime.DayOfWeek.MONDAY
        val all = listOf(
            series(1, "Omniscient Reader's Viewpoint", 1, 0, wed),
            series(2, "Lookism", 0, 7, wed),
            series(3, "The Greatest Estate Developer", 2, 0, wed, mon),
            series(4, "Eleceed", 0, 7, wed),
            series(5, "Return of the Mount Hua Sect", 0, 7, wed),
            series(6, "Nano Machine", 0, 2, mon),
        )
        return kurotoon.schedule.WeeklySchedule(
            today = today,
            byDay = kotlinx.datetime.DayOfWeek.entries.associateWith { d -> all.filter { d in it.days } },
            unscheduled = 4,
        )
    }

    @Test
    fun schedule() = shot("schedule", fullScreen = true) {
        kurotoon.ui.schedule.WeeklyScheduleContent(schedule = sampleSchedule(), onClick = {})
    }

    @Test
    fun homeSchedule() = shot("home_schedule") {
        val schedule = sampleSchedule()
        kurotoon.ui.home.HomeSchedule(
            items = schedule.dueToday,
            today = schedule.today,
            onMangaClick = {},
            onSeeAll = {},
        )
    }

    @Test
    fun seriesHeader() = shot("series_header") {
        Column {
            eu.kanade.presentation.manga.components.MangaInfoBox(
                isTabletUi = false,
                appBarPadding = 16.dp,
                manga = tachiyomi.domain.manga.model.Manga.create().copy(
                    id = 7L,
                    title = "Solo Leveling",
                    author = "Chugong",
                    artist = "DUBU",
                    status = eu.kanade.tachiyomi.source.model.SManga.COMPLETED.toLong(),
                ),
                source = tachiyomi.domain.source.model.StubSource(1L, "en", "Asura Scans"),
                sourceName = "Asura Scans",
                isStubSource = false,
                onCoverClick = {},
                doSearch = { _, _ -> },
            )
            eu.kanade.presentation.manga.components.MangaActionRow(
                favorite = true,
                trackingCount = 1,
                nextUpdate = null,
                isUserIntervalMode = false,
                onAddToLibraryClicked = {},
                onWebViewClicked = {},
                onWebViewLongClicked = null,
                onSimilarClicked = {},
                onTrackingClicked = {},
                onEditIntervalClicked = null,
                onEditCategory = null,
            )
        }
    }

    @Test
    fun episodeList() = shot("episode_list") {
        Column(Modifier.padding(vertical = 8.dp)) {
            eu.kanade.presentation.manga.components.ChapterHeader(
                enabled = true,
                chapterCount = 126,
                missingChapterCount = 0,
                onClick = {},
                sortDescending = true,
                onFlipSortOrder = {},
            )
            data class Row(val n: Int, val date: String, val read: Boolean, val new: Boolean, val progress: Boolean)
            listOf(
                Row(126, "Today", read = false, new = true, progress = false),
                Row(125, "Nov 5", read = false, new = false, progress = true),
                Row(124, "Oct 29", read = true, new = false, progress = false),
                Row(123, "Oct 22", read = true, new = false, progress = false),
            ).forEach { row ->
                eu.kanade.presentation.manga.components.MangaChapterListItem(
                    title = "Episode ${row.n}",
                    date = row.date,
                    readProgress = if (row.progress) "Page 14" else null,
                    scanlator = null,
                    read = row.read,
                    bookmark = false,
                    selected = false,
                    downloadIndicatorEnabled = true,
                    downloadStateProvider = { eu.kanade.tachiyomi.data.download.model.Download.State.NOT_DOWNLOADED },
                    downloadProgressProvider = { 0 },
                    chapterSwipeStartAction = SwipeAction.Disabled,
                    chapterSwipeEndAction = SwipeAction.Disabled,
                    onLongClick = {},
                    onClick = {},
                    onDownloadClick = {},
                    onChapterSwipe = {},
                    number = row.n.toString(),
                    isNew = row.new,
                    inProgress = row.progress,
                )
            }
        }
    }

    @Test
    fun selectionDock() = shot("selection_dock") {
        androidx.compose.foundation.layout.Box(Modifier.padding(vertical = 16.dp)) {
            eu.kanade.presentation.manga.components.MangaBottomActionMenu(
                visible = true,
                modifier = Modifier.fillMaxWidth(),
                onBookmarkClicked = {},
                onMarkAsReadClicked = {},
                onMarkPreviousAsReadClicked = {},
                onDownloadClicked = {},
                onDeleteClicked = {},
            )
        }
    }

    @Test
    fun sheetRows() = shot("sheet_rows") {
        androidx.compose.foundation.layout.Column(Modifier.padding(vertical = 12.dp)) {
            tachiyomi.presentation.core.components.HeadingItem("Filter")
            tachiyomi.presentation.core.components.TriStateItem("Downloaded", TriState.DISABLED, onClick = {})
            tachiyomi.presentation.core.components.TriStateItem("Unread", TriState.ENABLED_IS, onClick = {})
            tachiyomi.presentation.core.components.TriStateItem("Completed", TriState.ENABLED_NOT, onClick = {})
            tachiyomi.presentation.core.components.HeadingItem("Sort")
            tachiyomi.presentation.core.components.SortItem("Last read", sortDescending = true, onClick = {})
            tachiyomi.presentation.core.components.SortItem("Alphabetically", sortDescending = null, onClick = {})
            tachiyomi.presentation.core.components.HeadingItem("Display")
            tachiyomi.presentation.core.components.RadioItem("Comfortable grid", selected = true, onClick = {})
            tachiyomi.presentation.core.components.RadioItem("List", selected = false, onClick = {})
            tachiyomi.presentation.core.components.CheckboxItem("Unread badges", checked = true, onClick = {})
            tachiyomi.presentation.core.components.CheckboxItem("Language badges", checked = false, onClick = {})
        }
    }

    @Test
    fun segmentedTabs() = shot("segmented_tabs") {
        val pager = androidx.compose.foundation.pager.rememberPagerState { 3 }
        kurotoon.ui.KuroSegmentedTabs(
            titles = listOf("Sources", "Extensions", "Migrate"),
            pagerState = pager,
            onSelect = {},
            modifier = Modifier.padding(vertical = 10.dp),
        )
    }

    @Test
    fun upNextTransition() = shot("up_next") {
        fun chapter(id: Long, name: String, number: Double) = tachiyomi.domain.chapter.model.Chapter.create().copy(
            id = id,
            mangaId = 1L,
            name = name,
            chapterNumber = number,
            scanlator = "Official",
        )
        androidx.compose.foundation.layout.Box(Modifier.padding(horizontal = 20.dp, vertical = 32.dp)) {
            val from = chapter(1, "Episode 125 - Under the Red Moon", 125.0)
            val to = chapter(2, "Episode 126 - The Crown", 126.0)
            eu.kanade.presentation.reader.ChapterTransition(
                transition = eu.kanade.tachiyomi.ui.reader.model.ChapterTransition.Next(
                    eu.kanade.tachiyomi.ui.reader.model.ReaderChapter(from),
                    eu.kanade.tachiyomi.ui.reader.model.ReaderChapter(to),
                ),
                currChapterDownloaded = false,
                goingToChapterDownloaded = true,
            )
        }
    }

    @Test
    fun emptyState() = shot("empty_state") {
        androidx.compose.foundation.layout.Box(Modifier.height(520.dp)) {
            tachiyomi.presentation.core.screens.EmptyScreen(
                message = "Your library is empty",
                actions = listOf(
                    tachiyomi.presentation.core.screens.EmptyScreenAction(
                        tachiyomi.i18n.MR.strings.getting_started_guide,
                        mihon.icons.materialsymbols.MaterialSymbols.Rounded.Refresh,
                    ) {},
                ),
            )
        }
    }

    private fun shot(name: String, fullScreen: Boolean = false, content: @Composable () -> Unit) {
        captureRoboImage("build/screenshots/$name.png") {
            val covers = AsyncImagePreviewHandler { request -> fakeCover(request.data.hashCode()) }
            CompositionLocalProvider(
                LocalInspectionMode provides true,
                LocalAsyncImagePreviewHandler provides covers,
            ) {
                MaterialTheme(
                    colorScheme = KuroToonColorScheme.getColorScheme(isDark = true, isAmoled = false, true),
                    typography = eu.kanade.presentation.theme.KuroTypography,
                    shapes = eu.kanade.presentation.theme.KuroShapes,
                ) {
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
