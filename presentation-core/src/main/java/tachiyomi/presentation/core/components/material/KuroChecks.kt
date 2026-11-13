package tachiyomi.presentation.core.components.material

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import kurotoon.icons.materialsymbols.MaterialSymbols
import kurotoon.icons.materialsymbols.rounded.Check

private val MarkShape = RoundedCornerShape(8.dp)

/**
 * KuroToon's tick: a rounded square that fills with the accent and shows a check when on.
 * Pass a null [onCheckedChange] when the whole row handles the tap.
 */
@Composable
fun KuroCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    KuroTriStateCheckbox(
        state = if (checked) ToggleableState.On else ToggleableState.Off,
        onClick = onCheckedChange?.let { { it(!checked) } },
        modifier = modifier,
        enabled = enabled,
    )
}

/** [KuroCheckbox] with a third, partly-on state drawn as a dash. */
@Composable
fun KuroTriStateCheckbox(
    state: ToggleableState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val primary = MaterialTheme.colorScheme.primary
    val on = state != ToggleableState.Off
    val fill by animateColorAsState(if (on) primary else Color.Transparent, label = "kuroCheckFill")
    val click = if (onClick != null) {
        Modifier.triStateToggleable(state = state, enabled = enabled, role = Role.Checkbox, onClick = onClick)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .then(click)
            // Same footprint as Material's: a full touch target only when it takes taps itself
            .padding(if (onClick != null) 13.dp else 2.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .size(20.dp)
            .clip(MarkShape)
            .background(fill)
            .then(if (on) Modifier else Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, MarkShape)),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            ToggleableState.On -> Icon(
                imageVector = MaterialSymbols.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp),
            )
            ToggleableState.Indeterminate -> Box(
                Modifier
                    .size(width = 10.dp, height = 2.5.dp)
                    .background(MaterialTheme.colorScheme.onPrimary, CircleShape),
            )
            ToggleableState.Off -> Unit
        }
    }
}

/** KuroToon's radio: a ring that gains a solid accent dot when picked. */
@Composable
fun KuroRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val primary = MaterialTheme.colorScheme.primary
    val ring by animateColorAsState(
        if (selected) primary else MaterialTheme.colorScheme.outline,
        label = "kuroRadioRing",
    )
    val dot by animateDpAsState(if (selected) 10.dp else 0.dp, label = "kuroRadioDot")
    val click = if (onClick != null) {
        Modifier.selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .then(click)
            // Same footprint as Material's: a full touch target only when it takes taps itself
            .padding(if (onClick != null) 13.dp else 2.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .size(20.dp)
            .border(2.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(dot).background(primary, CircleShape))
    }
}
