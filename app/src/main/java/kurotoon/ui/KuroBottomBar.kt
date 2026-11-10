package kurotoon.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One entry of [KuroBottomBar]. */
data class KuroBottomBarItem(
    val key: Any,
    val title: String,
    val icon: Painter,
    val badge: (@Composable () -> Unit)?,
)

/**
 * KuroToon's bottom navigation: a soft crimson pill behind the active tab with a glowing line on
 * top, matching the web app's nav bar.
 */
@Composable
fun KuroBottomBar(
    items: List<KuroBottomBarItem>,
    selectedKey: Any,
    onClick: (KuroBottomBarItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f)),
    ) {
        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEach { item ->
                KuroBottomBarEntry(
                    item = item,
                    selected = item.key == selectedKey,
                    onClick = { onClick(item) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun KuroBottomBarEntry(
    item: KuroBottomBarItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.88f else 1f, label = "navPress")
    val iconScale by animateFloatAsState(if (selected) 1.12f else 1f, label = "navIcon")
    val tint by animateColorAsState(
        if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navTint",
    )
    val pill by animateColorAsState(if (selected) primary.copy(alpha = 0.1f) else Color.Transparent, label = "navPill")
    val ring by animateColorAsState(if (selected) primary.copy(alpha = 0.25f) else Color.Transparent, label = "navRing")

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .scale(pressScale),
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(16.dp)
        Column(
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .clip(shape)
                .background(pill)
                .border(1.dp, ring, shape)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box {
                Icon(
                    painter = item.icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier
                        .size(22.dp)
                        .scale(iconScale),
                )
                item.badge?.let { badge ->
                    Box(Modifier.align(Alignment.TopEnd).padding(start = 14.dp)) { badge() }
                }
            }
            Text(
                text = item.title,
                color = tint.copy(alpha = if (selected) 1f else 0.75f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(32.dp)
                    .height(2.dp)
                    .dropShadow(RoundedCornerShape(50), Shadow(radius = 12.dp, spread = 2.dp, color = primary))
                    .background(primary, RoundedCornerShape(50)),
            )
        }
    }
}
