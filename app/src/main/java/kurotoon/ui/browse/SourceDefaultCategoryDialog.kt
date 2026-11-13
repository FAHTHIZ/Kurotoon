package kurotoon.ui.browse

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.category.visualName
import kurotoon.app.di.appGraph
import kurotoon.ui.KuroAlertDialog
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.KuroRadioButton
import tachiyomi.presentation.core.components.material.KuroTextButton
import tachiyomi.presentation.core.i18n.stringResource

/** Pick where series added from this source land, overriding the global default category. */
@Composable
fun SourceDefaultCategoryDialog(sourceId: Long, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val kuro = remember { context.appGraph.kuroPreferences }
    val categories by produceState(emptyList<Category>()) {
        value = context.appGraph.getCategories.await().filterNot { it.isSystemCategory }
    }
    val current = kuro.defaultCategoryFor(sourceId, fallback = -1L).takeIf { it >= 0 }

    KuroAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(MR.strings.kurotoon_sourceCategory_title)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                item(key = "global") {
                    Option(
                        label = stringResource(MR.strings.kurotoon_sourceCategory_global),
                        selected = current == null,
                    ) {
                        kuro.setDefaultCategoryFor(sourceId, null)
                        onDismiss()
                    }
                }
                items(categories, key = { it.id }) { category ->
                    Option(label = category.visualName, selected = current == category.id) {
                        kuro.setDefaultCategoryFor(sourceId, category.id)
                        onDismiss()
                    }
                }
            }
        },
        confirmButton = {
            KuroTextButton(onClick = onDismiss) { Text(stringResource(MR.strings.action_cancel)) }
        },
    )
}

@Composable
private fun Option(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .heightIn(min = 48.dp),
    ) {
        KuroRadioButton(selected = selected, onClick = null)
        Text(text = label, modifier = Modifier.weight(1f))
    }
}
