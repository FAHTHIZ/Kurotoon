package kurotoon.prefs

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kurotoon.catalogue.RecentSearches
import kurotoon.catalogue.SavedSearch
import kurotoon.catalogue.SavedSearches
import tachiyomi.core.common.preference.PreferenceStore

/** Settings that only exist in KuroToon. */
@Inject
@SingleIn(AppScope::class)
class KuroPreferences(private val preferenceStore: PreferenceStore) {

    /** Your own edits of series details, by manga id, as JSON (see [kurotoon.manga.MangaEdit]). */
    val mangaEdits = preferenceStore.getString("kurotoon_manga_edits", "{}")

    fun mangaEdit(id: Long): kurotoon.manga.MangaEdit? = kurotoon.manga.MangaEdit.decodeAll(mangaEdits.get())[id]

    fun setMangaEdit(id: Long, edit: kurotoon.manga.MangaEdit?) {
        val all = kurotoon.manga.MangaEdit.decodeAll(mangaEdits.get()).toMutableMap()
        if (edit == null || edit.cleaned().isEmpty) all.remove(id) else all[id] = edit.cleaned()
        mangaEdits.set(kurotoon.manga.MangaEdit.encodeAll(all))
    }

    /** Updates fetched at or before this time (epoch ms) are hidden from the Updates tab (mihon#1534). */
    val updatesClearedAt = preferenceStore.getLong("kurotoon_updates_cleared_at", 0L)

    /** The category the library opens on; -1 means wherever you left it (mihon#2165). */
    val libraryStartCategory = preferenceStore.getLong("kurotoon_library_start_category", -1L)

    /** Manga ids kept out of Browse and Search results (mihon#2322). */
    val hiddenSeries = preferenceStore.getStringSet("kurotoon_hidden_series", emptySet())

    /** Per-series paged scale type and zoom start as "mangaId=scale,zoom" (mihon#1622). */
    val seriesPagedScale = preferenceStore.getStringSet("kurotoon_series_paged_scale", emptySet())

    /** The series' own (scale type, zoom start), or null when it follows the global setting. */
    fun pagedScaleFor(mangaId: Long): Pair<Int, Int>? = seriesPagedScale.get().firstNotNullOfOrNull { entry ->
        if (entry.substringBefore('=') != mangaId.toString()) return@firstNotNullOfOrNull null
        val parts = entry.substringAfter('=').split(',').mapNotNull { it.toIntOrNull() }
        parts.takeIf { it.size == 2 }?.let { it[0] to it[1] }
    }

    fun setPagedScaleFor(mangaId: Long, scale: Pair<Int, Int>?) {
        val rest = seriesPagedScale.get().filterNot { it.substringBefore('=') == mangaId.toString() }.toSet()
        seriesPagedScale.set(if (scale == null) rest else rest + "$mangaId=${scale.first},${scale.second}")
    }

    /** Order of the installed extension lists, a [kurotoon.extension.ExtensionSort] ordinal (mihon#2744). */
    val extensionSort = preferenceStore.getInt("kurotoon_extension_sort", 0)

    /** A folder (tree URI) saved pages go to instead of Pictures; empty for Pictures (mihon#295). */
    val pageSaveFolder = preferenceStore.getString("kurotoon_page_save_folder", "")

    /** Per-source default library category as "sourceId=categoryId" (mihon#336). */
    val sourceDefaultCategories = preferenceStore.getStringSet("kurotoon_source_default_categories", emptySet())

    /** The source's own default category if one is set, else [fallback] (the global default). */
    fun defaultCategoryFor(sourceId: Long, fallback: Long): Long =
        sourceDefaultCategories.get().firstNotNullOfOrNull { entry ->
            val (source, category) = entry.split('=').takeIf { it.size == 2 } ?: return@firstNotNullOfOrNull null
            category.toLongOrNull()?.takeIf { source.toLongOrNull() == sourceId }
        } ?: fallback

    fun setDefaultCategoryFor(sourceId: Long, categoryId: Long?) {
        val rest = sourceDefaultCategories.get().filterNot { it.substringBefore('=') == sourceId.toString() }
        sourceDefaultCategories.set(if (categoryId == null) rest.toSet() else rest.toSet() + "$sourceId=$categoryId")
    }

    /** Categories whose series are read without history or progress being saved (mihon#3367). */
    val incognitoCategories = preferenceStore.getStringSet("kurotoon_incognito_categories", emptySet())

    fun setIncognitoCategory(categoryId: Long, incognito: Boolean) {
        val id = categoryId.toString()
        incognitoCategories.set(incognitoCategories.get().let { if (incognito) it + id else it - id })
    }

    /** Reading modes picked for every series of a source, as "sourceId=flagValue" (mihon#2140). */
    val sourceReadingModes = preferenceStore.getStringSet("kurotoon_source_reading_modes", emptySet())

    fun readingModeFor(sourceId: Long): Int? =
        sourceReadingModes.get().firstNotNullOfOrNull { entry ->
            val (source, mode) = entry.split('=').takeIf { it.size == 2 } ?: return@firstNotNullOfOrNull null
            mode.toIntOrNull()?.takeIf { source.toLongOrNull() == sourceId }
        }

    fun setReadingModeFor(sourceId: Long, mode: Int?) {
        val rest = sourceReadingModes.get().filterNot { it.substringBefore('=') == sourceId.toString() }.toSet()
        sourceReadingModes.set(if (mode == null) rest else rest + "$sourceId=$mode")
    }

    /**
     * Read chapters being read again and left part-way, by chapter id, so History and the reader resume
     * on that page instead of skipping ahead or starting over (mihon#698). Capped to the latest few.
     */
    val rereading = preferenceStore.getStringSet("kurotoon_rereading", emptySet())

    fun isRereading(chapterId: Long): Boolean = chapterId.toString() in rereading.get()

    fun setRereading(chapterId: Long, active: Boolean) {
        val id = chapterId.toString()
        val current = rereading.get()
        if ((id in current) == active) return
        rereading.set(if (active) (current.toList().takeLast(REREADING_CAP - 1) + id).toSet() else current - id)
    }

    /** The reader's display name on the Me tab; empty means the default. */
    val nickname = preferenceStore.getString("kurotoon_nickname", "")

    /** Ambient embers behind the UI and sparks on key moments. */
    val particles = preferenceStore.getBoolean("kurotoon_particles", true)

    /** Tint a series' page with colours from its cover. */
    val coverColors = preferenceStore.getBoolean("kurotoon_cover_colors", true)

    /** A thin read-progress bar along the bottom of library covers. */
    val coverProgress = preferenceStore.getBoolean("kurotoon_cover_progress", true)

    /** A daily nudge to keep reading, sent only on days nothing was read. */
    val readingReminder = preferenceStore.getBoolean("kurotoon_reading_reminder", false)

    /** Hour of the day (0-23) the reading reminder fires. */
    val readingReminderHour = preferenceStore.getInt("kurotoon_reading_reminder_hour", 20)

    /** Chapters a day to aim for. 0 turns the goal off. */
    val dailyGoal = preferenceStore.getInt("kurotoon_daily_goal", 0)

    /** Searches kept from source browsing, for every source. Shown as chips there and rows on Home. */
    val savedSearches = preferenceStore.getObjectFromString<List<SavedSearch>>(
        key = "kurotoon_saved_searches",
        defaultValue = emptyList(),
        serializer = SavedSearches::encode,
        deserializer = SavedSearches::decode,
    )

    /** Recent Universal Search queries, newest first. */
    val recentSearches = preferenceStore.getObjectFromString<List<String>>(
        key = "kurotoon_recent_searches",
        defaultValue = emptyList(),
        serializer = RecentSearches::encode,
        deserializer = RecentSearches::decode,
    )

    /** Tags to keep out of browsing, comma separated. See [kurotoon.catalogue.HiddenTags]. */
    val hiddenTags = preferenceStore.getString("kurotoon_hidden_tags", "")

    /** Source ids whose Home row shows latest updates instead of popular titles. */
    val homeLatestSources = preferenceStore.getStringSet("kurotoon_home_latest_sources", emptySet())

    /** Result urls a saved search showed last time, newest first. See [kurotoon.catalogue.SearchFeed]. */
    fun savedSearchSeen(id: Long) = preferenceStore.getString("kurotoon_saved_seen_$id", "")

    /** Keys of [kurotoon.ui.home.HomeSection]s switched off on Home. */
    val homeHiddenSections = preferenceStore.getStringSet("kurotoon_home_hidden_sections", emptySet())

    /** Ids of library categories kept out of sight until "Show hidden categories" is tapped. */
    val hiddenCategories = preferenceStore.getStringSet("kurotoon_hidden_categories", emptySet())

    /** How the library splits into tabs, see [kurotoon.library.LibraryGroupBy]. */
    val libraryGroupBy = preferenceStore.getString("kurotoon_library_group_by", "category")

    /** Shows the source's name on library covers. */
    val sourceBadge = preferenceStore.getBoolean("kurotoon_source_badge", false)

    /** Index urls of extension repos switched off: their extensions aren't listed until switched back on. */
    val disabledExtensionStores = preferenceStore.getStringSet("kurotoon_disabled_extension_stores", emptySet())

    /** Fold same-day chapters of one series into a single Updates row. See [kurotoon.updates.UpdateGroups]. */
    val updatesGroupSeries = preferenceStore.getBoolean("kurotoon_updates_group_series", true)

    private companion object {
        const val REREADING_CAP = 50
    }
}
