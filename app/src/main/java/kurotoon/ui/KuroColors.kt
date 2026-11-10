package kurotoon.ui

import androidx.compose.ui.graphics.Color
import kurotoon.rewards.Rarity

/** KuroToon brand accents that sit on top of the Material color scheme. */
object KuroColors {
    val Crimson = Color(0xFFFF4D4D)
    val CrimsonDeep = Color(0xFFE0263A)
    val Gold = Color(0xFFF2C14E)
    val Aurora = Color(0xFF2FB0E6)
    val Galaxy = Color(0xFFA35BE8)
    val Flame = Color(0xFFFB923C)

    fun rarity(rarity: Rarity): Color = when (rarity) {
        Rarity.COMMON -> Color.White.copy(alpha = 0.15f)
        Rarity.RARE -> Aurora.copy(alpha = 0.5f)
        Rarity.EPIC -> Galaxy.copy(alpha = 0.55f)
        Rarity.LEGENDARY -> Gold.copy(alpha = 0.6f)
        Rarity.MYTHIC -> Crimson.copy(alpha = 0.7f)
    }
}
