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
class KuroPreferences(preferenceStore: PreferenceStore) {

    /** The reader's display name on the Me tab; empty means the default. */
    val nickname = preferenceStore.getString("kurotoon_nickname", "")

    /** Ambient embers behind the UI and sparks on key moments. */
    val particles = preferenceStore.getBoolean("kurotoon_particles", true)

    /** Tint a series' page with colours from its cover. */
    val coverColors = preferenceStore.getBoolean("kurotoon_cover_colors", true)

    /** A thin read-progress bar along the bottom of library covers. */
    val coverProgress = preferenceStore.getBoolean("kurotoon_cover_progress", true)

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
}
