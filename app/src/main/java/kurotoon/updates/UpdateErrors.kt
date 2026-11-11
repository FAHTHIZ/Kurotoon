package kurotoon.updates

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** One series that failed during the last library update. */
@Serializable
data class UpdateError(
    val mangaId: Long,
    val title: String,
    val source: String,
    val error: String,
)

/**
 * The failures from the most recent library update, kept so they can be fixed from inside the app
 * instead of from a text file (mihon#132). A series leaves the list once it updates cleanly.
 */
class UpdateErrors private constructor(private val file: File) {

    private val json = Json { ignoreUnknownKeys = true }
    private val _errors = MutableStateFlow(read())
    val errors: StateFlow<List<UpdateError>> = _errors.asStateFlow()

    private fun read(): List<UpdateError> = runCatching {
        if (file.exists()) json.decodeFromString<List<UpdateError>>(file.readText()) else emptyList()
    }.getOrDefault(emptyList())

    @Synchronized
    private fun write(list: List<UpdateError>) {
        runCatching {
            if (list.isEmpty()) file.delete() else file.writeText(json.encodeToString(list))
        }
    }

    fun replace(list: List<UpdateError>) {
        _errors.value = list
        write(list)
    }

    /** Forget old failures for every series that was just checked, then add the new ones. */
    fun record(checked: Set<Long>, failures: List<UpdateError>) {
        val failedIds = failures.map { it.mangaId }.toSet()
        _errors.update { old -> old.filterNot { it.mangaId in checked || it.mangaId in failedIds } + failures }
        write(_errors.value)
    }

    fun dismiss(mangaId: Long) {
        _errors.update { list -> list.filterNot { it.mangaId == mangaId } }
        write(_errors.value)
    }

    fun clear() = replace(emptyList())

    companion object {
        fun entry(mangaId: Long, title: String, source: String, error: String?) =
            UpdateError(mangaId, title, source, error.orEmpty())

        @Volatile private var instance: UpdateErrors? = null

        fun get(context: Context): UpdateErrors = instance ?: synchronized(this) {
            instance ?: UpdateErrors(File(context.applicationContext.filesDir, "kurotoon_update_errors.json"))
                .also { instance = it }
        }
    }
}
