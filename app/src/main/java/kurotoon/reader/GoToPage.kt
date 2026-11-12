package kurotoon.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kurotoon.ui.KuroAlertDialog
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.KuroFieldShape
import tachiyomi.presentation.core.components.material.KuroTextButton
import tachiyomi.presentation.core.components.material.kuroFieldColors
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The current page number in the reader menu, tappable to type a page to jump to. Dragging the
 * slider to one exact page of a 150-page chapter is fiddly; typing it isn't.
 */
@Composable
fun GoToPageNumber(
    currentPage: Int,
    totalPages: Int,
    onGoTo: (page: Int) -> Unit,
    content: @Composable () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                enabled = totalPages > 1,
                role = Role.Button,
                onClickLabel = stringResource(MR.strings.kurotoon_reader_goToPage),
            ) {
                open = true
            }
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        content()
    }
    if (open) {
        GoToPageDialog(
            currentPage = currentPage,
            totalPages = totalPages,
            onDismiss = { open = false },
            onGoTo = {
                open = false
                onGoTo(it)
            },
        )
    }
}

@Composable
private fun GoToPageDialog(currentPage: Int, totalPages: Int, onDismiss: () -> Unit, onGoTo: (Int) -> Unit) {
    val initial = currentPage.toString()
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val page = value.text.toIntOrNull()
    val valid = page != null && page in 1..totalPages
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    KuroAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(MR.strings.kurotoon_reader_goToPage)) },
        text = {
            OutlinedTextField(
                shape = KuroFieldShape,
                colors = kuroFieldColors(),
                value = value,
                onValueChange = { new -> value = new.copy(text = new.text.filter(Char::isDigit).take(5)) },
                singleLine = true,
                isError = value.text.isNotEmpty() && !valid,
                supportingText = { Text(stringResource(MR.strings.kurotoon_reader_goToPageRange, totalPages)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { if (valid) onGoTo(page!!) }),
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = {
            KuroTextButton(enabled = valid, onClick = { onGoTo(page!!) }) {
                Text(stringResource(MR.strings.kurotoon_reader_goToPageAction))
            }
        },
        dismissButton = {
            KuroTextButton(onClick = onDismiss) { Text(stringResource(MR.strings.action_cancel)) }
        },
    )
}
