package kurotoon.manga

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import java.io.File

/** A short note per bookmarked chapter, by chapter id (mihon#1509). Kept on the device. */
class BookmarkNotes private constructor(private val file: File) {

    private val json = Json { ignoreUnknownKeys = true }
    private val _notes = MutableStateFlow(read())
    val notes: StateFlow<Map<Long, String>> = _notes.asStateFlow()

    private fun read(): Map<Long, String> = runCatching {
        if (file.exists()) json.decodeFromString<Map<Long, String>>(file.readText()) else emptyMap()
    }.getOrDefault(emptyMap())

    fun get(chapterId: Long): String? = _notes.value[chapterId]

    /** A blank [note] removes it. */
    fun set(chapterId: Long, note: String) {
        val clean = note.trim().take(MAX_LENGTH)
        _notes.update { if (clean.isEmpty()) it - chapterId else it + (chapterId to clean) }
        synchronized(this) {
            runCatching { file.writeText(json.encodeToString(_notes.value)) }
        }
    }

    companion object {
        const val MAX_LENGTH = 280

        @Volatile private var instance: BookmarkNotes? = null

        fun get(context: Context): BookmarkNotes = instance ?: synchronized(this) {
            instance ?: BookmarkNotes(File(context.applicationContext.filesDir, "kurotoon_bookmark_notes.json"))
                .also { instance = it }
        }
    }
}
