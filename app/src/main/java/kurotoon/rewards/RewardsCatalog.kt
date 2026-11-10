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
)

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

    /** Daily counters kept next to mission progress; they reset at midnight too. */
    const val CHAPTERS_TODAY = "chapters_today"
    const val GOAL_MET = "goal_met"

    /** Items to own before [ACHIEVEMENT_COLLECTOR] unlocks (free ones don't count). */
    const val COLLECTOR_TARGET = 10

    val missions: List<Mission> = listOf(
        Mission(MISSION_READ, MR.strings.kurotoon_mission_read3, target = 3, reward = 30),
        Mission(MISSION_READ_MORE, MR.strings.kurotoon_mission_read10, target = 10, reward = 60),
        Mission(MISSION_BOOKMARK, MR.strings.kurotoon_mission_bookmark, target = 1, reward = 15),
        Mission(MISSION_SEARCH, MR.strings.kurotoon_mission_search, target = 1, reward = 10),
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
    )

    fun mission(code: String): Mission? = missions.firstOrNull { it.code == code }

    fun achievement(code: String): Achievement? = achievements.firstOrNull { it.code == code }
}
