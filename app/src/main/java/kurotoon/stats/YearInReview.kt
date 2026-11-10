package kurotoon.stats

import tachiyomi.domain.history.model.ReadEntry
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId

/** A series and how much of it was read in the year. */
data class TopSeries(val mangaId: Long, val title: String, val chapters: Int, val millis: Long)

/** A recap of one calendar year of reading, built from history. */
data class YearInReview(
    val year: Int,
    val chapters: Int,
    val millis: Long,
    val series: Int,
    val topSeries: List<TopSeries>,
    val topGenres: List<Pair<String, Int>>,
    val busiestMonth: Month?,
    val busiestMonthChapters: Int,
) {
    companion object {
        fun startOf(year: Int, zone: ZoneId) = LocalDate.of(year, 1, 1).atStartOfDay(zone).toInstant()

        /** Null when nothing was read in [year]. */
        fun build(entries: List<ReadEntry>, year: Int, zone: ZoneId, top: Int = 3): YearInReview? {
            val inYear = entries.filter { it.date(zone).year == year }
            if (inYear.isEmpty()) return null

            val bySeries = inYear.groupBy { it.mangaId }
            val topSeries = bySeries
                .map { (id, list) ->
                    TopSeries(id, list.first().title, list.size, list.sumOf { it.duration.coerceAtLeast(0) })
                }
                // Time is the better signal; chapters break ties and cover untimed history
                .sortedWith(compareByDescending<TopSeries> { it.millis }.thenByDescending { it.chapters })
                .take(top)

            // Each series counts once per genre, so one long binge doesn't own the list
            val topGenres = bySeries.values
                .flatMap { list -> list.first().genres.map { it.trim() }.filter { it.isNotEmpty() }.distinct() }
                .groupingBy { it.lowercase() }
                .eachCount()
                .entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .take(top)
                .map { entry ->
                    val label = bySeries.values.firstNotNullOf { list ->
                        list.first().genres.firstOrNull { it.trim().lowercase() == entry.key }?.trim()
                    }
                    label to entry.value
                }

            val byMonth = inYear.groupingBy { it.date(zone).month }.eachCount()
            val busiest = byMonth.maxByOrNull { it.value }

            return YearInReview(
                year = year,
                chapters = inYear.size,
                millis = inYear.sumOf { it.duration.coerceAtLeast(0) },
                series = bySeries.size,
                topSeries = topSeries,
                topGenres = topGenres,
                busiestMonth = busiest?.key,
                busiestMonthChapters = busiest?.value ?: 0,
            )
        }

        private fun ReadEntry.date(zone: ZoneId): LocalDate =
            java.time.Instant.ofEpochMilli(readAt.toEpochMilliseconds()).atZone(zone).toLocalDate()
    }
}
