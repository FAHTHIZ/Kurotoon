package kurotoon.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kurotoon.ui.kuroCard
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Public
import mihon.icons.materialsymbols.rounded.Search
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** The web app's "Universal Search" card: one query across every installed source. */
@Composable
fun UniversalSearchCard(
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    recent: List<String> = emptyList(),
    onClearRecent: () -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .kuroCard(shape)
            .background(
                Brush.radialGradient(listOf(primary.copy(alpha = 0.14f), Color.Transparent)),
                shape,
            )
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                MaterialSymbols.Rounded.Public,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(MR.strings.kurotoon_browse_universalSearch),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
            )
        }
        Text(
            text = stringResource(MR.strings.kurotoon_browse_universalSearchBody),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        Row(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                MaterialSymbols.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(MR.strings.kurotoon_browse_universalSearchHint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { query.trim().takeIf { it.isNotEmpty() }?.let(onSearch) },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        RecentSearchRow(
            recent = recent,
            onSearch = onSearch,
            onClear = onClearRecent,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
