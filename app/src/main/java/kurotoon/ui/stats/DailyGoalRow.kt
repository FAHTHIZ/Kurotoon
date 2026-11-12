package kurotoon.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kurotoon.ui.KuroAlertDialog
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.KuroRadioButton
import tachiyomi.presentation.core.i18n.stringResource

/** Today's chapters against the daily goal, as a ring. Tap to change or set the goal. */
@Composable
fun DailyGoalRow(today: Int, goal: Int, onGoalChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    var picking by rememberSaveable { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { picking = true }
            .padding(vertical = 4.dp),
    ) {
        val target = if (goal > 0) (today.toFloat() / goal).coerceIn(0f, 1f) else 0f
        val fraction by animateFloatAsState(target, spring(dampingRatio = 0.8f, stiffness = 120f), label = "goal")
        GoalRing(fraction = fraction, label = if (goal > 0) "$today" else "–")
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (goal > 0) {
                Text(
                    text = stringResource(MR.strings.kurotoon_goal_progress, today, goal),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (today >= goal) {
                        stringResource(MR.strings.kurotoon_goal_done)
                    } else {
                        stringResource(MR.strings.kurotoon_goal_left, goal - today)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.run { if (today >= goal) primary else onSurfaceVariant },
                )
            } else {
                Text(
                    text = stringResource(MR.strings.kurotoon_goal_set),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(MR.strings.kurotoon_goal_setSummary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (picking) {
        GoalPicker(current = goal, onDismiss = { picking = false }, onPick = {
            onGoalChange(it)
            picking = false
        })
    }
}

@Composable
private fun GoalRing(fraction: Float, label: String) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val accent = MaterialTheme.colorScheme.primary
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
        Canvas(modifier = Modifier.size(52.dp)) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            if (fraction > 0f) {
                drawArc(
                    accent,
                    -90f,
                    360f * fraction,
                    false,
                    Offset(inset, inset),
                    arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

private val GOAL_OPTIONS = listOf(0, 1, 2, 3, 5, 10, 20)

@Composable
private fun GoalPicker(current: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    var selected by remember { mutableStateOf(current) }
    KuroAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(MR.strings.kurotoon_goal_title)) },
        text = {
            Column {
                GOAL_OPTIONS.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .selectable(selected = selected == option, onClick = { selected = option })
                            .padding(vertical = 2.dp),
                    ) {
                        KuroRadioButton(selected = selected == option, onClick = { selected = option })
                        Text(
                            text = if (option == 0) {
                                stringResource(MR.strings.kurotoon_goal_off)
                            } else {
                                stringResource(MR.strings.kurotoon_goal_option, option)
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(selected) }) { Text(stringResource(MR.strings.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(MR.strings.action_cancel)) }
        },
    )
}
