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
import kurotoon.ui.KuroColors
import kurotoon.ui.profile.KuroProfileHeader
import kurotoon.ui.profile.MeGroup
import kurotoon.ui.profile.MeModeTile
import kurotoon.ui.profile.MeRow
import kurotoon.ui.profile.ProfileLook
import kurotoon.ui.profile.ProfileStats
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Help
import mihon.icons.materialsymbols.automirroredrounded.Label
import mihon.icons.materialsymbols.rounded.CloudOff
import mihon.icons.materialsymbols.rounded.CollectionsBookmark
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.Eyeglasses2
import mihon.icons.materialsymbols.rounded.Info
import mihon.icons.materialsymbols.rounded.QueryStats
import mihon.icons.materialsymbols.rounded.Settings
import mihon.icons.materialsymbols.rounded.Storage
import mihon.icons.materialsymbols.rounded.Sync
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
    onClickChapterReconciliation: () -> Unit,
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
                        tint = KuroColors.Aurora,
                        onClick = onClickDownloadQueue,
                    )
                    MeRow(
                        title = stringResource(MR.strings.label_stats),
                        icon = MaterialSymbols.Rounded.QueryStats,
                        tint = KuroColors.Galaxy,
                        onClick = onClickStats,
                    )
                    MeRow(
                        title = stringResource(MR.strings.kurotoon_panels_title),
                        subtitle = stringResource(MR.strings.kurotoon_panels_summary),
                        icon = MaterialSymbols.Rounded.CollectionsBookmark,
                        tint = KuroColors.Crimson,
                        onClick = onClickSavedPanels,
                        showDivider = false,
                    )
                }
            }

            item(key = "group-library") {
                MeGroup(title = stringResource(MR.strings.kurotoon_me_group_library)) {
                    MeRow(
                        title = stringResource(MR.strings.categories),
                        icon = MaterialSymbols.AutoMirroredRounded.Label,
                        tint = KuroColors.Gold,
                        onClick = onClickCategories,
                    )
                    MeRow(
                        title = stringResource(MR.strings.kurotoon_more_reconcile),
                        subtitle = stringResource(MR.strings.kurotoon_more_reconcileSummary),
                        icon = MaterialSymbols.Rounded.Sync,
                        tint = KuroColors.Aurora,
                        onClick = onClickChapterReconciliation,
                    )
                    MeRow(
                        title = stringResource(MR.strings.label_data_storage),
                        icon = MaterialSymbols.Rounded.Storage,
                        tint = KuroColors.Galaxy,
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
