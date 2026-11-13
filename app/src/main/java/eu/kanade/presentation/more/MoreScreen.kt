package eu.kanade.presentation.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import kurotoon.icons.materialsymbols.MaterialSymbols
import kurotoon.icons.materialsymbols.automirroredrounded.Help
import kurotoon.icons.materialsymbols.automirroredrounded.Label
import kurotoon.icons.materialsymbols.rounded.CloudOff
import kurotoon.icons.materialsymbols.rounded.CollectionsBookmark
import kurotoon.icons.materialsymbols.rounded.Download
import kurotoon.icons.materialsymbols.rounded.Eyeglasses2
import kurotoon.icons.materialsymbols.rounded.Info
import kurotoon.icons.materialsymbols.rounded.Palette
import kurotoon.icons.materialsymbols.rounded.QueryStats
import kurotoon.icons.materialsymbols.rounded.Settings
import kurotoon.icons.materialsymbols.rounded.Storage
import kurotoon.ui.profile.KuroProfileHeader
import kurotoon.ui.profile.MeGroup
import kurotoon.ui.profile.MeModeTile
import kurotoon.ui.profile.MeRow
import kurotoon.ui.profile.ProfileLook
import kurotoon.ui.profile.ProfileStats
import tachiyomi.core.common.Constants
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MoreScreen(
    downloadQueueStateProvider: () -> DownloadQueueState,
    downloadedOnly: Boolean,
    onDownloadedOnlyChange: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
    onClickDownloadQueue: () -> Unit,
    onClickCategories: () -> Unit,
    onClickStats: () -> Unit,
    onClickSavedPanels: () -> Unit = {},
    onClickDataAndStorage: () -> Unit,
    onClickSettings: () -> Unit,
    onClickAbout: () -> Unit,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    profileStats: ProfileStats,
    onClickLibrary: () -> Unit,
    onClickHistory: () -> Unit,
    profileLook: ProfileLook = ProfileLook(),
    onClickShop: () -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        // The profile has no app bar; keep the list from scrolling under the clock and battery
        topBar = {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsTopHeight(WindowInsets.statusBars),
            )
        },
    ) { contentPadding ->
        ScrollbarLazyColumn(contentPadding = contentPadding) {
            item {
                KuroProfileHeader(
                    nickname = nickname,
                    onNicknameChange = onNicknameChange,
                    stats = profileStats,
                    onLibraryClick = onClickLibrary,
                    onHistoryClick = onClickHistory,
                    look = profileLook,
                    onShopClick = onClickShop,
                )
            }
            item(key = "modes") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MeModeTile(
                        title = stringResource(MR.strings.label_downloaded_only),
                        subtitle = stringResource(MR.strings.downloaded_only_summary),
                        icon = MaterialSymbols.Rounded.CloudOff,
                        checked = downloadedOnly,
                        onCheckedChange = onDownloadedOnlyChange,
                        modifier = Modifier.weight(1f),
                    )
                    MeModeTile(
                        title = stringResource(MR.strings.pref_incognito_mode),
                        subtitle = stringResource(MR.strings.pref_incognito_mode_summary),
                        icon = MaterialSymbols.Rounded.Eyeglasses2,
                        checked = incognitoMode,
                        onCheckedChange = onIncognitoModeChange,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item(key = "group-reading") {
                MeGroup(title = stringResource(MR.strings.kurotoon_me_group_reading)) {
                    val downloadQueueState = downloadQueueStateProvider()
                    MeRow(
                        title = stringResource(MR.strings.label_download_queue),
                        subtitle = when (downloadQueueState) {
                            DownloadQueueState.Stopped -> null
                            is DownloadQueueState.Paused -> {
                                val pending = downloadQueueState.pending
                                if (pending == 0) {
                                    stringResource(MR.strings.paused)
                                } else {
                                    "${stringResource(MR.strings.paused)} • ${
                                        pluralStringResource(
                                            MR.plurals.download_queue_summary,
                                            count = pending,
                                            pending,
                                        )
                                    }"
                                }
                            }
                            is DownloadQueueState.Downloading -> {
                                val pending = downloadQueueState.pending
                                pluralStringResource(MR.plurals.download_queue_summary, count = pending, pending)
                            }
                        },
                        icon = MaterialSymbols.Rounded.Download,
                        onClick = onClickDownloadQueue,
                    )
                    MeRow(
                        title = stringResource(MR.strings.label_stats),
                        icon = MaterialSymbols.Rounded.QueryStats,
                        onClick = onClickStats,
                    )
                    MeRow(
                        title = stringResource(MR.strings.kurotoon_panels_title),
                        subtitle = stringResource(MR.strings.kurotoon_panels_summary),
                        icon = MaterialSymbols.Rounded.CollectionsBookmark,
                        onClick = onClickSavedPanels,
                    )
                    MeRow(
                        title = stringResource(MR.strings.kurotoon_collection_title),
                        subtitle = stringResource(MR.strings.kurotoon_collection_summary),
                        icon = MaterialSymbols.Rounded.Palette,
                        onClick = onClickShop,
                        showDivider = false,
                    )
                }
            }

            item(key = "group-library") {
                MeGroup(title = stringResource(MR.strings.kurotoon_me_group_library)) {
                    MeRow(
                        title = stringResource(MR.strings.categories),
                        icon = MaterialSymbols.AutoMirroredRounded.Label,
                        onClick = onClickCategories,
                    )
                    MeRow(
                        title = stringResource(MR.strings.label_data_storage),
                        icon = MaterialSymbols.Rounded.Storage,
                        onClick = onClickDataAndStorage,
                        showDivider = false,
                    )
                }
            }

            item(key = "group-app") {
                MeGroup(title = stringResource(MR.strings.kurotoon_me_group_app)) {
                    MeRow(
                        title = stringResource(MR.strings.label_settings),
                        icon = MaterialSymbols.Rounded.Settings,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onClickSettings,
                    )
                    MeRow(
                        title = stringResource(MR.strings.pref_category_about),
                        icon = MaterialSymbols.Rounded.Info,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onClickAbout,
                    )
                    MeRow(
                        title = stringResource(MR.strings.label_help),
                        icon = MaterialSymbols.AutoMirroredRounded.Help,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { uriHandler.openUri(Constants.URL_HELP) },
                        showDivider = false,
                    )
                }
            }

            item(key = "bottom-space") { Spacer(Modifier.height(16.dp)) }
        }
    }
}
