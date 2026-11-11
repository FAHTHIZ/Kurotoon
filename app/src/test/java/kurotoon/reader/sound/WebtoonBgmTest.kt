package kurotoon.reader.sound

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WebtoonBgmTest {

    private fun panel(n: Int) =
        "<img src=\"x.png\" class=\"_images\" data-url=\"https://webtoon-phinf.pstatic.net/2024/abc_EP_${"%03d".format(
            n,
        )}.jpg?type=q90\">"

    private val html = buildString {
        append("<div id=\"_imageList\">")
        (1..10).forEach { append(panel(it)) }
        append("</div><script>window.__audioProperties__ = {\n")
        append("episodeBgmList: [")
        append(
            "{\"sortOrder\":1,\"audioId\":\"A1\",\"playImageUrl\":\"/2024/abc_EP_001.jpg\",\"stopImageUrl\":\"/2024/abc_EP_004.jpg\"},",
        )
        append("{\"sortOrder\":2,\"audioId\":\"B2\",\"playImageUrl\":\"/2024/abc_EP_006.jpg\",\"stopImageUrl\":\"\"}")
        append("],\naudioPartnerKey: \"x\"};</script>")
    }

    @Test
    fun `maps start and stop panels from the viewer page`() {
        val tracks = WebtoonBgm.parse(html, emptyList())
        assertEquals(listOf("A1", "B2"), tracks.map { it.audioId })
        assertEquals(0, tracks[0].playPage)
        assertEquals(3, tracks[0].stopPage)
        assertEquals(5, tracks[1].playPage)
        assertNull(tracks[1].stopPage)
    }

    @Test
    fun `prefers the reader's own page urls`() {
        val pages = listOf(null, "https://cdn/2024/abc_EP_001.jpg?type=q90", "https://cdn/2024/abc_EP_006.jpg")
        val tracks = WebtoonBgm.parse(html, pages)
        assertEquals(1, tracks[0].playPage)
        assertEquals(2, tracks[1].playPage)
    }

    @Test
    fun `episodes without music give nothing`() {
        assertTrue(WebtoonBgm.parse("<html></html>", emptyList()).isEmpty())
    }

    @Test
    fun `reads the manifest out of a play token`() {
        val inner = """{"audioInfo":{"url":"https://apis.naver.com/x/manifest.m3u8?a=1"}}"""
        val token = java.util.Base64.getEncoder().encodeToString(inner.toByteArray())
        assertEquals(
            "https://apis.naver.com/x/manifest.m3u8?a=1",
            WebtoonBgm.tokenToUrl("""{"message":"Success.","result":{"playToken":"$token"}}"""),
        )
    }

    @Test
    fun `recognises webtoons hosts and viewer urls`() {
        assertTrue(WebtoonBgm.isWebtoonSource("https://www.webtoons.com"))
        assertTrue(WebtoonBgm.isWebtoonSource("https://m.webtoons.com/en"))
        assertFalse(WebtoonBgm.isWebtoonSource("https://mangadex.org"))
        assertEquals(
            "https://www.webtoons.com/en/drama/a/episode-1/viewer?title_no=1&episode_no=1",
            WebtoonBgm.viewerUrl("https://www.webtoons.com", "/en/drama/a/episode-1/viewer?title_no=1&episode_no=1"),
        )
        assertNull(WebtoonBgm.viewerUrl("https://www.webtoons.com", "/en/drama/a/list?title_no=1"))
    }

    @Test
    fun `genres pick a fitting soundscape`() {
        assertEquals(Soundscape.NIGHT, Soundscape.forGenres(listOf("Horror", "Action")))
        assertEquals(Soundscape.RAIN, Soundscape.forGenres(listOf("Romance")))
        assertEquals(Soundscape.FIREPLACE, Soundscape.forGenres(listOf("Fantasy")))
        assertEquals(Soundscape.FOCUS, Soundscape.forGenres(listOf("Noble")))
    }
}
