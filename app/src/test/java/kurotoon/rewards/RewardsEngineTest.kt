package kurotoon.rewards

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import java.time.Instant
import java.time.ZoneOffset

class RewardsEngineTest {

    private var now = Instant.parse("2026-11-10T12:00:00Z")
    private val engine = RewardsEngine(now = { now }, zone = { ZoneOffset.UTC })

    @Test
    fun `daily open pays once per day and grows the streak`() {
        var state = engine.claimDailyOpen(RewardsState()).also { it.grants.size shouldBe 1 }.state
        engine.claimDailyOpen(state).grants.size shouldBe 0
        state.points shouldBe 10
        state.streak shouldBe 1

        now = now.plusSeconds(86_400)
        state = engine.claimDailyOpen(state).state
        state.streak shouldBe 2
        state.points shouldBe 10 + 15

        now = now.plusSeconds(3 * 86_400)
        engine.claimDailyOpen(state).state.streak shouldBe 1
    }

    @Test
    fun `seven day streak unlocks its achievement`() {
        var state = RewardsState()
        repeat(7) {
            state = engine.claimDailyOpen(state).state
            now = now.plusSeconds(86_400)
        }
        state.streak shouldBe 7
        state.achievements.keys shouldContain RewardsCatalog.ACHIEVEMENT_SEVEN_DAY_STREAK
    }

    @Test
    fun `missions can be claimed once when complete`() {
        var state = engine.claimDailyOpen(RewardsState()).state
        engine.claimMission(state, RewardsCatalog.MISSION_READ) shouldBe null
        repeat(3) { state = engine.track(state, RewardEvent.CHAPTER_READ).state }
        state = engine.claimMission(state, RewardsCatalog.MISSION_READ)!!.state
        engine.claimMission(state, RewardsCatalog.MISSION_READ) shouldBe null
        state.points shouldBe 10 + 25 + 30
        state.chaptersRead shouldBe 3
    }

    @Test
    fun `missions reset on a new day`() {
        var state = engine.track(RewardsState(), RewardEvent.SEARCH).state
        state.progress[RewardsCatalog.MISSION_SEARCH] shouldBe 1
        now = now.plusSeconds(86_400)
        state = engine.rollDay(state)
        state.progress[RewardsCatalog.MISSION_SEARCH] shouldBe null
    }

    @Test
    fun `boost doubles mission rewards`() {
        var state = RewardsState(points = 1500)
        state = engine.buy(state, "boost_double").also { it.second shouldBe PurchaseResult.Success }.first
        state.points shouldBe 0
        state = engine.track(state, RewardEvent.SEARCH).state
        state = engine.claimMission(state, RewardsCatalog.MISSION_SEARCH)!!.state
        state.points shouldBe 20
    }

    @Test
    fun `night reading unlocks night reader`() {
        now = Instant.parse("2026-11-10T02:30:00Z")
        val state = engine.track(RewardsState(), RewardEvent.CHAPTER_READ).state
        state.achievements.keys shouldContain RewardsCatalog.ACHIEVEMENT_NIGHT_READER
    }

    @Test
    fun `shop rejects unknown, owned and unaffordable items`() {
        val state = RewardsState(points = 100)
        engine.buy(state, "does_not_exist").second shouldBe PurchaseResult.UnknownItem
        engine.buy(state, "sticker_sparkle").second shouldBe PurchaseResult.AlreadyOwned
        engine.buy(state, "sticker_crown").second shouldBe PurchaseResult.NotEnoughPoints
        val (bought, result) = engine.buy(state, "sticker_happy")
        result shouldBe PurchaseResult.Success
        bought.points shouldBe 0
        bought.owned shouldContain "sticker_happy"
    }

    @Test
    fun `store applies events and reports grants`() {
        val store = RewardsStore(InMemoryPreferenceStore()).apply { engine = this@RewardsEngineTest.engine }
        store.claimDailyOpen()
        store.track(RewardEvent.BOOKMARK)
        store.claimMission(RewardsCatalog.MISSION_BOOKMARK) shouldBe true
        store.claimMission(RewardsCatalog.MISSION_BOOKMARK) shouldBe false
        store.current.points shouldBe 10 + 25 + 15
        store.buy("sticker_sparkle") shouldBe PurchaseResult.AlreadyOwned
    }
}
