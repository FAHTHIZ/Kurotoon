package kurotoon.rewards

import kotlinx.serialization.Serializable
import kurotoon.shop.MAX_BADGES
import kurotoon.shop.ShopItem
import kurotoon.shop.ShopKind
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.time.Duration.Companion.days

/** Everything the rewards layer remembers. Stored as a preference, so it's part of backups. */
@Serializable
data class RewardsState(
    val points: Int = 0,
    /** Every point ever earned, for the profile; spending doesn't lower it. */
    val lifetimePoints: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val lastDay: String? = null,
    val day: String = "",
    val progress: Map<String, Int> = emptyMap(),
    val claimed: Set<String> = emptySet(),
    val owned: Set<String> = emptySet(),
    /** Worn items by [ShopKind.key], except badges. */
    val equipped: Map<String, String> = emptyMap(),
    val badges: List<String> = emptyList(),
    val achievements: Map<String, Long> = emptyMap(),
    val chaptersRead: Int = 0,
    val boostUntil: Long = 0,
    /** Monday of the week [weekProgress] belongs to. */
    val week: String = "",
    val weekProgress: Map<String, Int> = emptyMap(),
    val weekClaimed: Set<String> = emptySet(),
    /** Chapters finished per day (ISO date), for the reading tracker. */
    val history: Map<String, Int> = emptyMap(),
    /** Seasonal chore progress by [SeasonRun.choreKey]. */
    val seasonProgress: Map<String, Int> = emptyMap(),
    val seasonClaimed: Set<String> = emptySet(),
    /** Finished event runs by [SeasonRun.key]. */
    val seasonsDone: Set<String> = emptySet(),
    /** Every point ever spent in the shop. */
    val spent: Int = 0,
    val freezes: Int = 0,
    /** Highest level whose reward has been paid. */
    val levelPaid: Int = 1,
) {
    fun isBoosted(nowMillis: Long): Boolean = boostUntil > nowMillis

    val level: LevelInfo get() = Levels.of(lifetimePoints)

    fun owns(item: ShopItem): Boolean = item.isFree || item.code in owned

    fun isEquipped(item: ShopItem): Boolean =
        if (item.kind == ShopKind.BADGE) item.code in badges else equipped[item.kind.key] == item.code
}

/** Why points were paid, so the UI can say it in the reader's language. */
sealed interface GrantReason {
    /** [frozen] is how many streak freezes were spent to keep the streak. */
    data class Daily(val streak: Int, val frozen: Int = 0) : GrantReason
    data class StreakChest(val streak: Int) : GrantReason
    data class WeeklyDone(val code: String) : GrantReason
    data class LevelUp(val level: Int) : GrantReason
    data class SeasonChoreDone(val event: String, val chore: ChoreKind) : GrantReason
    data class SeasonComplete(val event: String, val item: String) : GrantReason
    data class MissionDone(val code: String) : GrantReason
    data class AchievementUnlocked(val code: String) : GrantReason
    data class GoalMet(val goal: Int) : GrantReason
}

data class RewardGrant(val points: Int, val reason: GrantReason)

enum class RewardEvent { CHAPTER_READ, BOOKMARK, SEARCH, SOURCE_INSTALLED, PACK_IMPORTED }

sealed interface PurchaseResult {
    data object Success : PurchaseResult
    data object UnknownItem : PurchaseResult
    data object AlreadyOwned : PurchaseResult
    data object NotEnoughPoints : PurchaseResult
    data object OutOfSeason : PurchaseResult
    data object FreezesFull : PurchaseResult
}

/** Boosts with this style are streak freezes instead of double points. */
const val FREEZE_STYLE = "freeze"

/**
 * Pure reward rules; [RewardsStore] applies them and saves the result. Shop items come from
 * [items] so new packs work without touching this class.
 */
class RewardsEngine(
    private val items: (String) -> ShopItem? = { null },
    private val now: () -> Instant = Instant::now,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {

    data class Outcome(val state: RewardsState, val grants: List<RewardGrant> = emptyList())

    fun today(): LocalDate = now().atZone(zone()).toLocalDate()

    fun rollDay(state: RewardsState): RewardsState {
        val today = today()
        val day = today.toString()
        val week = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
        var next = state
        if (next.day != day) next = next.copy(day = day, progress = emptyMap(), claimed = emptySet())
        if (next.week != week) next = next.copy(week = week, weekProgress = emptyMap(), weekClaimed = emptySet())
        return next
    }

    /** Events running today. */
    fun activeSeasons(): List<SeasonRun> = Seasons.active(today())

    /** Once per day on app open: keeps the streak and pays a small bonus. */
    fun claimDailyOpen(state: RewardsState): Outcome {
        val current = rollDay(state)
        val today = today()
        if (current.lastDay == today.toString()) return Outcome(current)
        val gap = current.lastDay?.let {
            runCatching { ChronoUnit.DAYS.between(LocalDate.parse(it), today) }.getOrNull()
        }
        // Missed days are covered by streak freezes, one per day, if there are enough of them
        val missed = (gap ?: 0L) - 1
        val frozen = if (current.streak > 0 && missed in 1..current.freezes.toLong()) missed.toInt() else 0
        val streak = if (gap == 1L || frozen > 0) current.streak + 1 else 1
        var outcome = grant(
            current.copy(
                lastDay = today.toString(),
                streak = streak,
                bestStreak = maxOf(current.bestStreak, streak),
                freezes = current.freezes - frozen,
            ),
            RewardGrant(minOf(50, 5 + streak * 5), GrantReason.Daily(streak, frozen)),
        )
        if (streak % RewardsCatalog.CHEST_EVERY == 0) {
            outcome = outcome.then { grant(it, RewardGrant(chestReward(streak), GrantReason.StreakChest(streak))) }
        }
        if (streak >= 3) outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_THREE_DAY_STREAK) }
        if (streak >= 7) outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_SEVEN_DAY_STREAK) }
        if (streak >= 30) outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_THIRTY_DAY_STREAK) }
        if (streak >= 100) outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_HUNDRED_DAY_STREAK) }
        return outcome
    }

    fun track(state: RewardsState, event: RewardEvent): Outcome {
        val current = rollDay(state)
        return when (event) {
            RewardEvent.CHAPTER_READ -> {
                val read = current.chaptersRead + 1
                val bumped = bump(bump(current, RewardsCatalog.MISSION_READ), RewardsCatalog.MISSION_READ_MORE)
                val today = (current.progress[RewardsCatalog.CHAPTERS_TODAY] ?: 0) + 1
                val day = today()
                val cutoff = day.minusDays(RewardsCatalog.HISTORY_DAYS.toLong()).toString()
                val history = (bumped.history + (day.toString() to (bumped.history[day.toString()] ?: 0) + 1))
                    .filterKeys { it > cutoff }
                var counted = bumpWeek(
                    bumped.copy(
                        chaptersRead = read,
                        progress = bumped.progress + (RewardsCatalog.CHAPTERS_TODAY to today),
                        history = history,
                    ),
                    RewardsCatalog.WEEK_READ,
                )
                if (today == 1) counted = bumpWeek(counted, RewardsCatalog.WEEK_DAYS)
                var outcome = Outcome(counted)
                    .then { unlock(it, RewardsCatalog.ACHIEVEMENT_FIRST_CHAPTER) }
                    .then { bumpSeason(it, ChoreKind.READ) }
                if (today == 1) outcome = outcome.then { bumpSeason(it, ChoreKind.DAYS) }
                val time = now().atZone(zone())
                if (time.hour < 4) {
                    outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_NIGHT_READER) }
                }
                if (time.hour in 5..7) outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_EARLY_BIRD) }
                if (time.dayOfWeek == DayOfWeek.SATURDAY || time.dayOfWeek == DayOfWeek.SUNDAY) {
                    outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_WEEKEND_READER) }
                }
                if (today >= RewardsCatalog.BINGE_TARGET) {
                    outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_BINGE_DAY) }
                }
                if (read >= 100) outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_HUNDRED_CHAPTERS) }
                if (read >= 500) outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_FIVE_HUNDRED_CHAPTERS) }
                if (read >= 1000) outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_THOUSAND_CHAPTERS) }
                outcome
            }
            RewardEvent.BOOKMARK -> Outcome(
                bumpWeek(bump(current, RewardsCatalog.MISSION_BOOKMARK), RewardsCatalog.WEEK_BOOKMARK),
            )
                .then { unlock(it, RewardsCatalog.ACHIEVEMENT_FIRST_BOOKMARK) }
                .then { bumpSeason(it, ChoreKind.BOOKMARK) }
            RewardEvent.SEARCH -> Outcome(bump(current, RewardsCatalog.MISSION_SEARCH))
                .then { bumpSeason(it, ChoreKind.SEARCH) }
            RewardEvent.SOURCE_INSTALLED -> unlock(current, RewardsCatalog.ACHIEVEMENT_FIRST_SOURCE)
            RewardEvent.PACK_IMPORTED -> unlock(current, RewardsCatalog.ACHIEVEMENT_FIRST_PACK)
        }
    }

    /**
     * Pays once a day when today's chapters reach [goal], and unlocks the first-goal achievement.
     * A goal of 0 means none is set.
     */
    fun meetDailyGoal(state: RewardsState, goal: Int): Outcome {
        val current = rollDay(state)
        if (goal <= 0) return Outcome(current)
        if ((current.progress[RewardsCatalog.CHAPTERS_TODAY] ?: 0) < goal) return Outcome(current)
        if ((current.progress[RewardsCatalog.GOAL_MET] ?: 0) > 0) return Outcome(current)
        val marked = current.copy(progress = current.progress + (RewardsCatalog.GOAL_MET to 1))
        return grant(marked, RewardGrant(goalReward(goal), GrantReason.GoalMet(goal)))
            .then { unlock(it, RewardsCatalog.ACHIEVEMENT_FIRST_GOAL) }
    }

    /** Bigger goals pay more, up to 100. */
    fun goalReward(goal: Int): Int = (15 + goal * 5).coerceAtMost(100)

    /** Missions that are done but not yet claimed. */
    fun claimable(state: RewardsState): List<Mission> = rollDay(state).let { current ->
        RewardsCatalog.missions.filter { (current.progress[it.code] ?: 0) >= it.target && it.code !in current.claimed }
    }

    fun claimMission(state: RewardsState, code: String): Outcome? {
        val current = rollDay(state)
        val mission = RewardsCatalog.mission(code) ?: return null
        if ((current.progress[code] ?: 0) < mission.target || code in current.claimed) return null
        val reward = if (current.isBoosted(now().toEpochMilli())) mission.reward * 2 else mission.reward
        return grant(
            bumpWeek(current.copy(claimed = current.claimed + code), RewardsCatalog.WEEK_MISSIONS),
            RewardGrant(reward, GrantReason.MissionDone(code)),
        )
    }

    /** Weekly missions that are done but not yet claimed. */
    fun claimableWeekly(state: RewardsState): List<Mission> = rollDay(state).let { current ->
        RewardsCatalog.weeklyMissions.filter {
            (current.weekProgress[it.code] ?: 0) >= it.target && it.code !in current.weekClaimed
        }
    }

    fun claimWeekly(state: RewardsState, code: String): Outcome? {
        val current = rollDay(state)
        val mission = RewardsCatalog.weeklyMission(code) ?: return null
        if ((current.weekProgress[code] ?: 0) < mission.target || code in current.weekClaimed) return null
        val reward = if (current.isBoosted(now().toEpochMilli())) mission.reward * 2 else mission.reward
        var outcome = grant(
            current.copy(weekClaimed = current.weekClaimed + code),
            RewardGrant(reward, GrantReason.WeeklyDone(code)),
        )
        if (RewardsCatalog.weeklyMissions.all { it.code in outcome.state.weekClaimed }) {
            outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_WEEKLY_HERO) }
        }
        return outcome
    }

    /** Points a streak chest holds; longer streaks fill bigger chests. */
    fun chestReward(streak: Int): Int =
        (RewardsCatalog.CHEST_REWARD * (1 + streak / (RewardsCatalog.CHEST_EVERY * 4))).coerceAtMost(500)

    /** Days until the next streak chest, 0 when one opened today. */
    fun daysToChest(state: RewardsState): Int {
        val streak = if (state.lastDay == today().toString()) state.streak else state.streak + 1
        val left = (RewardsCatalog.CHEST_EVERY - streak % RewardsCatalog.CHEST_EVERY) % RewardsCatalog.CHEST_EVERY
        return left
    }

    fun buy(state: RewardsState, code: String): Pair<Outcome, PurchaseResult> {
        val item = items(code) ?: return Outcome(state) to PurchaseResult.UnknownItem
        val isBoost = item.kind == ShopKind.BOOST
        val isFreeze = isBoost && item.style == FREEZE_STYLE
        if (!isBoost && state.owns(item)) return Outcome(state) to PurchaseResult.AlreadyOwned
        if (!Seasons.isInSeason(code, today())) return Outcome(state) to PurchaseResult.OutOfSeason
        if (isFreeze && state.freezes >= RewardsCatalog.MAX_FREEZES) return Outcome(state) to PurchaseResult.FreezesFull
        if (state.points < item.price) return Outcome(state) to PurchaseResult.NotEnoughPoints
        val nowMillis = now().toEpochMilli()
        val bought = state.copy(
            points = state.points - item.price,
            spent = state.spent + item.price,
            owned = if (isBoost) state.owned else state.owned + code,
            freezes = if (isFreeze) state.freezes + 1 else state.freezes,
            boostUntil = if (isBoost && !isFreeze) {
                maxOf(nowMillis, state.boostUntil) + 7.days.inWholeMilliseconds
            } else {
                state.boostUntil
            },
        )
        // Wear it straight away; that's almost always why it was bought
        var outcome = Outcome(if (isBoost) bought else equip(bought, code))
        val paidItems = outcome.state.owned.count { owned -> items(owned)?.isFree == false }
        if (paidItems >= RewardsCatalog.COLLECTOR_TARGET) {
            outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_COLLECTOR) }
        }
        if (paidItems >= RewardsCatalog.COLLECTOR_TARGET_BIG) {
            outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_COLLECTOR_25) }
        }
        if (outcome.state.spent >= RewardsCatalog.SPENDER_TARGET) {
            outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_BIG_SPENDER) }
        }
        return outcome to PurchaseResult.Success
    }

    /** Wears [code]. Badges stack up to [MAX_BADGES], dropping the oldest. */
    fun equip(state: RewardsState, code: String): RewardsState {
        val item = items(code) ?: return state
        if (!item.kind.equippable || !state.owns(item)) return state
        return if (item.kind == ShopKind.BADGE) {
            if (code in state.badges) state else state.copy(badges = (state.badges + code).takeLast(MAX_BADGES))
        } else {
            state.copy(equipped = state.equipped + (item.kind.key to code))
        }
    }

    fun unequip(state: RewardsState, code: String): RewardsState {
        val item = items(code)
        return if (item?.kind == ShopKind.BADGE || code in state.badges) {
            state.copy(badges = state.badges - code)
        } else {
            state.copy(equipped = state.equipped.filterValues { it != code })
        }
    }

    private fun bump(state: RewardsState, code: String): RewardsState {
        val mission = RewardsCatalog.mission(code) ?: return state
        val current = state.progress[code] ?: 0
        if (current >= mission.target) return state
        return state.copy(progress = state.progress + (code to current + 1))
    }

    private fun bumpWeek(state: RewardsState, code: String): RewardsState {
        val mission = RewardsCatalog.weeklyMission(code) ?: return state
        val current = state.weekProgress[code] ?: 0
        if (current >= mission.target) return state
        return state.copy(weekProgress = state.weekProgress + (code to current + 1))
    }

    /**
     * Counts [kind] towards every running event. Chores pay as soon as they're done, and finishing
     * the last one gifts the event's item.
     */
    private fun bumpSeason(state: RewardsState, kind: ChoreKind): Outcome {
        var outcome = Outcome(state)
        for (run in activeSeasons()) {
            val chore = run.event.chores.firstOrNull { it.kind == kind } ?: continue
            val key = run.choreKey(chore)
            if (key in outcome.state.seasonClaimed) continue
            val progress = (outcome.state.seasonProgress[key] ?: 0) + 1
            outcome = outcome.then { Outcome(it.copy(seasonProgress = it.seasonProgress + (key to progress))) }
            if (progress < chore.target) continue
            outcome = outcome.then {
                grant(
                    it.copy(seasonClaimed = it.seasonClaimed + key),
                    RewardGrant(chore.reward, GrantReason.SeasonChoreDone(run.event.id, kind)),
                )
            }
            val finished = run.event.chores.all { run.choreKey(it) in outcome.state.seasonClaimed }
            if (finished && run.key !in outcome.state.seasonsDone) {
                outcome = outcome.then {
                    grant(
                        it.copy(seasonsDone = it.seasonsDone + run.key, owned = it.owned + run.event.reward),
                        RewardGrant(SEASON_BONUS, GrantReason.SeasonComplete(run.event.id, run.event.reward)),
                    )
                }.then { unlock(it, RewardsCatalog.ACHIEVEMENT_SEASON_COMPLETE) }
                if (outcome.state.seasonsDone.size >= 4) {
                    outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_SEASON_VETERAN) }
                }
            }
        }
        return outcome
    }

    private fun unlock(state: RewardsState, code: String): Outcome {
        if (code in state.achievements) return Outcome(state)
        val achievement = RewardsCatalog.achievement(code) ?: return Outcome(state)
        return grant(
            state.copy(achievements = state.achievements + (code to now().toEpochMilli())),
            RewardGrant(achievement.reward, GrantReason.AchievementUnlocked(code)),
        )
    }

    /** Pays [grant], then any levels it reached (as one combined level-up grant). */
    private fun grant(state: RewardsState, grant: RewardGrant): Outcome {
        var next = state.copy(
            points = state.points + grant.points,
            lifetimePoints = state.lifetimePoints + grant.points,
        )
        var bonus = 0
        while (Levels.of(next.lifetimePoints).level > next.levelPaid) {
            val reward = Levels.reward(next.levelPaid + 1)
            bonus += reward
            next = next.copy(
                levelPaid = next.levelPaid + 1,
                points = next.points + reward,
                lifetimePoints = next.lifetimePoints + reward,
            )
        }
        var outcome =
            Outcome(
                next,
                listOfNotNull(
                    grant,
                    RewardGrant(bonus, GrantReason.LevelUp(next.levelPaid)).takeIf {
                        bonus >
                            0
                    },
                ),
            )
        if (next.levelPaid >= 10) outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_LEVEL_10) }
        if (next.levelPaid >= 25) outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_LEVEL_25) }
        return outcome
    }

    companion object {
        /** Extra points for finishing every chore in an event. */
        const val SEASON_BONUS = 150
    }

    private inline fun Outcome.then(block: (RewardsState) -> Outcome): Outcome {
        val next = block(state)
        return Outcome(next.state, grants + next.grants)
    }
}
