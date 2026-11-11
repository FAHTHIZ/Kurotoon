package kurotoon.rewards

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kurotoon.shop.Rarity
import kurotoon.shop.ShopItem
import kurotoon.shop.ShopKind
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneOffset

class RewardsEngineTest {

    private var now = Instant.parse("2026-11-10T12:00:00Z")

    private val items = listOf(
        item("theme", ShopKind.THEME, 100),
        item("theme2", ShopKind.THEME, 100),
        item("free", ShopKind.STICKER, 0),
        item("boost", ShopKind.BOOST, 50),
        item("b1", ShopKind.BADGE, 1),
        item("b2", ShopKind.BADGE, 1),
        item("b3", ShopKind.BADGE, 1),
        item("b4", ShopKind.BADGE, 1),
        item("freeze", ShopKind.BOOST, 300, FREEZE_STYLE),
        ShopItem(
            "kuro.seasonal/halloween", "kuro.seasonal", ShopKind.STICKER, "Halloween", "", 100,
            Rarity.RARE, null, null, emptyList(), null,
        ),
    ).associateBy { it.code }

    private val engine = RewardsEngine(items = items::get, now = { now }, zone = { ZoneOffset.UTC })

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
        state.bestStreak shouldBe 7
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
    fun `buying spends points, wears the item and refuses repeats`() {
        val rich = RewardsState(points = 150)
        val (outcome, result) = engine.buy(rich, "test/theme")
        result shouldBe PurchaseResult.Success
        outcome.state.points shouldBe 50
        outcome.state.equipped[ShopKind.THEME.key] shouldBe "test/theme"
        engine.buy(outcome.state, "test/theme").second shouldBe PurchaseResult.AlreadyOwned
        engine.buy(outcome.state, "test/theme2").second shouldBe PurchaseResult.NotEnoughPoints
        engine.buy(outcome.state, "nope/nope").second shouldBe PurchaseResult.UnknownItem
    }

    @Test
    fun `free items are owned without buying`() {
        engine.buy(RewardsState(), "test/free").second shouldBe PurchaseResult.AlreadyOwned
        engine.equip(RewardsState(), "test/free").equipped[ShopKind.STICKER.key] shouldBe "test/free"
    }

    @Test
    fun `boosts stack and double mission rewards`() {
        var state = engine.buy(RewardsState(points = 100), "test/boost").first.state
        state = engine.buy(state, "test/boost").first.state
        state.points shouldBe 0
        state.boostUntil shouldBe now.toEpochMilli() + 14 * 86_400_000L

        state = engine.track(state, RewardEvent.SEARCH).state
        engine.claimMission(state, RewardsCatalog.MISSION_SEARCH)!!.grants.single().points shouldBe 20
    }

    @Test
    fun `badges stack up to three and drop the oldest`() {
        var state = RewardsState(points = 10)
        listOf("b1", "b2", "b3", "b4").forEach { state = engine.buy(state, "test/$it").first.state }
        state.badges shouldContainExactly listOf("test/b2", "test/b3", "test/b4")
        engine.unequip(state, "test/b3").badges shouldContainExactly listOf("test/b2", "test/b4")
    }

    @Test
    fun `items that are not owned can't be worn`() {
        engine.equip(RewardsState(), "test/theme").equipped shouldBe emptyMap()
    }

    @Test
    fun `importing a pack unlocks its achievement once`() {
        val first = engine.track(RewardsState(), RewardEvent.PACK_IMPORTED)
        first.grants.size shouldBe 1
        engine.track(first.state, RewardEvent.PACK_IMPORTED).grants.size shouldBe 0
    }

    @Test
    fun `daily goal pays once when today's chapters reach it`() {
        var state = RewardsState()
        state = engine.track(state, RewardEvent.CHAPTER_READ).state
        engine.meetDailyGoal(state, 2).grants.size shouldBe 0
        state = engine.track(state, RewardEvent.CHAPTER_READ).state
        val met = engine.meetDailyGoal(state, 2)
        met.grants.map { it.reason } shouldContain GrantReason.GoalMet(2)
        met.grants.map { it.reason } shouldContain
            GrantReason.AchievementUnlocked(RewardsCatalog.ACHIEVEMENT_FIRST_GOAL)
        state = met.state
        state = engine.track(state, RewardEvent.CHAPTER_READ).state
        engine.meetDailyGoal(state, 2).grants.size shouldBe 0
        engine.meetDailyGoal(state, 0).grants.size shouldBe 0

        // A new day starts the count again
        now = now.plusSeconds(86_400)
        state = engine.track(state, RewardEvent.CHAPTER_READ).state
        state = engine.track(state, RewardEvent.CHAPTER_READ).state
        engine.meetDailyGoal(state, 2).grants.map { it.reason } shouldContainExactly listOf(GrantReason.GoalMet(2))
    }

    @Test
    fun `streak freezes cover missed days`() {
        val state = RewardsState(lastDay = "2026-11-08", streak = 5, bestStreak = 5, freezes = 1)
        val out = engine.claimDailyOpen(state)
        out.state.streak shouldBe 6
        out.state.freezes shouldBe 0
        out.grants.map { it.reason } shouldContain GrantReason.Daily(6, 1)
        // Without a freeze the streak starts over
        engine.claimDailyOpen(state.copy(freezes = 0)).state.streak shouldBe 1
    }

    @Test
    fun `freezes are bought up to the cap`() {
        val (out, result) = engine.buy(RewardsState(points = 1000), "test/freeze")
        result shouldBe PurchaseResult.Success
        out.state.freezes shouldBe 1
        engine.buy(RewardsState(points = 1000, freezes = RewardsCatalog.MAX_FREEZES), "test/freeze").second shouldBe
            PurchaseResult.FreezesFull
    }

    @Test
    fun `every seventh day opens a streak chest`() {
        val state = RewardsState(lastDay = "2026-11-09", streak = 6, bestStreak = 6)
        engine.daysToChest(state) shouldBe 0
        engine.claimDailyOpen(state).grants.map { it.reason } shouldContain GrantReason.StreakChest(7)
        engine.daysToChest(state.copy(streak = 2)) shouldBe 4
    }

    @Test
    fun `weekly missions pay once and reset on monday`() {
        var state = RewardsState()
        repeat(25) { state = engine.track(state, RewardEvent.CHAPTER_READ).state }
        engine.claimableWeekly(state).map { it.code } shouldContain RewardsCatalog.WEEK_READ
        state = engine.claimWeekly(state, RewardsCatalog.WEEK_READ)!!.state
        engine.claimWeekly(state, RewardsCatalog.WEEK_READ) shouldBe null
        now = Instant.parse("2026-11-16T12:00:00Z")
        engine.rollDay(state).weekProgress shouldBe emptyMap()
    }

    @Test
    fun `seasonal events track chores while running`() {
        // 10 November falls in the anniversary event
        val run = engine.activeSeasons().single()
        run.event.id shouldBe "anniversary"
        val state = engine.track(RewardsState(), RewardEvent.CHAPTER_READ).state
        val read = run.event.chores.first { it.kind == ChoreKind.READ }
        state.seasonProgress[run.choreKey(read)] shouldBe 1
    }

    @Test
    fun `seasonal items only sell in season`() {
        engine.buy(RewardsState(points = 500), "kuro.seasonal/halloween").second shouldBe PurchaseResult.OutOfSeason
        now = Instant.parse("2026-10-20T12:00:00Z")
        engine.buy(RewardsState(points = 500), "kuro.seasonal/halloween").second shouldBe PurchaseResult.Success
    }

    @Test
    fun `levels grow with lifetime points`() {
        Levels.of(0).level shouldBe 1
        Levels.of(99).level shouldBe 1
        Levels.of(100).level shouldBe 2
        Levels.of(250).level shouldBe 3
        Levels.of(Int.MAX_VALUE).level shouldBe Levels.MAX_LEVEL
        RewardsState(lifetimePoints = 260).level.into shouldBe 10
    }

    private fun item(id: String, kind: ShopKind, price: Int, style: String? = null) = ShopItem(
        code = "test/$id",
        packId = "test",
        kind = kind,
        name = id,
        description = "",
        price = price,
        rarity = Rarity.COMMON,
        image = null,
        emoji = null,
        colors = emptyList(),
        style = style,
    )
}
