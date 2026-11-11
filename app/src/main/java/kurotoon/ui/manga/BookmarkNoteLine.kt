package kurotoon.ui.manga

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import kurotoon.manga.BookmarkNotes

/** The note on a bookmarked chapter, or null. Notes on un-bookmarked chapters stay hidden. */
@Composable
fun bookmarkNoteOf(chapterId: Long, bookmarked: Boolean): String? {
    if (!bookmarked || LocalInspectionMode.current) return null
    val context = LocalContext.current
    val store = remember { BookmarkNotes.get(context) }
    val notes by store.notes.collectAsState()
    return notes[chapterId]
}
