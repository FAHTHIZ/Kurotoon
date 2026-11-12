package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Rounded corners for KuroToon's text fields. */
val KuroFieldShape = RoundedCornerShape(14.dp)

/**
 * KuroToon's text field look: a filled, borderless surface that lights an accent outline when focused,
 * and turns red on error. Pass with [KuroFieldShape] to an outlined text field.
 */
@Composable
fun kuroFieldColors(): TextFieldColors {
    val scheme = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = scheme.surfaceContainerHigh,
        unfocusedContainerColor = scheme.surfaceContainerHigh,
        disabledContainerColor = scheme.surfaceContainer,
        errorContainerColor = scheme.surfaceContainerHigh,
        focusedBorderColor = scheme.primary,
        unfocusedBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent,
        errorBorderColor = scheme.error,
        cursorColor = scheme.primary,
        focusedLabelColor = scheme.primary,
        unfocusedLabelColor = scheme.onSurfaceVariant,
        focusedLeadingIconColor = scheme.primary,
        focusedTrailingIconColor = scheme.primary,
    )
}
