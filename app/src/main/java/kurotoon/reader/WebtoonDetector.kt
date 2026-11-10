package kurotoon.reader

/**
 * Guesses whether a series is a long-strip comic (webtoon, manhwa, manhua) from its genre tags and
 * the name of the source it comes from. Used to open such series in the webtoon reader when the user
 * hasn't picked a reading mode for them.
 */
object WebtoonDetector {

    private val genreHints = listOf(
        "webtoon",
        "web toon",
        "manhwa",
        "manhua",
        "long strip",
        "longstrip",
        "vertical",
        "webcomic",
        "web comic",
        "full color",
        "full colour",
    )

    private val sourceHints = listOf("webtoon", "toon", "manhwa", "manhua", "asura", "flame", "reaper")

    fun isLongStrip(genres: List<String>?, sourceName: String?): Boolean {
        val tags = genres.orEmpty().map { it.trim().lowercase() }
        if (tags.any { tag -> genreHints.any { tag.contains(it) } }) return true
        val source = sourceName?.lowercase() ?: return false
        return sourceHints.any { source.contains(it) }
    }
}
