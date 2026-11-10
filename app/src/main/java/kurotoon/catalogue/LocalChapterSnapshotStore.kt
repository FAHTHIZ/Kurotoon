package kurotoon.catalogue

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Stores chapter snapshots in a KuroToon-owned private file, outside tachiyomi.db. */
class LocalChapterSnapshotStore(context: Context) {

    private val snapshotFile = File(context.filesDir, "kurotoon/catalogue/chapter-snapshots.json")
    private val atomicFile = AtomicFile(snapshotFile)
    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    init {
        snapshotFile.parentFile?.mkdirs()
    }

    suspend fun loadSnapshots(): List<ChapterSourceSnapshot> = withContext(Dispatchers.IO) {
        mutex.withLock { readSnapshots() }
    }

    suspend fun importSnapshot(jsonText: String): ChapterSourceSnapshot = withContext(Dispatchers.IO) {
        require(jsonText.length <= MAX_IMPORT_CHARACTERS) { "The selected file is too large to import." }
        val decoded = try {
            json.decodeFromString<ChapterSourceSnapshot>(jsonText)
        } catch (error: SerializationException) {
            throw IllegalArgumentException("This file is not a valid KuroToon chapter snapshot.", error)
        }
        validate(decoded)

        mutex.withLock {
            val imported = decoded.copy(
                snapshotId = UUID.randomUUID().toString(),
                importedAtEpochMillis = System.currentTimeMillis(),
            )
            writeSnapshots(readSnapshots() + imported)
            imported
        }
    }

    suspend fun deleteSnapshot(snapshotId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            writeSnapshots(readSnapshots().filterNot { it.snapshotId == snapshotId })
        }
    }

    private fun validate(snapshot: ChapterSourceSnapshot) {
        require(snapshot.schemaVersion == CURRENT_SCHEMA_VERSION) {
            "Unsupported snapshot version: " + snapshot.schemaVersion
        }
        require(snapshot.seriesTitle.isNotBlank() && snapshot.seriesTitle.length <= 256) {
            "Series title must contain between 1 and 256 characters."
        }
        require(snapshot.sourceLabel.isNotBlank() && snapshot.sourceLabel.length <= 128) {
            "Source label must contain between 1 and 128 characters."
        }
        require(snapshot.chapters.isNotEmpty() && snapshot.chapters.size <= MAX_CHAPTERS_PER_SNAPSHOT) {
            "A snapshot must contain between 1 and 20,000 chapter records."
        }
        snapshot.chapters.forEach { chapter ->
            require(chapter.title.isNotBlank() && chapter.title.length <= 512) {
                "Every chapter needs a title of 1 to 512 characters."
            }
            require(chapter.number == null || chapter.number.length <= 32) { "A chapter number is too long." }
            require(chapter.volume == null || chapter.volume.length <= 32) { "A volume number is too long." }
            require(chapter.releaseLabel == null || chapter.releaseLabel.length <= 128) {
                "A release label is too long."
            }
            require(chapter.recordId == null || chapter.recordId.length <= 256) {
                "A record identifier is too long."
            }
        }
    }

    private fun readSnapshots(): List<ChapterSourceSnapshot> {
        val content = try {
            atomicFile.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: FileNotFoundException) {
            return emptyList()
        }
        return json.decodeFromString<ChapterSnapshotEnvelope>(content).snapshots
    }

    private fun writeSnapshots(snapshots: List<ChapterSourceSnapshot>) {
        val output = atomicFile.startWrite()
        try {
            val content = json.encodeToString(ChapterSnapshotEnvelope(snapshots = snapshots))
            output.write(content.toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(output)
        } catch (error: Throwable) {
            atomicFile.failWrite(output)
            throw error
        }
    }

    private companion object {
        const val MAX_IMPORT_CHARACTERS = 10_000_000
        const val MAX_CHAPTERS_PER_SNAPSHOT = 20_000
    }
}
