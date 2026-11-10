package kurotoon.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR

/**
 * Keeps a "Continue <series>" launcher shortcut pointing at the chapter the reader was last on, so a
 * long press on the app icon jumps straight back in. Never updated in incognito mode.
 */
object ContinueShortcut {

    private const val ID = "kurotoon_continue"
    private const val ICON_SIZE = 192
    private const val SHORT_LABEL_MAX = 24

    @Volatile
    private var lastChapterId: Long? = null

    suspend fun update(context: Context, manga: Manga, chapterId: Long) {
        if (lastChapterId == chapterId) return
        lastChapterId = chapterId
        try {
            // Launchers persist shortcut extras, so they must be plain longs rather than boxed values
            val intent = Intent(context, ReaderActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("manga", manga.id)
                putExtra("chapter", chapterId)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val shortcut = ShortcutInfoCompat.Builder(context, ID)
                .setShortLabel(manga.title.take(SHORT_LABEL_MAX))
                .setLongLabel(context.stringResource(MR.strings.kurotoon_shortcut_continue, manga.title))
                .setIcon(coverIcon(context, manga) ?: IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(intent)
                .setRank(0)
                .build()
            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Couldn't update the continue shortcut" }
        }
    }

    /** Drops the shortcut, e.g. after the history it points at was cleared. */
    fun remove(context: Context) {
        lastChapterId = null
        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(ID))
    }

    private suspend fun coverIcon(context: Context, manga: Manga): IconCompat? {
        val request = ImageRequest.Builder(context)
            .data(manga)
            .size(ICON_SIZE)
            .allowHardware(false)
            .transformations(CircleCropTransformation())
            .build()
        val drawable = context.imageLoader.execute(request).image?.asDrawable(context.resources) ?: return null
        return IconCompat.createWithBitmap(drawable.toBitmap(ICON_SIZE, ICON_SIZE))
    }
}
