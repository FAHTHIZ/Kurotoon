package kurotoon.shop

import android.content.Context
import android.net.Uri
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Every shop pack the app knows about: the ones shipped in `assets/kuro/packs/<folder>/` and the
 * ones the user imported as `.kpack` zips (kept in `files/kuro/packs/<pack id>/`).
 */
@Inject
@SingleIn(AppScope::class)
class PackRepository(private val context: Context) {

    private val lock = Any()
    private val _packs = MutableStateFlow<List<Pack>?>(null)

    /** Built-in packs first, then imported ones, each in a stable order. */
    val packs: StateFlow<List<Pack>?> = _packs.asStateFlow()

    @Volatile
    private var itemIndex: Map<String, ShopItem> = emptyMap()

    private val userDir: File get() = File(context.filesDir, USER_DIR)

    /** All items by [ShopItem.code], loading the packs the first time it's needed. */
    val items: Map<String, ShopItem>
        get() {
            ensureLoaded()
            return itemIndex
        }

    fun item(code: String): ShopItem? = items[code]

    fun ensureLoaded() {
        if (_packs.value != null) return
        synchronized(lock) {
            if (_packs.value == null) publish(loadAll())
        }
    }

    fun reload() = synchronized(lock) { publish(loadAll()) }

    private fun publish(packs: List<Pack>) {
        itemIndex = packs.flatMap { it.items }.associateBy { it.code }
        _packs.value = packs
    }

    private fun loadAll(): List<Pack> = loadBuiltIn() + loadImported()

    private fun loadBuiltIn(): List<Pack> {
        val folders = runCatching { context.assets.list(ASSET_DIR).orEmpty().sorted() }.getOrDefault(emptyList())
        return folders.mapNotNull { folder ->
            runCatching {
                val manifest = context.assets.open("$ASSET_DIR/$folder/$MANIFEST").use(::decode)
                manifest.toPack(PackOrigin.BUILT_IN) { "file:///android_asset/$ASSET_DIR/$folder/$it" }
            }
                .onFailure { logcat(LogPriority.WARN, it) { "Skipping built-in pack $folder" } }
                .getOrNull()
        }
    }

    private fun loadImported(): List<Pack> {
        val folders = userDir.listFiles { file -> file.isDirectory }.orEmpty().sortedBy { it.name }
        val builtInIds = loadBuiltInIds()
        return folders.mapNotNull { folder ->
            runCatching {
                val manifest = File(folder, MANIFEST).inputStream().use(::decode)
                if (manifest.id != folder.name || manifest.id in builtInIds) return@runCatching null
                manifest.toPack(PackOrigin.IMPORTED) { "file://" + File(folder, it).absolutePath }
            }
                .onFailure { logcat(LogPriority.WARN, it) { "Skipping imported pack ${folder.name}" } }
                .getOrNull()
        }
    }

    private fun loadBuiltInIds(): Set<String> =
        _packs.value?.filter { it.origin == PackOrigin.BUILT_IN }?.map { it.id }?.toSet()
            ?: loadBuiltIn().map { it.id }.toSet()

    sealed interface ImportResult {
        data class Success(val pack: Pack) : ImportResult
        data class Failure(val reason: Reason) : ImportResult

        enum class Reason { UNREADABLE, NO_MANIFEST, INVALID_MANIFEST, TOO_LARGE, RESERVED_ID }
    }

    /** Imports a `.kpack` (a zip with `pack.json` at its root or inside a single folder). */
    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "kpack-${System.nanoTime()}")
        try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: return@withContext ImportResult.Failure(ImportResult.Reason.UNREADABLE)
            val unzipped = stream.use { unzip(it, staging) }
                ?: return@withContext ImportResult.Failure(ImportResult.Reason.TOO_LARGE)
            installFrom(unzipped)
        } catch (e: IOException) {
            logcat(LogPriority.WARN, e) { "Couldn't import pack" }
            ImportResult.Failure(ImportResult.Reason.UNREADABLE)
        } finally {
            staging.deleteRecursively()
        }
    }

    /** Installs an unpacked pack folder; also used by tests and future in-app pack sources. */
    internal fun installFrom(root: File): ImportResult {
        val manifestFile = File(root, MANIFEST).takeIf { it.isFile }
            ?: root.listFiles()?.singleOrNull { it.isDirectory }?.let { File(it, MANIFEST) }?.takeIf { it.isFile }
            ?: return ImportResult.Failure(ImportResult.Reason.NO_MANIFEST)
        val packRoot = manifestFile.parentFile!!
        val manifest = runCatching { manifestFile.inputStream().use(::decode) }.getOrNull()
            ?: return ImportResult.Failure(ImportResult.Reason.INVALID_MANIFEST)
        if (!isValidId(manifest.id)) return ImportResult.Failure(ImportResult.Reason.INVALID_MANIFEST)
        if (manifest.id in loadBuiltInIds()) return ImportResult.Failure(ImportResult.Reason.RESERVED_ID)

        val target = File(userDir, manifest.id)
        synchronized(lock) {
            target.deleteRecursively()
            target.parentFile?.mkdirs()
            if (!packRoot.renameTo(target)) {
                packRoot.copyRecursively(target, overwrite = true)
            }
            publish(loadAll())
        }
        val pack = _packs.value.orEmpty().firstOrNull { it.id == manifest.id }
            ?: return ImportResult.Failure(ImportResult.Reason.INVALID_MANIFEST)
        return ImportResult.Success(pack)
    }

    /** Removes an imported pack. Built-in packs can't be removed. */
    fun remove(packId: String): Boolean {
        if (!isValidId(packId)) return false
        val pack = _packs.value?.firstOrNull { it.id == packId } ?: return false
        if (pack.origin != PackOrigin.IMPORTED) return false
        synchronized(lock) {
            File(userDir, packId).deleteRecursively()
            publish(loadAll())
        }
        return true
    }

    private fun decode(stream: InputStream): PackManifest =
        packJson.decodeFromString(PackManifest.serializer(), stream.readBytes().decodeToString())

    /** Unzips into [dir], refusing entries that escape it. Returns null when the archive is too big. */
    private fun unzip(input: InputStream, dir: File): File? {
        dir.mkdirs()
        val canonicalRoot = dir.canonicalPath + File.separator
        var total = 0L
        var count = 0
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (++count > MAX_ENTRIES) return null
                val out = File(dir, entry.name)
                if (!out.canonicalPath.startsWith(canonicalRoot)) continue
                if (entry.isDirectory) {
                    out.mkdirs()
                    continue
                }
                out.parentFile?.mkdirs()
                out.outputStream().use { sink ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = zip.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_BYTES) return null
                        sink.write(buffer, 0, read)
                    }
                }
            }
        }
        return dir
    }

    companion object {
        const val ASSET_DIR = "kuro/packs"
        const val USER_DIR = "kuro/packs"
        const val MANIFEST = "pack.json"
        private const val MAX_ENTRIES = 512
        private const val MAX_BYTES = 64L * 1024 * 1024
    }
}
