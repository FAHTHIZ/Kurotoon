package kurotoon.reader

import kotlin.math.ceil

/**
 * Learns how long a page takes from page turns and estimates the time left in a chapter.
 * Pauses (putting the phone down) and fast skims are left out so they don't skew the pace.
 */
class ReadingPace(
    private val minGapMillis: Long = 800,
    private val maxGapMillis: Long = 120_000,
    private val minSamples: Int = 3,
) {
    private var lastTurn: Long? = null
    private var average: Double? = null
    private var samples = 0

    /** Records a page turn at [nowMillis]. */
    fun onPageTurn(nowMillis: Long) {
        val previous = lastTurn
        lastTurn = nowMillis
        if (previous == null) return
        val gap = nowMillis - previous
        if (gap !in minGapMillis..maxGapMillis) return
        samples++
        // Moving average that settles quickly but still follows changes in pace
        val weight = if (samples < 5) 1.0 / samples else 0.2
        average = average?.let { it + (gap - it) * weight } ?: gap.toDouble()
    }

    /** Whole minutes left for [pagesLeft] pages, or null until the pace is known. */
    fun minutesLeft(pagesLeft: Int): Int? {
        val perPage = average ?: return null
        if (samples < minSamples || pagesLeft <= 0) return null
        return ceil(perPage * pagesLeft / 60_000).toInt().coerceAtLeast(1)
    }
}
