package kurotoon.ui.shop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kurotoon.rewards.GrantReason
import kurotoon.rewards.RewardGrant
import kurotoon.rewards.RewardsCatalog
import kurotoon.rewards.Seasons
import kurotoon.ui.KuroColors
import kurotoon.ui.fx.KuroFx
import mihon.app.di.appGraph
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Shows earned Kuro Points anywhere in the app with a coin burst, and pays the daily visit
 * bonus once the listener is ready so that one is celebrated too.
 */
@Composable
fun RewardToastHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { context.appGraph.rewardsStore }
    var current by remember { mutableStateOf<RewardGrant?>(null) }
    var visible by remember { mutableStateOf(false) }
    val width = LocalWindowInfo.current.containerSize.width

    LaunchedEffect(store) {
        val queue = kotlinx.coroutines.channels.Channel<RewardGrant>(capacity = 16)
        launch { store.grants.collect { queue.trySend(it) } }
        store.claimDailyOpen()
        for (grant in queue) {
            current = grant
            visible = true
            KuroFx.burst(
                Offset(width / 2f, 160f),
                count = 8,
                power = 3f,
                colors = listOf(KuroColors.Gold, Color(0xFFFFE08A), Color.White),
            )
            delay(2400)
            visible = false
            delay(350)
        }
    }

    Box(modifier.fillMaxWidth().statusBarsPadding().padding(top = 12.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(spring(dampingRatio = 0.6f)) { -it } + fadeIn() + scaleIn(initialScale = 0.8f),
            exit = slideOutVertically { -it } + fadeOut(),
        ) {
            current?.let { GrantPill(it) }
        }
    }
}

@Composable
private fun GrantPill(grant: RewardGrant) {
    val reason = when (val r = grant.reason) {
        is GrantReason.Daily -> if (r.frozen > 0) {
            stringResource(MR.strings.kurotoon_rewards_freezeUsed, r.streak)
        } else {
            stringResource(MR.strings.kurotoon_rewards_dailyReason) +
                " · " + stringResource(MR.strings.kurotoon_rewards_streak, r.streak)
        }
        is GrantReason.MissionDone -> RewardsCatalog.mission(r.code)?.let { stringResource(it.title) }.orEmpty()
        is GrantReason.WeeklyDone -> RewardsCatalog.weeklyMission(r.code)?.let { stringResource(it.title) }.orEmpty()
        is GrantReason.AchievementUnlocked -> RewardsCatalog.achievement(r.code)
            ?.let { stringResource(MR.strings.kurotoon_rewards_unlocked, stringResource(it.title)) }
            .orEmpty()
        is GrantReason.GoalMet -> stringResource(MR.strings.kurotoon_goal_done)
        is GrantReason.StreakChest -> stringResource(MR.strings.kurotoon_rewards_streakChest, r.streak)
        is GrantReason.LevelUp -> stringResource(MR.strings.kurotoon_rewards_levelUp, r.level)
        is GrantReason.SeasonChoreDone -> Seasons.event(r.event)?.let { stringResource(it.title) }.orEmpty()
        is GrantReason.SeasonComplete -> Seasons.event(r.event)
            ?.let { stringResource(MR.strings.kurotoon_rewards_seasonDone, stringResource(it.title)) }
            .orEmpty()
    }
    val art = when (val r = grant.reason) {
        is GrantReason.AchievementUnlocked -> RewardsCatalog.achievement(r.code)?.art
        is GrantReason.StreakChest -> "file:///android_asset/kuro/ui/chest.webp"
        is GrantReason.Daily -> if (r.frozen > 0) "file:///android_asset/kuro/ui/freeze.webp" else null
        else -> null
    }
    Row(
        modifier = Modifier
            .widthIn(max = 360.dp)
            .padding(horizontal = 24.dp)
            .dropShadow(CircleShape, Shadow(radius = 20.dp, color = KuroColors.Gold.copy(alpha = 0.35f)))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
            .border(1.dp, KuroColors.Gold.copy(alpha = 0.5f), CircleShape)
            .padding(start = 8.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (art != null) {
            coil3.compose.AsyncImage(art, null, Modifier.size(36.dp))
        } else {
            KuroCoin(size = 30.dp)
        }
        Column {
            Text(
                "+${grant.points} " + stringResource(MR.strings.kurotoon_rewards_points),
                fontWeight = FontWeight.Bold,
                color = KuroColors.Gold,
                style = MaterialTheme.typography.labelLarge,
            )
            if (reason.isNotBlank()) {
                Text(
                    reason,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
