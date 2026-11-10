package kurotoon.ui

import androidx.compose.ui.graphics.Color
import kurotoon.shop.KuroCosmetics

/** KuroToon brand accents that sit on top of the Material color scheme. */
object KuroColors {
    val CrimsonBase = Color(0xFFFF4D4D)
    val CrimsonDeepBase = Color(0xFFE0263A)

    /** The accent: crimson, unless a shop theme is worn. */
    val Crimson: Color get() = KuroCosmetics.accent ?: CrimsonBase
    val CrimsonDeep: Color get() = KuroCosmetics.accentDeep ?: CrimsonDeepBase
    val Gold = Color(0xFFF2C14E)
    val Aurora = Color(0xFF2FB0E6)
    val Galaxy = Color(0xFFA35BE8)
    val Flame = Color(0xFFFB923C)
}
