package kurotoon.library

import eu.kanade.tachiyomi.source.model.SManga

/**
 * Whether a series is a one-shot: a single, finished story. Sources say so in different ways, so
 * this trusts a "Oneshot" style tag first, then falls back to one chapter on a finished series.
 * A brand-new ongoing series with one chapter so far is not a one-shot.
 */
object OneShot {

    fun matches(genres: List<String>?, status: Long, chapters: Long): Boolean {
        if (genres.orEmpty().any { normalize(it) in TAGS }) return true
        return chapters == 1L && status.toInt() in FINISHED
    }

    private fun normalize(tag: String) = tag.lowercase().filter { it.isLetter() }

    private val TAGS = setOf("oneshot", "oneshots")
    private val FINISHED = setOf(SManga.COMPLETED, SManga.PUBLISHING_FINISHED)
}
