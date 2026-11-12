package kurotoon.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Short reader messages ("Crop borders on", "Long strip", "Saved") as a glass capsule that drops in
 * at the top of the page and leaves on its own, instead of the system toast at the bottom that sat
 * over the page counter and looked like any other app.
 */
@Stable
class ReaderHintState {
    internal var current by mutableStateOf<Hint?>(null)
        private set
    private var serial = 0L

    fun show(text: String?) {
        if (text.isNullOrBlank()) return
        current = Hint(text, serial++)
    }

    fun hide() {
        current = null
    }

    internal fun expire(hint: Hint) {
        if (current == hint) current = null
    }

    internal data class Hint(val text: String, val id: Long)
}

@Composable
fun ReaderHintHost(state: ReaderHintState, modifier: Modifier = Modifier) {
    val hint = state.current
    var last by remember { mutableStateOf("") }
    if (hint != null) last = hint.text
    LaunchedEffect(hint) {
        if (hint == null) return@LaunchedEffect
        delay(HINT_MILLIS + hint.text.length * PER_CHAR_MILLIS)
        state.expire(hint)
    }
    AnimatedVisibility(
        visible = hint != null,
        modifier = modifier,
        enter = fadeIn() + slideInVertically(spring(dampingRatio = 0.7f)) { -it } + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + slideOutVertically { -it / 2 } + scaleOut(targetScale = 0.95f),
    ) {
        Text(
            text = last,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.72f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                .padding(horizontal = 18.dp, vertical = 10.dp),
        )
    }
}

private const val HINT_MILLIS = 1600L
private const val PER_CHAR_MILLIS = 25L
