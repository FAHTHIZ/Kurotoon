package kurotoon.similar

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SimilarTitlesTest {

    @Test
    fun `parses recommendations best first and drops downvoted ones`() {
        val result = SimilarTitles.parse(
            """
            {"data":{"Media":{"title":{"romaji":"Ore dake Level Up na Ken","english":"Solo Leveling"},
            "recommendations":{"nodes":[
              {"rating":120,"mediaRecommendation":{"title":{"romaji":"Tower of God","english":null},
                "coverImage":{"large":"https://img/1.jpg"},"format":"MANGA","countryOfOrigin":"KR","averageScore":79}},
              {"rating":0,"mediaRecommendation":{"title":{"romaji":"Skip me"}}},
              {"rating":40,"mediaRecommendation":{"title":{"romaji":"Na Honjaman Level Up","english":"The Beginning"},
                "coverImage":null,"format":"MANGA","countryOfOrigin":"KR","averageScore":null}}
            ]}}}}
            """.trimIndent(),
        )!!
        result.matchedTitle shouldBe "Solo Leveling"
        result.titles.map { it.title } shouldBe listOf("Tower of God", "The Beginning")
        result.titles[0].altTitle shouldBe null
        result.titles[1].altTitle shouldBe "Na Honjaman Level Up"
        result.titles[0].score shouldBe 79
    }

    @Test
    fun `no match gives null`() {
        SimilarTitles.parse("""{"data":{"Media":null}}""") shouldBe null
    }
}
