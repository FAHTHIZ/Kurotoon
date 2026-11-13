package kurotoon.ui.reader

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import eu.kanade.presentation.more.settings.Preference
import kurotoon.app.di.appGraph
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/**
 * Where saved pages go: the Pictures folder by default, or any folder the reader picks, such as one
 * on an SD card. Tapping it again while a folder is set goes back to Pictures.
 */
@Composable
fun pageSaveFolderPreference(): Preference.PreferenceItem.TextPreference {
    val context = LocalContext.current
    val preference = remember { context.appGraph.kuroPreferences.pageSaveFolder }
    val folder by preference.collectAsState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: SecurityException) {
            // Some devices refuse persistable grants yet still let the app write there
            PageSaveFolder.logcat(LogPriority.WARN, e)
        }
        preference.set(uri.toString())
    }

    return Preference.PreferenceItem.TextPreference(
        title = stringResource(MR.strings.kurotoon_reader_saveFolder),
        subtitle = if (folder.isEmpty()) {
            stringResource(MR.strings.kurotoon_reader_saveFolderDefault)
        } else {
            stringResource(MR.strings.kurotoon_reader_saveFolderCustom, PageSaveFolder.displayName(context, folder))
        },
        onClick = {
            if (folder.isEmpty()) {
                runCatching { picker.launch(null) }.onFailure { PageSaveFolder.logcat(LogPriority.ERROR, it) }
            } else {
                preference.set("")
            }
        },
    )
}

object PageSaveFolder {

    /** The picked folder if it's still writable, else null so saving falls back to Pictures. */
    fun resolve(context: Context, raw: String): UniFile? {
        if (raw.isEmpty()) return null
        return runCatching { UniFile.fromUri(context, raw.toUri()) }.getOrNull()
            ?.takeIf { it.isDirectory && it.canWrite() }
    }

    fun displayName(context: Context, raw: String): String =
        resolve(context, raw)?.name ?: Uri.decode(raw).substringAfterLast(':').ifEmpty { raw }
}
