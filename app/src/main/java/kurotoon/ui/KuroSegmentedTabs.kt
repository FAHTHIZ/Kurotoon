package kurotoon.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * KuroToon's tab switcher: a rounded track with a solid accent thumb that slides under the labels as you swipe
 * between pages. Replaces Material's underlined tab strip, so Browse, Updates and every tabbed screen share one look.
 */
@Composable
fun KuroSegmentedTabs(
    titles: List<String>,
    pagerState: PagerState,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    badges: List<Int?> = emptyList(),
    height: Dp = 44.dp,
) {
    if (titles.isEmpty()) return
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
    ) {
        val segment = maxWidth / titles.size
        // Follows the finger: page plus the fraction already swiped
        val position = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
            .coerceIn(0f, (titles.size - 1).toFloat())
        Box(
            modifier = Modifier
                .offset(x = segment * position)
                .width(segment)
                .fillMaxHeight()
                .shadow(6.dp, CircleShape, ambientColor = MaterialTheme.colorScheme.primary)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight(), horizontalArrangement = Arrangement.Start) {
            titles.forEachIndexed { index, title ->
                val selected = pagerState.currentPage == index
                val color by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "kuroSegmentLabel",
                )
                val interaction = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .width(segment)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(interactionSource = interaction, indication = null) { onSelect(index) }
                        .semantics {
                            role = Role.Tab
                            this.selected = selected
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        color = color,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    val badge = badges.getOrNull(index)
                    if (badge != null && badge > 0) {
                        Text(
                            text = if (badge > 99) "99+" else badge.toString(),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                )
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The same sliding-thumb track for a plain choice, such as the theme mode: [selected] picks the
 * segment and the thumb springs to it.
 */
@Composable
fun KuroSegmentedChoice(
    titles: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
) {
    if (titles.isEmpty()) return
    val position by androidx.compose.animation.core.animateFloatAsState(
        selected.coerceIn(0, titles.lastIndex).toFloat(),
        androidx.compose.animation.core.spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "kuroSegmentedChoice",
    )
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
    ) {
        val segment = maxWidth / titles.size
        if (selected in titles.indices) {
            Box(
                modifier = Modifier
                    .offset(x = segment * position)
                    .width(segment)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            titles.forEachIndexed { index, title ->
                val isSelected = index == selected
                val color by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "kuroSegmentedChoiceLabel",
                )
                Box(
                    modifier = Modifier
                        .width(segment)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(index) }
                        .semantics {
                            role = Role.RadioButton
                            this.selected = isSelected
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = title,
                        color = color,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
