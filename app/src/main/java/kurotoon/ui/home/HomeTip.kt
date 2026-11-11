package kurotoon.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kurotoon.ui.KuroColors
import kurotoon.ui.kuroCard
import mihon.app.di.appGraph
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/** KuroToon's own features, one per tip, so they get found without reading the README. */
private val tips = listOf(
    MR.strings.kurotoon_tip_panels,
    MR.strings.kurotoon_tip_sound,
    MR.strings.kurotoon_tip_bgm,
    MR.strings.kurotoon_tip_data_saver,
    MR.strings.kurotoon_tip_saved_search,
    MR.strings.kurotoon_tip_schedule,
    MR.strings.kurotoon_tip_tags,
    MR.strings.kurotoon_tip_edit_info,
)

/**
 * A small card on Home where Kuro points out one KuroToon-only feature at a time. Next tip moves on
 * (and the next launch starts from there), Hide tips switches the card off for good.
 */
@Composable
fun HomeTip(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.appGraph.kuroPreferences }
    val hidden by prefs.homeTipsHidden.collectAsState()
    val index by prefs.homeTipIndex.collectAsState()
    if (hidden) return

    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(shape)
            .kuroCard(shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(MR.strings.kurotoon_tip_label).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AnimatedContent(
                targetState = index.mod(tips.size),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "homeTip",
            ) { i ->
                Text(text = stringResource(tips[i]), style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TipAction(
                    text = stringResource(MR.strings.kurotoon_tip_next),
                    highlighted = true,
                    onClick = { prefs.homeTipIndex.set((index + 1).mod(tips.size)) },
                )
                TipAction(
                    text = stringResource(MR.strings.kurotoon_tip_hide),
                    highlighted = false,
                    onClick = { prefs.homeTipsHidden.set(true) },
                )
            }
        }
    }
}

@Composable
private fun TipAction(text: String, highlighted: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = if (highlighted) colors.onPrimary else colors.onSurfaceVariant,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (highlighted) colors.primary else colors.surfaceContainerHighest)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}
