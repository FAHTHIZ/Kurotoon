package kurotoon.rewards

import kotlinx.serialization.Serializable
import kurotoon.shop.MAX_BADGES
import kurotoon.shop.ShopItem
import kurotoon.shop.ShopKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
) {
    fun isBoosted(nowMillis: Long): Boolean = boostUntil > nowMillis

    fun owns(item: ShopItem): Boolean = item.isFree || item.code in owned

    fun isEquipped(item: ShopItem): Boolean =
        if (item.kind == ShopKind.BADGE) item.code in badges else equipped[item.kind.key] == item.code
}

/** Why points were paid, so the UI can say it in the reader's language. */
sealed interface GrantReason {
    data class Daily(val streak: Int) : GrantReason
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
}

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

    private fun today(): LocalDate = now().atZone(zone()).toLocalDate()

    fun rollDay(state: RewardsState): RewardsState {
        val day = today().toString()
        return if (state.day == day) state else state.copy(day = day, progress = emptyMap(), claimed = emptySet())
    }

    /** Once per day on app open: keeps the streak and pays a small bonus. */
    fun claimDailyOpen(state: RewardsState): Outcome {
        val current = rollDay(state)
        val today = today()
        if (current.lastDay == today.toString()) return Outcome(current)
        val streak = if (current.lastDay == today.minusDays(1).toString()) current.streak + 1 else 1
        var outcome = grant(
            current.copy(lastDay = today.toString(), streak = streak, bestStreak = maxOf(current.bestStreak, streak)),
            RewardGrant(minOf(50, 5 + streak * 5), GrantReason.Daily(streak)),
        )
        if (streak >= 7) outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_SEVEN_DAY_STREAK) }
        if (streak >= 30) outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_THIRTY_DAY_STREAK) }
        return outcome
    }

    fun track(state: RewardsState, event: RewardEvent): Outcome {
        val current = rollDay(state)
        return when (event) {
            RewardEvent.CHAPTER_READ -> {
                val read = current.chaptersRead + 1
                val bumped = bump(bump(current, RewardsCatalog.MISSION_READ), RewardsCatalog.MISSION_READ_MORE)
                val today = (current.progress[RewardsCatalog.CHAPTERS_TODAY] ?: 0) + 1
                val counted = bumped.copy(
                    chaptersRead = read,
                    progress = bumped.progress + (RewardsCatalog.CHAPTERS_TODAY to today),
                )
                var outcome = Outcome(counted)
                    .then { unlock(it, RewardsCatalog.ACHIEVEMENT_FIRST_CHAPTER) }
                if (now().atZone(zone()).hour < 4) {
                    outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_NIGHT_READER) }
                }
                if (read >= 100) outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_HUNDRED_CHAPTERS) }
                if (read >= 1000) outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_THOUSAND_CHAPTERS) }
                outcome
            }
            RewardEvent.BOOKMARK -> Outcome(bump(current, RewardsCatalog.MISSION_BOOKMARK))
                .then { unlock(it, RewardsCatalog.ACHIEVEMENT_FIRST_BOOKMARK) }
            RewardEvent.SEARCH -> Outcome(bump(current, RewardsCatalog.MISSION_SEARCH))
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
        return grant(current.copy(claimed = current.claimed + code), RewardGrant(reward, GrantReason.MissionDone(code)))
    }

    fun buy(state: RewardsState, code: String): Pair<Outcome, PurchaseResult> {
        val item = items(code) ?: return Outcome(state) to PurchaseResult.UnknownItem
        val isBoost = item.kind == ShopKind.BOOST
        if (!isBoost && state.owns(item)) return Outcome(state) to PurchaseResult.AlreadyOwned
        if (state.points < item.price) return Outcome(state) to PurchaseResult.NotEnoughPoints
        val nowMillis = now().toEpochMilli()
        val bought = state.copy(
            points = state.points - item.price,
            owned = if (isBoost) state.owned else state.owned + code,
            boostUntil = if (isBoost) {
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

    private fun unlock(state: RewardsState, code: String): Outcome {
        if (code in state.achievements) return Outcome(state)
        val achievement = RewardsCatalog.achievement(code) ?: return Outcome(state)
        return grant(
            state.copy(achievements = state.achievements + (code to now().toEpochMilli())),
            RewardGrant(achievement.reward, GrantReason.AchievementUnlocked(code)),
        )
    }

    private fun grant(state: RewardsState, grant: RewardGrant): Outcome = Outcome(
        state.copy(points = state.points + grant.points, lifetimePoints = state.lifetimePoints + grant.points),
        listOf(grant),
    )

    private inline fun Outcome.then(block: (RewardsState) -> Outcome): Outcome {
        val next = block(state)
        return Outcome(next.state, grants + next.grants)
    }
}
