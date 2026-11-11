package kurotoon.reader.sound

import android.content.Context
import androidx.core.content.edit

/**
 * The ambience picked in the reader for each series, so going back to a rainy thriller brings
 * the rain back even after a cosy fireplace somewhere else.
 */
class SeriesAmbience(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(mangaId: Long): String? = prefs.getString(mangaId.toString(), null)

    fun put(mangaId: Long, key: String) {
        prefs.edit { putString(mangaId.toString(), key) }
        if (prefs.all.size > MAX_ENTRIES) {
            // Oldest-first order isn't kept, so drop an arbitrary slice; it only costs a default.
            prefs.edit { prefs.all.keys.take(prefs.all.size - KEEP_ENTRIES).forEach(::remove) }
        }
    }

    private companion object {
        const val PREFS = "kurotoon_series_ambience"
        const val MAX_ENTRIES = 500
        const val KEEP_ENTRIES = 400
    }
}
