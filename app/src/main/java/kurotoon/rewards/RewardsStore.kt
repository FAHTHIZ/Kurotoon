package kurotoon.rewards

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import tachiyomi.core.common.preference.PreferenceStore

/** Kuro Points, missions, achievements and shop unlocks. Nothing here can be bought with money. */
@Inject
@SingleIn(AppScope::class)
class RewardsStore(
    preferenceStore: PreferenceStore,
) {

    internal var engine = RewardsEngine()

    private val json = Json { ignoreUnknownKeys = true }
    private val preference = preferenceStore.getString(PREF_KEY, "")
    private val lock = Any()

    /** Follows the stored value, so restoring a backup is reflected immediately. */
    val state: Flow<RewardsState> = preference.changes().map { engine.rollDay(decode(it)) }

    val current: RewardsState
        get() = engine.rollDay(read())

    private val _grants = MutableSharedFlow<RewardGrant>(extraBufferCapacity = 8)

    /** Points earned, so the UI can celebrate them. */
    val grants: SharedFlow<RewardGrant> = _grants.asSharedFlow()

    fun claimDailyOpen() = update { engine.claimDailyOpen(it) }

    fun track(event: RewardEvent) = update { engine.track(it, event) }

    fun claimMission(code: String): Boolean {
        var claimed = false
        update { state ->
            engine.claimMission(state, code)?.also { claimed = true } ?: RewardsEngine.Outcome(state)
        }
        return claimed
    }

    fun buy(code: String): PurchaseResult = synchronized(lock) {
        val (next, result) = engine.buy(read(), code)
        if (result == PurchaseResult.Success) write(next)
        result
    }

    private fun update(block: (RewardsState) -> RewardsEngine.Outcome) {
        val outcome = synchronized(lock) {
            block(read()).also { write(it.state) }
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
