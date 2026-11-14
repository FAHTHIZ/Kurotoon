package eu.kanade.presentation.more.settings.screen.browse.components

import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import kurotoon.domain.extension.StoreLink
import kurotoon.ui.KuroAlertDialog
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.KuroFieldShape
import tachiyomi.presentation.core.components.material.KuroTextButton
import tachiyomi.presentation.core.components.material.kuroFieldColors
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ExtensionStoreCreateDialog(
    onDismissRequest: () -> Unit,
    onCreate: (String) -> Unit,
    storeIndexUrls: Set<String>,
    processing: Boolean,
    errorMessage: String?,
) {
    val context = LocalContext.current
    val state = rememberTextFieldState()
    val storeAlreadyExists by remember(storeIndexUrls) {
        derivedStateOf {
            val indexUrl = StoreLink.normalize(state.text.toString())
            storeIndexUrls.contains(indexUrl)
        }
    }

    val focusRequester = remember { FocusRequester() }

    KuroAlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(MR.strings.extensionStoresScreen_addStore_title))
        },
        text = {
            OutlinedTextField(
                shape = KuroFieldShape,
                colors = kuroFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                state = state,
                label = {
                    Text(text = stringResource(MR.strings.extensionStoresScreen_addStoreInput_inputLabel))
                },
                supportingText = {
                    val msgRes = if (storeAlreadyExists) {
                        MR.strings.extensionStoresScreen_addStore_alreadyExists
                    } else {
                        MR.strings.extensionStoresScreen_addStoreInput_hint
                    }
                    Text(text = errorMessage ?: stringResource(msgRes))
                },
                trailingIcon = {
                    KuroTextButton(
                        onClick = {
                            val clip = context.getSystemService<ClipboardManager>()
                                ?.primaryClip
                                ?.takeIf { it.itemCount > 0 }
                                ?.getItemAt(0)
                                ?.coerceToText(context)
                                ?.toString()
                                ?.trim()
                            if (!clip.isNullOrEmpty()) state.setTextAndPlaceCursorAtEnd(clip)
                        },
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        Text(text = stringResource(MR.strings.action_paste))
                    }
                },
                isError = errorMessage != null || storeAlreadyExists,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                lineLimits = TextFieldLineLimits.SingleLine,
            )
        },
        confirmButton = {
            KuroTextButton(
                onClick = { onCreate(state.text.toString()) },
                enabled = !processing && state.text.isNotEmpty() && !storeAlreadyExists,
            ) {
                Text(
                    text = stringResource(
                        resource = if (processing) {
                            MR.strings.extensionStoresScreen_addStore_processing
                        } else {
                            MR.strings.action_add
                        },
                    ),
                )
            }
        },
        dismissButton = {
            KuroTextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
}

@Composable
fun ExtensionStoreDeleteDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    storeName: String,
    storeIndexUrl: String,
) {
    KuroAlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(MR.strings.extensionStoresScreen_deleteStore_title))
        },
        text = {
            Text(text = stringResource(MR.strings.extensionStoresScreen_deleteStore_body, storeName, storeIndexUrl))
        },
        confirmButton = {
            KuroTextButton(onClick = {
                onDelete()
                onDismissRequest()
            }) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            KuroTextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
fun ExtensionStoreConfirmDialog(
    onDismissRequest: () -> Unit,
    onCreate: () -> Unit,
    storeIndexUrl: String,
    storeAlreadyExists: Boolean,
    processing: Boolean,
    errorMessage: String?,
) {
    val state = rememberTextFieldState(initialText = storeIndexUrl)
    KuroAlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(MR.strings.extensionStoresScreen_addStore_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = stringResource(MR.strings.extensionStoresScreen_addStoreDeeplink_bodyText))
                OutlinedTextField(
                    shape = KuroFieldShape,
                    colors = kuroFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    state = state,
                    readOnly = true,
                    supportingText = when {
                        storeAlreadyExists -> {
                            {
                                Text(text = stringResource(MR.strings.extensionStoresScreen_addStore_alreadyExists))
                            }
                        }
                        errorMessage != null -> {
                            {
                                Text(text = errorMessage)
                            }
                        }
                        else -> null
                    },
                    isError = errorMessage != null || storeAlreadyExists,
                )
            }
        },
        confirmButton = {
            KuroTextButton(onClick = onCreate, enabled = !storeAlreadyExists && !processing) {
                Text(
                    text = stringResource(
                        resource = if (processing) {
                            MR.strings.extensionStoresScreen_addStore_processing
                        } else {
                            MR.strings.action_add
                        },
                    ),
                )
            }
        },
        dismissButton = {
            KuroTextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}
