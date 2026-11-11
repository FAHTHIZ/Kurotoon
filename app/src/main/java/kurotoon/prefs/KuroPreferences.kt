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
}
