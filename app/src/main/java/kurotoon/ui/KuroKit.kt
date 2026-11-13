package kurotoon.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.theme.KuroFontFamily
import kurotoon.icons.materialsymbols.MaterialSymbols
import kurotoon.icons.materialsymbols.roundedfilled.Star

/*
 * KuroToon's shared building blocks. Every redesigned screen draws its buttons, badges and scrims from here, so
 * the app reads as one product: solid accent pills for the one thing to do next, tonal pills for everything else,
 * gold stars for ratings and a dark ink scrim wherever text sits on artwork.
 */

/** The single call to action on a surface: a solid accent pill. */
@Composable
fun KuroPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 44.dp,
) {
    KuroPill(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        height = height,
        container = MaterialTheme.colorScheme.primary,
        content = MaterialTheme.colorScheme.onPrimary,
    )
}

/** A secondary action: a tonal pill that sits quietly next to [KuroPrimaryButton]. */
@Composable
fun KuroTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 44.dp,
    onArtwork: Boolean = false,
) {
    KuroPill(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        height = height,
        container = if (onArtwork) {
            Color.White.copy(
                alpha = 0.16f,
            )
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        content = if (onArtwork) Color.White else MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun KuroPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    icon: ImageVector?,
    height: Dp,
    container: Color,
    content: Color,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .kuroPressScale(interaction, pressed = 0.94f)
            .height(height)
            .clip(CircleShape)
            .background(container)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
            .padding(start = if (icon != null) 16.dp else 22.dp, end = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
            Box(Modifier.size(6.dp))
        }
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A small label pill: NEW, UP, ×3, One-shot. Solid accent by default. */
@Composable
fun KuroTag(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Text(
        text = text,
        color = content,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(container)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** A gold star and score on a dark glass chip, for the corner of a cover. */
@Composable
fun KuroRatingBadge(score: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(start = 5.dp, end = 7.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            MaterialSymbols.RoundedFilled.Star,
            contentDescription = null,
            tint = KuroColors.Gold,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = score,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 3.dp),
        )
    }
}

/** Ink fading up from the bottom of artwork, so text on top stays readable on any cover. */
fun Modifier.kuroBottomScrim(start: Float = 0.35f, strength: Float = 0.92f): Modifier = background(
    Brush.verticalGradient(
        start to Color.Transparent,
        ((1f + start) / 2f) to Color.Black.copy(alpha = strength * 0.55f),
        1f to Color.Black.copy(alpha = strength),
    ),
)

/** Text style for type laid over artwork: white with a soft shadow. */
@Composable
fun onArtwork(style: TextStyle): TextStyle = style.copy(
    color = Color.White,
    shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 12f),
)

/**
 * The big outlined ranking numeral that sits behind the bottom-left of a cover in Top lists, like the ranked
 * shelves on MANGA Plus and streaming apps.
 */
@Composable
fun BoxScope.KuroRankNumeral(rank: Int, modifier: Modifier = Modifier) {
    val color = if (rank <= 3) MaterialTheme.colorScheme.primary else Color.White
    val base = TextStyle(
        fontFamily = KuroFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 64.sp,
        lineHeight = 64.sp,
        letterSpacing = (-4).sp,
    )
    Box(modifier.align(Alignment.BottomStart)) {
        Text(
            text = rank.toString(),
            style = base.copy(drawStyle = Stroke(width = 6f)),
            color = Color.Black.copy(alpha = 0.85f),
        )
        Text(text = rank.toString(), style = base, color = color)
    }
}

/** A row of quiet meta facts separated by dots: "Ongoing · 214 chapters · Action". */
@Composable
fun KuroMetaLine(
    parts: List<String>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    style: TextStyle = MaterialTheme.typography.labelMedium,
) {
    val visible = parts.filter { it.isNotBlank() }
    if (visible.isEmpty()) return
    Text(
        text = visible.joinToString("  ·  "),
        color = color,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** Horizontal group with the kit's standard gap. */
@Composable
fun KuroButtonRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * A filter pill: solid accent when on, a quiet filled pill when off. The colour change is animated and a
 * press squishes it slightly. Replaces Material's outlined FilterChip on KuroToon screens.
 */
@Composable
fun KuroFilterPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    icon: ImageVector? = null,
) {
    val container by androidx.compose.animation.animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "kuroFilterPill",
    )
    val content by androidx.compose.animation.animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        label = "kuroFilterPillContent",
    )
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .kuroPressScale(interaction, pressed = 0.93f)
            .height(36.dp)
            .clip(CircleShape)
            .background(container)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick)
            .padding(start = if (icon != null) 12.dp else 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        }
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
        )
        if (count != null && count > 0) {
            Text(
                text = count.toString(),
                color = content.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}
