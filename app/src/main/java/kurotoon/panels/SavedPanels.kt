package kurotoon.panels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream

/** One page kept from the reader: the picture and where it came from. */
@Serializable
data class SavedPanel(
    val id: Long,
    val mangaId: Long,
    val chapterId: Long,
    val mangaTitle: String,
    val chapterName: String,
    val page: Int,
    val savedAt: Long,
    val file: String,
    val width: Int = 0,
    val height: Int = 0,
)

/**
 * Saved panels, the bookmarks-with-pictures from Kotatsu: long-press a page, keep it, and find it again in a
 * gallery that opens straight back to the chapter. Pictures live in the app's own files so nothing touches the
 * database or the shared gallery.
 */
class SavedPanels(context: Context) {

    private val dir = File(context.filesDir, "kurotoon_panels").apply { mkdirs() }
    private val index = File(dir, "index.json")
    private val json = Json { ignoreUnknownKeys = true }

    private val _panels = MutableStateFlow(load())
    val panels: StateFlow<List<SavedPanel>> = _panels.asStateFlow()

    fun fileOf(panel: SavedPanel) = File(dir, panel.file)

    fun isSaved(chapterId: Long, page: Int) = _panels.value.any { it.chapterId == chapterId && it.page == page }

    suspend fun save(
        mangaId: Long,
        chapterId: Long,
        mangaTitle: String,
        chapterName: String,
        page: Int,
        open: () -> InputStream,
    ): SavedPanel? = withContext(Dispatchers.IO) {
        _panels.value.firstOrNull { it.chapterId == chapterId && it.page == page }?.let { return@withContext it }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open().use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return@withContext null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_WIDTH) sample *= 2
        val bitmap = open().use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return@withContext null
        val id = System.currentTimeMillis()
        val name = "$id.webp"
        File(dir, name).outputStream().use { out ->
            @Suppress("DEPRECATION")
            bitmap.compress(Bitmap.CompressFormat.WEBP, 85, out)
        }
        val panel = SavedPanel(
            id = id,
            mangaId = mangaId,
            chapterId = chapterId,
            mangaTitle = mangaTitle,
            chapterName = chapterName,
            page = page,
            savedAt = id,
            file = name,
            width = bitmap.width,
            height = bitmap.height,
        )
        bitmap.recycle()
        update(listOf(panel) + _panels.value)
        panel
    }

    fun remove(panel: SavedPanel) {
        fileOf(panel).delete()
        update(_panels.value.filterNot { it.id == panel.id })
    }

    private fun update(list: List<SavedPanel>) {
        _panels.value = list
        runCatching { index.writeText(json.encodeToString(list)) }
    }

    private fun load(): List<SavedPanel> = runCatching {
        json.decodeFromString<List<SavedPanel>>(index.readText()).filter { File(dir, it.file).exists() }
    }.getOrDefault(emptyList())

    companion object {
        private const val MAX_WIDTH = 1080

        @Volatile private var instance: SavedPanels? = null

        fun get(context: Context): SavedPanels =
            instance
                ?: synchronized(this) { instance ?: SavedPanels(context.applicationContext).also { instance = it } }
    }
}
