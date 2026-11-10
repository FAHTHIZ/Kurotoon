package kurotoon.updates

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class UpdateGroupsTest {

    private val items = listOf("a1", "b1", "a2", "a3", "c1")

    @Test
    fun `folds chapters of one series`() {
        UpdateGroups.fold(items, { it.take(1) }, emptySet())
            .map { it.item to it.hidden } shouldBe listOf("a1" to 2, "b1" to 0, "c1" to 0)
    }

    @Test
    fun `expanded group keeps every chapter`() {
        val rows = UpdateGroups.fold(items, { it.take(1) }, setOf("a"))
        rows.map { it.item } shouldBe listOf("a1", "a2", "a3", "b1", "c1")
        rows.first().hidden shouldBe 2
        rows.first().expanded shouldBe true
    }
}
