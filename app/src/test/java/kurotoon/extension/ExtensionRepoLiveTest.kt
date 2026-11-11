package kurotoon.extension

import eu.kanade.tachiyomi.network.NetworkHelper
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import mihon.data.extension.service.ExtensionStoreService
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

/**
 * Adds the community repo the way a user would, by its index.min.json url, and checks the
 * extension list that comes back. Hits the network, so it only runs with KUROTOON_NETWORK_TESTS=1.
 */
class ExtensionRepoLiveTest {

    private val service = ExtensionStoreService(
        network = mockk<NetworkHelper> { every { client } returns OkHttpClient() },
        json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        },
        protoBuf = ProtoBuf,
    )

    @Test
    fun `keiyoushi repo resolves and lists real extensions`() = runBlocking {
        assumeTrue(System.getenv("KUROTOON_NETWORK_TESTS") == "1")
        val url = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json"

        val store = service.fetch(url).getOrThrow()
        println("Store: ${store.name} (${store.indexUrl}), legacy=${store.isLegacy}")

        val extensions = service.getExtensions(store).getOrThrow()
        val sources = extensions.flatMap { it.sources }
        println("Extensions: ${extensions.size}, sources: ${sources.size}")
        println(
            "Languages: ${extensions.groupingBy {
                it.lang
            }.eachCount().entries.sortedByDescending { it.value }.take(8)}",
        )
        listOf("MangaDex", "Comick", "Asura Scans", "Webtoons.com", "Bato.to", "MangaPark").forEach { name ->
            val hit = extensions.firstOrNull { ext -> ext.sources.any { it.name.equals(name, ignoreCase = true) } }
            println(
                "  $name -> ${hit?.let {
                    "${it.pkgName} v${it.versionName}, ${it.sources.size} sources"
                } ?: "not listed"}",
            )
        }

        assertTrue(extensions.size > 100, "expected the full catalogue, got ${extensions.size}")
        assertTrue(extensions.none { it.name.contains("Update to Mihon", ignoreCase = true) })
        assertTrue(extensions.all { it.apkUrl.startsWith("http") })
    }
}
