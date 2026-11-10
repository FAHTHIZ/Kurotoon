package kurotoon.rewards

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kurotoon.shop.KuroCosmetics
import kurotoon.shop.PackRepository
import tachiyomi.core.common.preference.PreferenceStore

/**
 * Kuro Points, missions, achievements and shop unlocks, all on the device. Nothing here can be
 * bought with money; points are only earned by reading.
 */
@Inject
@SingleIn(AppScope::class)
class RewardsStore(
    preferenceStore: PreferenceStore,
    val packs: PackRepository,
) {

    internal var engine = RewardsEngine(items = packs::item)

    private val json = Json { ignoreUnknownKeys = true }
    private val preference = preferenceStore.getString(PREF_KEY, "")
    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Follows the stored value, so restoring a backup shows up straight away. */
    val state: Flow<RewardsState> = preference.changes().map { engine.rollDay(decode(it)) }

    val current: RewardsState
        get() = engine.rollDay(read())

    private val _grants = MutableSharedFlow<RewardGrant>(extraBufferCapacity = 8)

    /** Points earned, so the UI can celebrate them. */
    val grants: SharedFlow<RewardGrant> = _grants.asSharedFlow()

    init {
        // Keep the worn theme and particles in sync with what's saved and what packs exist
        combine(state, packs.packs.filterNotNull()) { state, _ -> state }
            .onEach { KuroCosmetics.apply(it, packs.items) }
            .launchIn(scope)
        scope.launch { packs.ensureLoaded() }
    }

    fun claimDailyOpen() = update { engine.claimDailyOpen(it) }

    fun track(event: RewardEvent) = update { engine.track(it, event) }

    /** Call after a chapter is read; pays out when today's chapters reach [goal]. */
    fun meetDailyGoal(goal: Int) = update { engine.meetDailyGoal(it, goal) }

    fun claimMission(code: String): Boolean {
        var claimed = false
        update { state ->
            engine.claimMission(state, code)?.also { claimed = true } ?: RewardsEngine.Outcome(state)
        }
        return claimed
    }

    /** Claims every finished mission at once. */
    fun claimAll(): Int {
        var count = 0
        update { state ->
            engine.claimable(state).fold(RewardsEngine.Outcome(state)) { acc, mission ->
                val next = engine.claimMission(acc.state, mission.code) ?: return@fold acc
                count++
                RewardsEngine.Outcome(next.state, acc.grants + next.grants)
            }
        }
        return count
    }

    fun buy(code: String): PurchaseResult {
        var result: PurchaseResult = PurchaseResult.UnknownItem
        update { state ->
            val (outcome, purchase) = engine.buy(state, code)
            result = purchase
            outcome
        }
        return result
    }

    fun equip(code: String) = update { RewardsEngine.Outcome(engine.equip(it, code)) }

    fun unequip(code: String) = update { RewardsEngine.Outcome(engine.unequip(it, code)) }

    private fun update(block: (RewardsState) -> RewardsEngine.Outcome) {
        val outcome = synchronized(lock) {
            val before = current
            block(before).also { if (it.state != before) write(it.state) }
        }
        outcome.grants.forEach { _grants.tryEmit(it) }
    }

    private fun read(): RewardsState = decode(preference.get())

    private fun decode(raw: String): RewardsState {
        if (raw.isBlank()) return RewardsState()
        return runCatching { json.decodeFromString<RewardsState>(raw) }.getOrDefault(RewardsState())
    }

    private fun write(state: RewardsState) {
        preference.set(json.encodeToString(state))
    }

    companion object {
        const val PREF_KEY = "kurotoon_rewards_state"
    }
}
