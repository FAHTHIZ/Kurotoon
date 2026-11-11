package kurotoon.rewards

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

/** A daily goal; progress resets at local midnight. */
data class Mission(
    val code: String,
    val title: StringResource,
    val target: Int,
    val reward: Int,
)

/** A one-time milestone. */
data class Achievement(
    val code: String,
    val title: StringResource,
    val description: StringResource,
    val icon: String,
    val reward: Int,
) {
    /** Medal artwork shipped in assets, one per achievement. */
    val art: String get() = "file:///android_asset/kuro/achievements/$code.webp"
}

/** Missions and achievements. Shop items live in packs, see [kurotoon.shop.PackRepository]. */
object RewardsCatalog {

    const val MISSION_READ = "read_3"
    const val MISSION_READ_MORE = "read_10"
    const val MISSION_BOOKMARK = "bookmark_1"
    const val MISSION_SEARCH = "search_1"

    const val ACHIEVEMENT_FIRST_CHAPTER = "first_chapter"
    const val ACHIEVEMENT_FIRST_BOOKMARK = "first_bookmark"
    const val ACHIEVEMENT_FIRST_SOURCE = "first_source"
    const val ACHIEVEMENT_NIGHT_READER = "night_reader"
    const val ACHIEVEMENT_SEVEN_DAY_STREAK = "seven_day_streak"
    const val ACHIEVEMENT_THIRTY_DAY_STREAK = "thirty_day_streak"
    const val ACHIEVEMENT_HUNDRED_CHAPTERS = "hundred_chapters"
    const val ACHIEVEMENT_THOUSAND_CHAPTERS = "thousand_chapters"
    const val ACHIEVEMENT_COLLECTOR = "collector_10"
    const val ACHIEVEMENT_FIRST_PACK = "first_pack"
    const val ACHIEVEMENT_FIRST_GOAL = "first_goal"
    const val ACHIEVEMENT_THREE_DAY_STREAK = "three_day_streak"
    const val ACHIEVEMENT_HUNDRED_DAY_STREAK = "hundred_day_streak"
    const val ACHIEVEMENT_FIVE_HUNDRED_CHAPTERS = "five_hundred_chapters"
    const val ACHIEVEMENT_BINGE_DAY = "binge_day"
    const val ACHIEVEMENT_EARLY_BIRD = "early_bird"
    const val ACHIEVEMENT_WEEKEND_READER = "weekend_reader"
    const val ACHIEVEMENT_SEASON_COMPLETE = "season_complete"
    const val ACHIEVEMENT_SEASON_VETERAN = "season_veteran"
    const val ACHIEVEMENT_LEVEL_10 = "level_10"
    const val ACHIEVEMENT_LEVEL_25 = "level_25"
    const val ACHIEVEMENT_BIG_SPENDER = "big_spender"
    const val ACHIEVEMENT_COLLECTOR_25 = "collector_25"
    const val ACHIEVEMENT_WEEKLY_HERO = "weekly_hero"

    const val WEEK_READ = "week_read_25"
    const val WEEK_DAYS = "week_days_5"
    const val WEEK_BOOKMARK = "week_bookmark_3"
    const val WEEK_MISSIONS = "week_missions_8"

    /** Chapters in a day for [ACHIEVEMENT_BINGE_DAY]. */
    const val BINGE_TARGET = 10

    /** Points [ACHIEVEMENT_BIG_SPENDER] needs spent. */
    const val SPENDER_TARGET = 5000

    /** Streak freezes a reader can hold at once. */
    const val MAX_FREEZES = 3

    /** Every [CHEST_EVERY] days of streak opens a chest worth [CHEST_REWARD]. */
    const val CHEST_EVERY = 7
    const val CHEST_REWARD = 100

    /** Days of reading history kept for the tracker. */
    const val HISTORY_DAYS = 120

    /** Daily counters kept next to mission progress; they reset at midnight too. */
    const val CHAPTERS_TODAY = "chapters_today"
    const val GOAL_MET = "goal_met"

    /** Items to own before [ACHIEVEMENT_COLLECTOR] unlocks (free ones don't count). */
    const val COLLECTOR_TARGET = 10
    const val COLLECTOR_TARGET_BIG = 25

    val missions: List<Mission> = listOf(
        Mission(MISSION_READ, MR.strings.kurotoon_mission_read3, target = 3, reward = 30),
        Mission(MISSION_READ_MORE, MR.strings.kurotoon_mission_read10, target = 10, reward = 60),
        Mission(MISSION_BOOKMARK, MR.strings.kurotoon_mission_bookmark, target = 1, reward = 15),
        Mission(MISSION_SEARCH, MR.strings.kurotoon_mission_search, target = 1, reward = 10),
    )

    /** Weekly goals; progress resets every Monday. */
    val weeklyMissions: List<Mission> = listOf(
        Mission(WEEK_READ, MR.strings.kurotoon_week_read25, target = 25, reward = 200),
        Mission(WEEK_DAYS, MR.strings.kurotoon_week_days5, target = 5, reward = 150),
        Mission(WEEK_BOOKMARK, MR.strings.kurotoon_week_bookmark3, target = 3, reward = 80),
        Mission(WEEK_MISSIONS, MR.strings.kurotoon_week_missions8, target = 8, reward = 120),
    )

    val achievements: List<Achievement> = listOf(
        Achievement(
            ACHIEVEMENT_FIRST_CHAPTER,
            MR.strings.kurotoon_ach_firstChapter,
            MR.strings.kurotoon_ach_firstChapter_desc,
            "📖",
            25,
        ),
        Achievement(
            ACHIEVEMENT_FIRST_BOOKMARK,
            MR.strings.kurotoon_ach_firstBookmark,
            MR.strings.kurotoon_ach_firstBookmark_desc,
            "🔖",
            25,
        ),
        Achievement(
            ACHIEVEMENT_FIRST_SOURCE,
            MR.strings.kurotoon_ach_firstSource,
            MR.strings.kurotoon_ach_firstSource_desc,
            "🧭",
            25,
        ),
        Achievement(
            ACHIEVEMENT_NIGHT_READER,
            MR.strings.kurotoon_ach_nightReader,
            MR.strings.kurotoon_ach_nightReader_desc,
            "🌙",
            50,
        ),
        Achievement(
            ACHIEVEMENT_SEVEN_DAY_STREAK,
            MR.strings.kurotoon_ach_streak7,
            MR.strings.kurotoon_ach_streak7_desc,
            "🔥",
            150,
        ),
        Achievement(
            ACHIEVEMENT_THIRTY_DAY_STREAK,
            MR.strings.kurotoon_ach_streak30,
            MR.strings.kurotoon_ach_streak30_desc,
            "🌋",
            500,
        ),
        Achievement(
            ACHIEVEMENT_HUNDRED_CHAPTERS,
            MR.strings.kurotoon_ach_chapters100,
            MR.strings.kurotoon_ach_chapters100_desc,
            "💯",
            300,
        ),
        Achievement(
            ACHIEVEMENT_THOUSAND_CHAPTERS,
            MR.strings.kurotoon_ach_chapters1000,
            MR.strings.kurotoon_ach_chapters1000_desc,
            "🏆",
            1500,
        ),
        Achievement(
            ACHIEVEMENT_COLLECTOR,
            MR.strings.kurotoon_ach_collector,
            MR.strings.kurotoon_ach_collector_desc,
            "💎",
            100,
        ),
        Achievement(
            ACHIEVEMENT_FIRST_PACK,
            MR.strings.kurotoon_ach_firstPack,
            MR.strings.kurotoon_ach_firstPack_desc,
            "📦",
            50,
        ),
        Achievement(
            ACHIEVEMENT_FIRST_GOAL,
            MR.strings.kurotoon_ach_firstGoal,
            MR.strings.kurotoon_ach_firstGoal_desc,
            "🎯",
            50,
        ),
        Achievement(
            ACHIEVEMENT_THREE_DAY_STREAK,
            MR.strings.kurotoon_ach_streak3,
            MR.strings.kurotoon_ach_streak3_desc,
            "🔥",
            40,
        ),
        Achievement(
            ACHIEVEMENT_HUNDRED_DAY_STREAK,
            MR.strings.kurotoon_ach_streak100,
            MR.strings.kurotoon_ach_streak100_desc,
            "🐦‍🔥",
            2000,
        ),
        Achievement(
            ACHIEVEMENT_FIVE_HUNDRED_CHAPTERS,
            MR.strings.kurotoon_ach_chapters500,
            MR.strings.kurotoon_ach_chapters500_desc,
            "🗼",
            800,
        ),
        Achievement(
            ACHIEVEMENT_BINGE_DAY,
            MR.strings.kurotoon_ach_bingeDay,
            MR.strings.kurotoon_ach_bingeDay_desc,
            "⚡",
            100,
        ),
        Achievement(
            ACHIEVEMENT_EARLY_BIRD,
            MR.strings.kurotoon_ach_earlyBird,
            MR.strings.kurotoon_ach_earlyBird_desc,
            "🌅",
            50,
        ),
        Achievement(
            ACHIEVEMENT_WEEKEND_READER,
            MR.strings.kurotoon_ach_weekend,
            MR.strings.kurotoon_ach_weekend_desc,
            "☕",
            30,
        ),
        Achievement(
            ACHIEVEMENT_WEEKLY_HERO,
            MR.strings.kurotoon_ach_weeklyHero,
            MR.strings.kurotoon_ach_weeklyHero_desc,
            "🗓️",
            250,
        ),
        Achievement(
            ACHIEVEMENT_SEASON_COMPLETE,
            MR.strings.kurotoon_ach_seasonComplete,
            MR.strings.kurotoon_ach_seasonComplete_desc,
            "🏮",
            200,
        ),
        Achievement(
            ACHIEVEMENT_SEASON_VETERAN,
            MR.strings.kurotoon_ach_seasonVeteran,
            MR.strings.kurotoon_ach_seasonVeteran_desc,
            "🍁",
            1000,
        ),
        Achievement(
            ACHIEVEMENT_LEVEL_10,
            MR.strings.kurotoon_ach_level10,
            MR.strings.kurotoon_ach_level10_desc,
            "🛡️",
            200,
        ),
        Achievement(
            ACHIEVEMENT_LEVEL_25,
            MR.strings.kurotoon_ach_level25,
            MR.strings.kurotoon_ach_level25_desc,
            "👑",
            750,
        ),
        Achievement(
            ACHIEVEMENT_BIG_SPENDER,
            MR.strings.kurotoon_ach_bigSpender,
            MR.strings.kurotoon_ach_bigSpender_desc,
            "💰",
            300,
        ),
        Achievement(
            ACHIEVEMENT_COLLECTOR_25,
            MR.strings.kurotoon_ach_collector25,
            MR.strings.kurotoon_ach_collector25_desc,
            "💎",
            400,
        ),
    )

    fun mission(code: String): Mission? = missions.firstOrNull { it.code == code }

    fun weeklyMission(code: String): Mission? = weeklyMissions.firstOrNull { it.code == code }

    fun achievement(code: String): Achievement? = achievements.firstOrNull { it.code == code }
}
