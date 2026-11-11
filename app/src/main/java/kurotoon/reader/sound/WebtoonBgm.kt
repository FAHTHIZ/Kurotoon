package kurotoon.reader.sound

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * One piece of episode music on WEBTOON. It starts when the reader reaches [playPage] and fades out
 * at [stopPage] (or the end of the episode when that's null), the way the official app plays it.
 */
data class BgmTrack(
    val audioId: String,
    val order: Int,
    val playPage: Int,
    val stopPage: Int?,
)

/**
 * Reads the background music WEBTOON attaches to some episodes. The viewer page carries a
 * `window.__audioProperties__` block with each track and the panels it starts and stops on, and
 * Naver's audio API turns a track id into a short-lived HLS stream that Android plays natively.
 */
object WebtoonBgm {

    private val json = Json { ignoreUnknownKeys = true }

    /** Whether a chapter from this source may carry WEBTOON episode music. */
    fun isWebtoonSource(baseUrl: String?): Boolean {
        val host = baseUrl?.substringAfter("://")?.substringBefore('/')?.lowercase() ?: return false
        return host == "webtoons.com" || host.endsWith(".webtoons.com")
    }

    /** The desktop viewer page for a chapter url, which lists every panel in order. */
    fun viewerUrl(baseUrl: String, chapterUrl: String): String? {
        val absolute = when {
            chapterUrl.startsWith("http") -> chapterUrl
            chapterUrl.startsWith("/") -> baseUrl.trimEnd('/') + chapterUrl
            else -> return null
        }
        if (!absolute.contains("viewer") || !absolute.contains("episode_no")) return null
        return absolute.replace("://m.webtoons.com", "://www.webtoons.com")
    }

    /**
     * Tracks in [html], with their start and stop panels resolved to page indexes. [pageUrls] are
     * the image urls the reader loaded, used first; the viewer page's own image list is the fallback
     * for downloaded chapters that no longer know their remote urls.
     */
    fun parse(html: String, pageUrls: List<String?>): List<BgmTrack> {
        val list = bgmListJson(html) ?: return emptyList()
        val panels = IMAGE_RE.findAll(html).map { it.groupValues[1] }.toList()
        return runCatching {
            json.parseToJsonElement(list).jsonArray.mapNotNull { element ->
                val o = element.jsonObject
                val id = o.string("audioId") ?: return@mapNotNull null
                val play = o.string("playImageUrl")?.let { pageIndexOf(it, pageUrls, panels) } ?: 0
                val stop = o.string("stopImageUrl")?.takeIf { it.isNotBlank() }
                    ?.let { pageIndexOf(it, pageUrls, panels) }
                BgmTrack(
                    audioId = id,
                    order = o["sortOrder"]?.jsonPrimitive?.intOrNull ?: 0,
                    playPage = play,
                    stopPage = stop?.takeIf { it > play },
                )
            }.sortedBy { it.playPage * 1000 + it.order }
        }.getOrDefault(emptyList())
    }

    /** The playable HLS address for [audioId], or null when the API refuses. */
    fun streamUrl(client: OkHttpClient, audioId: String): String? {
        val request = Request.Builder()
            .url("$TOKEN_API$audioId/hls/token?quality=HIGH&acceptCodecs=AAC,MP3")
            .header("Referer", "https://www.webtoons.com/")
            .header("User-Agent", DESKTOP_UA)
            .build()
        val body = runCatching { client.newCall(request).execute().use { it.body.string() } }.getOrNull()
            ?: return null
        return tokenToUrl(body)
    }

    /** Pulls the manifest address out of the token response (base64 JSON inside JSON). */
    fun tokenToUrl(body: String): String? = runCatching {
        val token = json.parseToJsonElement(body).jsonObject["result"]?.jsonObject?.string("playToken")
            ?: return null
        val decoded = String(java.util.Base64.getMimeDecoder().decode(token))
        json.parseToJsonElement(decoded).jsonObject["audioInfo"]?.jsonObject?.string("url")
    }.getOrNull()

    fun viewerRequest(url: String): Request = Request.Builder()
        .url(url)
        .header("User-Agent", DESKTOP_UA)
        .header("Referer", "https://www.webtoons.com/")
        .header("Cookie", "ageGatePass=true; needGDPR=false; needCCPA=false; needCOPPA=false; pagGDPR=true")
        .build()

    private fun bgmListJson(html: String): String? {
        val start = html.indexOf("episodeBgmList").takeIf { it >= 0 } ?: return null
        val open = html.indexOf('[', start).takeIf { it >= 0 } ?: return null
        var depth = 0
        for (i in open until html.length) {
            when (html[i]) {
                '[' -> depth++
                ']' -> if (--depth == 0) return html.substring(open, i + 1)
            }
        }
        return null
    }

    private fun pageIndexOf(path: String, pageUrls: List<String?>, panels: List<String>): Int? {
        val key = path.substringBefore('?').trim()
        if (key.isEmpty()) return null
        pageUrls.indexOfFirst { it?.substringBefore('?')?.endsWith(key) == true }.takeIf { it >= 0 }?.let { return it }
        return panels.indexOfFirst { it.substringBefore('?').endsWith(key) }.takeIf { it >= 0 }
    }

    private fun JsonObject.string(key: String) = this[key]?.jsonPrimitive?.contentOrNull

    private val IMAGE_RE = Regex("""class="_images"[^>]*data-url="([^"]+)"""")
    private const val TOKEN_API = "https://apis.naver.com/audiocweb/audiocplayogwweb/play/audio/"
    const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
}
