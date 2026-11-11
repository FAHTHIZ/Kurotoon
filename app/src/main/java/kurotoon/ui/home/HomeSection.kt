package kurotoon.ui.home

/** Parts of Home the reader can switch off in Settings > Appearance. Keys are stored, so keep them stable. */
enum class HomeSection(val key: String) {
    HERO("hero"),
    SHORTCUTS("shortcuts"),
    CONTINUE("continue"),
    NEW_CHAPTERS("new"),
    SCHEDULE("schedule"),
    STALLED("stalled"),
    BINGE("binge"),
    PANELS("panels"),
    PICKS("picks"),
    SAVED("saved"),
    SOURCES("sources"),
}
