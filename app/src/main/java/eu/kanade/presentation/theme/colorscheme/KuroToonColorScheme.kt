package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * KuroToon's default theme: ink-black surfaces with a faint cool tint, a crimson accent and gold for ratings.
 *
 * Key colors:
 * Primary #FF4757 (crimson)
 * Tertiary #FFC548 (rating gold)
 * Neutral #09090B (ink)
 */
internal object KuroToonColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFFF4757),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF3D0F17),
        onPrimaryContainer = Color(0xFFFFB2B9),
        inversePrimary = Color(0xFFC21E35),
        secondary = Color(0xFFFF6B78),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFF2A1218),
        onSecondaryContainer = Color(0xFFFF8A94),
        tertiary = Color(0xFFFFC548),
        onTertiary = Color(0xFF2A1F00),
        tertiaryContainer = Color(0xFF3F3000),
        onTertiaryContainer = Color(0xFFFFE08A),
        error = Color(0xFFFF6B60),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF09090B),
        onBackground = Color(0xFFF4F4F6),
        surface = Color(0xFF09090B),
        onSurface = Color(0xFFF4F4F6),
        surfaceVariant = Color(0xFF17171C),
        onSurfaceVariant = Color(0xFF9D9DA8),
        surfaceTint = Color(0xFFFF4757),
        outline = Color(0xFF4A4A54),
        outlineVariant = Color(0xFF26262D),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFF4F4F6),
        inverseOnSurface = Color(0xFF18181C),
        surfaceDim = Color(0xFF09090B),
        surfaceBright = Color(0xFF2B2B33),
        surfaceContainerLowest = Color(0xFF060607),
        surfaceContainerLow = Color(0xFF0F0F12),
        surfaceContainer = Color(0xFF141418),
        surfaceContainerHigh = Color(0xFF1B1B21),
        surfaceContainerHighest = Color(0xFF24242B),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFFD7263D),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDAD6),
        onPrimaryContainer = Color(0xFF410002),
        inversePrimary = Color(0xFFFF8A80),
        secondary = Color(0xFFD7263D),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFE1DE),
        onSecondaryContainer = Color(0xFF8C1D18),
        tertiary = Color(0xFF7A5900),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDF9E),
        onTertiaryContainer = Color(0xFF261A00),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFAFAFA),
        onBackground = Color(0xFF161616),
        surface = Color(0xFFFAFAFA),
        onSurface = Color(0xFF161616),
        surfaceVariant = Color(0xFFF0ECEC),
        onSurfaceVariant = Color(0xFF555050),
        outline = Color(0xFF857373),
        outlineVariant = Color(0xFFD8C2C0),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF2B2B2B),
        inverseOnSurface = Color(0xFFF5F5F5),
        surfaceDim = Color(0xFFE2DADA),
        surfaceBright = Color(0xFFFFFFFF),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF7F3F3),
        surfaceContainer = Color(0xFFF0ECEC),
        surfaceContainerHigh = Color(0xFFEAE5E5),
        surfaceContainerHighest = Color(0xFFE4DFDF),
    )
}
