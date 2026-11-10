package kurotoon.similar

import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody

/** A title AniList readers recommend alongside the one being looked up. */
data class SimilarTitle(
    val title: String,
    val altTitle: String?,
    val coverUrl: String?,
    val format: String?,
    val country: String?,
    val score: Int?,
    val votes: Int,
)

/** The AniList entry the lookup matched, plus its recommendations best-first. */
data class SimilarResult(val matchedTitle: String, val titles: List<SimilarTitle>)

/**
 * Looks up "more like this" titles on AniList's public API. Nothing about the user is sent, only the
 * series title, and no account is needed.
 */
class SimilarTitles(private val client: OkHttpClient) {

    suspend fun find(title: String): SimilarResult? {
        val body = buildJsonObject {
            put("query", QUERY)
            putJsonObject("variables") { put("search", title) }
        }.toString().toRequestBody(JSON_TYPE)
        val text = try {
            client.newCall(POST(API_URL, body = body)).awaitSuccess().use { it.body.string() }
        } catch (e: HttpException) {
            // AniList answers 404 when no series matches the title
            if (e.code == 404) return null
            throw e
        }
        return parse(text)
    }

    companion object {
        private const val API_URL = "https://graphql.anilist.co/"
        private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
        private val json = Json { ignoreUnknownKeys = true }

        private val QUERY = """
            query (${'$'}search: String) {
              Media(search: ${'$'}search, type: MANGA) {
                title { romaji english userPreferred }
                recommendations(sort: RATING_DESC, perPage: 30) {
                  nodes {
                    rating
                    mediaRecommendation {
                      title { romaji english userPreferred }
                      coverImage { large }
                      format
                      countryOfOrigin
                      averageScore
                    }
                  }
                }
              }
            }
        """.trimIndent()

        /** Turns an AniList response into a result, or null when nothing matched. */
        fun parse(text: String): SimilarResult? {
            val media = json.decodeFromString<Response>(text).data?.media ?: return null
            val titles = media.recommendations?.nodes.orEmpty()
                .filter { (it.rating ?: 0) > 0 }
                .mapNotNull { node ->
                    val rec = node.mediaRecommendation ?: return@mapNotNull null
                    val main = rec.title?.english ?: rec.title?.userPreferred ?: rec.title?.romaji
                        ?: return@mapNotNull null
                    SimilarTitle(
                        title = main,
                        altTitle = (rec.title?.romaji ?: rec.title?.userPreferred)?.takeIf { it != main },
                        coverUrl = rec.coverImage?.large,
                        format = rec.format,
                        country = rec.countryOfOrigin,
                        score = rec.averageScore,
                        votes = node.rating ?: 0,
                    )
                }
                .distinctBy { it.title.lowercase() }
            val matched = media.title?.english ?: media.title?.userPreferred ?: media.title?.romaji ?: ""
            return SimilarResult(matched, titles)
        }
    }

    @Serializable
    private data class Response(val data: Data? = null)

    @Serializable
    private data class Data(@kotlinx.serialization.SerialName("Media") val media: Media? = null)

    @Serializable
    private data class Media(
        val title: Title? = null,
        val recommendations: Recommendations? = null,
    )

    @Serializable
    private data class Recommendations(val nodes: List<Node>? = null)

    @Serializable
    private data class Node(val rating: Int? = null, val mediaRecommendation: Rec? = null)

    @Serializable
    private data class Rec(
        val title: Title? = null,
        val coverImage: Cover? = null,
        val format: String? = null,
        val countryOfOrigin: String? = null,
        val averageScore: Int? = null,
    )

    @Serializable
    private data class Title(val romaji: String? = null, val english: String? = null, val userPreferred: String? = null)

    @Serializable
    private data class Cover(val large: String? = null)
}
