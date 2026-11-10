package eu.kanade.tachiyomi.ui.browse.source

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.model.Source

class SourceMatchTest {

    private val source = Source(id = 42L, lang = "en", name = "Asura Scans", supportsLatest = true, isStub = false)

    @Test
    fun `blank query matches everything`() {
        source.matches("") shouldBe true
        source.matches("   ") shouldBe true
    }

    @Test
    fun `matches name case-insensitively`() {
        source.matches("asura") shouldBe true
        source.matches("SCANS") shouldBe true
        source.matches("mangadex") shouldBe false
    }

    @Test
    fun `every word must match name or language`() {
        source.matches("asura en") shouldBe true
        source.matches("asura fr") shouldBe false
    }
}
