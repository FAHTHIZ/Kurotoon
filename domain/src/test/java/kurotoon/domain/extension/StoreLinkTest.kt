package kurotoon.domain.extension

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class StoreLinkTest {

    private val index = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json"

    @Test
    fun `full index link is kept`() {
        StoreLink.normalize(index) shouldBe index
        StoreLink.normalize("  $index  ") shouldBe index
    }

    @Test
    fun `missing scheme gets https`() {
        StoreLink.normalize("raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json") shouldBe index
    }

    @Test
    fun `github page points at the repo branch index`() {
        StoreLink.normalize("https://github.com/keiyoushi/extensions") shouldBe index
        StoreLink.normalize("github.com/keiyoushi/extensions/") shouldBe index
        StoreLink.normalize("https://github.com/keiyoushi/extensions.git") shouldBe index
    }

    @Test
    fun `github tree and blob links keep their branch`() {
        StoreLink.normalize("https://github.com/keiyoushi/extensions/tree/repo") shouldBe index
        StoreLink.normalize("https://github.com/keiyoushi/extensions/blob/repo/index.min.json") shouldBe index
    }

    @Test
    fun `raw folder gets the index appended`() {
        StoreLink.normalize("https://raw.githubusercontent.com/keiyoushi/extensions/repo") shouldBe index
        StoreLink.normalize("https://raw.githubusercontent.com/keiyoushi/extensions/repo/") shouldBe index
    }

    @Test
    fun `add repo links are unwrapped`() {
        val encoded = "https%3A%2F%2Fraw.githubusercontent.com%2Fkeiyoushi%2Fextensions%2Frepo%2Findex.min.json"
        StoreLink.normalize("tachiyomi://add-repo?url=$index") shouldBe index
        StoreLink.normalize("mihon://add-repo?url=$encoded") shouldBe index
        StoreLink.normalize("kurotoon://add-repo?url=$index") shouldBe index
    }

    @Test
    fun `other hosts are left alone`() {
        StoreLink.normalize("https://example.org/store.pb") shouldBe "https://example.org/store.pb"
        StoreLink.normalize("https://example.org/store/") shouldBe "https://example.org/store/index.min.json"
    }
}
