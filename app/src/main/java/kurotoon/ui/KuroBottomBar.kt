package kurotoon.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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

/** One entry of [KuroBottomBar]. */
data class KuroBottomBarItem(
    val key: Any,
    val title: String,
    val icon: Painter,
    val badge: (@Composable () -> Unit)?,
)

/**
 * KuroToon's bottom navigation: a floating rounded dock. The open tab's icon sits in a solid accent pill that
 * grows in with a spring; the others stay quiet so the artwork above keeps the attention.
 */
@Composable
fun KuroBottomBar(
    items: List<KuroBottomBarItem>,
    selectedKey: Any,
    onClick: (KuroBottomBarItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dockShape = RoundedCornerShape(26.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(dockShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, Color.White.copy(alpha = 0.05f), dockShape)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
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
    val pressScale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "navPress")
    val tint by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navTint",
    )
    val pill by animateColorAsState(
        if (selected) primary else Color.Transparent,
        label = "navPill",
    )
    val pillWidth by animateDpAsState(
        targetValue = if (selected) 52.dp else 36.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "navPillWidth",
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
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
                    .width(pillWidth)
                    .height(30.dp)
                    .clip(RoundedCornerShape(50))
                    .background(pill),
            ) {
                Icon(
                    painter = item.icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(21.dp),
                )
                item.badge?.let { badge ->
                    Box(Modifier.align(Alignment.TopEnd).padding(top = 1.dp, end = 4.dp)) { badge() }
                }
            }
            Text(
                text = item.title,
                color = MaterialTheme.colorScheme.run { if (selected) onSurface else onSurfaceVariant },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}
