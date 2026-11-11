package kurotoon.ui.shop

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.KuroArt
import tachiyomi.presentation.core.components.KuroPose

/** What Kuro is up to; each maps to one of his official expressions. */
enum class KuroMood(val pose: KuroPose) {
    IDLE(KuroPose.NEUTRAL),
    HAPPY(KuroPose.HAPPY),
    READING(KuroPose.READING),
    SLEEPY(KuroPose.SLEEPY),
    HYPED(KuroPose.EXCITED),
    THINKING(KuroPose.THINKING),
    SAD(KuroPose.SAD),
    WAVING(KuroPose.WAVING),
}

/**
 * Kuro, KuroToon's mascot, from his official artwork. [colors] and [animated] are accepted for older call sites and
 * ignored: the artwork is the one look he has.
 */
@Composable
fun KuroMascot(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    mood: KuroMood = KuroMood.IDLE,
    @Suppress("UNUSED_PARAMETER") colors: List<Color> = emptyList(),
    @Suppress("UNUSED_PARAMETER") animated: Boolean = true,
) {
    KuroArt(pose = mood.pose, size = size, modifier = modifier)
}
