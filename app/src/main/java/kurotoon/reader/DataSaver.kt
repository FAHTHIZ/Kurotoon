package kurotoon.reader

import android.app.Application
import android.content.Context
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.newCachelessCallWithProgress
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.util.system.isConnectedToWifi
import kurotoon.app.di.appGraph
import logcat.LogPriority
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Response
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.ConcurrentHashMap

/**
 * Data saver, as in Kotatsu, TachiyomiSY and Komikku: pages go through the free wsrv.nl image proxy, which
 * shrinks and re-encodes them as WebP before they reach the phone. Sites that refuse the proxy (most of them
 * check the referrer) fall straight back to the normal download, and after two misses a source is skipped
 * for the rest of the session so it costs nothing.
 */
object DataSaver {
    const val OFF = "off"
    const val MOBILE = "mobile"
    const val ALWAYS = "always"
    val MODES = listOf(OFF, MOBILE, ALWAYS)

    private const val MAX_WIDTH = 1080

    // Only Application and NetworkHelper are exposed to Injekt, everything else comes from the app graph
    private val context: Application by injectLazy()
    private val preferences: ReaderPreferences get() = context.appGraph.readerPreferences
    private val network: NetworkHelper get() = context.appGraph.networkHelper
    private val misses = ConcurrentHashMap<Long, Int>()

    /** The proxied address for [url], or null when it can't go through the proxy. */
    fun proxyUrl(url: String, quality: Int): String? {
        val parsed = url.toHttpUrlOrNull() ?: return null
        if (parsed.host == "wsrv.nl") return null
        return "https://wsrv.nl/".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("url", url)
            .addQueryParameter("w", MAX_WIDTH.toString())
            .addQueryParameter("we", "")
            .addQueryParameter("q", quality.coerceIn(20, 95).toString())
            .addQueryParameter("output", "webp")
            .build()
            .toString()
    }

    fun isActive(context: Context = this.context): Boolean = when (preferences.dataSaver.get()) {
        ALWAYS -> true
        MOBILE -> !context.isConnectedToWifi()
        else -> false
    }

    /** Fetches [page] through the proxy, or returns null so the caller downloads it as usual. */
    suspend fun fetch(sourceId: Long, page: Page): Response? {
        if ((misses[sourceId] ?: 0) >= 2) return null
        return try {
            if (!isActive()) return null
            val url = proxyUrl(page.imageUrl ?: return null, preferences.dataSaverQuality.get()) ?: return null
            network.client.newCachelessCallWithProgress(GET(url), page).awaitSuccess().also {
                misses.remove(sourceId)
            }
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            misses.merge(sourceId, 1, Int::plus)
            logcat(LogPriority.INFO) { "Data saver skipped a page: ${e.message}" }
            null
        }
    }
}
