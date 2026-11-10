package kurotoon.shop

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The `pack.json` at the root of every shop pack. A pack is a folder (or a `.kpack` zip of that
 * folder) holding this file and the images its items point to. See `docs/shop-packs.md`.
 */
@Serializable
data class PackManifest(
    val id: String,
    val name: String,
    val author: String = "",
    val version: Int = 1,
    val description: String = "",
    /** Image shown on the pack's banner in the shop, relative to the pack folder. */
    val cover: String? = null,
    val items: List<PackItemManifest> = emptyList(),
)

@Serializable
data class PackItemManifest(
    val id: String,
    val kind: String,
    val name: String,
    val description: String = "",
    val price: Int = 0,
    val rarity: String = "common",
    /** Picture for the item, relative to the pack folder. PNG, WebP, JPEG or GIF. */
    val image: String? = null,
    /** Shown when there's no image (stickers and badges are often just an emoji). */
    val emoji: String? = null,
    /** Hex colours, e.g. "#FF4D4D". Themes use [accent, deep]; frames, particles and badges use them as a palette. */
    val colors: List<String> = emptyList(),
    /** Built-in look for items without an image, e.g. "nebula" for a frame or "kuro" for an avatar. */
    val style: String? = null,
)

enum class ShopKind(val key: String) {
    AVATAR("avatar"),
    FRAME("frame"),
    THEME("theme"),
    PARTICLES("particles"),
    STICKER("sticker"),
    BADGE("badge"),
    BOOST("boost"),
    ;

    /** Kinds that can be worn, one at a time (badges allow up to [MAX_BADGES]). */
    val equippable: Boolean get() = this != BOOST

    companion object {
        fun of(key: String): ShopKind? = entries.firstOrNull { it.key.equals(key, ignoreCase = true) }
    }
}

const val MAX_BADGES = 3

enum class Rarity(val key: String, val color: Color) {
    COMMON("common", Color(0xFF9CA3AF)),
    RARE("rare", Color(0xFF2FB0E6)),
    EPIC("epic", Color(0xFFA35BE8)),
    LEGENDARY("legendary", Color(0xFFF2C14E)),
    MYTHIC("mythic", Color(0xFFFF4D4D)),
    ;

    companion object {
        fun of(key: String): Rarity = entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: COMMON
    }
}

/** Where a pack came from: shipped inside the app, or imported by the user. */
enum class PackOrigin { BUILT_IN, IMPORTED }

@Immutable
data class Pack(
    val id: String,
    val name: String,
    val author: String,
    val version: Int,
    val description: String,
    val origin: PackOrigin,
    /** Something Coil can load: a `file:///android_asset/…` URI or an absolute file path. */
    val cover: String?,
    val items: List<ShopItem>,
)

@Immutable
data class ShopItem(
    /** Unique across packs: `<pack id>/<item id>`. */
    val code: String,
    val packId: String,
    val kind: ShopKind,
    val name: String,
    val description: String,
    val price: Int,
    val rarity: Rarity,
    val image: String?,
    val emoji: String?,
    val colors: List<Color>,
    val style: String?,
) {
    val isFree: Boolean get() = price <= 0
}

internal val packJson = Json {
    ignoreUnknownKeys = true
}

private val idPattern = Regex("[A-Za-z0-9._-]{1,64}")

/** Whether [id] is safe to use as a pack or item id (and as a folder name). */
internal fun isValidId(id: String): Boolean = idPattern.matches(id)

/**
 * Turns a manifest into a [Pack], dropping items it can't use (unknown kind, bad id, a path that
 * escapes the pack). [resolve] maps a path inside the pack to something Coil can load.
 */
internal fun PackManifest.toPack(origin: PackOrigin, resolve: (String) -> String): Pack? {
    if (!isValidId(id)) return null
    val seen = HashSet<String>()
    val parsed = items.mapNotNull { item ->
        val kind = ShopKind.of(item.kind) ?: return@mapNotNull null
        if (!isValidId(item.id) || !seen.add(item.id)) return@mapNotNull null
        ShopItem(
            code = "$id/${item.id}",
            packId = id,
            kind = kind,
            name = item.name.take(48),
            description = item.description.take(160),
            price = item.price.coerceIn(0, 1_000_000),
            rarity = Rarity.of(item.rarity),
            image = item.image?.let(::safeRelativePath)?.let(resolve),
            emoji = item.emoji?.take(8),
            colors = item.colors.mapNotNull(::parseHexColor),
            style = item.style,
        )
    }
    return Pack(
        id = id,
        name = name.take(48),
        author = author.take(48),
        version = version,
        description = description.take(240),
        origin = origin,
        cover = cover?.let(::safeRelativePath)?.let(resolve),
        items = parsed,
    )
}

/** Rejects absolute paths and `..` so a pack can only point at its own files. */
internal fun safeRelativePath(path: String): String? {
    val clean = path.trim().replace('\\', '/').trimStart('/')
    if (clean.isEmpty() || clean.split('/').any { it == ".." || it == "." || it.isEmpty() }) return null
    return clean
}

/** Parses "#RGB", "#RRGGBB" or "#AARRGGBB". */
internal fun parseHexColor(hex: String): Color? {
    val raw = hex.trim().removePrefix("#")
    val expanded = when (raw.length) {
        3 -> "FF" + raw.map { "$it$it" }.joinToString("")
        6 -> "FF$raw"
        8 -> raw
        else -> return null
    }
    return expanded.toLongOrNull(16)?.let { Color(it.toInt()) }
}
