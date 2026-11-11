package kurotoon.reader

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DataSaverTest {
    @Test
    fun `proxies through wsrv as webp`() {
        val url = DataSaver.proxyUrl("https://cdn.example.com/a b/1.jpg?x=1&y=2", 70)!!
        assertTrue(url.startsWith("https://wsrv.nl/?url=https%3A%2F%2Fcdn.example.com"), url)
        assertTrue("q=70" in url && "output=webp" in url, url)
    }

    @Test
    fun `clamps quality`() {
        assertTrue("q=20" in DataSaver.proxyUrl("https://x.com/1.png", 5)!!)
    }

    @Test
    fun `skips non http and already proxied urls`() {
        assertNull(DataSaver.proxyUrl("file:///sdcard/1.png", 70))
        assertNull(DataSaver.proxyUrl("https://wsrv.nl/?url=x", 70))
    }
}
