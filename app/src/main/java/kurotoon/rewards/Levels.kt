package kurotoon.rewards

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

/** Where a reader stands: level, XP into it, and XP the level needs. XP is lifetime Kuro Points. */
data class LevelInfo(val level: Int, val into: Int, val needed: Int) {
    val fraction: Float get() = if (needed <= 0) 1f else (into.toFloat() / needed).coerceIn(0f, 1f)
    val title: StringResource get() = Levels.title(level)
}

object Levels {

    const val MAX_LEVEL = 99

    /** XP to go from [level] to the next one: 100 at first, growing by 50 each level. */
    fun cost(level: Int): Int = 100 + 50 * (level - 1)

    fun of(xp: Int): LevelInfo {
        var level = 1
        var left = xp.coerceAtLeast(0)
        while (level < MAX_LEVEL && left >= cost(level)) {
            left -= cost(level)
            level++
        }
        return LevelInfo(level, left, if (level >= MAX_LEVEL) 0 else cost(level))
    }

    /** Points paid once when [level] is reached. */
    fun reward(level: Int): Int = (level * 10).coerceAtMost(500)

    fun title(level: Int): StringResource = when {
        level >= 50 -> MR.strings.kurotoon_rewards_title_legend
        level >= 40 -> MR.strings.kurotoon_rewards_title_galaxyReader
        level >= 30 -> MR.strings.kurotoon_rewards_title_nightScholar
        level >= 20 -> MR.strings.kurotoon_rewards_title_storySeeker
        level >= 15 -> MR.strings.kurotoon_rewards_title_arcChaser
        level >= 10 -> MR.strings.kurotoon_rewards_title_bookworm
        level >= 5 -> MR.strings.kurotoon_rewards_title_pageTurner
        else -> MR.strings.kurotoon_rewards_title_rookie
    }
}
