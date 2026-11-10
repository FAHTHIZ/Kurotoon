package kurotoon.catalogue

import kotlinx.serialization.Serializable

@Serializable
data class ChapterSourceSnapshot(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val seriesTitle: String,
    val sourceLabel: String,
    val chapters: List<LocalChapterRecord>,
    val snapshotId: String = "",
    val importedAtEpochMillis: Long = 0L,
)

@Serializable
data class LocalChapterRecord(
    val recordId: String? = null,
    val title: String,
    val number: String? = null,
    val volume: String? = null,
    val releaseLabel: String? = null,
    val publishedAtEpochMillis: Long? = null,
)

@Serializable
internal data class ChapterSnapshotEnvelope(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val snapshots: List<ChapterSourceSnapshot> = emptyList(),
)

const val CURRENT_SCHEMA_VERSION = 1
