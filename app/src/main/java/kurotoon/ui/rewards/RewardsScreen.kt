package kurotoon.ui.rewards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.icerock.moko.resources.StringResource
import kurotoon.rewards.RewardsCatalog
import kurotoon.rewards.RewardsState
import kurotoon.rewards.ShopItem
import kurotoon.rewards.ShopKind
import kurotoon.ui.KuroAmount
import kurotoon.ui.KuroCoin
import kurotoon.ui.KuroColors
import kurotoon.ui.KuroSectionTitle
import kurotoon.ui.KuroTip
import kurotoon.ui.kuroCard
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.EmojiEvents
import mihon.icons.materialsymbols.rounded.LocalFireDepartment
import mihon.icons.materialsymbols.rounded.Lock
import mihon.icons.materialsymbols.rounded.Pets
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat

enum class RewardsSection(val label: StringResource) {
    MISSIONS(MR.strings.kurotoon_rewards_section_missions),
    SHOP(MR.strings.kurotoon_rewards_section_shop),
    ACHIEVEMENTS(MR.strings.kurotoon_rewards_section_achievements),
    COLLECTION(MR.strings.kurotoon_rewards_section_collection),
}

@Composable
fun RewardsScreen(
    state: RewardsState,
    nowMillis: Long,
    onClaimMission: (String) -> Unit,
    onBuy: (ShopItem) -> Unit,
    initialSection: RewardsSection = RewardsSection.MISSIONS,
) {
    var section by rememberSaveable { mutableStateOf(initialSection) }
    Scaffold { contentPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (section == RewardsSection.COLLECTION) 4 else 2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            fullWidth { PointsHeader(state) }
            fullWidth {
                SectionSwitcher(
                    selected = section,
                    onSelect = { section = it },
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
            when (section) {
                RewardsSection.MISSIONS -> missions(state, nowMillis, onClaimMission)
                RewardsSection.SHOP -> shop(state, onBuy)
                RewardsSection.ACHIEVEMENTS -> achievements(state)
                RewardsSection.COLLECTION -> collection(state)
            }
        }
    }
}

private fun LazyGridScope.fullWidth(key: Any? = null, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

private fun formatPoints(points: Int): String = NumberFormat.getIntegerInstance().format(points)

@Composable
private fun PointsHeader(state: RewardsState) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .kuroCard(shape)
            .clip(shape)
            .background(
                Brush.radialGradient(
                    colors = listOf(KuroColors.Crimson.copy(alpha = 0.18f), Color.Transparent),
                    radius = 700f,
                ),
            )
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Pets,
                        contentDescription = null,
                        tint = KuroColors.Gold,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = stringResource(MR.strings.kurotoon_rewards_points).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KuroCoin(size = 28.dp)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = formatPoints(state.points),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Row(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = KuroColors.Flame,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = stringResource(MR.strings.kurotoon_rewards_streak, state.streak),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(text = if (state.streak > 0) "😸" else "🐱", fontSize = 64.sp)
        }
    }
}

@Composable
private fun SectionSwitcher(
    selected: RewardsSection,
    onSelect: (RewardsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .kuroCard(CircleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RewardsSection.entries.forEach { entry ->
            val active = entry == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(entry) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(entry.label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (active) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun PillButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun LazyGridScope.missions(state: RewardsState, nowMillis: Long, onClaim: (String) -> Unit) {
    val boosted = state.isBoosted(nowMillis)
    fullWidth { KuroSectionTitle(stringResource(MR.strings.kurotoon_rewards_todaysMissions)) }
    if (boosted) {
        fullWidth { KuroTip(stringResource(MR.strings.kurotoon_rewards_boostActive), mood = "😸") }
    }
    RewardsCatalog.missions.forEach { mission ->
        fullWidth(key = mission.code) {
            val progress = state.progress[mission.code] ?: 0
            val done = progress >= mission.target
            val claimed = mission.code in state.claimed
            val fraction by animateFloatAsState((progress.toFloat() / mission.target).coerceIn(0f, 1f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .kuroCard()
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mission.title, fontWeight = FontWeight.Bold)
                        Text(
                            mission.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    KuroAmount("+${if (boosted) mission.reward * 2 else mission.reward}")
                }
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    drawStopIndicator = {},
                )
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$progress/${mission.target}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    PillButton(
                        text = when {
                            claimed -> stringResource(MR.strings.kurotoon_rewards_claimed)
                            done -> stringResource(MR.strings.kurotoon_rewards_claim)
                            else -> stringResource(MR.strings.kurotoon_rewards_inProgress)
                        },
                        enabled = done && !claimed,
                        onClick = { onClaim(mission.code) },
                    )
                }
            }
        }
    }
    fullWidth {
        Text(
            text = stringResource(MR.strings.kurotoon_rewards_missionsFooter),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

private fun LazyGridScope.shop(state: RewardsState, onBuy: (ShopItem) -> Unit) {
    fullWidth { KuroTip(stringResource(MR.strings.kurotoon_rewards_shopTip)) }
    ShopKind.entries.forEach { kind ->
        val items = RewardsCatalog.shop.filter { it.kind == kind }
        if (items.isEmpty()) return@forEach
        fullWidth(key = "shop_header_$kind") {
            KuroSectionTitle(stringResource(kind.title()), modifier = Modifier.padding(top = 8.dp))
        }
        items(items, key = { it.code }) { item ->
            ShopCard(
                item = item,
                owned = item.kind != ShopKind.BOOST && item.code in state.owned,
                affordable = state.points >= item.price,
                onBuy = { onBuy(item) },
            )
        }
    }
}

private fun ShopKind.title(): StringResource = when (this) {
    ShopKind.STICKER -> MR.strings.kurotoon_rewards_kind_sticker
    ShopKind.AVATAR -> MR.strings.kurotoon_rewards_kind_avatar
    ShopKind.FRAME -> MR.strings.kurotoon_rewards_kind_frame
    ShopKind.THEME -> MR.strings.kurotoon_rewards_kind_theme
    ShopKind.BADGE -> MR.strings.kurotoon_rewards_kind_badge
    ShopKind.BOOST -> MR.strings.kurotoon_rewards_kind_boost
}

@Composable
private fun ShopCard(item: ShopItem, owned: Boolean, affordable: Boolean, onBuy: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .kuroCard(borderColor = KuroColors.rarity(item.rarity))
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = item.emoji.orEmpty(), fontSize = 34.sp)
        }
        Text(
            text = item.name,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = item.rarity.name,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = item.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.price == 0) {
                Text(
                    stringResource(MR.strings.kurotoon_rewards_free),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                KuroCoin(size = 14.dp)
                Spacer(Modifier.size(4.dp))
                Text(
                    formatPoints(item.price),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.weight(1f))
            PillButton(
                text = stringResource(
                    if (owned) MR.strings.kurotoon_rewards_owned else MR.strings.kurotoon_rewards_unlock,
                ),
                enabled = !owned && affordable,
                onClick = onBuy,
            )
        }
    }
}

private fun LazyGridScope.achievements(state: RewardsState) {
    items(RewardsCatalog.achievements, key = { it.code }) { achievement ->
        val unlocked = achievement.code in state.achievements
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .kuroCard(borderColor = if (unlocked) KuroColors.Gold.copy(alpha = 0.45f) else Color.White.copy(0.06f))
                .alpha(if (unlocked) 1f else 0.7f)
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (unlocked) {
                            MaterialSymbols.Rounded.EmojiEvents
                        } else {
                            MaterialSymbols.Rounded.Lock
                        },
                        contentDescription = null,
                        tint = if (unlocked) KuroColors.Gold else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(if (unlocked) 18.dp else 16.dp),
                    )
                }
                Spacer(Modifier.size(8.dp))
                Text(achievement.icon, fontSize = 20.sp)
            }
            Text(achievement.title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Text(
                achievement.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2,
            )
            KuroAmount("+${achievement.reward}", modifier = Modifier.padding(top = 6.dp))
        }
    }
}

private fun LazyGridScope.collection(state: RewardsState) {
    val owned = RewardsCatalog.shop.filter { it.code in state.owned }
    val stickers = owned.filter { it.kind == ShopKind.STICKER }
    val others = owned.filter { it.kind != ShopKind.STICKER }
    val totalStickers = RewardsCatalog.shop.count { it.kind == ShopKind.STICKER }

    fullWidth {
        KuroSectionTitle(stringResource(MR.strings.kurotoon_rewards_stickerCollection, stickers.size, totalStickers))
    }
    items(stickers, key = { it.code }) { sticker ->
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                .border(1.dp, KuroColors.rarity(sticker.rarity), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(sticker.emoji.orEmpty(), fontSize = 28.sp)
        }
    }
    fullWidth {
        KuroSectionTitle(
            stringResource(MR.strings.kurotoon_rewards_unlockedItems),
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    if (others.isEmpty()) {
        fullWidth { KuroTip(stringResource(MR.strings.kurotoon_rewards_unlockedEmpty)) }
    } else {
        others.forEach { item ->
            fullWidth(key = "owned_${item.code}") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .kuroCard(borderColor = KuroColors.rarity(item.rarity))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(KuroColors.Crimson.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(item.emoji.orEmpty(), fontSize = 18.sp)
                    }
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text(item.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${item.rarity.name} · ${item.kind.name}",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
    fullWidth {
        Text(
            text = stringResource(MR.strings.kurotoon_rewards_collectionFooter),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
    }
}
