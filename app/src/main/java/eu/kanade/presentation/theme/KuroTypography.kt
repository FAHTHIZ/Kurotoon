package eu.kanade.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em

/**
 * Material's type scale with firmer titles and tighter large text, so headers read clearly over cover art and the
 * small print stays light.
 */
internal val KuroTypography: Typography = Typography().run {
    fun TextStyle.tight(weight: FontWeight, tracking: Double) = copy(fontWeight = weight, letterSpacing = tracking.em)
    copy(
        displayLarge = displayLarge.tight(FontWeight.Bold, -0.02),
        displayMedium = displayMedium.tight(FontWeight.Bold, -0.02),
        displaySmall = displaySmall.tight(FontWeight.Bold, -0.015),
        headlineLarge = headlineLarge.tight(FontWeight.Bold, -0.015),
        headlineMedium = headlineMedium.tight(FontWeight.Bold, -0.01),
        headlineSmall = headlineSmall.tight(FontWeight.SemiBold, -0.01),
        titleLarge = titleLarge.tight(FontWeight.SemiBold, -0.005),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}
