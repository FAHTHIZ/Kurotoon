package kurotoon.manga

import kurotoon.library.LibraryDuplicates
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import kotlin.math.roundToLong

/**
 * One series read from several sources as a single chapter list.
 *
 * Sites rarely carry the same chapters: one may have 1–4 and 9, another 5–7 and 10. When the same
 * series sits in your library more than once (same title, and the same author when both list one),
 * the copies are merged by chapter number. Every number appears once and opens from the copy that
 * has it, preferring the series you are looking at, then the copy with the most chapters. Nothing
 * is moved or rewritten: each chapter still belongs to its own entry, so downloads, history and
 * tracking keep working as before.
 */
object MergedChapters {

    data class Copy(val manga: Manga, val sourceName: String, val chapters: List<Chapter>)

    data class Entry(
        val number: Double,
        val chapter: Chapter,
        val sourceName: String,
        /** True when the chapter comes from the series being viewed rather than another copy. */
        val here: Boolean,
        /** Read in any copy. */
        val read: Boolean,
        /** How many copies have this number. */
        val copies: Int,
    )

    /** Library entries that are the same series as [manga] from other sources. */
    fun sameSeries(manga: Manga, candidates: List<Manga>): List<Manga> {
        val key = LibraryDuplicates.keyOf(manga.title)
        if (key.isEmpty()) return emptyList()
        return candidates.filter {
            it.id != manga.id &&
                LibraryDuplicates.keyOf(it.title) == key &&
                authorsAgree(manga.author, it.author)
        }
    }

    /** Authors match when either side is unknown or one name contains the other ("ONE" / "ONE, Murata"). */
    fun authorsAgree(a: String?, b: String?): Boolean {
        val x = a?.let(LibraryDuplicates::keyOf).orEmpty()
        val y = b?.let(LibraryDuplicates::keyOf).orEmpty()
        return x.isEmpty() || y.isEmpty() || x.contains(y) || y.contains(x)
    }

    /** The union of [here] and [others] by chapter number, newest first like the series page. */
    fun merge(here: Copy, others: List<Copy>): List<Entry> {
        val ordered = listOf(here) + others.sortedByDescending { it.chapters.size }
        val picked = HashMap<Long, Entry>()
        val readKeys = HashSet<Long>()
        val counts = HashMap<Long, Int>()
        ordered.forEachIndexed { index, copy ->
            copy.chapters
                .filter { it.isRecognizedNumber }
                .distinctBy { keyOf(it.chapterNumber) }
                .forEach { chapter ->
                    val key = keyOf(chapter.chapterNumber)
                    counts[key] = (counts[key] ?: 0) + 1
                    if (chapter.read) readKeys += key
                    if (key !in picked) {
                        picked[key] = Entry(
                            number = chapter.chapterNumber,
                            chapter = chapter,
                            sourceName = copy.sourceName,
                            here = index == 0,
                            read = false,
                            copies = 0,
                        )
                    }
                }
        }
        return picked.map { (key, entry) -> entry.copy(read = key in readKeys, copies = counts[key] ?: 1) }
            .sortedByDescending { it.number }
    }

    private fun keyOf(number: Double) = (number * 100).roundToLong()
}
