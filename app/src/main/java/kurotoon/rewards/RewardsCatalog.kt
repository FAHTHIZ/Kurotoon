package kurotoon.rewards

enum class Rarity { COMMON, RARE, EPIC, LEGENDARY, MYTHIC }

enum class ShopKind { STICKER, AVATAR, FRAME, THEME, BADGE, BOOST }

data class ShopItem(
    val code: String,
    val name: String,
    val description: String,
    val kind: ShopKind,
    val price: Int,
    val rarity: Rarity,
    val emoji: String? = null,
)

data class Mission(
    val code: String,
    val title: String,
    val description: String,
    val target: Int,
    val reward: Int,
)

data class Achievement(
    val code: String,
    val title: String,
    val description: String,
    val icon: String,
    val reward: Int,
)

object RewardsCatalog {

    const val MISSION_READ = "read_3"
    const val MISSION_BOOKMARK = "bookmark_1"
    const val MISSION_SEARCH = "search_1"

    const val ACHIEVEMENT_FIRST_CHAPTER = "first_chapter"
    const val ACHIEVEMENT_FIRST_BOOKMARK = "first_bookmark"
    const val ACHIEVEMENT_FIRST_SOURCE = "first_source"
    const val ACHIEVEMENT_NIGHT_READER = "night_reader"
    const val ACHIEVEMENT_SEVEN_DAY_STREAK = "seven_day_streak"
    const val ACHIEVEMENT_HUNDRED_CHAPTERS = "hundred_chapters"

    val starterItems = setOf("sticker_sparkle", "sticker_heart", "sticker_fire")

    val shop: List<ShopItem> = listOf(
        ShopItem("sticker_sparkle", "Sparkle Eyes", "A free starter sticker.", ShopKind.STICKER, 0, Rarity.COMMON, "✨"),
        ShopItem("sticker_heart", "Heart Burst", "A free starter sticker.", ShopKind.STICKER, 0, Rarity.COMMON, "💖"),
        ShopItem("sticker_fire", "Fire", "For chapters that hit hard.", ShopKind.STICKER, 0, Rarity.COMMON, "🔥"),
        ShopItem(
            "sticker_happy",
            "Happy Kuro",
            "Kuro beaming with a big chibi smile.",
            ShopKind.STICKER,
            100,
            Rarity.COMMON,
            "😺",
        ),
        ShopItem(
            "sticker_reading",
            "Reading Kuro",
            "Hood up, nose in a chapter. The classic.",
            ShopKind.STICKER,
            150,
            Rarity.COMMON,
            "📖",
        ),
        ShopItem(
            "sticker_sleepy",
            "Sleepy Kuro",
            "One more chapter... zzz.",
            ShopKind.STICKER,
            150,
            Rarity.COMMON,
            "😴",
        ),
        ShopItem(
            "sticker_love",
            "Love Kuro",
            "Heart eyes for your favourite series.",
            ShopKind.STICKER,
            200,
            Rarity.RARE,
            "😻",
        ),
        ShopItem(
            "sticker_shock",
            "Shock Kuro",
            "That cliffhanger reaction, bottled.",
            ShopKind.STICKER,
            200,
            Rarity.RARE,
            "🙀",
        ),
        ShopItem("sticker_sakura", "Sakura", "Blossom season.", ShopKind.STICKER, 250, Rarity.RARE, "🌸"),
        ShopItem("sticker_crown", "Crown", "Royalty of the reading list.", ShopKind.STICKER, 400, Rarity.EPIC, "👑"),
        ShopItem("sticker_rich", "Rich Kuro", "Showered in K Coins.", ShopKind.STICKER, 800, Rarity.EPIC, "🪙"),
        ShopItem(
            "sticker_dragon",
            "Dragon",
            "Legendary reader energy.",
            ShopKind.STICKER,
            1000,
            Rarity.LEGENDARY,
            "🐉",
        ),
        ShopItem(
            "sticker_galaxy",
            "Galaxy Kuro",
            "Kuro dissolving into cosmic particles.",
            ShopKind.STICKER,
            2000,
            Rarity.MYTHIC,
            "🌌",
        ),
        ShopItem(
            "avatar_kuro_classic",
            "Classic Kuro",
            "The original hoodie-and-paw-gloves Kuro.",
            ShopKind.AVATAR,
            200,
            Rarity.COMMON,
            "🐱",
        ),
        ShopItem(
            "avatar_kuro_hood",
            "Hood-Up Kuro",
            "Night reading mode Kuro.",
            ShopKind.AVATAR,
            400,
            Rarity.RARE,
            "🐈‍⬛",
        ),
        ShopItem(
            "avatar_kuro_winter",
            "Winter Kuro",
            "Snow-dusted scarf edition.",
            ShopKind.AVATAR,
            1500,
            Rarity.EPIC,
            "❄️",
        ),
        ShopItem(
            "frame_starfield",
            "Starfield Frame",
            "Quiet drifting stars around your avatar.",
            ShopKind.FRAME,
            250,
            Rarity.COMMON,
            "⭐",
        ),
        ShopItem(
            "frame_nebula",
            "Nebula Frame",
            "Slow-swirling nebula ring in cosmic purple.",
            ShopKind.FRAME,
            700,
            Rarity.RARE,
            "🔮",
        ),
        ShopItem(
            "frame_coin",
            "Coin Holder Frame",
            "A ring of spinning K Coins.",
            ShopKind.FRAME,
            1400,
            Rarity.EPIC,
            "🪙",
        ),
        ShopItem(
            "frame_mythic",
            "Mythic Frame",
            "Shifting mythic gradient frame.",
            ShopKind.FRAME,
            3000,
            Rarity.MYTHIC,
            "💠",
        ),
        ShopItem(
            "theme_midnight",
            "Midnight Theme",
            "Deepest black surface with crimson accents.",
            ShopKind.THEME,
            300,
            Rarity.COMMON,
            "🌑",
        ),
        ShopItem(
            "theme_aurora",
            "Aurora Theme",
            "Aurora-blue accents and glow.",
            ShopKind.THEME,
            600,
            Rarity.RARE,
            "🌌",
        ),
        ShopItem(
            "theme_galaxy",
            "Galaxy Theme",
            "Galaxy-purple accents and glow.",
            ShopKind.THEME,
            600,
            Rarity.RARE,
            "🪐",
        ),
        ShopItem(
            "theme_gold",
            "Soft Gold Theme",
            "Warm gold accents for a premium mood.",
            ShopKind.THEME,
            1200,
            Rarity.EPIC,
            "✨",
        ),
        ShopItem("badge_kuro_scout", "Kuro Scout", "For early explorers.", ShopKind.BADGE, 150, Rarity.COMMON, "🧭"),
        ShopItem(
            "badge_night_reader",
            "Night Reader",
            "For those who read past midnight.",
            ShopKind.BADGE,
            400,
            Rarity.RARE,
            "🌙",
        ),
        ShopItem(
            "badge_galaxy_sage",
            "Galaxy Sage",
            "Purple shimmer reserved for deep readers.",
            ShopKind.BADGE,
            1800,
            Rarity.LEGENDARY,
            "🔭",
        ),
        ShopItem(
            "boost_double",
            "Double Points (7 days)",
            "Double points from missions for a week.",
            ShopKind.BOOST,
            1500,
            Rarity.EPIC,
            "⚡",
        ),
    )

    val missions: List<Mission> = listOf(
        Mission(MISSION_READ, "Read 3 chapters", "Finish 3 chapters today", 3, 30),
        Mission(MISSION_BOOKMARK, "Add to library", "Add 1 series to your library", 1, 15),
        Mission(MISSION_SEARCH, "Global search", "Search across your sources once", 1, 10),
    )

    val achievements: List<Achievement> = listOf(
        Achievement(ACHIEVEMENT_FIRST_CHAPTER, "First Chapter", "Finish your first chapter", "📖", 25),
        Achievement(ACHIEVEMENT_FIRST_BOOKMARK, "Collector", "Add your first series to the library", "🔖", 25),
        Achievement(ACHIEVEMENT_FIRST_SOURCE, "Explorer", "Install your first source", "🧭", 25),
        Achievement(ACHIEVEMENT_NIGHT_READER, "Night Reader", "Finish a chapter between midnight and 4am", "🌙", 50),
        Achievement(ACHIEVEMENT_SEVEN_DAY_STREAK, "7-Day Reader", "Open KuroToon 7 days in a row", "📚", 150),
        Achievement(ACHIEVEMENT_HUNDRED_CHAPTERS, "100 Chapters", "Finish 100 chapters", "💯", 300),
    )

    fun item(code: String): ShopItem? = shop.firstOrNull { it.code == code }
    fun mission(code: String): Mission? = missions.firstOrNull { it.code == code }
    fun achievement(code: String): Achievement? = achievements.firstOrNull { it.code == code }
}
