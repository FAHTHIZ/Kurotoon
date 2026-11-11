package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.graphics.ColorFilter
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.WebtoonLayoutManager
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.setting.GeneralSettings
import eu.kanade.tachiyomi.ui.reader.viewer.LocalPageColorFilter
import eu.kanade.tachiyomi.ui.reader.viewer.LongStripSettings
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.ui.reader.viewer.collectChanges
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kurotoon.reader.StripPosition
import mihon.app.di.appGraph
import tachiyomi.core.common.util.system.logcat
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.max
import kotlin.math.min

/**
 * Implementation of a [Viewer] to display pages with a [RecyclerView].
 */
class WebtoonViewer(val activity: ReaderActivity, val isContinuous: Boolean = true) : Viewer {

    val graph by lazy { activity.appGraph }
    val downloadManager by lazy { graph.downloadManager }
    val readerPreferences by lazy { graph.readerPreferences }

    private val scope = MainScope()

    /**
     * Recycler view used by this viewer.
     */
    val recycler = WebtoonRecyclerView(activity)

    /**
     * Frame containing the recycler view.
     */
    private val frame = WebtoonFrame(activity)

    /**
     * Distance to scroll when the user taps on one side of the recycler view.
     */
    private val scrollDistance = activity.resources.displayMetrics.heightPixels * 3 / 4

    /**
     * Layout manager of the recycler view.
     */
    private val layoutManager = WebtoonLayoutManager(activity, scrollDistance)

    private val settingsViewModel = activity.settingsViewModel

    /**
     * Settings every long strip viewer uses, like crop image borders.
     */
    val longStrip: LongStripSettings
        get() = settingsViewModel.longStrip.value

    /**
     * Settings only the webtoon viewers use, like side padding or tap zones.
     */
    val simpleLongStrip: SimpleLongStripSettings
        get() = settingsViewModel.simpleLongStrip.value

    /**
     * Settings shared by every viewer, like allow taps or page transitions.
     */
    val general: GeneralSettings
        get() = settingsViewModel.general.value

    private val webtoonItems = WebtoonItems()

    /** Exact spot inside a page, so a chapter reopens on the panel it was left on. */
    private val stripPosition = StripPosition(activity)
    private var lastAnchorSave = 0L
    private var restoring: Runnable? = null

    /**
     * Adapter of the recycler view.
     */
    private val adapter = WebtoonAdapter(this, webtoonItems)

    private val tracker = WebtoonPageTracker(
        activity,
        webtoonItems,
        readerPreferences.readerHideThreshold.get().threshold,
    )

    init {
        recycler.sidePaddingPercent = simpleLongStrip.page.sidePadding
        recycler.setItemViewCacheSize(RECYCLER_VIEW_CACHE_SIZE)
        recycler.isVisible = false // Don't let the recycler layout yet
        recycler.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        recycler.isFocusable = false
        recycler.itemAnimator = null
        recycler.layoutManager = layoutManager
        recycler.adapter = adapter
        recycler.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) cancelRestore()
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) saveAnchor(force = true)
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    onScrolled()
                    if (restoring == null) saveAnchor(force = false)
                    tracker.onScrolledBy(
                        dy = dy.toFloat(),
                        firstVisible = layoutManager.findFirstVisibleItemPosition(),
                        lastEndVisible = layoutManager.findLastEndVisibleItemPosition(),
                    )
                }
            },
        )
        recycler.tapListener = { event ->
            val viewPosition = IntArray(2)
            recycler.getLocationOnScreen(viewPosition)
            val viewPositionRelativeToWindow = IntArray(2)
            recycler.getLocationInWindow(viewPositionRelativeToWindow)
            val pos = Offset(
                (event.rawX - viewPosition[0] + viewPositionRelativeToWindow[0]) / recycler.width,
                (event.rawY - viewPosition[1] + viewPositionRelativeToWindow[1]) / recycler.originalHeight,
            )
            activity.onViewerTap(pos)
        }
        recycler.longTapListener = f@{ event ->
            if (activity.viewModel.state.value.menuVisible || general.longTapEnabled) {
                val child = recycler.findChildViewUnder(event.x, event.y)
                if (child != null) {
                    val position = recycler.getChildAdapterPosition(child)
                    val item = adapter.items.getOrNull(position)
                    if (item is ReaderPage) {
                        activity.onPageLongTap(item)
                        return@f true
                    }
                }
            }
            false
        }

        settingsViewModel.general.collectChanges(scope) { old, new ->
            if (old != null && old.theme != new.theme) {
                ActivityCompat.recreate(activity)
            }
        }
        settingsViewModel.longStrip.collectChanges(scope) { old, new ->
            if (old != null && old.cropBorders != new.cropBorders) {
                refreshAdapter()
            }
            if (old?.zoomOutDisabled != new.zoomOutDisabled) {
                frame.zoomOutDisabled = new.zoomOutDisabled
            }
            if (old?.zoomLocked != new.zoomLocked) {
                frame.zoomLocked = new.zoomLocked
            }
        }
        settingsViewModel.simpleLongStrip.collectChanges(scope) { old, new ->
            if (old != null && old.page != new.page) {
                recycler.sidePaddingPercent = new.page.sidePadding
                refreshAdapter()
            }
            if (old?.doubleTapZoom != new.doubleTapZoom) {
                frame.doubleTapZoom = new.doubleTapZoom
            }
        }

        frame.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        frame.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        frame.addView(recycler)
    }

    /** The page images' colours, for pages bound later - see [LocalPageColorFilter]. */
    var pageColorFilter: ColorFilter? = null
        private set

    /** Every page view made, as the recycler's cached ones are neither children nor always bound again. */
    private val pageViews = Collections.newSetFromMap(WeakHashMap<ReaderPageImageView, Boolean>())

    fun onPageViewCreated(view: ReaderPageImageView) {
        pageViews += view
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val filter = LocalPageColorFilter.current
        val pageFilter = remember(filter) { filter?.asAndroidColorFilter() }
        SideEffect {
            pageColorFilter = pageFilter
            pageViews.forEach { it.pageColorFilter = pageFilter }
        }
        AndroidView(factory = { frame }, modifier = modifier)
    }

    /**
     * Destroys this viewer. Called when leaving the reader or swapping viewers.
     */
    override fun destroy() {
        saveAnchor(force = true)
        cancelRestore()
        super.destroy()
        scope.cancel()
    }

    /**
     * Tells this viewer to set the given [chapters] as active.
     */
    override fun setChapters(chapters: ViewerChapters) {
        val forceTransition = general.alwaysShowChapterTransition || tracker.currentPage is ChapterTransition
        adapter.setChapters(chapters, forceTransition)

        if (recycler.isGone) {
            logcat { "Recycler first layout" }
            val pages = chapters.currChapter.pages ?: return
            val requested = min(chapters.currChapter.requestedPage, pages.lastIndex)
            val anchor = stripPosition.get(chapters.currChapter.chapter.id!!)
                ?.takeIf { it.page in (requested - 3)..requested && it.page in pages.indices }
            moveToPage(pages[anchor?.page ?: requested])
            recycler.isVisible = true
            if (anchor != null && anchor.fraction > 0.01f) restoreAnchor(pages[anchor.page], anchor.fraction)
        }
    }

    /**
     * Tells this viewer to move to the given [page].
     */
    override fun moveToPage(page: ReaderPage) {
        // A jump (slider, chapter list, Start over) wins over a pending panel restore.
        cancelRestore()
        val position = adapter.items.indexOf(page)
        if (position != -1) {
            layoutManager.scrollToPositionWithOffset(position, 0)
            if (layoutManager.findLastEndVisibleItemPosition() == -1) {
                onScrolled(pos = position)
            }
        } else {
            logcat { "Page $page not found in adapter" }
        }
    }

    /** Remembers the first visible page and how far into it the top of the screen is. */
    private fun saveAnchor(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastAnchorSave < ANCHOR_SAVE_MS) return
        lastAnchorSave = now
        val position = layoutManager.findFirstVisibleItemPosition().takeIf { it >= 0 } ?: return
        val page = adapter.items.getOrNull(position) as? ReaderPage ?: return
        if (page.status != Page.State.Ready) return
        val view = layoutManager.findViewByPosition(position) ?: return
        if (view.height <= 0) return
        val id = page.chapter.chapter.id ?: return
        stripPosition.put(id, page.index, (-view.top).toFloat() / view.height)
    }

    /**
     * Scrolls into [page] by [fraction] of its height once its image has loaded and settled.
     * Gives up if the reader starts scrolling first or the image takes too long.
     */
    private fun restoreAnchor(page: ReaderPage, fraction: Float) {
        cancelRestore()
        var tries = 0
        var lastHeight = -1
        val task = object : Runnable {
            override fun run() {
                val position = adapter.items.indexOf(page)
                val view = layoutManager.findViewByPosition(position)
                val placeholder = (recycler.height * 0.8).toInt()
                val height = view?.height ?: -1
                val settled =
                    page.status == Page.State.Ready && height > 0 && height != placeholder && height == lastHeight
                if (settled && position >= 0) {
                    layoutManager.scrollToPositionWithOffset(position, -(fraction * height).toInt())
                    restoring = null
                    return
                }
                lastHeight = height
                if (++tries < RESTORE_TRIES &&
                    position >= 0
                ) {
                    recycler.postDelayed(this, RESTORE_POLL_MS)
                } else {
                    restoring = null
                }
            }
        }
        restoring = task
        recycler.postDelayed(task, RESTORE_POLL_MS)
    }

    private fun cancelRestore() {
        restoring?.let { recycler.removeCallbacks(it) }
        restoring = null
    }

    private fun onScrolled(pos: Int? = null) {
        tracker.onScrolled(pos ?: layoutManager.findLastEndVisibleItemPosition())
    }

    /**
     * Scrolls up by [scrollDistance].
     */
    override fun moveBackward() {
        if (general.usePageTransitions) {
            recycler.smoothScrollBy(0, -scrollDistance)
        } else {
            recycler.scrollBy(0, -scrollDistance)
        }
    }

    /**
     * Scrolls down by [scrollDistance].
     */
    override fun moveForward() {
        if (general.usePageTransitions) {
            recycler.smoothScrollBy(0, scrollDistance)
        } else {
            recycler.scrollBy(0, scrollDistance)
        }
    }

    override fun autoScrollBy(distance: Int): Boolean {
        if (!recycler.canScrollVertically(1)) return false
        // Let a fling or a page-turn animation finish before gliding again
        if (distance > 0 && recycler.scrollState == RecyclerView.SCROLL_STATE_IDLE) recycler.scrollBy(0, distance)
        return true
    }

    /**
     * Notifies adapter of changes around the current page to trigger a relayout in the recycler.
     * Used when an image configuration is changed.
     */
    private fun refreshAdapter() {
        val position = layoutManager.findLastEndVisibleItemPosition()
        adapter.refresh()
        adapter.notifyItemRangeChanged(
            max(0, position - 3),
            min(position + 3, adapter.itemCount - 1),
        )
    }
}

// Double the cache size to reduce rebinds/recycles incurred by the extra layout space on scroll direction changes
private const val RECYCLER_VIEW_CACHE_SIZE = 4
private const val ANCHOR_SAVE_MS = 500L
private const val RESTORE_POLL_MS = 120L
private const val RESTORE_TRIES = 50
