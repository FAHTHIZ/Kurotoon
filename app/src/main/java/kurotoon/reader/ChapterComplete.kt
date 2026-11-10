package kurotoon.reader

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kurotoon.ui.KuroColors
import kurotoon.ui.fx.FxEvent
import kurotoon.ui.fx.ParticleField
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private const val RAYS = 10
private const val SHOW_MS = 2_600L

/** What the reader shows when a chapter is finished. */
@Stable
class ChapterCompleteState {
    internal var chapterName by mutableStateOf<String?>(null)
        private set

    /** Bumped on every completion so back-to-back chapters replay the animation. */
    internal var serial by mutableIntStateOf(0)
        private set

    internal val fx = MutableSharedFlow<FxEvent>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    fun show(chapterName: String) {
        this.chapterName = chapterName
        serial++
        fx.tryEmit(FxEvent.Celebrate)
    }

    internal fun dismiss() {
        chapterName = null
    }
}

/**
 * A short celebration when a chapter is finished: confetti, a glowing seal with rays and a card
 * naming the chapter. It never takes touches and hides itself, so reading carries on.
 */
@Composable
fun ChapterCompleteHost(state: ChapterCompleteState, modifier: Modifier = Modifier) {
    val view = LocalView.current
    LaunchedEffect(state.serial) {
        if (state.chapterName == null) return@LaunchedEffect
        val haptic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        view.performHapticFeedback(haptic)
        delay(SHOW_MS)
        state.dismiss()
    }

    Box(modifier) {
        ParticleField(ambient = false, events = state.fx)

        AnimatedVisibility(
            visible = state.chapterName != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 24.dp, end = 24.dp),
            enter = fadeIn(tween(220)) +
                slideInVertically(spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)) { -it / 2 } +
                scaleIn(spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.92f),
            exit = fadeOut(tween(200)) + slideOutVertically(tween(240)) { -it / 3 },
        ) {
            // Remember the last name so the card doesn't empty out while it leaves
            val name = remember(state.serial) { state.chapterName.orEmpty() }
            key(state.serial) {
                ChapterCompleteCard(chapterName = name)
            }
        }
    }
}

@Composable
internal fun ChapterCompleteCard(chapterName: String, modifier: Modifier = Modifier, settled: Boolean = false) {
    Row(
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xF0121214))
            .background(
                Brush.linearGradient(
                    listOf(
                        KuroColors.Crimson.copy(alpha = 0.16f),
                        KuroColors.Galaxy.copy(alpha = 0.10f),
                        Color.Transparent,
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CompletionSeal(settled = settled)
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(MR.strings.kurotoon_reader_chapterComplete).uppercase(),
                color = KuroColors.Gold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Text(
                text = chapterName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A gradient check that pops in, with gold rays and a soft glow flaring out behind it. */
@Composable
private fun CompletionSeal(settled: Boolean) {
    val pop = remember { Animatable(if (settled) 1f else 0.3f) }
    val flare = remember { Animatable(if (settled) 0.45f else 0f) }
    LaunchedEffect(Unit) {
        if (settled) return@LaunchedEffect
        launch { pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)) }
        flare.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }

    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(56.dp)) {
            val t = flare.value
            // Glow that swells and fades
            val glow = (if (t < 0.5f) t / 0.5f else 1f - (t - 0.5f) / 0.5f).coerceIn(0f, 1f)
            drawCircle(
                Brush.radialGradient(
                    listOf(KuroColors.Gold.copy(alpha = 0.5f * (1f - t * 0.6f)), Color.Transparent),
                    radius = size.minDimension / 2 * (0.6f + t * 0.5f),
                ),
                radius = size.minDimension / 2 * (0.6f + t * 0.5f),
            )
            // Rays shoot out and shrink back
            val rayAlpha = glow
            val rayLength = size.minDimension * 0.2f * (0.3f + glow * 0.7f)
            val inner = size.minDimension * 0.36f
            repeat(RAYS) { i ->
                rotate(360f / RAYS * i) {
                    drawLine(
                        brush = Brush.verticalGradient(
                            listOf(KuroColors.Gold.copy(alpha = rayAlpha), Color.Transparent),
                            startY = center.y - inner - rayLength,
                            endY = center.y - inner,
                        ),
                        start = Offset(center.x, center.y - inner),
                        end = Offset(center.x, center.y - inner - rayLength),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                    rotationZ = (1f - pop.value) * -35f
                }
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(KuroColors.Crimson, KuroColors.Galaxy))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
