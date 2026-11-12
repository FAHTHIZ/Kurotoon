package kurotoon.ui.shop

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kurotoon.rewards.RewardsState
import kurotoon.shop.Pack
import kurotoon.shop.PackOrigin
import kurotoon.shop.PackRepository.ImportResult
import kurotoon.shop.ShopItem
import kurotoon.shop.ShopKind
import kurotoon.ui.KuroAlertDialog
import kurotoon.ui.kuroBounceClickable
import kurotoon.ui.kuroCard
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.Delete
import mihon.icons.materialsymbols.rounded.Share
import mihon.icons.materialsymbols.rounded.UploadFile
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.KuroButton
import tachiyomi.presentation.core.components.material.KuroButtonStyle
import tachiyomi.presentation.core.components.material.KuroTextButton
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import java.io.File

/** The order kinds appear in. Boosts belonged to the old points shop and aren't shown. */
private val KINDS = listOf(
    ShopKind.AVATAR,
    ShopKind.FRAME,
    ShopKind.STICKER,
    ShopKind.BADGE,
    ShopKind.THEME,
    ShopKind.PARTICLES,
)

/**
 * Avatars, frames, stickers, badges and looks for the app, all free. Pick one to use it on your
 * profile or around the app, or share a sticker. Packs (.kpack files) add more.
 */
class CollectionScreen : Screen() {

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
                    is ImportResult.Success ->
                        context.toast(context.stringResource(MR.strings.kurotoon_packs_imported, result.pack.name))
                    is ImportResult.Failure -> context.toast(result.reason.label)
                }
            }
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.kurotoon_collection_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                    actions = {
                        IconButton(onClick = {
                            importer.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                        }) {
                            Icon(MaterialSymbols.Rounded.UploadFile, stringResource(MR.strings.kurotoon_packs_import))
                        }
                    },
                )
            },
        ) { padding ->
            CollectionContent(
                state = state,
                packs = packs.orEmpty(),
                contentPadding = padding,
                onUse = { store.equip(it.code) },
                onStopUsing = { store.unequip(it.code) },
                onShare = { item -> scope.launch { shareSticker(context, item) } },
                onRemovePack = { pack ->
                    if (store.packs.remove(pack.id)) context.toast(MR.strings.kurotoon_packs_removed)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionContent(
    state: RewardsState,
    packs: List<Pack>,
    contentPadding: PaddingValues,
    onUse: (ShopItem) -> Unit,
    onStopUsing: (ShopItem) -> Unit,
    onShare: (ShopItem) -> Unit,
    onRemovePack: (Pack) -> Unit,
) {
    var kind by rememberSaveable { mutableStateOf<ShopKind?>(null) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var removing by remember { mutableStateOf<Pack?>(null) }
    val items = remember(packs) { packs.flatMap { it.items }.filter { it.kind in KINDS } }
    val byKind = remember(items) {
        KINDS.associateWith { k -> items.filter { it.kind == k } }.filterValues { it.isNotEmpty() }
    }
    val imported = remember(packs) { packs.filter { it.origin == PackOrigin.IMPORTED } }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(96.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 32.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        full("intro") {
            Text(
                stringResource(MR.strings.kurotoon_collection_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        full("kinds") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                kurotoon.ui.KuroFilterPill(
                    text = stringResource(MR.strings.kurotoon_rewards_all),
                    selected = kind == null,
                    onClick = { kind = null },
                )
                byKind.forEach { (k, list) ->
                    kurotoon.ui.KuroFilterPill(
                        text = stringResource(k.label),
                        selected = kind == k,
                        onClick = { kind = if (kind == k) null else k },
                        count = list.size,
                    )
                }
            }
        }
        byKind.forEach { (k, list) ->
            if (kind != null && kind != k) return@forEach
            full("header:${k.key}") { SectionHeader(stringResource(k.label), list.size) }
            items(list, key = { "item:${it.code}" }, contentType = { "item" }) { item ->
                CollectionTile(item, inUse = state.isEquipped(item)) { selected = item.code }
            }
        }
        if (imported.isNotEmpty() && kind == null) {
            full("header:packs") { SectionHeader(stringResource(MR.strings.kurotoon_collection_packs), imported.size) }
            imported.forEach { pack ->
                full("pack:${pack.id}") { PackLine(pack) { removing = pack } }
            }
        }
        full("hint") {
            Text(
                stringResource(MR.strings.kurotoon_packs_importHint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    val item = selected?.let { code -> items.firstOrNull { it.code == code } }
    if (item != null) {
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            ItemSheet(
                item = item,
                pack = packs.firstOrNull { it.id == item.packId },
                inUse = state.isEquipped(item),
                onUse = { onUse(item) },
                onStopUsing = { onStopUsing(item) },
                onShare = { onShare(item) },
            )
        }
    }

    removing?.let { pack ->
        KuroAlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(MR.strings.kurotoon_packs_remove)) },
            text = { Text(stringResource(MR.strings.kurotoon_collection_removeConfirm, pack.name)) },
            confirmButton = {
                KuroTextButton(
                    onClick = {
                        onRemovePack(pack)
                        removing = null
                    },
                ) { Text(stringResource(MR.strings.kurotoon_packs_remove)) }
            },
            dismissButton = {
                KuroTextButton(onClick = { removing = null }) { Text(stringResource(MR.strings.action_cancel)) }
            },
        )
    }
}

private fun LazyGridScope.full(key: Any, content: @Composable () -> Unit) =
    item(key = key, span = { GridItemSpan(maxLineSpan) }, contentType = key.toString().substringBefore(':')) {
        content()
    }

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun CollectionTile(item: ShopItem, inUse: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .kuroBounceClickable(onClick = onClick)
            .clip(shape),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .kuroCard(shape)
                .then(
                    if (inUse) {
                        Modifier
                            .background(
                                androidx.compose.ui.graphics.Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                        androidx.compose.ui.graphics.Color.Transparent,
                                    ),
                                ),
                            )
                            .border(2.dp, MaterialTheme.colorScheme.primary, shape)
                    } else {
                        Modifier
                    },
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            ShopItemArt(item, Modifier.fillMaxSize(), animated = false)
            if (inUse) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        MaterialSymbols.Rounded.Check,
                        contentDescription = stringResource(MR.strings.kurotoon_collection_inUse),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Text(
            item.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (inUse) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
        )
    }
}

@Composable
private fun PackLine(pack: Pack, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(pack.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val by = pack.author.takeIf { it.isNotBlank() }?.let {
                stringResource(MR.strings.kurotoon_packs_byAuthor, it)
            }
            Text(
                listOfNotNull(
                    by,
                    stringResource(MR.strings.kurotoon_packs_itemCount, pack.items.size),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(MaterialSymbols.Rounded.Delete, stringResource(MR.strings.kurotoon_packs_remove))
        }
    }
}

@Composable
private fun ItemSheet(
    item: ShopItem,
    pack: Pack?,
    inUse: Boolean,
    onUse: () -> Unit,
    onStopUsing: () -> Unit,
    onShare: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
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
                .size(168.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(20.dp),
        ) {
            ShopItemArt(item, Modifier.fillMaxSize())
        }
        Text(
            stringResource(item.kind.label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            item.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        val about = item.description.ifBlank { pack?.name.orEmpty() }
        if (about.isNotBlank()) {
            Text(
                about,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Text(
            stringResource(item.kind.useHint),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        Spacer(Modifier.height(20.dp))
        val buttonModifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
        if (inUse) {
            KuroButton(style = KuroButtonStyle.Outlined, onClick = onStopUsing, modifier = buttonModifier) {
                Text(stringResource(MR.strings.kurotoon_collection_stopUsing))
            }
        } else {
            KuroButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onUse()
                },
                modifier = buttonModifier,
            ) { Text(stringResource(MR.strings.kurotoon_collection_use)) }
        }
        if (item.kind == ShopKind.STICKER && item.image != null) {
            KuroTextButton(onClick = onShare, modifier = Modifier.padding(top = 4.dp)) {
                Icon(MaterialSymbols.Rounded.Share, null, Modifier.size(18.dp))
                Text(stringResource(MR.strings.action_share), Modifier.padding(start = 8.dp))
            }
        }
    }
}

private val ShopKind.useHint
    get() = when (this) {
        ShopKind.AVATAR -> MR.strings.kurotoon_collection_hint_avatar
        ShopKind.FRAME -> MR.strings.kurotoon_collection_hint_frame
        ShopKind.STICKER -> MR.strings.kurotoon_collection_hint_sticker
        ShopKind.BADGE -> MR.strings.kurotoon_collection_hint_badge
        ShopKind.THEME -> MR.strings.kurotoon_collection_hint_theme
        ShopKind.PARTICLES, ShopKind.BOOST -> MR.strings.kurotoon_collection_hint_particles
    }

/** Copies the sticker somewhere shareable and opens the share sheet, so it can go in a chat. */
private suspend fun shareSticker(context: Context, item: ShopItem) {
    val image = item.image ?: return
    val file = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "stickers").apply { mkdirs() }
            val out = File(dir, image.substringAfterLast('/'))
            val assetPrefix = "file:///android_asset/"
            if (image.startsWith(assetPrefix)) {
                context.assets.open(image.removePrefix(assetPrefix)).use { input ->
                    out.outputStream().use { input.copyTo(it) }
                }
            } else {
                File(image.removePrefix("file://")).copyTo(out, overwrite = true)
            }
            out
        }.getOrNull()
    } ?: return
    context.startActivity(file.getUriCompat(context).toShareIntent(context, type = "image/webp"))
}
