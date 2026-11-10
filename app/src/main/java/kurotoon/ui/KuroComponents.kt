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

/** The gold "K" coin used next to every Kuro Points amount. */
@Composable
fun KuroCoin(modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                Brush.linearGradient(listOf(Color(0xFFFFE08A), KuroColors.Gold, Color(0xFFC9932B))),
                CircleShape,
            )
            .border(1.dp, Color(0x33000000), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "K",
            color = Color(0xFF3A2600),
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.58f).sp,
        )
    }
}

/** A coin followed by an amount, e.g. "+30". */
@Composable
fun KuroAmount(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KuroCoin(size = 14.dp)
        Text(text = text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

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
