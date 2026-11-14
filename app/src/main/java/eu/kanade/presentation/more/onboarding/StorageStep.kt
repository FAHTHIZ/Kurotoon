package eu.kanade.presentation.more.onboarding

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.screen.SettingsDataScreen
import eu.kanade.presentation.more.settings.screen.inAppStorageUri
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import kurotoon.app.di.appGraph
import kurotoon.icons.materialsymbols.MaterialSymbols
import kurotoon.icons.materialsymbols.rounded.Folder
import kurotoon.icons.materialsymbols.rounded.Mobile
import kurotoon.ui.KuroTip
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/**
 * Where downloads, backups and local series live. KuroToon's own folder works straight away with no
 * permission, so a new reader never has to face the system folder picker; a folder of their own is one tap away.
 */
internal class StorageStep : OnboardingStep {

    override val title = MR.strings.onboarding_storage_title
    override val subtitle = MR.strings.onboarding_storage_subtitle

    private var _isComplete by mutableStateOf(false)

    override val isComplete: Boolean
        get() = _isComplete

    private var pick: (() -> Unit)? = null

    override fun onIncompleteAccept() {
        pick?.invoke()
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current

        val storagePref = remember { context.appGraph.storagePreferences.baseStorageDirectory }
        val storageDir by storagePref.collectAsState()
        val appFolder = remember { context.inAppStorageUri() }
        val pickStorageLocation = SettingsDataScreen.storageLocationPicker(storagePref)

        val launchPicker: () -> Unit = {
            try {
                pickStorageLocation.launch(null)
            } catch (e: ActivityNotFoundException) {
                context.toast(MR.strings.file_picker_error)
            }
        }
        pick = launchPicker

        // A fresh install starts on KuroToon's own folder, so Next works without a detour
        LaunchedEffect(Unit) {
            if (!storagePref.isSet()) storagePref.set(appFolder)
        }
        LaunchedEffect(Unit) {
            storagePref.changes().collectLatest { _isComplete = storagePref.isSet() }
        }

        val usingAppFolder = storageDir == appFolder
        val usingOwnFolder = storagePref.isSet() && !usingAppFolder

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OnboardingChoice(
                icon = MaterialSymbols.Rounded.Mobile,
                title = stringResource(MR.strings.onboarding_storage_app_title),
                body = stringResource(MR.strings.onboarding_storage_app_body),
                badge = stringResource(MR.strings.onboarding_recommended),
                selected = usingAppFolder,
                onClick = { storagePref.set(appFolder) },
            )
            OnboardingChoice(
                icon = MaterialSymbols.Rounded.Folder,
                title = stringResource(MR.strings.onboarding_storage_own_title),
                body = stringResource(MR.strings.onboarding_storage_own_body),
                footer = if (usingOwnFolder) SettingsDataScreen.storageLocationText(storagePref) else null,
                selected = usingOwnFolder,
                onClick = launchPicker,
            )
            KuroTip(text = stringResource(MR.strings.onboarding_storage_later))
        }
    }
}
