package kurotoon.reader

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Pages that failed because the connection dropped are retried as soon as it comes back, so a
 * tunnel or a lift doesn't leave a chapter full of Retry buttons.
 */
object PageAutoRetry {

    /** Emits true whenever there's a working internet connection, false when it's lost. */
    fun online(context: Context): Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        if (cm == null) {
            close()
            return@callbackFlow
        }
        val validated = mutableSetOf<Network>()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val ok = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                synchronized(validated) { if (ok) validated.add(network) else validated.remove(network) }
                trySend(synchronized(validated) { validated.isNotEmpty() })
            }

            override fun onLost(network: Network) {
                synchronized(validated) { validated.remove(network) }
                trySend(synchronized(validated) { validated.isNotEmpty() })
            }
        }
        val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        runCatching { cm.registerNetworkCallback(request, callback) }.onFailure { close(it) }
        awaitClose { runCatching { cm.unregisterNetworkCallback(callback) } }
    }.distinctUntilChanged()

    /** Retries every failed page in the loaded chapters. Returns how many were retried. */
    fun retryFailed(chapters: ViewerChapters?): Int {
        chapters ?: return 0
        var count = 0
        listOfNotNull(chapters.currChapter, chapters.nextChapter, chapters.prevChapter).forEach { chapter ->
            val loader = chapter.pageLoader ?: return@forEach
            chapter.pages.orEmpty()
                .filter { it.status is Page.State.Error }
                .forEach {
                    loader.retryPage(it)
                    count++
                }
        }
        return count
    }
}
