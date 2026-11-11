package eu.kanade.tachiyomi.ui.library

import tachiyomi.domain.library.model.LibraryManga

data class LibraryItem(
    val libraryManga: LibraryManga,
    val downloadCount: Int,
    val unreadCount: Long,
    val isLocal: Boolean,
    val sourceName: String,
    val sourceLanguage: String,
    val badges: Badges,
    /** The source's name as written, unlike [sourceName] which is lowercased for searching. */
    val sourceTitle: String = sourceName,
) {
    val id: Long = libraryManga.id

    data class Badges(
        val downloadCount: Int,
        val unreadCount: Long,
        val isLocal: Boolean,
        val sourceLanguage: String,
        /** Empty unless the source badge is turned on. */
        val sourceName: String = "",
        /** How many library entries this cover stands for when copies are combined. */
        val copies: Int = 1,
    )
}
