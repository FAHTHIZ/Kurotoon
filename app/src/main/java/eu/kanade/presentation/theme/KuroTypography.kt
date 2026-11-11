package eu.kanade.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.R

/**
 * Plus Jakarta Sans: a modern geometric sans with open counters. It holds up over cover art at display sizes and
 * stays readable in dense chapter lists. Bundled under the SIL Open Font License (docs/licenses).
 */
val KuroFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.Black),
)

private val Trim = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = KuroFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
    lineHeightStyle = Trim,
)

/**
 * KuroToon's type scale. Editorial display sizes (extra bold, tight) for hero titles and series names, firm
 * semibold titles for shelves and app bars, and slightly roomier body text for synopses and chapter rows.
 */
internal val KuroTypography: Typography = Typography(
    displayLarge = style(52, 58, FontWeight.ExtraBold, -0.03),
    displayMedium = style(42, 48, FontWeight.ExtraBold, -0.025),
    displaySmall = style(34, 40, FontWeight.ExtraBold, -0.02),
    headlineLarge = style(30, 36, FontWeight.ExtraBold, -0.02),
    headlineMedium = style(26, 32, FontWeight.Bold, -0.015),
    headlineSmall = style(22, 28, FontWeight.Bold, -0.01),
    titleLarge = style(20, 26, FontWeight.Bold, -0.01),
    titleMedium = style(16, 22, FontWeight.Bold, -0.005),
    titleSmall = style(14, 20, FontWeight.SemiBold),
    bodyLarge = style(16, 24, FontWeight.Normal),
    bodyMedium = style(14, 21, FontWeight.Normal),
    bodySmall = style(12, 17, FontWeight.Normal, 0.005),
    labelLarge = style(14, 20, FontWeight.SemiBold, 0.005),
    labelMedium = style(12, 16, FontWeight.SemiBold, 0.01),
    labelSmall = style(11, 14, FontWeight.SemiBold, 0.02),
)
