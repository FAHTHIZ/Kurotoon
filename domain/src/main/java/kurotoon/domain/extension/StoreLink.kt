package kurotoon.domain.extension

import java.net.URLDecoder

/**
 * Turns whatever a reader pastes into the address of a store index. People copy the GitHub page, an add-repo
 * link from a website or a link without https://, and all of those should just work.
 */
object StoreLink {

    private const val INDEX = "index.min.json"
    private val ADD_REPO_SCHEMES = listOf("kurotoon", "mihon", "tachiyomi", "app.mihon")

    // github.com/owner/repo, optionally followed by /tree/<branch> or /blob/<branch>/<path>
    private val GITHUB = Regex(
        """^https?://(?:www\.)?github\.com/([^/]+)/([^/]+?)(?:\.git)?(?:/(tree|blob)/([^/]+)(/.*)?)?/?$""",
    )

    fun normalize(input: String): String {
        var link = input.trim().trim('"', '\'', '<', '>').trim()
        if (link.isEmpty()) return link

        fromAddRepoLink(link)?.let { link = it }

        if (!link.contains("://")) link = "https://$link"

        GITHUB.matchEntire(link)?.let { match ->
            val (owner, repo, kind, branch, path) = match.destructured
            return when {
                kind == "blob" && path.isNotEmpty() ->
                    "https://raw.githubusercontent.com/$owner/$repo/$branch$path"
                else -> "https://raw.githubusercontent.com/$owner/$repo/${branch.ifEmpty { "repo" }}/$INDEX"
            }
        }

        if (link.endsWith("/")) return link + INDEX

        val host = link.substringAfter("://").substringBefore("/")
        val lastSegment = link.substringAfter("://").substringAfter("/", "").substringAfterLast("/")
        if (host == "raw.githubusercontent.com" && !lastSegment.contains('.')) return "$link/$INDEX"

        return link
    }

    private fun fromAddRepoLink(link: String): String? {
        val scheme = link.substringBefore("://", "").lowercase()
        if (scheme !in ADD_REPO_SCHEMES) return null
        val query = link.substringAfter("?", "")
        val url = query.split('&')
            .firstOrNull { it.startsWith("url=") }
            ?.removePrefix("url=")
            ?: return null
        return URLDecoder.decode(url, Charsets.UTF_8.name())
    }
}
