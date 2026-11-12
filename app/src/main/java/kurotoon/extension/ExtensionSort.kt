package kurotoon.extension

import android.content.Context
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.extension.model.Extension
import kurotoon.ui.KuroAlertDialog
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.KuroRadioButton
import tachiyomi.presentation.core.components.material.KuroTextButton
import tachiyomi.presentation.core.i18n.stringResource

/** How the installed extension sections are ordered (mihon#2744). */
enum class ExtensionSort(val label: StringResource) {
    NAME(MR.strings.kurotoon_extensions_sortName),
    RECENTLY_UPDATED(MR.strings.kurotoon_extensions_sortRecent),
    LANGUAGE(MR.strings.kurotoon_extensions_sortLanguage),
    ;

    /** Sorts [extensions]; ties and unknown install times fall back to the name. */
    fun <T : Extension> sort(extensions: List<T>, context: Context): List<T> {
        val byName = compareBy<T, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
        return when (this) {
            NAME -> extensions.sortedWith(byName)
            LANGUAGE -> extensions.sortedWith(compareBy<T> { it.lang }.then(byName))
            RECENTLY_UPDATED -> {
                val times = extensions.associate { it.pkgName to lastUpdateTime(context, it.pkgName) }
                extensions.sortedWith(compareByDescending<T> { times[it.pkgName] ?: 0L }.then(byName))
            }
        }
    }

    companion object {
        fun from(value: Int): ExtensionSort = entries.getOrElse(value) { NAME }

        @Suppress("DEPRECATION")
        private fun lastUpdateTime(context: Context, pkgName: String): Long = runCatching {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkgName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                pm.getPackageInfo(pkgName, 0)
            }.lastUpdateTime
        }.getOrDefault(0L)
    }
}

@Composable
fun ExtensionSortDialog(
    current: ExtensionSort,
    onSelect: (ExtensionSort) -> Unit,
    onDismissRequest: () -> Unit,
) {
    KuroAlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(MR.strings.kurotoon_extensions_sort)) },
        text = {
            Column {
                ExtensionSort.entries.forEach { sort ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(sort)
                                onDismissRequest()
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        KuroRadioButton(selected = sort == current, onClick = null)
                        Text(stringResource(sort.label), modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {
            KuroTextButton(onClick = onDismissRequest) { Text(stringResource(MR.strings.action_cancel)) }
        },
    )
}
