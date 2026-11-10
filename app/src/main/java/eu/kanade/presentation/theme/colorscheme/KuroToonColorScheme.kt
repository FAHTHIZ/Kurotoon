package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * KuroToon's default theme: near-black surfaces with a crimson accent and gold highlights.
 *
 * Key colors:
 * Primary #FF4D4D
 * Tertiary #F2C14E
 * Neutral #0B0B0B
 */
internal object KuroToonColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFFFF4D4D),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF5C1414),
        onPrimaryContainer = Color(0xFFFFDAD6),
        inversePrimary = Color(0xFFB3261E),
        secondary = Color(0xFFFF4D4D),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFF2E1313),
        onSecondaryContainer = Color(0xFFFF6B6B),
        tertiary = Color(0xFFF2C14E),
        onTertiary = Color(0xFF2A1F00),
        tertiaryContainer = Color(0xFF4D3A00),
        onTertiaryContainer = Color(0xFFFFE08A),
        error = Color(0xFFFF6B60),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF0B0B0B),
        onBackground = Color(0xFFF5F5F5),
        surface = Color(0xFF0B0B0B),
        onSurface = Color(0xFFF5F5F5),
        surfaceVariant = Color(0xFF151515),
        onSurfaceVariant = Color(0xFFA1A1A1),
        outline = Color(0xFF5A5A5A),
        outlineVariant = Color(0xFF262626),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFF5F5F5),
        inverseOnSurface = Color(0xFF1A1A1A),
        surfaceDim = Color(0xFF0B0B0B),
        surfaceBright = Color(0xFF2A2A2A),
        surfaceContainerLowest = Color(0xFF080808),
        surfaceContainerLow = Color(0xFF111111),
        surfaceContainer = Color(0xFF151515),
        surfaceContainerHigh = Color(0xFF1C1C1C),
        surfaceContainerHighest = Color(0xFF242424),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFFC62828),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDAD6),
        onPrimaryContainer = Color(0xFF410002),
        inversePrimary = Color(0xFFFF8A80),
        secondary = Color(0xFFC62828),
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
