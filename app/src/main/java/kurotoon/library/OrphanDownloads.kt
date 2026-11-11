package kurotoon.library

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.app.di.appGraph

/**
 * Downloads that belong to series no longer in the library (mihon#565).
 *
 * Deliberately cautious: only folders of installed online sources are looked at, so downloads of a
 * series whose extension is missing are never touched, and anything queued for download is kept.
 */
object OrphanDownloads {

    class Orphan(val title: String, val source: String, internal val dir: UniFile)

    suspend fun find(context: Context): List<Orphan> = withContext(Dispatchers.IO) {
        val graph = context.appGraph
        val provider = graph.downloadProvider
        val favorites = graph.getFavorites.await().groupBy { it.source }
        val queued = graph.downloadManager.queueState.value.map { it.manga.source to it.manga.title }
            .groupBy({ it.first }, { it.second })

        graph.sourceManager.getOnlineSources().flatMap { source ->
            val sourceDir = provider.findSourceDir(source) ?: return@flatMap emptyList()
            // Both spellings, in case the "non-ASCII file names" setting changed since downloading
            val keep = (favorites[source.id].orEmpty().map { it.title } + queued[source.id].orEmpty())
                .flatMapTo(HashSet()) { title ->
                    listOf(
                        DiskUtil.buildValidFilename(title, disallowNonAscii = false),
                        DiskUtil.buildValidFilename(title, disallowNonAscii = true),
                    )
                }
            sourceDir.listFiles().orEmpty()
                .filter { it.isDirectory && it.name != null && it.name !in keep }
                .map { Orphan(it.name!!, source.toString(), it) }
        }.sortedBy { it.title.lowercase() }
    }

    /** Returns how many series folders were removed. */
    suspend fun delete(context: Context, orphans: List<Orphan>): Int = withContext(Dispatchers.IO) {
        val removed = orphans.count { runCatching { it.dir.delete() }.getOrDefault(false) }
        context.appGraph.downloadCache.invalidateCache()
        removed
    }
}
