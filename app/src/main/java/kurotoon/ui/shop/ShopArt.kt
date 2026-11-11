package kurotoon.ui.shop

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kurotoon.shop.ShopItem
import kurotoon.shop.ShopKind
import kurotoon.ui.KuroColors
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Person
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** The KuroToon coin, the currency of the shop. */
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
            lineHeight = (size.value * 0.6f).sp,
        )
    }
}

/** A coin followed by an amount, e.g. "+30". */
@Composable
fun KuroAmount(text: String, modifier: Modifier = Modifier, coin: Dp = 14.dp) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KuroCoin(size = coin)
        Text(text = text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

/**
 * A preview of any shop item. Pack images win; without one the item is drawn from its
 * emoji, colours and style, so a pack can ship with no images at all.
 */
@Composable
fun ShopItemArt(item: ShopItem, modifier: Modifier = Modifier, animated: Boolean = true) {
    when (item.kind) {
        ShopKind.AVATAR -> KuroAvatar(
            avatar = item,
            frame = null,
            modifier = modifier,
            animated = animated,
            ring = false,
        )
        ShopKind.FRAME -> KuroAvatar(avatar = null, frame = item, modifier = modifier, animated = animated)
        ShopKind.THEME -> ThemeSwatch(item, modifier)
        ShopKind.PARTICLES -> ParticleSwatch(item.colors, modifier, animated)
        ShopKind.STICKER, ShopKind.BADGE, ShopKind.BOOST -> Box(modifier, contentAlignment = Alignment.Center) {
            when {
                item.image != null -> AsyncImage(
                    model = item.image,
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                item.kind == ShopKind.BADGE -> KuroBadge(item, Modifier.fillMaxSize(0.8f))
                item.kind == ShopKind.BOOST -> BoostArt(Modifier.fillMaxSize(0.8f))
                else -> EmojiArt(item.emoji ?: "✨", Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun EmojiArt(emoji: String, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        // Sized from the box, so the emoji fills tiles and chips alike
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
            Text(
                text = emoji,
                fontSize = (minOf(maxWidth, maxHeight).value * 0.62f).sp,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

/** A badge: an emoji on a gradient shield. */
@Composable
fun KuroBadge(item: ShopItem, modifier: Modifier = Modifier) {
    val colors = item.colors.ifEmpty { listOf(KuroColors.Crimson, KuroColors.Galaxy) }
    Box(
        modifier = modifier
            .drawBehind {
                val path = shield(size.width, size.height)
                drawPath(path, Brush.linearGradient(if (colors.size > 1) colors else colors + colors))
                drawPath(path, Color.White.copy(alpha = 0.35f), style = Stroke(size.minDimension * 0.05f))
            },
        contentAlignment = Alignment.Center,
    ) {
        if (item.image != null) {
            AsyncImage(item.image, item.name, Modifier.fillMaxSize(0.7f), contentScale = ContentScale.Fit)
        } else {
            EmojiArt(item.emoji ?: "★", Modifier.fillMaxSize(0.8f))
        }
    }
}

private fun shield(w: Float, h: Float) = Path().apply {
    moveTo(w / 2, 0f)
    lineTo(w, h * 0.18f)
    lineTo(w * 0.94f, h * 0.62f)
    quadraticTo(w * 0.8f, h * 0.88f, w / 2, h)
    quadraticTo(w * 0.2f, h * 0.88f, w * 0.06f, h * 0.62f)
    lineTo(0f, h * 0.18f)
    close()
}

@Composable
private fun BoostArt(modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
            val coin = minOf(maxWidth, maxHeight) * 0.62f
            KuroCoin(Modifier.align(Alignment.TopStart), size = coin)
            KuroCoin(Modifier.align(Alignment.BottomEnd), size = coin)
            Text(
                "×2",
                modifier = Modifier.align(Alignment.Center),
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontSize = (coin.value * 0.5f).sp,
            )
        }
    }
}

/** A tiny mock of the app in the theme's colours. */
@Composable
private fun ThemeSwatch(item: ShopItem, modifier: Modifier) {
    val main = item.colors.getOrNull(0) ?: KuroColors.Crimson
    val deep = item.colors.getOrNull(1) ?: main
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(main.copy(alpha = 0.28f), Color(0xFF0D0B12))))
            .border(1.dp, main.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
    ) {
        if (item.image != null) {
            AsyncImage(item.image, item.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                val w = size.width
                val h = size.height
                drawRoundRect(
                    Color.White.copy(alpha = 0.14f),
                    size = androidx.compose.ui.geometry.Size(w, h * 0.14f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(h),
                )
                val cell = (w - 8.dp.toPx()) / 3
                repeat(3) { i ->
                    drawRoundRect(
                        Brush.verticalGradient(listOf(main.copy(alpha = 0.7f), deep.copy(alpha = 0.25f))),
                        topLeft = Offset(i * (cell + 4.dp.toPx()), h * 0.24f),
                        size = androidx.compose.ui.geometry.Size(cell, h * 0.48f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                    )
                }
                drawRoundRect(
                    Brush.horizontalGradient(listOf(main, deep)),
                    topLeft = Offset(w * 0.3f, h * 0.84f),
                    size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.16f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(h),
                )
            }
        }
    }
}

/** Twinkling sparks in the set's colours. */
@Composable
private fun ParticleSwatch(colors: List<Color>, modifier: Modifier, animated: Boolean) {
    val palette = colors.ifEmpty { listOf(KuroColors.Galaxy, KuroColors.Crimson, KuroColors.Aurora) }
    val time by if (animated) {
        rememberInfiniteTransition(label = "sparks").animateFloat(
            0f,
            1f,
            infiniteRepeatable(tween(3200, easing = LinearEasing)),
            label = "sparksTime",
        )
    } else {
        remember { mutableFloatStateOf(0.3f) }
    }
    val seeds = remember(palette) { List(18) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat()) } }
    Canvas(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0D0B12)),
    ) {
        seeds.forEachIndexed { i, (sx, sy, phase) ->
            val y = ((sy - time * (0.3f + phase * 0.4f)) % 1f + 1f) % 1f
            val twinkle = 0.5f + 0.5f * sin((time + phase) * 2 * PI.toFloat() * 2)
            val c = palette[i % palette.size]
            val r = size.minDimension * (0.02f + phase * 0.03f)
            val center = Offset(sx * size.width, y * size.height)
            drawCircle(c.copy(alpha = 0.25f * twinkle), radius = r * 3, center = center)
            drawCircle(c.copy(alpha = 0.6f + 0.4f * twinkle), radius = r, center = center)
        }
    }
}

/**
 * The reader's avatar with its frame. Either can be null: no avatar shows the first letter of
 * [initial] (or a person glyph), no frame shows a soft ring.
 */
@Composable
fun KuroAvatar(
    avatar: ShopItem?,
    frame: ShopItem?,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    ring: Boolean = true,
    initial: String? = null,
) {
    val spin by if (animated && frame != null) {
        rememberInfiniteTransition(label = "frame").animateFloat(
            0f,
            1f,
            infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
            label = "frameSpin",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        val accent = MaterialTheme.colorScheme.outlineVariant
        val plain = MaterialTheme.colorScheme.surfaceContainerHighest
        val onPlain = MaterialTheme.colorScheme.onSurfaceVariant
        Canvas(Modifier.fillMaxSize()) {
            if (frame == null && ring) {
                drawCircle(
                    accent,
                    radius = size.minDimension / 2 - 2.dp.toPx(),
                    style = Stroke(2.dp.toPx()),
                )
            } else if (frame != null && frame.image == null) {
                drawFrame(frame, spin)
            }
        }
        Box(
            Modifier
                .fillMaxSize(0.8f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        (avatar?.colors?.takeIf { it.size >= 2 }?.take(2)?.map { it.copy(alpha = 0.45f) })
                            ?: listOf(plain, plain),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                avatar?.image != null -> AsyncImage(
                    avatar.image,
                    avatar.name,
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                avatar?.emoji != null -> EmojiArt(avatar.emoji, Modifier.fillMaxSize())
                // No avatar picked yet: a plain letter, like most apps
                else -> {
                    val letter = initial?.trim()?.firstOrNull()?.uppercaseChar()
                    if (letter != null) {
                        Text(
                            letter.toString(),
                            color = onPlain,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    } else {
                        Icon(
                            MaterialSymbols.Rounded.Person,
                            contentDescription = null,
                            tint = onPlain,
                            modifier = Modifier.fillMaxSize(0.5f),
                        )
                    }
                }
            }
        }
        if (frame?.image != null) {
            // Image frames are drawn on top, like a picture frame with a transparent middle
            AsyncImage(
                frame.image,
                frame.name,
                Modifier.fillMaxSize().graphicsLayer { if (frame.style == "spin") rotationZ = spin * 360f },
                contentScale = ContentScale.Fit,
            )
        }
    }
}

private fun DrawScope.drawFrame(frame: ShopItem, spin: Float) {
    val colors = frame.colors.ifEmpty { listOf(KuroColors.Crimson, KuroColors.Galaxy, KuroColors.Aurora) }
    val radius = size.minDimension / 2
    val ring = radius * 0.86f
    val stroke = radius * 0.1f
    val sweep = Brush.sweepGradient(colors + colors.first(), center)
    when (frame.style) {
        "starfield" -> {
            drawCircle(sweep, ring, style = Stroke(stroke * 0.5f))
            repeat(10) { i ->
                val a = (i / 10f + spin) * 2 * PI.toFloat()
                val r = ring + stroke * 0.4f * sin(a * 3)
                val p = Offset(center.x + cos(a) * r, center.y + sin(a) * r)
                drawCircle(Color.White.copy(alpha = 0.3f), radius = stroke * 0.7f, center = p)
                drawCircle(colors[i % colors.size], radius = stroke * 0.32f, center = p)
            }
        }
        "coins" -> {
            drawCircle(KuroColors.Gold.copy(alpha = 0.7f), ring, style = Stroke(stroke * 0.4f))
            repeat(8) { i ->
                val a = (i / 8f + spin) * 2 * PI.toFloat()
                val p = Offset(center.x + cos(a) * ring, center.y + sin(a) * ring)
                drawCircle(
                    Brush.linearGradient(
                        listOf(Color(0xFFFFE08A), KuroColors.Gold, Color(0xFFC9932B)),
                        p - Offset(stroke, stroke),
                        p + Offset(stroke, stroke),
                    ),
                    radius = stroke * 0.9f,
                    center = p,
                )
            }
        }
        "nebula" -> rotate(spin * 360f) {
            drawCircle(sweep, ring + stroke * 0.3f, alpha = 0.35f, style = Stroke(stroke * 2.2f))
            drawCircle(sweep, ring, style = Stroke(stroke * 0.8f))
        }
        "mythic" -> {
            rotate(spin * 360f) { drawCircle(sweep, ring, style = Stroke(stroke)) }
            rotate(-spin * 720f) {
                drawCircle(sweep, ring + stroke * 0.9f, alpha = 0.5f, style = Stroke(stroke * 0.3f))
            }
            repeat(4) { i ->
                val a = (i / 4f - spin) * 2 * PI.toFloat()
                val p = Offset(center.x + cos(a) * (ring + stroke * 0.9f), center.y + sin(a) * (ring + stroke * 0.9f))
                drawCircle(Color.White, radius = stroke * 0.28f, center = p)
            }
        }
        "aurora" -> {
            repeat(3) { i ->
                val wobble = sin((spin + i / 3f) * 2 * PI.toFloat()) * stroke * 0.5f
                drawCircle(colors[i % colors.size], ring + wobble, alpha = 0.55f, style = Stroke(stroke * 0.5f))
            }
        }
        else -> rotate(spin * 360f) { drawCircle(sweep, ring, style = Stroke(stroke)) }
    }
}
