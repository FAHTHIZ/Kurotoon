package kurotoon.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kurotoon.shop.ShopItem
import kurotoon.ui.KuroColors
import kurotoon.ui.kuroCard
import kurotoon.ui.shop.KuroAvatar
import kurotoon.ui.shop.KuroBadge
import kurotoon.ui.shop.ShopItemArt
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CollectionsBookmark
import mihon.icons.materialsymbols.rounded.Edit
import mihon.icons.materialsymbols.rounded.History
import mihon.icons.materialsymbols.rounded.Storefront
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** Local reading numbers shown on the Me tab. */
@Immutable
data class ProfileStats(
    val inLibrary: Int = 0,
    val chaptersRead: Long = 0,
    val unread: Long = 0,
)

/** What the reader is wearing from the shop, plus their balance. */
@Immutable
data class ProfileLook(
    val points: Int = 0,
    val avatar: ShopItem? = null,
    val frame: ShopItem? = null,
    val sticker: ShopItem? = null,
    val badges: List<ShopItem> = emptyList(),
)

@Composable
fun KuroProfileHeader(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    stats: ProfileStats,
    onLibraryClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
    look: ProfileLook = ProfileLook(),
    onShopClick: () -> Unit = {},
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProfileCard(
            nickname = nickname.ifBlank { stringResource(MR.strings.kurotoon_me_defaultNickname) },
            chaptersRead = stats.chaptersRead,
            look = look,
            onAvatarClick = onShopClick,
            onNicknameChange = { onNicknameChange(it.trim().take(NICKNAME_MAX)) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                icon = MaterialSymbols.Rounded.CollectionsBookmark,
                value = stats.inLibrary.toLong(),
                label = stringResource(MR.strings.kurotoon_me_inLibrary),
                onClick = onLibraryClick,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = MaterialSymbols.Rounded.History,
                value = stats.unread,
                label = stringResource(MR.strings.kurotoon_me_unread),
                onClick = onHistoryClick,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = MaterialSymbols.Rounded.Storefront,
                value = look.points.toLong(),
                label = stringResource(MR.strings.kurotoon_rewards_points),
                onClick = onShopClick,
                modifier = Modifier.weight(1f),
                tint = KuroColors.Gold,
            )
        }
    }
}

@Composable
private fun ProfileCard(
    nickname: String,
    chaptersRead: Long,
    look: ProfileLook,
    onAvatarClick: () -> Unit,
    onNicknameChange: (String) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(24.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .kuroCard(shape)
            .background(
                Brush.radialGradient(listOf(KuroColors.Crimson.copy(alpha = 0.12f), Color.Transparent)),
                shape,
            )
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .clickable(onClick = onAvatarClick),
        ) {
            KuroAvatar(avatar = look.avatar, frame = look.frame, modifier = Modifier.matchParentSize())
            look.sticker?.let { sticker ->
                ShopItemArt(
                    sticker,
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(26.dp),
                    animated = false,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            if (editing) {
                NicknameField(
                    initial = nickname,
                    onDone = {
                        onNicknameChange(it)
                        editing = false
                    },
                )
            } else {
                Row(
                    modifier = Modifier.clickable { editing = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = nickname,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Icon(
                        MaterialSymbols.Rounded.Edit,
                        contentDescription = stringResource(MR.strings.kurotoon_me_editNickname),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Text(
                text = stringResource(MR.strings.kurotoon_me_onDevice),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = stringResource(MR.strings.kurotoon_me_chaptersRead, chaptersRead).uppercase(),
                color = KuroColors.Gold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (look.badges.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    look.badges.forEach { KuroBadge(it, Modifier.size(width = 22.dp, height = 24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun NicknameField(initial: String, onDone: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    var hadFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    BasicTextField(
        value = value,
        onValueChange = { value = it.take(NICKNAME_MAX) },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        ),
        cursorBrush = SolidColor(KuroColors.Crimson),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone(value) }),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .border(1.dp, KuroColors.Crimson.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .focusRequester(focusRequester)
            .onFocusChanged {
                if (it.isFocused) {
                    hadFocus = true
                } else if (hadFocus) {
                    onDone(value)
                }
            },
    )
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    value: Long,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = KuroColors.Crimson,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .kuroCard(shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(
            text = "%,d".format(value),
            fontSize = 22.sp,
            maxLines = 1,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val NICKNAME_MAX = 32
