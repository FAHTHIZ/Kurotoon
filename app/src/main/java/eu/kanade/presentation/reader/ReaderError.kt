package eu.kanade.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kurotoon.ui.KuroPrimaryButton
import kurotoon.ui.KuroTonalButton
import kurotoon.ui.kuroCard
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Image
import mihon.icons.materialsymbols.rounded.Public
import mihon.icons.materialsymbols.rounded.Refresh
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A page that failed to load or decode: the message, Retry, and Open in WebView when [onOpenInWebView] is given.
 *
 * [onButtonPressChange] tells a viewer that reads every touch for its own taps when a button is held.
 */
@Composable
fun ReaderError(
    message: String?,
    onRetry: () -> Unit,
    onOpenInWebView: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onButtonPressChange: ((pressed: Boolean) -> Unit)? = null,
) {
    val buttonModifier = if (onButtonPressChange != null) Modifier.reportPress(onButtonPressChange) else Modifier
    val accent = MaterialTheme.colorScheme.primary
    Box(modifier = modifier.padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .kuroCard(RoundedCornerShape(24.dp))
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(accent.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Image,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = stringResource(MR.strings.kurotoon_reader_pageFailed),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message ?: stringResource(MR.strings.decode_image_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KuroPrimaryButton(
                    text = stringResource(MR.strings.action_retry),
                    onClick = onRetry,
                    icon = MaterialSymbols.Rounded.Refresh,
                    modifier = buttonModifier.fillMaxWidth(),
                )
                if (onOpenInWebView != null) {
                    KuroTonalButton(
                        text = stringResource(MR.strings.kurotoon_reader_openWeb),
                        onClick = onOpenInWebView,
                        icon = MaterialSymbols.Rounded.Public,
                        modifier = buttonModifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

internal fun Modifier.reportPress(onPressChange: (Boolean) -> Unit) = pointerInput(onPressChange) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        onPressChange(true)
        waitForUpOrCancellation(pass = PointerEventPass.Initial)
        onPressChange(false)
    }
}
