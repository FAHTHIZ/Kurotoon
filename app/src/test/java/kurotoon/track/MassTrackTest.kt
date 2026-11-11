package kurotoon.track

import eu.kanade.tachiyomi.data.track.model.TrackSearch
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MassTrackTest {

    private fun result(title: String) = TrackSearch.create(1L).apply { this.title = title }

    @Test
    fun `exact title wins over earlier results`() {
        val results = listOf(result("Solo Leveling: Ragnarok"), result("Solo Leveling"))
        MassTrack.bestMatch("solo leveling!", results)?.title shouldBe "Solo Leveling"
    }

    @Test
    fun `falls back to containing title then first`() {
        MassTrack.bestMatch("Tower of God", listOf(result("Other"), result("Tower of God (Season 3)")))
            ?.title shouldBe "Tower of God (Season 3)"
        MassTrack.bestMatch("Tower of God", listOf(result("Other")))?.title shouldBe "Other"
        MassTrack.bestMatch("Tower of God", emptyList<TrackSearch>()) shouldBe null
    }
}
