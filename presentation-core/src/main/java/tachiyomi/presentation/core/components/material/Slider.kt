package tachiyomi.presentation.core.components.material

import androidx.annotation.IntRange
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun Slider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: IntProgression = 0..1,
    @IntRange(from = 0) steps: Int = with(valueRange) { (last - first) - 1 },
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = kuroSliderColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    thumb: @Composable (SliderState) -> Unit = {
        KuroSliderThumb(interactionSource = interactionSource, enabled = enabled)
    },
    track: @Composable (SliderState) -> Unit = { sliderState ->
        SliderDefaults.Track(
            sliderState = sliderState,
            modifier = Modifier.height(6.dp),
            enabled = enabled,
            colors = colors,
            drawStopIndicator = null,
            drawTick = { _, _ -> },
            thumbTrackGapSize = 0.dp,
        )
    },
) {
    val state = key(steps, valueRange) {
        rememberSliderState(
            value = value.toFloat(),
            steps = steps,
            trackRange = with(valueRange) { first.toFloat()..last.toFloat() },
        )
    }
    state.value = value.toFloat()
    Slider(
        state = state,
        modifier = modifier,
        enabled = enabled,
        onValueChange = { onValueChange(it.roundToInt()) },
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = interactionSource,
        thumb = thumb,
        track = track,
    )
}

/** KuroToon slider colours: an accent fill on a faint track, with no step dots. */
@Composable
fun kuroSliderColors(): SliderColors = SliderDefaults.colors(
    thumbColor = MaterialTheme.colorScheme.primary,
    activeTrackColor = MaterialTheme.colorScheme.primary,
    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    activeTickColor = Color.Transparent,
    inactiveTickColor = Color.Transparent,
)

/** A round accent thumb with a light ring that grows while you hold it. */
@Composable
fun KuroSliderThumb(interactionSource: MutableInteractionSource, enabled: Boolean = true) {
    val pressed by interactionSource.collectIsPressedAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    val size by animateDpAsState(if (pressed || dragged) 24.dp else 18.dp, label = "kuroSliderThumb")
    val color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .shadow(3.dp, CircleShape)
                .background(color, CircleShape)
                .border(3.dp, Color.White.copy(alpha = 0.92f), CircleShape),
        )
    }
}
