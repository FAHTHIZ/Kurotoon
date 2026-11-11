package kurotoon.manga

/** Display-only tidying of chapter names sources give us. The stored name is never changed. */
object ChapterTitles {

    private val TRAILING_NUMBER = Regex(
        """\s*\((?:ch|chap|chapter)\.?\s*(\d+(?:\.\d+)?)\)\s*$""",
        RegexOption.IGNORE_CASE,
    )
    private val NUMBER = Regex("""\d+(?:\.\d+)?""")

    /** "Episode 42 (ch. 42)" reads as "Episode 42"; names without a repeated number stay as they are. */
    fun tidy(name: String): String {
        val match = TRAILING_NUMBER.find(name) ?: return name
        val head = name.substring(0, match.range.first)
        val number = match.groupValues[1].toDouble()
        val repeated = NUMBER.findAll(head).any { it.value.toDouble() == number }
        return if (repeated && head.isNotBlank()) head.trimEnd() else name
    }
}
