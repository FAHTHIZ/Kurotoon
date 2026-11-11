package kurotoon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Kuro, the mascot, offering a short hint. */
@Composable
fun KuroTip(text: String, modifier: Modifier = Modifier, mood: String = "😺") {
    Row(
        modifier = modifier
            .kuroCard(RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = mood, fontSize = 22.sp)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The elevated card surface used across KuroToon screens. */
@Composable
fun Modifier.kuroCard(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    borderColor: Color = Color.White.copy(alpha = 0.06f),
): Modifier = this
    .background(MaterialTheme.colorScheme.surfaceContainer, shape)
    .border(1.dp, borderColor, shape)

/** Section heading in the small uppercase style of the web app. */
@Composable
fun KuroSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * List section header used on Updates, Sources and other long lists: a short accent bar followed by
 * the [KuroSectionTitle] style, with an optional [count] pill.
 */
@Composable
fun KuroSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    Row(
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 14.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
                    ),
                    CircleShape,
                ),
        )
        KuroSectionTitle(text = text, modifier = Modifier.padding(start = 8.dp))
        if (count != null) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    .padding(horizontal = 7.dp, vertical = 1.dp),
            )
        }
    }
}
