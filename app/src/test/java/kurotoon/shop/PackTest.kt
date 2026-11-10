package kurotoon.shop

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.io.File

class PackTest {

    @Test
    fun `manifests turn into packs and skip items they can't use`() {
        val manifest = packJson.decodeFromString<PackManifest>(
            """
            {
              "id": "my.pack",
              "name": "My pack",
              "future": "ignored",
              "items": [
                { "id": "a", "kind": "avatar", "name": "A", "price": 10, "rarity": "epic", "image": "img/a.png",
                  "colors": ["#FFF", "#112233", "nope"] },
                { "id": "a", "kind": "avatar", "name": "Duplicate" },
                { "id": "b", "kind": "spaceship", "name": "Unknown kind" },
                { "id": "c d", "kind": "badge", "name": "Bad id" },
                { "id": "e", "kind": "sticker", "name": "Escapes", "image": "../../secret.png", "price": -5 }
              ]
            }
            """.trimIndent(),
        )
        val pack = manifest.toPack(PackOrigin.IMPORTED) { "file:///packs/my.pack/$it" }.shouldNotBeNull()
        pack.items shouldHaveSize 2
        val a = pack.items[0]
        a.code shouldBe "my.pack/a"
        a.rarity shouldBe Rarity.EPIC
        a.image shouldBe "file:///packs/my.pack/img/a.png"
        a.colors shouldHaveSize 2
        val e = pack.items[1]
        e.image.shouldBeNull()
        e.isFree shouldBe true
    }

    @Test
    fun `bad pack ids are rejected`() {
        PackManifest(id = "../x", name = "x").toPack(PackOrigin.IMPORTED) { it }.shouldBeNull()
    }

    @Test
    fun `paths can't leave the pack`() {
        safeRelativePath("a/b.png") shouldBe "a/b.png"
        safeRelativePath("/a.png") shouldBe "a.png"
        safeRelativePath("a/../../b.png").shouldBeNull()
        safeRelativePath("a\\..\\b.png").shouldBeNull()
        safeRelativePath("").shouldBeNull()
    }

    @Test
    fun `built-in packs are valid`() {
        val root = File("src/main/assets/kuro/packs")
        val folders = root.listFiles { file -> file.isDirectory }.orEmpty()
        folders.isNotEmpty() shouldBe true
        val ids = mutableSetOf<String>()
        val problems = folders.flatMap { folder ->
            val manifest = packJson.decodeFromString<PackManifest>(File(folder, "pack.json").readText())
            val pack = manifest.toPack(PackOrigin.BUILT_IN) { it }
            buildList {
                if (pack == null) add("${folder.name}: invalid id")
                if (!ids.add(manifest.id)) add("${folder.name}: duplicate pack id ${manifest.id}")
                if (pack != null && pack.items.size != manifest.items.size) add("${folder.name}: items were dropped")
                manifest.items.mapNotNull { it.image }.forEach { image ->
                    if (!File(folder, image).isFile) add("${folder.name}: missing $image")
                }
            }
        }
        problems.shouldBeEmpty()
    }
}
