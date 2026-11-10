package kurotoon.catalogue

import java.math.BigDecimal
import java.text.Normalizer
import java.util.Locale

/** Reconciles chapter metadata from local snapshots only. It performs no network requests. */
object ChapterReconciler {

    fun reconcile(snapshots: List<ChapterSourceSnapshot>): List<ReconciledSeries> =
        snapshots
            .filter { it.seriesTitle.isNotBlank() && it.chapters.isNotEmpty() }
            .groupBy { normalizeText(it.seriesTitle) }
            .values
            .mapNotNull { seriesSnapshots ->
                val records = seriesSnapshots.flatMap { snapshot ->
                    snapshot.chapters.map { chapter ->
                        Candidate(
                            sourceLabel = snapshot.sourceLabel,
                            record = chapter,
                            number = chapter.number?.let(::normalizeNumber) ?: extractNumber(chapter.title),
                            titleKey = normalizeText(chapter.title),
                        )
                    }
                }
                if (records.isEmpty()) return@mapNotNull null

                val chapters = records
                    .groupBy { candidate ->
                        candidate.number?.let { "number:" + it } ?: "title:" + candidate.titleKey
                    }
                    .values
                    .map(::toReconciledChapter)
                    .sortedWith(Comparator(::compareChapters))

                ReconciledSeries(
                    seriesTitle = mostCommon(seriesSnapshots.map { it.seriesTitle }),
                    sourceCount = seriesSnapshots.map { it.sourceLabel.trim().lowercase(Locale.ROOT) }.distinct().size,
                    importedSnapshotCount = seriesSnapshots.size,
                    importedRecordCount = records.size,
                    chapters = chapters,
                    missingChapterNumbers = findIntegerGaps(chapters),
                )
            }
            .sortedBy { normalizeText(it.seriesTitle) }

    private fun toReconciledChapter(candidates: List<Candidate>): ReconciledChapter {
        val variants = candidates.map { candidate ->
            ReconciledChapterVariant(
                sourceLabel = candidate.sourceLabel,
                recordId = candidate.record.recordId,
                title = candidate.record.title,
                number = candidate.number,
                volume = candidate.record.volume,
                releaseLabel = candidate.record.releaseLabel,
                publishedAtEpochMillis = candidate.record.publishedAtEpochMillis,
            )
        }
        val distinctTitles = candidates.map { it.titleKey }.distinct()
        val distinctVolumes = candidates.mapNotNull { it.record.volume?.let(::normalizeVolume) }.distinct()
        val duplicateSourceEntries = variants
            .groupBy { it.sourceLabel.trim().lowercase(Locale.ROOT) }
            .any { (_, entries) -> entries.size > 1 }

        return ReconciledChapter(
            chapterNumber = candidates.first().number,
            displayTitle = mostCommon(candidates.map { it.record.title }),
            variants = variants,
            sourceCount = variants.map { it.sourceLabel.trim().lowercase(Locale.ROOT) }.distinct().size,
            hasTitleVariants = distinctTitles.size > 1,
            hasVolumeConflict = distinctVolumes.size > 1,
            hasDuplicateEntriesWithinSource = duplicateSourceEntries,
        )
    }

    private fun compareChapters(left: ReconciledChapter, right: ReconciledChapter): Int {
        val leftNumber = left.chapterNumber?.let(::sortableNumber)
        val rightNumber = right.chapterNumber?.let(::sortableNumber)
        return when {
            leftNumber != null && rightNumber != null -> {
                val numeric = leftNumber.value.compareTo(rightNumber.value)
                if (numeric != 0) numeric else leftNumber.suffix.compareTo(rightNumber.suffix)
            }
            leftNumber != null -> -1
            rightNumber != null -> 1
            else -> normalizeText(left.displayTitle).compareTo(normalizeText(right.displayTitle))
        }
    }

    private fun findIntegerGaps(chapters: List<ReconciledChapter>): List<String> {
        val numbers = chapters.mapNotNull { chapter ->
            chapter.chapterNumber
                ?.let(::sortableNumber)
                ?.takeIf { it.suffix.isEmpty() && it.value.scale() <= 0 && it.value >= BigDecimal.ZERO }
                ?.value
                ?.toLong()
        }.distinct().sorted()
        if (numbers.size < 2) return emptyList()
        val first = numbers.first()
        val last = numbers.last()
        if (last - first > MAX_GAP_SCAN) return emptyList()
        val present = numbers.toHashSet()
        return (first..last).filterNot(present::contains).map(Long::toString)
    }

    private fun mostCommon(values: List<String>): String {
        val key = values.groupingBy(::normalizeText).eachCount().maxByOrNull { it.value }?.key
        return key?.let { normalized -> values.first { normalizeText(it) == normalized } }
            ?: values.firstOrNull().orEmpty()
    }

    private fun extractNumber(title: String): String? {
        val explicit = EXPLICIT_CHAPTER_NUMBER.find(title)?.groupValues?.getOrNull(1)
        val leading = if (explicit == null) LEADING_CHAPTER_NUMBER.find(title)?.groupValues?.getOrNull(1) else null
        return (explicit ?: leading)?.let(::normalizeNumber)
    }

    private fun normalizeNumber(raw: String): String? {
        val match = NORMALIZED_NUMBER.matchEntire(raw.trim()) ?: return null
        val numeric = runCatching {
            BigDecimal(match.groupValues[1].replace(',', '.')).stripTrailingZeros().toPlainString()
        }.getOrNull() ?: return null
        return numeric + match.groupValues[2].lowercase(Locale.ROOT)
    }

    private fun normalizeVolume(raw: String): String? {
        val stripped = raw.trim().replace(VOLUME_PREFIX, "")
        return normalizeNumber(stripped)
    }

    private fun sortableNumber(normalized: String): SortableNumber? {
        val match = NORMALIZED_NUMBER.matchEntire(normalized) ?: return null
        val numeric = runCatching { BigDecimal(match.groupValues[1]) }.getOrNull() ?: return null
        return SortableNumber(numeric, match.groupValues[2].lowercase(Locale.ROOT))
    }

    private fun normalizeText(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace(BRACKETED_TEXT, " ")
            .replace(NON_ALPHANUMERIC, " ")
            .trim()
            .replace(MULTIPLE_SPACES, " ")

    private data class Candidate(
        val sourceLabel: String,
        val record: LocalChapterRecord,
        val number: String?,
        val titleKey: String,
    )

    private data class SortableNumber(val value: BigDecimal, val suffix: String)

    private val NORMALIZED_NUMBER = Regex("""^(\d+(?:\.\d+)?)([a-z]?)$""")
    private val VOLUME_PREFIX = Regex("""^(?:vol(?:ume)?\.?\s*)""", RegexOption.IGNORE_CASE)
    private val EXPLICIT_CHAPTER_NUMBER = Regex(
        """\b(?:chapter|chap|ch|episode|ep)\.?\s*#?\s*(\d+(?:[.,]\d+)?[a-z]?)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val LEADING_CHAPTER_NUMBER = Regex(
        """^\s*#?(\d+(?:[.,]\d+)?[a-z]?)\s*(?:[-:|.)]|$)""",
        RegexOption.IGNORE_CASE,
    )
    private val BRACKETED_TEXT = Regex("""\[[^]]*]|\([^)]*\)""")
    private val NON_ALPHANUMERIC = Regex("""[^\p{L}\p{N}]+""")
    private val MULTIPLE_SPACES = Regex("""\s+""")
    private const val MAX_GAP_SCAN = 5_000L
}

data class ReconciledSeries(
    val seriesTitle: String,
    val sourceCount: Int,
    val importedSnapshotCount: Int,
    val importedRecordCount: Int,
    val chapters: List<ReconciledChapter>,
    val missingChapterNumbers: List<String>,
)

data class ReconciledChapter(
    val chapterNumber: String?,
    val displayTitle: String,
    val variants: List<ReconciledChapterVariant>,
    val sourceCount: Int,
    val hasTitleVariants: Boolean,
    val hasVolumeConflict: Boolean,
    val hasDuplicateEntriesWithinSource: Boolean,
) {
    val needsReview: Boolean
        get() = hasVolumeConflict || hasDuplicateEntriesWithinSource
}

data class ReconciledChapterVariant(
    val sourceLabel: String,
    val recordId: String?,
    val title: String,
    val number: String?,
    val volume: String?,
    val releaseLabel: String?,
    val publishedAtEpochMillis: Long?,
)
