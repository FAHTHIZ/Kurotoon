package kurotoon.catalogue

/** Extra things to do with a tag tapped on a series page. */
enum class TagAction {
    /** Look the tag up in every installed source. */
    SearchEverywhere,

    /** Add the tag to the hidden tags. */
    Hide,
}

/** Adds [tag] to a comma-separated hidden-tags value, keeping it unchanged if already there. */
fun addHiddenTag(raw: String, tag: String): String {
    val name = tag.trim()
    if (name.isEmpty() || HiddenTags.parse(raw).contains(name.lowercase())) return raw
    return listOf(raw.trim().trimEnd(','), name).filter { it.isNotEmpty() }.joinToString(", ")
}
