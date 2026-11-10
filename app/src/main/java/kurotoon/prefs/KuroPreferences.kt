package kurotoon.prefs

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
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
}
