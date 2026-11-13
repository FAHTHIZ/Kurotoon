package kurotoon.reader

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kurotoon.icons.materialsymbols.MaterialSymbols
import kurotoon.icons.materialsymbols.rounded.Check
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private const val SHOW_MS = 2_600L

/** What the reader shows when a chapter is finished. */
@Stable
class ChapterCompleteState {
    internal var chapterName by mutableStateOf<String?>(null)
        private set

    /** Bumped on every completion so back-to-back chapters replay the animation. */
    internal var serial by mutableIntStateOf(0)
        private set

    fun show(chapterName: String) {
        this.chapterName = chapterName
        serial++
    }

    internal fun dismiss() {
        chapterName = null
    }
}

/**
 * A small card when a chapter is finished, naming the chapter with a check. It never takes touches and hides itself, so reading carries on.
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
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xF0161618))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CompletionSeal(settled = settled)
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(MR.strings.kurotoon_reader_chapterComplete),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = chapterName,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A check that pops in once. */
@Composable
private fun CompletionSeal(settled: Boolean) {
    val pop = remember { Animatable(if (settled) 1f else 0.6f) }
    LaunchedEffect(Unit) {
        if (!settled) pop.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium))
    }
    Box(
        modifier = Modifier
            .size(32.dp)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MaterialSymbols.Rounded.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}
