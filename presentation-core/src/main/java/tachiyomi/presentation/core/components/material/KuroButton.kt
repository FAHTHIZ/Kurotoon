package tachiyomi.presentation.core.components.material

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class KuroButtonStyle { Filled, Tonal, Outlined }

/**
 * KuroToon's button: a pill with a press spring. [KuroButtonStyle.Filled] is the accent call to
 * action, [KuroButtonStyle.Tonal] a quieter accent wash, [KuroButtonStyle.Outlined] a hairline pill.
 */
@Composable
fun KuroButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: KuroButtonStyle = KuroButtonStyle.Filled,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
    accent: Color = MaterialTheme.colorScheme.primary,
    onAccent: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && enabled) 0.95f else 1f,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "kuroButtonScale",
    )
    val base = when (style) {
        KuroButtonStyle.Filled -> accent
        KuroButtonStyle.Tonal -> accent.copy(alpha = 0.16f)
        KuroButtonStyle.Outlined -> Color.Transparent
    }
    val fill by animateColorAsState(
        when {
            !enabled && style != KuroButtonStyle.Outlined -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            pressed && style == KuroButtonStyle.Filled -> accent.copy(alpha = 0.85f)
            pressed -> accent.copy(alpha = 0.24f)
            else -> base
        },
        label = "kuroButtonFill",
    )
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        style == KuroButtonStyle.Filled -> onAccent
        else -> accent
    }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(fill)
            .then(
                if (style == KuroButtonStyle.Outlined) {
                    Modifier.border(
                        1.dp,
                        if (enabled) accent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape,
                    )
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides tint,
            LocalTextStyle provides MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        ) {
            content()
        }
    }
}
