package kurotoon.catalogue

/**
 * Turns a saved search into a feed: titles that weren't in its results the last time it was
 * loaded count as new. The first load only learns what's there, so nothing is new yet.
 */
object SearchFeed {

    /** How many urls to remember per search; older ones are forgotten first. */
    const val MEMORY = 200

    fun fresh(seen: List<String>, urls: List<String>): Set<String> =
        if (seen.isEmpty()) emptySet() else urls.toSet() - seen.toSet()

    /** [seen] with [urls] added at the front, capped at [cap]. */
    fun remember(seen: List<String>, urls: List<String>, cap: Int = MEMORY): List<String> =
        (urls + seen).distinct().take(cap)

    fun encode(urls: List<String>): String = urls.joinToString("\n")

    fun decode(raw: String): List<String> = raw.split('\n').filter { it.isNotEmpty() }
}
