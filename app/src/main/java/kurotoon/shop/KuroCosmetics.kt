package kurotoon.shop

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import kurotoon.rewards.RewardsState

/**
 * What the worn shop items change outside the profile: the accent colour (a theme) and the
 * particle palette. Backed by snapshot state, so everything that reads it updates live.
 */
object KuroCosmetics {

    /** Accent from the worn theme, or null for KuroToon's crimson. */
    var accent by mutableStateOf<Color?>(null)
        private set

    var accentDeep by mutableStateOf<Color?>(null)
        private set

    /** Colours for embers and sparks from the worn particle set, or null for the default mix. */
    var particleColors by mutableStateOf<List<Color>?>(null)
        private set

    fun apply(state: RewardsState, items: Map<String, ShopItem>) {
        val theme = state.equipped[ShopKind.THEME.key]?.let(items::get)?.takeIf { state.owns(it) }
        val main = theme?.colors?.firstOrNull()
        accent = main
        accentDeep = main?.let { theme.colors.getOrNull(1) ?: lerp(it, Color.Black, 0.18f) }

        val particles = state.equipped[ShopKind.PARTICLES.key]?.let(items::get)?.takeIf { state.owns(it) }
        particleColors = particles?.colors?.takeIf { it.isNotEmpty() }
    }

    /** Recolours a KuroToon scheme with the worn accent; other themes are left alone. */
    fun tint(scheme: ColorScheme, isDark: Boolean): ColorScheme {
        val main = accent ?: return scheme
        val deep = accentDeep ?: main
        val primary = if (isDark) main else deep
        return scheme.copy(
            primary = primary,
            secondary = primary,
            inversePrimary = if (isDark) deep else main,
            primaryContainer = main.copy(alpha = if (isDark) 0.32f else 0.18f).compositeOver(scheme.surface),
            onPrimaryContainer = if (isDark) lerp(main, Color.White, 0.7f) else lerp(deep, Color.Black, 0.6f),
            secondaryContainer = main.copy(alpha = if (isDark) 0.16f else 0.12f).compositeOver(scheme.surface),
            onSecondaryContainer = if (isDark) lerp(main, Color.White, 0.2f) else deep,
        )
    }
}
