package kurotoon.rewards

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.days

/** Everything the rewards layer remembers. Kept on the device and included in backups via preferences. */
@Serializable
data class RewardsState(
    val points: Int = 0,
    val streak: Int = 0,
    val lastDay: String? = null,
    val day: String = "",
    val progress: Map<String, Int> = emptyMap(),
    val claimed: Set<String> = emptySet(),
    val owned: Set<String> = RewardsCatalog.starterItems,
    val achievements: Map<String, Long> = emptyMap(),
    val chaptersRead: Int = 0,
    val boostUntil: Long = 0,
) {
    fun isBoosted(nowMillis: Long): Boolean = boostUntil > nowMillis
}

data class RewardGrant(val points: Int, val title: String, val description: String? = null)

enum class RewardEvent { CHAPTER_READ, BOOKMARK, SEARCH, SOURCE_INSTALLED }

sealed interface PurchaseResult {
    data object Success : PurchaseResult
    data object UnknownItem : PurchaseResult
    data object AlreadyOwned : PurchaseResult
    data object NotEnoughPoints : PurchaseResult
}

/** Pure reward rules; [RewardsStore] applies them and persists the result. */
class RewardsEngine(
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
            current.copy(lastDay = today.toString(), streak = streak),
            RewardGrant(minOf(50, 5 + streak * 5), "Daily visit", "Day $streak streak — keep it up"),
        )
        if (streak >= 7) outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_SEVEN_DAY_STREAK) }
        return outcome
    }

    fun track(state: RewardsState, event: RewardEvent): Outcome {
        val current = rollDay(state)
        return when (event) {
            RewardEvent.CHAPTER_READ -> {
                val read = current.chaptersRead + 1
                var outcome = Outcome(bump(current, RewardsCatalog.MISSION_READ).copy(chaptersRead = read))
                    .then { unlock(it, RewardsCatalog.ACHIEVEMENT_FIRST_CHAPTER) }
                if (now().atZone(zone()).hour < 4) {
                    outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_NIGHT_READER) }
                }
                if (read >= 100) outcome = outcome.then { unlock(it, RewardsCatalog.ACHIEVEMENT_HUNDRED_CHAPTERS) }
                outcome
            }
            RewardEvent.BOOKMARK -> Outcome(bump(current, RewardsCatalog.MISSION_BOOKMARK))
                .then { unlock(it, RewardsCatalog.ACHIEVEMENT_FIRST_BOOKMARK) }
            RewardEvent.SEARCH -> Outcome(bump(current, RewardsCatalog.MISSION_SEARCH))
            RewardEvent.SOURCE_INSTALLED -> unlock(current, RewardsCatalog.ACHIEVEMENT_FIRST_SOURCE)
        }
    }

    fun claimMission(state: RewardsState, code: String): Outcome? {
        val current = rollDay(state)
        val mission = RewardsCatalog.mission(code) ?: return null
        if ((current.progress[code] ?: 0) < mission.target || code in current.claimed) return null
        val reward = if (current.isBoosted(now().toEpochMilli())) mission.reward * 2 else mission.reward
        return grant(current.copy(claimed = current.claimed + code), RewardGrant(reward, mission.title))
    }

    fun buy(state: RewardsState, code: String): Pair<RewardsState, PurchaseResult> {
        val item = RewardsCatalog.item(code) ?: return state to PurchaseResult.UnknownItem
        val isBoost = item.kind == ShopKind.BOOST
        if (!isBoost && code in state.owned) return state to PurchaseResult.AlreadyOwned
        if (state.points < item.price) return state to PurchaseResult.NotEnoughPoints
        val nowMillis = now().toEpochMilli()
        val next = state.copy(
            points = state.points - item.price,
            owned = if (isBoost) state.owned else state.owned + code,
            boostUntil = if (isBoost) {
                maxOf(nowMillis, state.boostUntil) + 7.days.inWholeMilliseconds
            } else {
                state.boostUntil
            },
        )
        return next to PurchaseResult.Success
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
            RewardGrant(achievement.reward, achievement.title, "Achievement unlocked: ${achievement.title}"),
        )
    }

    private fun grant(state: RewardsState, grant: RewardGrant): Outcome {
        return Outcome(state.copy(points = state.points + grant.points), listOf(grant))
    }

    private inline fun Outcome.then(block: (RewardsState) -> Outcome): Outcome {
        val next = block(state)
        return Outcome(next.state, grants + next.grants)
    }
}
