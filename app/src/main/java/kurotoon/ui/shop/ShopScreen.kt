package kurotoon.ui.shop

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import kurotoon.rewards.Achievement
import kurotoon.rewards.Mission
import kurotoon.rewards.PurchaseResult
import kurotoon.rewards.RewardEvent
import kurotoon.rewards.RewardsCatalog
import kurotoon.rewards.RewardsEngine
import kurotoon.rewards.RewardsState
import kurotoon.rewards.Seasons
import kurotoon.shop.Pack
import kurotoon.shop.PackOrigin
import kurotoon.shop.PackRepository.ImportResult
import kurotoon.shop.ShopItem
import kurotoon.shop.ShopKind
import kurotoon.ui.KuroColors
import kurotoon.ui.KuroSectionTitle
import kurotoon.ui.fx.KuroFx
import kurotoon.ui.kuroCard
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Delete
import mihon.icons.materialsymbols.rounded.LocalFireDepartment
import mihon.icons.materialsymbols.rounded.Lock
import mihon.icons.materialsymbols.rounded.UploadFile
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters

enum class ShopSection(val label: StringResource) {
    MISSIONS(MR.strings.kurotoon_rewards_section_missions),
    EVENTS(MR.strings.kurotoon_rewards_section_events),
    SHOP(MR.strings.kurotoon_rewards_section_shop),
    ACHIEVEMENTS(MR.strings.kurotoon_rewards_section_achievements),
    COLLECTION(MR.strings.kurotoon_rewards_section_collection),
    PACKS(MR.strings.kurotoon_rewards_section_packs),
}

/** Kuro Points, missions, the shop and packs. Everything is earned by reading and stays on the device. */
class KuroShopScreen(private val initial: ShopSection = ShopSection.SHOP) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val store = remember { context.appGraph.rewardsStore }
        val packs by store.packs.packs.collectAsState()
        val state by store.state.collectAsState(initial = store.current)
        val scope = rememberCoroutineScope()

        val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                when (val result = store.packs.import(uri)) {
                    is ImportResult.Success -> {
                        context.toast(context.stringResource(MR.strings.kurotoon_packs_imported, result.pack.name))
                        store.track(RewardEvent.PACK_IMPORTED)
                        KuroFx.celebrate()
                    }
                    is ImportResult.Failure -> context.toast(result.reason.label)
                }
            }
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.kurotoon_shop_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                    actions = { PointsPill(state.points, Modifier.padding(end = 12.dp)) },
                )
            },
        ) { padding ->
            ShopContent(
                state = state,
                packs = packs.orEmpty(),
                contentPadding = padding,
                initialSection = initial,
                onClaim = { store.claimMission(it) },
                onClaimAll = { store.claimAll() },
                onClaimWeekly = { store.claimWeekly(it) },
                onBuy = { item ->
                    val result = store.buy(item.code)
                    when (result) {
                        PurchaseResult.Success -> KuroFx.celebrate()
                        PurchaseResult.NotEnoughPoints -> context.toast(MR.strings.kurotoon_rewards_notEnoughPoints)
                        PurchaseResult.FreezesFull -> context.toast(MR.strings.kurotoon_rewards_freezeFull)
                        PurchaseResult.OutOfSeason -> context.toast(
                            context.stringResource(
                                MR.strings.kurotoon_rewards_outOfSeason,
                                Seasons.eventsFor(item.code).joinToString { context.stringResource(it.title) },
                            ),
                        )
                        else -> Unit
                    }
                    result
                },
                onEquip = { store.equip(it.code) },
                onUnequip = { store.unequip(it.code) },
                onImport = { importer.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                onRemovePack = { pack ->
                    if (store.packs.remove(pack.id)) context.toast(MR.strings.kurotoon_packs_removed)
                },
            )
        }
    }
}

@Composable
fun PointsPill(points: Int, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val shown by animateIntAsState(points, tween(700), label = "points")
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(KuroColors.Gold.copy(alpha = 0.14f))
            .border(1.dp, KuroColors.Gold.copy(alpha = 0.35f), CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        KuroCoin(size = 18.dp)
        Text(formatNumber(shown), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopContent(
    state: RewardsState,
    packs: List<Pack>,
    contentPadding: PaddingValues,
    onClaim: (String) -> Unit,
    onClaimAll: () -> Unit,
    onBuy: (ShopItem) -> PurchaseResult,
    onEquip: (ShopItem) -> Unit,
    onUnequip: (ShopItem) -> Unit,
    onImport: () -> Unit,
    onRemovePack: (Pack) -> Unit,
    initialSection: ShopSection = ShopSection.SHOP,
    onClaimWeekly: (String) -> Unit = {},
    nowMillis: Long = System.currentTimeMillis(),
    animated: Boolean = true,
) {
    var section by rememberSaveable { mutableStateOf(initialSection) }
    var kind by rememberSaveable { mutableStateOf<ShopKind?>(null) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var removing by remember { mutableStateOf<Pack?>(null) }
    val itemsByCode = remember(packs) { packs.flatMap { it.items }.associateBy { it.code } }
    val today = remember(nowMillis) { Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalDate() }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(104.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        full("wallet") {
            fun worn(kind: ShopKind) = state.equipped[kind.key]?.let(itemsByCode::get)?.takeIf(state::owns)
            WalletCard(state, nowMillis, animated, worn(ShopKind.AVATAR), worn(ShopKind.FRAME))
        }
        full("sections") { SectionPills(section) { section = it } }
        when (section) {
            ShopSection.MISSIONS -> missions(state, nowMillis, today, onClaim, onClaimAll, onClaimWeekly)
            ShopSection.EVENTS -> events(state, today, itemsByCode::get) { selected = it.code }
            ShopSection.SHOP -> shop(state, packs, today, kind, { kind = it }, { selected = it.code })
            ShopSection.ACHIEVEMENTS -> achievements(state)
            ShopSection.COLLECTION -> collection(state, packs) { selected = it.code }
            ShopSection.PACKS -> packList(packs, onImport) { removing = it }
        }
    }

    val item = selected?.let(itemsByCode::get)
    if (item != null) {
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            ItemDetail(
                item = item,
                pack = packs.firstOrNull { it.id == item.packId },
                state = state,
                nowMillis = nowMillis,
                today = today,
                onBuy = { onBuy(item) },
                onEquip = { onEquip(item) },
                onUnequip = { onUnequip(item) },
            )
        }
    }

    removing?.let { pack ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(MR.strings.kurotoon_packs_remove)) },
            text = { Text(stringResource(MR.strings.kurotoon_packs_removeConfirm, pack.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemovePack(pack)
                        removing = null
                    },
                ) { Text(stringResource(MR.strings.kurotoon_packs_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { removing = null }) { Text(stringResource(MR.strings.action_cancel)) }
            },
        )
    }
}

private fun LazyGridScope.full(key: Any, content: @Composable () -> Unit) =
    item(key = key, span = { GridItemSpan(maxLineSpan) }, contentType = key.toString().substringBefore(':')) {
        content()
    }

@Composable
private fun WalletCard(
    state: RewardsState,
    nowMillis: Long,
    animated: Boolean,
    avatar: ShopItem?,
    frame: ShopItem?,
) {
    val shape = RoundedCornerShape(24.dp)
    val shown by animateIntAsState(state.points, tween(900), label = "wallet")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .kuroCard(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        KuroColors.Crimson.copy(alpha = 0.18f),
                        KuroColors.Galaxy.copy(alpha = 0.10f),
                        Color.Transparent,
                    ),
                ),
                shape,
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        KuroAvatar(avatar = avatar, frame = frame, modifier = Modifier.size(76.dp), animated = animated)
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(MR.strings.kurotoon_rewards_points).uppercase(),
                color = KuroColors.Gold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KuroCoin(size = 26.dp)
                Text(formatNumber(shown), fontSize = 32.sp, fontWeight = FontWeight.Black)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                Icon(
                    MaterialSymbols.Rounded.LocalFireDepartment,
                    null,
                    tint = KuroColors.Flame,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    stringResource(MR.strings.kurotoon_rewards_streak, state.streak),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    " · " + stringResource(MR.strings.kurotoon_rewards_bestStreak, state.bestStreak),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.isBoosted(nowMillis)) {
                Text(
                    stringResource(MR.strings.kurotoon_rewards_boostUntil, formatTime(state.boostUntil)),
                    style = MaterialTheme.typography.labelSmall,
                    color = KuroColors.Gold,
                    modifier = Modifier.padding(top = 2.dp),
                )
            } else {
                Text(
                    stringResource(MR.strings.kurotoon_rewards_lifetime, formatNumber(state.lifetimePoints)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            LevelBar(state, Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun SectionPills(selected: ShopSection, onSelect: (ShopSection) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ShopSection.entries.forEach { section ->
            Pill(stringResource(section.label), selected == section) { onSelect(section) }
        }
    }
}

@Composable
private fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg by androidx.compose.animation.animateColorAsState(
        if (selected) KuroColors.Crimson else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "pill",
    )
    Text(
        text = text,
        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier
            .then(
                if (selected) {
                    Modifier.dropShadow(
                        CircleShape,
                        Shadow(radius = 12.dp, color = KuroColors.Crimson.copy(alpha = 0.5f)),
                    )
                } else {
                    Modifier
                },
            )
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

private fun LazyGridScope.missions(
    state: RewardsState,
    nowMillis: Long,
    today: LocalDate,
    onClaim: (String) -> Unit,
    onClaimAll: () -> Unit,
    onClaimWeekly: (String) -> Unit,
) {
    val boosted = state.isBoosted(nowMillis)
    val engine = RewardsEngine(now = { Instant.ofEpochMilli(nowMillis) })
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
    val weekProgress = if (state.week == monday) state.weekProgress else emptyMap()
    val weekClaimed = if (state.week == monday) state.weekClaimed else emptySet()
    val claimable = RewardsCatalog.missions.count {
        (state.progress[it.code] ?: 0) >= it.target && it.code !in state.claimed
    } + RewardsCatalog.weeklyMissions.count { (weekProgress[it.code] ?: 0) >= it.target && it.code !in weekClaimed }
    full("streakTracker") { StreakTracker(state, today, engine.daysToChest(state)) }
    full("missionsTitle") {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            KuroSectionTitle(stringResource(MR.strings.kurotoon_rewards_todaysMissions), Modifier.weight(1f))
            if (claimable > 1) {
                TextButton(onClick = onClaimAll) { Text(stringResource(MR.strings.kurotoon_rewards_claimAll)) }
            }
        }
    }
    if (boosted) {
        full("boost") {
            Text(
                stringResource(MR.strings.kurotoon_rewards_boostActive),
                color = KuroColors.Gold,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    RewardsCatalog.missions.forEach { mission ->
        full("mission:${mission.code}") {
            MissionRow(
                mission = mission,
                progress = state.progress[mission.code] ?: 0,
                claimed = mission.code in state.claimed,
                boosted = boosted,
                onClaim = { onClaim(mission.code) },
            )
        }
    }
    full("weeklyTitle") {
        val nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, nextMonday).toInt()
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 8.dp)) {
            KuroSectionTitle(stringResource(MR.strings.kurotoon_rewards_weeklyMissions), Modifier.weight(1f))
            Text(
                stringResource(
                    MR.strings.kurotoon_rewards_resetsIn,
                    stringResource(MR.strings.kurotoon_rewards_daysShort, days),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    RewardsCatalog.weeklyMissions.forEach { mission ->
        full("weekly:${mission.code}") {
            MissionRow(
                mission = mission,
                progress = weekProgress[mission.code] ?: 0,
                claimed = mission.code in weekClaimed,
                boosted = boosted,
                onClaim = { onClaimWeekly(mission.code) },
            )
        }
    }
    full("tracker") { Box(Modifier.padding(top = 8.dp)) { ReadingTracker(state, today) } }
    full("missionsFooter") { Footer(stringResource(MR.strings.kurotoon_rewards_missionsFooter)) }
}

private fun LazyGridScope.events(
    state: RewardsState,
    today: LocalDate,
    item: (String) -> ShopItem?,
    onSelect: (ShopItem) -> Unit,
) {
    val active = Seasons.active(today)
    if (active.isEmpty()) {
        full("noEvent") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                KuroMascot(size = 96.dp, mood = KuroMood.SLEEPY)
                Text(
                    stringResource(MR.strings.kurotoon_rewards_noEvent),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
    active.forEach { run ->
        full("event:${run.key}") { SeasonCard(run, state, item(run.event.reward), today) }
    }
    val upcoming = Seasons.upcoming(today, limit = 4)
    if (upcoming.isNotEmpty()) {
        full("upcomingTitle") {
            KuroSectionTitle(stringResource(MR.strings.kurotoon_rewards_upcoming), Modifier.padding(top = 8.dp))
        }
        upcoming.forEach { run ->
            full("upcoming:${run.key}") { UpcomingRow(run, item(run.event.reward), today) }
        }
    }
    val seasonal = Seasons.events.mapNotNull { item(it.reward) }.distinctBy { it.code }
    if (seasonal.isNotEmpty()) {
        full("seasonalTitle") {
            KuroSectionTitle(stringResource(MR.strings.kurotoon_rewards_seasonal), Modifier.padding(top = 8.dp))
        }
        items(seasonal, key = { "seasonal:${it.code}" }, contentType = { "item" }) { shopItem ->
            ItemTile(shopItem, state, Modifier.animateItem(), inSeason = Seasons.isInSeason(shopItem.code, today)) {
                onSelect(shopItem)
            }
        }
    }
}

@Composable
private fun MissionRow(mission: Mission, progress: Int, claimed: Boolean, boosted: Boolean, onClaim: () -> Unit) {
    val done = progress >= mission.target
    val fraction by animateFloatAsState((progress.toFloat() / mission.target).coerceIn(0f, 1f), label = "mission")
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .kuroCard(
                RoundedCornerShape(18.dp),
                if (done &&
                    !claimed
                ) {
                    KuroColors.Gold.copy(alpha = 0.5f)
                } else {
                    Color.White.copy(alpha = 0.06f)
                },
            )
            .padding(14.dp)
            .alpha(if (claimed) 0.6f else 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(mission.title), fontWeight = FontWeight.SemiBold, maxLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                LinearProgressIndicator(
                    progress = { fraction },
                    color = if (done) KuroColors.Gold else KuroColors.Crimson,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape),
                    drawStopIndicator = {},
                )
                Text(
                    "${progress.coerceAtMost(mission.target)}/${mission.target}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        when {
            claimed -> Text(
                stringResource(MR.strings.kurotoon_rewards_claimed),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            done -> Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onClaim()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = KuroColors.Gold,
                    contentColor = Color(0xFF3A2600),
                ),
                contentPadding = PaddingValues(horizontal = 14.dp),
            ) {
                Text(stringResource(MR.strings.kurotoon_rewards_claim), fontWeight = FontWeight.Bold)
            }
            else -> KuroAmount("+${if (boosted) mission.reward * 2 else mission.reward}")
        }
    }
}

private fun LazyGridScope.shop(
    state: RewardsState,
    packs: List<Pack>,
    today: LocalDate,
    kind: ShopKind?,
    onKind: (ShopKind?) -> Unit,
    onSelect: (ShopItem) -> Unit,
) {
    val kinds = packs.flatMap { pack -> pack.items.map { it.kind } }.distinct().sortedBy { it.ordinal }
    full("kinds") {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallChip(stringResource(MR.strings.kurotoon_rewards_all), kind == null) { onKind(null) }
            kinds.forEach { k -> SmallChip(stringResource(k.label), kind == k) { onKind(k) } }
        }
    }
    full("shopTip") { Footer(stringResource(MR.strings.kurotoon_rewards_shopTip)) }
    packs.forEach { pack ->
        val items = pack.items.filter { kind == null || it.kind == kind }
        if (items.isEmpty()) return@forEach
        full("pack:${pack.id}") { PackHeader(pack) }
        items(items, key = { "item:${it.code}" }, contentType = { "item" }) { item ->
            ItemTile(item, state, Modifier.animateItem(), Seasons.isInSeason(item.code, today)) { onSelect(item) }
        }
    }
}

@Composable
private fun SmallChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) KuroColors.Crimson else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, if (selected) KuroColors.Crimson else MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun PackHeader(pack: Pack) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 8.dp)) {
        KuroSectionTitle(pack.name, Modifier.weight(1f, fill = false))
        if (pack.author.isNotBlank()) {
            Text(
                stringResource(MR.strings.kurotoon_packs_byAuthor, pack.author),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
                maxLines = 1,
            )
        }
    }
}

@Composable
fun ItemTile(
    item: ShopItem,
    state: RewardsState,
    modifier: Modifier = Modifier,
    inSeason: Boolean = true,
    onClick: () -> Unit,
) {
    val owned = state.owns(item) && item.kind != ShopKind.BOOST
    val equipped = state.isEquipped(item)
    val shape = RoundedCornerShape(18.dp)
    val ring = if (equipped) KuroColors.Crimson else item.rarity.color.copy(alpha = 0.55f)
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .dropShadow(
                    shape,
                    Shadow(radius = 14.dp, color = item.rarity.color.copy(alpha = if (equipped) 0.45f else 0.18f)),
                )
                .clip(shape)
                .background(
                    Brush.radialGradient(
                        listOf(item.rarity.color.copy(alpha = 0.18f), MaterialTheme.colorScheme.surfaceContainer),
                    ),
                )
                .border(if (equipped) 2.dp else 1.dp, ring, shape)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            ShopItemArt(
                item,
                Modifier
                    .fillMaxSize()
                    .alpha(if (!owned && !inSeason) 0.45f else 1f),
                animated = false,
            )
            if (!owned && (state.points < item.price || !inSeason)) {
                Icon(
                    MaterialSymbols.Rounded.Lock,
                    null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(14.dp),
                )
            }
        }
        Text(
            item.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (!owned && !inSeason) {
            Text(stringResource(MR.strings.kurotoon_rewards_seasonal), color = KuroColors.Galaxy, fontSize = 11.sp)
        } else {
            PriceLabel(item, state, owned, equipped)
        }
    }
}

@Composable
private fun PriceLabel(item: ShopItem, state: RewardsState, owned: Boolean, equipped: Boolean) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    when {
        equipped -> Text(
            stringResource(MR.strings.kurotoon_rewards_equipped),
            color = KuroColors.Crimson,
            fontSize = 11.sp,
        )
        owned && !item.isFree -> Text(
            stringResource(MR.strings.kurotoon_rewards_owned),
            color = muted,
            fontSize = 11.sp,
        )
        item.isFree -> Text(
            stringResource(MR.strings.kurotoon_rewards_free),
            color = KuroColors.Aurora,
            fontSize = 11.sp,
        )
        else -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            KuroCoin(size = 12.dp)
            Text(
                formatNumber(item.price),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (state.points >= item.price) KuroColors.Gold else muted,
            )
        }
    }
}

private fun LazyGridScope.achievements(state: RewardsState) {
    val all = RewardsCatalog.achievements
    full("achTitle") {
        KuroSectionTitle(
            stringResource(
                MR.strings.kurotoon_rewards_achievementsCount,
                all.count {
                    it.code in state.achievements
                },
                all.size,
            ),
            Modifier.padding(top = 4.dp),
        )
    }
    all.forEach { achievement ->
        full("ach:${achievement.code}") { AchievementRow(achievement, state.achievements[achievement.code]) }
    }
}

@Composable
private fun AchievementRow(achievement: Achievement, unlockedAt: Long?) {
    val unlocked = unlockedAt != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .kuroCard(
                RoundedCornerShape(18.dp),
                if (unlocked) KuroColors.Gold.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f),
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AchievementMedal(achievement, unlocked)
        Column(Modifier.weight(1f)) {
            Text(stringResource(achievement.title), fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(achievement.description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (unlockedAt != null) {
                Text(
                    formatDate(unlockedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = KuroColors.Gold,
                )
            }
        }
        KuroAmount("+${achievement.reward}", Modifier.alpha(if (unlocked) 0.6f else 1f))
    }
}

private fun LazyGridScope.collection(state: RewardsState, packs: List<Pack>, onSelect: (ShopItem) -> Unit) {
    val owned = packs.flatMap { it.items }.filter { it.kind != ShopKind.BOOST && state.owns(it) }
    if (owned.isEmpty()) {
        full("collectionEmpty") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                KuroMascot(size = 96.dp, mood = KuroMood.SLEEPY)
                Text(
                    stringResource(MR.strings.kurotoon_rewards_collectionEmpty),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        return
    }
    owned.groupBy { it.kind }.toSortedMap(compareBy { it.ordinal }).forEach { (kind, items) ->
        full("owned:${kind.key}") { KuroSectionTitle(stringResource(kind.label), Modifier.padding(top = 8.dp)) }
        items(items, key = { "owned:${it.code}" }, contentType = { "item" }) { item ->
            ItemTile(item, state, Modifier.animateItem()) { onSelect(item) }
        }
    }
    full("collectionFooter") { Footer(stringResource(MR.strings.kurotoon_rewards_collectionFooter)) }
}

private fun LazyGridScope.packList(packs: List<Pack>, onImport: () -> Unit, onRemove: (Pack) -> Unit) {
    full("import") {
        Column(
            Modifier
                .fillMaxWidth()
                .kuroCard(RoundedCornerShape(18.dp), KuroColors.Crimson.copy(alpha = 0.4f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(MR.strings.kurotoon_packs_importHint), style = MaterialTheme.typography.bodySmall)
            Button(onClick = onImport, colors = ButtonDefaults.buttonColors(containerColor = KuroColors.Crimson)) {
                Icon(MaterialSymbols.Rounded.UploadFile, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(MR.strings.kurotoon_packs_import), fontWeight = FontWeight.Bold)
            }
        }
    }
    packs.forEach { pack -> full("packRow:${pack.id}") { PackRow(pack) { onRemove(pack) } } }
}

@Composable
private fun PackRow(pack: Pack, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .kuroCard(RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(8.dp),
        ) {
            val cover = pack.cover
            if (cover != null) {
                coil3.compose.AsyncImage(cover, pack.name, Modifier.fillMaxSize())
            } else {
                pack.items.firstOrNull()?.let { ShopItemArt(it, Modifier.fillMaxSize(), animated = false) }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(pack.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val meta = buildList {
                add(stringResource(MR.strings.kurotoon_packs_itemCount, pack.items.size))
                if (pack.author.isNotBlank()) add(stringResource(MR.strings.kurotoon_packs_byAuthor, pack.author))
                if (pack.origin == PackOrigin.BUILT_IN) add(stringResource(MR.strings.kurotoon_packs_builtIn))
            }
            Text(
                meta.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (pack.description.isNotBlank()) {
                Text(
                    pack.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (pack.origin == PackOrigin.IMPORTED) {
            androidx.compose.material3.IconButton(onClick = onRemove) {
                Icon(MaterialSymbols.Rounded.Delete, stringResource(MR.strings.kurotoon_packs_remove))
            }
        }
    }
}

@Composable
private fun ItemDetail(
    item: ShopItem,
    pack: Pack?,
    state: RewardsState,
    nowMillis: Long,
    today: LocalDate,
    onBuy: () -> PurchaseResult,
    onEquip: () -> Unit,
    onUnequip: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val owned = state.owns(item) && item.kind != ShopKind.BOOST
    val equipped = state.isEquipped(item)
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(180.dp)
                .dropShadow(
                    RoundedCornerShape(32.dp),
                    Shadow(radius = 32.dp, color = item.rarity.color.copy(alpha = 0.4f)),
                )
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.radialGradient(
                        listOf(item.rarity.color.copy(alpha = 0.25f), MaterialTheme.colorScheme.surfaceContainer),
                    ),
                )
                .border(1.dp, item.rarity.color.copy(alpha = 0.6f), RoundedCornerShape(32.dp))
                .padding(20.dp),
        ) {
            ShopItemArt(item, Modifier.fillMaxSize())
        }
        Text(
            stringResource(item.rarity.label).uppercase() + " · " + stringResource(item.kind.label).uppercase(),
            color = item.rarity.color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            item.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (pack != null) {
            Text(
                pack.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (item.description.isNotBlank()) {
            Text(
                item.description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        val events = Seasons.eventsFor(item.code)
        val inSeason = Seasons.isInSeason(item.code, today)
        if (events.isNotEmpty()) {
            Text(
                stringResource(
                    MR.strings.kurotoon_rewards_seasonOnly,
                    events.map { stringResource(it.title) }.joinToString(),
                ),
                color = KuroColors.Galaxy,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (item.style == kurotoon.rewards.FREEZE_STYLE) {
            Text(
                stringResource(MR.strings.kurotoon_rewards_freezes, state.freezes),
                color = KuroColors.Aurora,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else if (item.kind == ShopKind.BOOST && state.isBoosted(nowMillis)) {
            Text(
                stringResource(MR.strings.kurotoon_rewards_boostUntil, formatTime(state.boostUntil)),
                color = KuroColors.Gold,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        AnimatedContent(
            targetState = Triple(owned, equipped, state.points >= item.price && inSeason),
            transitionSpec = { fadeIn(spring()) togetherWith fadeOut(spring()) },
            label = "itemAction",
        ) { (isOwned, isEquipped, affordable) ->
            val modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
            when {
                isEquipped -> OutlinedButton(onClick = onUnequip, modifier = modifier) {
                    Text(stringResource(MR.strings.kurotoon_rewards_unequip))
                }
                isOwned && item.kind.equippable -> Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        onEquip()
                    },
                    modifier = modifier,
                    colors = ButtonDefaults.buttonColors(containerColor = KuroColors.Crimson),
                ) { Text(stringResource(MR.strings.kurotoon_rewards_equip), fontWeight = FontWeight.Bold) }
                else -> Button(
                    onClick = {
                        if (onBuy() == PurchaseResult.Success) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    },
                    enabled = affordable,
                    modifier = modifier,
                    colors = ButtonDefaults.buttonColors(containerColor = KuroColors.Crimson),
                ) {
                    if (affordable) {
                        Text(
                            stringResource(
                                if (item.kind ==
                                    ShopKind.BOOST
                                ) {
                                    MR.strings.kurotoon_rewards_use
                                } else {
                                    MR.strings.kurotoon_rewards_unlock
                                },
                            ),
                            fontWeight = FontWeight.Bold,
                        )
                        if (!item.isFree) {
                            Spacer(Modifier.width(10.dp))
                            KuroCoin(size = 18.dp)
                            Spacer(Modifier.width(4.dp))
                            Text(formatNumber(item.price), fontWeight = FontWeight.Bold)
                        }
                    } else if (!inSeason) {
                        Text(
                            stringResource(
                                MR.strings.kurotoon_rewards_outOfSeason,
                                events.map { stringResource(it.title) }.joinToString(),
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        Text(stringResource(MR.strings.kurotoon_rewards_needMore, item.price - state.points))
                    }
                }
            }
        }
    }
}

@Composable
private fun Footer(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

internal fun formatNumber(value: Int): String = NumberFormat.getIntegerInstance().format(value)

private fun formatTime(millis: Long): String = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
    .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

private fun formatDate(millis: Long): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
