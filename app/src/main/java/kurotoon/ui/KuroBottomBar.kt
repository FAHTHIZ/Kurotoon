package kurotoon.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
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
    val tint by animateColorAsState(
        if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navTint",
    )
    val pill by animateColorAsState(
        if (selected) primary.copy(alpha = 0.14f) else Color.Transparent,
        label = "navPill",
    )

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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(pill)
                    .padding(horizontal = 18.dp, vertical = 4.dp),
            ) {
                Icon(
                    painter = item.icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
                item.badge?.let { badge ->
                    Box(Modifier.align(Alignment.TopEnd).padding(start = 14.dp)) { badge() }
                }
            }
            Text(
                text = item.title,
                color = MaterialTheme.colorScheme.run { if (selected) onSurface else onSurfaceVariant },
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
