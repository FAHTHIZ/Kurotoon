package tachiyomi.presentation.core.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import dev.icerock.moko.resources.StringResource
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

data class EmptyScreenAction(
    val stringRes: StringResource,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun EmptyScreen(
    stringRes: StringResource,
    modifier: Modifier = Modifier,
    actions: List<EmptyScreenAction>? = null,
) {
    EmptyScreen(
        message = stringResource(stringRes),
        modifier = modifier,
        actions = actions,
    )
}

@Composable
fun EmptyScreen(
    message: String,
    modifier: Modifier = Modifier,
    actions: List<EmptyScreenAction>? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyStateArt()

        Text(
            text = message,
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (!actions.isNullOrEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                actions.fastForEach {
                    FilledTonalButton(onClick = it.onClick) {
                        Icon(
                            imageVector = it.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = stringResource(it.stringRes))
                    }
                }
            }
        }
    }
}
