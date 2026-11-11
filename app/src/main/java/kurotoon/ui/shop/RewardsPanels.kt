package kurotoon.ui.shop

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kurotoon.rewards.Achievement
import kurotoon.rewards.Chore
import kurotoon.rewards.RewardsCatalog
import kurotoon.rewards.RewardsState
import kurotoon.rewards.SeasonRun
import kurotoon.shop.ShopItem
import kurotoon.ui.KuroColors
import kurotoon.ui.kuroCard
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.LocalFireDepartment
import mihon.icons.materialsymbols.rounded.Lock
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private const val UI_ASSETS = "file:///android_asset/kuro/ui"

/** Level, title and an XP bar, shown under the wallet. */
@Composable
internal fun LevelBar(state: RewardsState, modifier: Modifier = Modifier) {
    val level = state.level
    val fraction by animateFloatAsState(level.fraction, tween(900), label = "xp")
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(MR.strings.kurotoon_rewards_level, level.level),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(KuroColors.Galaxy, KuroColors.Crimson)))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Text(
                stringResource(level.title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(
                    MR.strings.kurotoon_rewards_levelProgress,
                    formatNumber(level.into),
                    formatNumber(level.needed),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(listOf(KuroColors.Galaxy, KuroColors.Crimson, KuroColors.Gold)),
                    ),
            )
        }
    }
}

/** This week's check-ins, the next streak chest and banked freezes. */
@Composable
internal fun StreakTracker(state: RewardsState, today: LocalDate, daysToChest: Int) {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val last = state.lastDay?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    // Days covered by the current streak, counting back from the last visit
    val streakStart = last?.minusDays((state.streak - 1L).coerceAtLeast(0))
    val glow = rememberInfiniteTransition(label = "chest")
        .animateFloat(0.9f, 1.08f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "chestScale")
    Column(
        Modifier
            .fillMaxWidth()
            .kuroCard(RoundedCornerShape(22.dp), KuroColors.Flame.copy(alpha = 0.25f))
            .background(
                Brush.linearGradient(listOf(KuroColors.Flame.copy(alpha = 0.10f), Color.Transparent)),
                RoundedCornerShape(22.dp),
            )
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                MaterialSymbols.Rounded.LocalFireDepartment,
                null,
                tint = KuroColors.Flame,
                modifier = Modifier.size(20.dp),
            )
            Text(
                stringResource(MR.strings.kurotoon_rewards_streak, state.streak),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .weight(1f),
            )
            if (state.freezes > 0) {
                AsyncImage("$UI_ASSETS/freeze.webp", null, Modifier.size(20.dp))
                Text(
                    stringResource(MR.strings.kurotoon_rewards_freezes, state.freezes),
                    style = MaterialTheme.typography.labelSmall,
                    color = KuroColors.Aurora,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            (0 until 7).forEach { offset ->
                val day = monday.plusDays(offset.toLong())
                val lit = last != null && streakStart != null && !day.isBefore(streakStart) && !day.isAfter(last)
                val isToday = day == today
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (lit) {
                                    Brush.radialGradient(listOf(KuroColors.Gold, KuroColors.Flame, KuroColors.Crimson))
                                } else {
                                    Brush.radialGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surfaceContainerHighest,
                                            MaterialTheme.colorScheme.surfaceContainerHigh,
                                        ),
                                    )
                                },
                            )
                            .border(
                                if (isToday) 2.dp else 0.dp,
                                if (isToday) KuroColors.Gold else Color.Transparent,
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (lit) {
                            Icon(
                                MaterialSymbols.Rounded.LocalFireDepartment,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Text(
                        day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) KuroColors.Gold else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            AsyncImage(
                "$UI_ASSETS/chest.webp",
                null,
                Modifier
                    .size(36.dp)
                    .graphicsLayer {
                        val s = if (daysToChest == 0) glow.value else 1f
                        scaleX = s
                        scaleY = s
                    },
            )
            Text(
                if (daysToChest == 0) {
                    stringResource(MR.strings.kurotoon_rewards_chestToday)
                } else {
                    stringResource(MR.strings.kurotoon_rewards_nextChest, daysToChest)
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (daysToChest == 0) KuroColors.Gold else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}

/** A running event: banner art, countdown, chores and the item it gifts. */
@Composable
internal fun SeasonCard(run: SeasonRun, state: RewardsState, reward: ShopItem?, today: LocalDate) {
    val done = run.key in state.seasonsDone
    val shape = RoundedCornerShape(24.dp)
    val left = ChronoUnit.DAYS.between(today, run.end) + 1
    Column(
        Modifier
            .fillMaxWidth()
            .kuroCard(shape, KuroColors.Galaxy.copy(alpha = 0.45f))
            .background(
                Brush.linearGradient(
                    listOf(
                        KuroColors.Galaxy.copy(alpha = 0.28f),
                        KuroColors.Crimson.copy(alpha = 0.10f),
                        Color.Transparent,
                    ),
                ),
                shape,
            )
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(MR.strings.kurotoon_rewards_seasonal).uppercase(),
                    color = KuroColors.Gold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                )
                Text(
                    stringResource(run.event.title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    stringResource(
                        MR.strings.kurotoon_rewards_endsIn,
                        stringResource(MR.strings.kurotoon_rewards_daysShort, left.toInt()),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (reward?.image != null) {
                AsyncImage(
                    reward.image,
                    reward.name,
                    Modifier.size(96.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        run.event.chores.forEach { chore ->
            ChoreRow(
                chore,
                state.seasonProgress[run.choreKey(chore)] ?: 0,
                run.choreKey(chore) in state.seasonClaimed,
            )
        }
        Text(
            if (done) {
                stringResource(MR.strings.kurotoon_rewards_seasonDone, reward?.name.orEmpty())
            } else {
                stringResource(MR.strings.kurotoon_rewards_seasonReward, reward?.name.orEmpty())
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (done) KuroColors.Gold else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun ChoreRow(chore: Chore, progress: Int, done: Boolean) {
    val fraction by animateFloatAsState((progress.toFloat() / chore.target).coerceIn(0f, 1f), label = "chore")
    Column(
        Modifier
            .padding(top = 10.dp)
            .alpha(if (done) 0.65f else 1f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(chore.kind.title, chore.target),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (done) {
                Text("✓", color = KuroColors.Gold, fontWeight = FontWeight.Black)
            } else {
                KuroAmount("+${chore.reward}")
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            LinearProgressIndicator(
                progress = { fraction },
                color = if (done) KuroColors.Gold else KuroColors.Galaxy,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape),
                drawStopIndicator = {},
            )
            Text(
                "${progress.coerceAtMost(chore.target)}/${chore.target}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** Upcoming events with their gift artwork, so readers know what's next. */
@Composable
internal fun UpcomingRow(run: SeasonRun, reward: ShopItem?, today: LocalDate) {
    val days = ChronoUnit.DAYS.between(today, run.start).toInt()
    Row(
        Modifier
            .fillMaxWidth()
            .kuroCard(RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (reward?.image != null) {
                AsyncImage(reward.image, null, Modifier.fillMaxSize().padding(4.dp).alpha(0.85f))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(run.event.title), fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(
                    MR.strings.kurotoon_rewards_startsIn,
                    stringResource(MR.strings.kurotoon_rewards_daysShort, days),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            MaterialSymbols.Rounded.Lock,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** A four-week heatmap of chapters read per day. */
@Composable
internal fun ReadingTracker(state: RewardsState, today: LocalDate) {
    val weeks = 4
    val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(weeks - 1L)
    val days = remember(state.history, today) {
        (0 until weeks * 7).map { start.plusDays(it.toLong()) }
    }
    val counts = days.map { if (it.isAfter(today)) -1 else state.history[it.toString()] ?: 0 }
    val total = counts.filter { it > 0 }.sum()
    val active = counts.count { it > 0 }
    val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    Column(
        Modifier
            .fillMaxWidth()
            .kuroCard(RoundedCornerShape(22.dp))
            .padding(14.dp),
    ) {
        Text(stringResource(MR.strings.kurotoon_rewards_tracker), fontWeight = FontWeight.Bold)
        Text(
            stringResource(MR.strings.kurotoon_rewards_trackerSummary, total, active),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            (0 until weeks).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    (0 until 7).forEach { d ->
                        val count = counts[week * 7 + d]
                        val color = when {
                            count < 0 -> Color.Transparent
                            count == 0 -> MaterialTheme.colorScheme.surfaceContainerHighest
                            else -> androidx.compose.ui.graphics.lerp(
                                KuroColors.Galaxy.copy(alpha = 0.45f),
                                KuroColors.Crimson,
                                (count.toFloat() / max).coerceIn(0f, 1f),
                            )
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(color)
                                .border(
                                    if (days[week * 7 + d] == today) 1.5.dp else 0.dp,
                                    if (days[week * 7 + d] == today) KuroColors.Gold else Color.Transparent,
                                    RoundedCornerShape(6.dp),
                                ),
                        )
                    }
                }
            }
        }
    }
}

/** Medal artwork for an achievement; greyed out until it's unlocked. */
@Composable
internal fun AchievementMedal(achievement: Achievement, unlocked: Boolean, size: Dp = 52.dp) {
    val grey = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    AsyncImage(
        achievement.art,
        null,
        Modifier
            .size(size)
            .alpha(if (unlocked) 1f else 0.45f),
        colorFilter = if (unlocked) null else grey,
    )
}

/** How many achievements are unlocked, as a fraction. */
internal fun achievementProgress(state: RewardsState): Float =
    RewardsCatalog.achievements.count { it.code in state.achievements }.toFloat() / RewardsCatalog.achievements.size
