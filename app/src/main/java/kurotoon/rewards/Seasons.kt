package kurotoon.rewards

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR
import java.time.LocalDate
import java.time.MonthDay

/** What a seasonal chore counts. */
enum class ChoreKind(val title: StringResource) {
    READ(MR.strings.kurotoon_chore_read),
    DAYS(MR.strings.kurotoon_chore_days),
    BOOKMARK(MR.strings.kurotoon_chore_bookmark),
    SEARCH(MR.strings.kurotoon_chore_search),
}

/** A goal that only counts while its event runs. */
data class Chore(val kind: ChoreKind, val target: Int, val reward: Int) {
    val code: String get() = kind.name.lowercase()
}

/**
 * A yearly event. Fixed events run between two calendar days; moving ones (like Eid) list their
 * start date for each year in [dates]. Finishing every chore unlocks [reward], a shop item code.
 */
data class SeasonEvent(
    val id: String,
    val title: StringResource,
    val reward: String,
    val chores: List<Chore>,
    val start: MonthDay? = null,
    val end: MonthDay? = null,
    val dates: Map<Int, LocalDate> = emptyMap(),
    val length: Int = 7,
)

/** One run of an event, e.g. Halloween 2026. Progress is keyed by [key] so events return every year. */
data class SeasonRun(val event: SeasonEvent, val start: LocalDate, val end: LocalDate) {
    val key: String get() = "${event.id}-${start.year}"

    fun isActive(day: LocalDate): Boolean = !day.isBefore(start) && !day.isAfter(end)

    fun choreKey(chore: Chore): String = "$key/${chore.code}"
}

object Seasons {

    private fun chores(read: Int, days: Int, bookmark: Int, search: Int? = null) = buildList {
        add(Chore(ChoreKind.READ, read, read * 4))
        add(Chore(ChoreKind.DAYS, days, days * 15))
        add(Chore(ChoreKind.BOOKMARK, bookmark, bookmark * 20))
        if (search != null) add(Chore(ChoreKind.SEARCH, search, search * 10))
    }

    val events: List<SeasonEvent> = listOf(
        SeasonEvent(
            "newyear",
            MR.strings.kurotoon_season_newyear,
            "kuro.seasonal/newyear",
            chores(20, 5, 2, 3),
            start = MonthDay.of(1, 1),
            end = MonthDay.of(1, 14),
        ),
        SeasonEvent(
            "valentine",
            MR.strings.kurotoon_season_valentine,
            "kuro.seasonal/valentine",
            chores(15, 4, 2),
            start = MonthDay.of(2, 7),
            end = MonthDay.of(2, 21),
        ),
        SeasonEvent(
            "spring",
            MR.strings.kurotoon_season_spring,
            "kuro.seasonal/spring",
            chores(40, 10, 3, 5),
            start = MonthDay.of(3, 1),
            end = MonthDay.of(3, 31),
        ),
        SeasonEvent(
            "eid_fitr",
            MR.strings.kurotoon_season_eidFitr,
            "kuro.seasonal/eid",
            chores(15, 4, 1),
            dates = mapOf(
                2026 to LocalDate.of(2026, 3, 20),
                2027 to LocalDate.of(2027, 3, 10),
                2028 to LocalDate.of(2028, 2, 27),
                2029 to LocalDate.of(2029, 2, 15),
                2030 to LocalDate.of(2030, 2, 5),
            ),
            length = 7,
        ),
        SeasonEvent(
            "boishakh",
            MR.strings.kurotoon_season_boishakh,
            "kuro.seasonal/boishakh",
            chores(15, 4, 2),
            start = MonthDay.of(4, 10),
            end = MonthDay.of(4, 20),
        ),
        SeasonEvent(
            "eid_adha",
            MR.strings.kurotoon_season_eidAdha,
            "kuro.seasonal/eid",
            chores(15, 4, 1),
            dates = mapOf(
                2026 to LocalDate.of(2026, 5, 27),
                2027 to LocalDate.of(2027, 5, 16),
                2028 to LocalDate.of(2028, 5, 5),
                2029 to LocalDate.of(2029, 4, 24),
                2030 to LocalDate.of(2030, 4, 13),
            ),
            length = 7,
        ),
        SeasonEvent(
            "summer",
            MR.strings.kurotoon_season_summer,
            "kuro.seasonal/summer",
            chores(80, 20, 5, 10),
            start = MonthDay.of(6, 1),
            end = MonthDay.of(6, 30),
        ),
        SeasonEvent(
            "monsoon",
            MR.strings.kurotoon_season_monsoon,
            "kuro.seasonal/monsoon",
            chores(60, 15, 4, 6),
            start = MonthDay.of(7, 15),
            end = MonthDay.of(8, 31),
        ),
        SeasonEvent(
            "halloween",
            MR.strings.kurotoon_season_halloween,
            "kuro.seasonal/halloween",
            chores(25, 6, 2, 3),
            start = MonthDay.of(10, 15),
            end = MonthDay.of(10, 31),
        ),
        SeasonEvent(
            "anniversary",
            MR.strings.kurotoon_season_anniversary,
            "kuro.seasonal/anniversary",
            chores(25, 7, 3, 3),
            start = MonthDay.of(11, 10),
            end = MonthDay.of(11, 24),
        ),
        SeasonEvent(
            "winter",
            MR.strings.kurotoon_season_winter,
            "kuro.seasonal/winter",
            chores(30, 8, 3, 3),
            start = MonthDay.of(12, 1),
            end = MonthDay.of(12, 31),
        ),
    )

    fun event(id: String): SeasonEvent? = events.firstOrNull { it.id == id }

    /** The run of [event] in [year], or null when a moving event has no date that year. */
    fun run(event: SeasonEvent, year: Int): SeasonRun? {
        if (event.start != null && event.end != null) {
            return SeasonRun(event, event.start.atYear(year), event.end.atYear(year))
        }
        val start = event.dates[year] ?: return null
        return SeasonRun(event, start, start.plusDays(event.length - 1L))
    }

    /** Every event running on [day]; they can overlap. */
    fun active(day: LocalDate): List<SeasonRun> = events.mapNotNull { run(it, day.year) }.filter { it.isActive(day) }

    /** The next few runs that haven't started yet, soonest first. */
    fun upcoming(day: LocalDate, limit: Int = 3): List<SeasonRun> =
        events.flatMap { listOfNotNull(run(it, day.year), run(it, day.year + 1)) }
            .filter { it.start.isAfter(day) }
            .sortedBy { it.start }
            .take(limit)

    /** Event that sells or gifts [itemCode], if it's a seasonal item. */
    fun eventsFor(itemCode: String): List<SeasonEvent> = events.filter { it.reward == itemCode }

    /** Whether [itemCode] can be bought on [day]: always for normal items, only in season for seasonal ones. */
    fun isInSeason(itemCode: String, day: LocalDate): Boolean {
        val events = eventsFor(itemCode)
        return events.isEmpty() || active(day).any { it.event.reward == itemCode }
    }
}
