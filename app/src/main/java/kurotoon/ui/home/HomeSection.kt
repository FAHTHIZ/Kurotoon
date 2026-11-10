package kurotoon.ui.home

/** Parts of Home the reader can switch off in Settings > Appearance. Keys are stored, so keep them stable. */
enum class HomeSection(val key: String) {
    HERO("hero"),
    SHORTCUTS("shortcuts"),
    CONTINUE("continue"),
    NEW_CHAPTERS("new"),
    STALLED("stalled"),
    BINGE("binge"),
    PICKS("picks"),
    SAVED("saved"),
    SOURCES("sources"),
}
