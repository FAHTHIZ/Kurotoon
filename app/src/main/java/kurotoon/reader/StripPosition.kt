package kurotoon.reader

import android.content.Context
import androidx.core.content.edit

/**
 * Where inside a page the long-strip reader was left, so reopening a chapter lands on the exact
 * panel instead of the top of a page that may be many screens tall.
 *
 * Saved per chapter as the first visible page and how far down it the screen's top edge was.
 */
class StripPosition(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class Anchor(val page: Int, val fraction: Float)

    fun get(chapterId: Long): Anchor? {
        val raw = prefs.getString(chapterId.toString(), null) ?: return null
        val page = raw.substringBefore(':').toIntOrNull() ?: return null
        val fraction = raw.substringAfter(':').substringBefore(':').toFloatOrNull() ?: return null
        if (fraction !in 0f..1f) return null
        return Anchor(page, fraction)
    }

    fun put(chapterId: Long, page: Int, fraction: Float) {
        val value = "$page:${"%.4f".format(
            java.util.Locale.ROOT,
            fraction.coerceIn(0f, 1f),
        )}:${System.currentTimeMillis()}"
        if (prefs.getString(chapterId.toString(), null)?.substringBeforeLast(':') ==
            value.substringBeforeLast(':')
        ) {
            return
        }
        prefs.edit {
            putString(chapterId.toString(), value)
        }
        if (prefs.all.size > MAX_ENTRIES) prune()
    }

    fun clear(chapterId: Long) {
        prefs.edit { remove(chapterId.toString()) }
    }

    /** Keeps the most recent entries; chapters read long ago fall back to page-level resume. */
    private fun prune() {
        val entries = prefs.all.mapNotNull { (k, v) ->
            val time = (v as? String)?.substringAfterLast(':')?.toLongOrNull() ?: 0L
            k to time
        }.sortedByDescending { it.second }
        prefs.edit {
            entries.drop(KEEP_ENTRIES).forEach { remove(it.first) }
        }
    }

    private companion object {
        const val PREFS = "kurotoon_strip_positions"
        const val MAX_ENTRIES = 600
        const val KEEP_ENTRIES = 400
    }
}
