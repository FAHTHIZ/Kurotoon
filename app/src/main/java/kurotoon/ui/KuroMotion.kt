package kurotoon.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * KuroToon's motion kit: small, physical feedback so the app feels alive without getting in the way.
 * Everything here is springs and short tweens; nothing loops except the loading shimmer.
 */
object KuroMotion {
    /** How far a pressed card sinks. */
    const val PRESS_SCALE = 0.95f

    /** Delay between neighbouring items of a staggered entrance. */
    const val STAGGER_MS = 45L

    /** Items past this index appear together, so long lists don't trickle in. */
    const val STAGGER_LIMIT = 8

    val pressSpring = spring<Float>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)
}

/** Scales the element down a touch while [interaction] is pressed, then springs back with a little overshoot. */
fun Modifier.kuroPressScale(interaction: MutableInteractionSource, pressed: Float = KuroMotion.PRESS_SCALE): Modifier =
    composed {
        val isPressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) pressed else 1f,
            animationSpec = KuroMotion.pressSpring,
            label = "kuroPressScale",
        )
        graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    }

/**
 * A clickable that squishes on press and gives a light haptic tick on long press. Use it for covers,
 * cards and tiles; buttons already get this from [KuroPrimaryButton] and friends.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.kuroBounceClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    role: Role? = Role.Button,
    pressed: Float = KuroMotion.PRESS_SCALE,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    this
        .kuroPressScale(interaction, pressed)
        .combinedClickable(
            interactionSource = interaction,
            indication = androidx.compose.foundation.LocalIndication.current,
            role = role,
            onClick = onClick,
            onLongClick = onLongClick?.let {
                {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    it()
                }
            },
        )
}

/**
 * Fades and lifts the element in the first time it is composed, delayed by its [index] so a row or grid
 * cascades in. Previews and screenshot tests skip the animation and draw the final state.
 */
fun Modifier.kuroEntrance(index: Int = 0, lift: Float = 18f): Modifier = composed {
    // Lazy lists keep saveable state per item key, so an item that scrolls away and back doesn't replay
    var played by rememberSaveable { mutableStateOf(false) }
    if (LocalInspectionMode.current || played || index > KuroMotion.STAGGER_LIMIT) return@composed this
    val alpha = remember { Animatable(0f) }
    val offset = remember { Animatable(lift) }
    LaunchedEffect(Unit) {
        delay(KuroMotion.STAGGER_MS * index)
        launch { alpha.animateTo(1f, tween(durationMillis = 260, easing = FastOutSlowInEasing)) }
        offset.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow))
        played = true
    }
    graphicsLayer {
        this.alpha = alpha.value
        translationY = offset.value * density
    }
}

/**
 * A pop for state changes: when [trigger] flips to true the element overshoots to [peak] and settles,
 * with a small tilt. Used for the bottom bar icon and library hearts.
 */
fun Modifier.kuroPopOn(trigger: Boolean, peak: Float = 1.18f, tilt: Float = -8f): Modifier = composed {
    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(trigger) {
        if (first[0]) {
            first[0] = false
            return@LaunchedEffect
        }
        if (!trigger) return@LaunchedEffect
        launch {
            rotation.snapTo(tilt)
            rotation.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMediumLow))
        }
        scale.snapTo(0.82f)
        scale.animateTo(peak, tween(110))
        scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
    }
    graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
        rotationZ = rotation.value
    }
}

/** A soft diagonal sweep for loading placeholders. */
fun Modifier.kuroShimmer(shape: Shape, base: Color, highlight: Color = Color.White.copy(alpha = 0.07f)): Modifier =
    composed {
        if (LocalInspectionMode.current) return@composed background(base, shape)
        val transition = rememberInfiniteTransition(label = "kuroShimmer")
        val progress by transition.animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
            label = "kuroShimmerProgress",
        )
        this
            .background(base, shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, highlight, Color.Transparent),
                    start = Offset(progress * 600f - 300f, 0f),
                    end = Offset(progress * 600f, 300f),
                ),
                shape = shape,
            )
    }

/** Counts up from zero to [target] the first time it shows, then eases between later values. */
@Composable
fun animatedCount(target: Long, durationMillis: Int = 700): Long {
    if (LocalInspectionMode.current) return target
    val value = remember { Animatable(0f) }
    LaunchedEffect(target) {
        value.animateTo(target.toFloat(), tween(durationMillis, easing = FastOutSlowInEasing))
    }
    return value.value.toLong()
}
