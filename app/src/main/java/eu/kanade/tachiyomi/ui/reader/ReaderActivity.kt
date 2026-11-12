package eu.kanade.tachiyomi.ui.reader

import android.app.assist.AssistContent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.zacsweers.metro.Inject
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.reader.DisplayRefreshHost
import eu.kanade.presentation.reader.OrientationSelectDialog
import eu.kanade.presentation.reader.ReaderContentOverlay
import eu.kanade.presentation.reader.ReaderNavigationOverlay
import eu.kanade.presentation.reader.ReaderNavigationOverlayState
import eu.kanade.presentation.reader.ReaderPageActionsDialog
import eu.kanade.presentation.reader.ReaderPageIndicator
import eu.kanade.presentation.reader.ReadingModeSelectDialog
import eu.kanade.presentation.reader.appbars.ReaderAppBars
import eu.kanade.presentation.reader.components.ChapterNavigatorType
import eu.kanade.presentation.reader.settings.ReaderSettingsDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult.AddToLibraryFirst
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult.Error
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.SetAsCoverResult.Success
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsViewModel
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.viewer.LocalPageColorFilter
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation.Action
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation.NavigationRegion
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.readerBackgroundColor
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.setComposeContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kurotoon.manga.ChapterTitles
import kurotoon.reader.AutoScrollDriver
import kurotoon.reader.AutoScrollState
import kurotoon.reader.ChapterCompleteHost
import kurotoon.reader.ChapterCompleteState
import kurotoon.reader.ReaderProgressOrb
import kurotoon.reader.SleepTimerCurtain
import kurotoon.reader.SleepTimerDriver
import kurotoon.reader.SleepTimerState
import kurotoon.reader.sound.ReaderSoundtrack
import kurotoon.reader.sound.Soundscape
import kurotoon.ui.reader.ReaderSoundBadge
import kurotoon.ui.reader.ReaderSoundSheet
import logcat.LogPriority
import mihon.app.di.AppGraph
import mihon.core.metro.metroGraph
import tachiyomi.core.common.Constants
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.collectAsState

class ReaderActivity : BaseActivity() {

    private val graph: AppGraph by lazy { metroGraph() }

    companion object {
        private const val STATE_SOUND_MUTED = "kurotoon_sound_muted"
        private const val STATE_VOLUME_HINTED = "kurotoon_volume_hinted"
        private const val STATE_AUTO_SCROLL = "kurotoon_auto_scroll"

        fun newIntent(context: Context, mangaId: Long?, chapterId: Long?): Intent {
            return Intent(context, ReaderActivity::class.java).apply {
                putExtra("manga", mangaId)
                putExtra("chapter", chapterId)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    }

    @Inject private lateinit var readerPreferences: ReaderPreferences

    @Inject private lateinit var preferences: BasePreferences

    val viewModel by viewModels<ReaderViewModel> { graph.viewModelFactory }
    val settingsViewModel by viewModels<ReaderSettingsViewModel> {
        viewModelFactory {
            initializer { ReaderSettingsViewModel(viewModel, readerPreferences, preferences, graph.kuroPreferences) }
        }
    }
    private var assistUrl: String? = null

    private var menuToggleToast: Toast? = null
    private val readerHint = kurotoon.ui.reader.ReaderHintState()
    private var readingModeToast: Toast? = null
    private val displayRefreshHost by lazy { DisplayRefreshHost(readerPreferences) }
    private val navigationOverlayState = ReaderNavigationOverlayState()

    private val windowInsetsController by lazy { WindowInsetsControllerCompat(window, window.decorView) }

    private var isLoadingViewer by mutableStateOf(false)

    /**
     * Viewer used to display the pages (pager, webtoon, ...).
     */
    private val viewerFlow = MutableStateFlow<Viewer?>(null)
    private val viewer: Viewer? inline get() = viewerFlow.value

    var isScrollingThroughPages = false
        private set

    private val autoScroll = AutoScrollState()
    private val chapterComplete = ChapterCompleteState()
    private val resumeHint = kurotoon.reader.ResumeHintState()
    private val nextElsewhere = kurotoon.reader.NextElsewhereState()
    private val wakefulness = kurotoon.reader.ReaderWakefulness()
    private val seriesAmbience by lazy { kurotoon.reader.sound.SeriesAmbience(this) }

    /** The resume hint is offered once per opening, not after rotation or chapter changes. */
    private var resumeHintChecked = false
    private val soundtrack by lazy { ReaderSoundtrack(this) }
    private val sleepTimer = SleepTimerState()

    /**
     * Called when the activity is created. Initializes the presenter and configuration.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        graph.inject(this)
        registerSecureActivity(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                R.anim.shared_axis_x_push_enter,
                R.anim.shared_axis_x_push_exit,
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.shared_axis_x_push_enter, R.anim.shared_axis_x_push_exit)
        }

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        super.onCreate(savedInstanceState)
        resumeHintChecked = savedInstanceState != null
        // Rotating recreates the reader: keep it quiet if it was muted, and don't re-check silent mode
        if (savedInstanceState != null) {
            silentChecked = true
            mediaVolumeHinted = savedInstanceState.getBoolean(STATE_VOLUME_HINTED)
            if (savedInstanceState.getBoolean(STATE_SOUND_MUTED)) soundtrack.mute(true, fade = false)
            sleepTimer.restore(savedInstanceState)
            autoScroll.enabled = savedInstanceState.getBoolean(STATE_AUTO_SCROLL)
        }

        setComposeContent {
            val state by viewModel.state.collectAsState()
            val general by settingsViewModel.general.collectAsState()
            val showPageNumber by readerPreferences.showPageNumber.collectAsState()
            val showTimeLeft by readerPreferences.showTimeLeft.collectAsState()
            val showClockBattery by readerPreferences.showClockBattery.collectAsState()
            val clock = kurotoon.reader.rememberReaderClock(showPageNumber && showClockBattery)
            val battery = kurotoon.reader.rememberBatteryLevel(showPageNumber && showClockBattery)
            val showProgressOrb by readerPreferences.showProgressOrb.collectAsState()
            val bedtimeMode by readerPreferences.bedtimeMode.collectAsState()
            val bedtimeWarmth = kurotoon.reader.rememberBedtimeWarmth(bedtimeMode)
            val flashOnPageChange by readerPreferences.flashOnPageChange.collectAsState()
            val colorOverlayEnabled by readerPreferences.colorFilter.collectAsState()
            val colorOverlay by readerPreferences.colorFilterValue.collectAsState()
            val colorOverlayMode by readerPreferences.colorFilterMode.collectAsState()
            val brightness by settingsViewModel.brightness.collectAsState()
            val navigation = settingsViewModel.navigation.collectAsState().value

            val backgroundColor = remember(general.theme) { Color(baseContext.readerBackgroundColor(general.theme)) }
            val colorOverlayBlendMode = remember(colorOverlayMode) {
                ReaderPreferences.ColorFilterMode.getOrNull(colorOverlayMode)?.second
            }

            LaunchedEffect(brightness) { setWindowBrightness(brightness) }

            val forceNavigationOverlay = remember { settingsViewModel.takeNavigationOverlayForNewUser() }
            LaunchedEffect(navigation) {
                if (navigation == null) return@LaunchedEffect
                val showOnStart = forceNavigationOverlay || settingsViewModel.general.value.navigationOverlayOnStart
                navigationOverlayState.setNavigation(navigation, showOnStart)
            }

            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            ReaderSoundEffects(state)
            SleepTimerDriver(sleepTimer, onFire = ::onSleepTimer)

            val autoScrollSpeed by readerPreferences.autoScrollSpeed.collectAsState()
            val lifecycleState by lifecycle.currentStateFlow.collectAsState()
            AutoScrollDriver(
                state = autoScroll,
                viewer = viewerFlow.collectAsState().value,
                speed = autoScrollSpeed,
                paused = state.menuVisible ||
                    state.dialog != null ||
                    state.isLoadingAdjacentChapter ||
                    !lifecycleState.isAtLeast(Lifecycle.State.RESUMED),
                onReachedEnd = {
                    hint(MR.strings.kurotoon_reader_autoScrollEnd)
                },
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .onPreviewKeyEvent(::onKeyEvent)
                    .focusRequester(focusRequester)
                    .focusable()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            navigationOverlayState.hide()
                            autoScroll.holding = true
                            try {
                                do {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                } while (event.changes.any { it.pressed })
                            } finally {
                                autoScroll.holding = false
                            }
                        }
                    }
                    .then(
                        if (general.keepScreenOn || autoScroll.enabled || wakefulness.awake) {
                            Modifier.keepScreenOn()
                        } else {
                            Modifier
                        },
                    ),
            ) {
                wakefulness.Track()
                ReaderViewer()

                if (isLoadingViewer) {
                    LoadingScreen()
                    return@Box
                }

                ReaderContentOverlay(
                    brightness = brightness,
                    color = colorOverlay.takeIf { colorOverlayEnabled },
                    colorBlendMode = colorOverlayBlendMode,
                )
                kurotoon.reader.BedtimeOverlay(warmth = bedtimeWarmth)

                if (!state.menuVisible && showPageNumber) {
                    ReaderPageIndicator(
                        currentPage = state.currentPage,
                        totalPages = state.totalPages,
                        minutesLeft = state.minutesLeft.takeIf { showTimeLeft },
                        clock = clock,
                        batteryPercent = battery?.percent,
                        charging = battery?.charging == true,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding(),
                    )
                }

                if (!state.menuVisible && showProgressOrb) {
                    ReaderProgressOrb(
                        currentPage = state.currentPage,
                        totalPages = state.totalPages,
                        onClick = { setMenuVisibility(true) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(16.dp),
                    )
                }

                if (flashOnPageChange) {
                    DisplayRefreshHost(hostState = displayRefreshHost)
                }

                ReaderNavigationOverlay(state = navigationOverlayState)

                ChapterCompleteHost(state = chapterComplete, modifier = Modifier.fillMaxSize())

                kurotoon.reader.NextElsewhereHost(
                    state = nextElsewhere,
                    onContinue = { offer ->
                        // singleTask: the finishing reader steps aside so a fresh one opens the other copy
                        finish()
                        startActivity(newIntent(this@ReaderActivity, offer.mangaId, offer.chapterId))
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                kurotoon.reader.ResumeHintHost(
                    state = resumeHint,
                    menuVisible = state.menuVisible,
                    onStartOver = { moveToPageIndex(0) },
                    modifier = Modifier.fillMaxSize(),
                )

                ReaderSoundBadge(
                    visible = !state.menuVisible && (
                        soundtrack.musicHere ||
                            (soundtrack.interruption != null && soundtrack.activeScape != Soundscape.OFF)
                        ),
                    muted = soundtrack.muted,
                    buffering = soundtrack.buffering,
                    interrupted = soundtrack.interruption != null,
                    onClick = soundtrack::toggleMute,
                    onLongClick = viewModel::openSoundDialog,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(8.dp),
                )

                AppBars(state = state)

                kurotoon.ui.reader.ReaderHintHost(
                    state = readerHint,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = if (state.menuVisible) 84.dp else 16.dp),
                )

                SleepTimerCurtain(
                    state = sleepTimer,
                    onClose = ::finish,
                    // The timer muted the soundtrack; carrying on should bring it back
                    onKeepReading = { soundtrack.mute(false) },
                )
            }

            if (state.isLoadingAdjacentChapter) {
                kurotoon.ui.reader.ReaderLoadingChapter()
            }

            val onDismissRequest = viewModel::closeDialog
            when (val dialog = state.dialog) {
                is ReaderViewModel.Dialog.Settings -> {
                    ReaderSettingsDialog(
                        onDismissRequest = onDismissRequest,
                        onShowMenus = { setMenuVisibility(true) },
                        onHideMenus = { setMenuVisibility(false) },
                        viewModel = settingsViewModel,
                    )
                }
                is ReaderViewModel.Dialog.ReadingModeSelect -> {
                    ReadingModeSelectDialog(
                        onDismissRequest = onDismissRequest,
                        viewModel = settingsViewModel,
                        onChange = { stringRes ->
                            menuToggleToast?.cancel()
                            if (!readerPreferences.showReadingMode.get()) {
                                hint(stringRes)
                            }
                        },
                    )
                }
                is ReaderViewModel.Dialog.OrientationModeSelect -> {
                    OrientationSelectDialog(
                        onDismissRequest = onDismissRequest,
                        viewModel = settingsViewModel,
                        onChange = { stringRes ->
                            hint(stringRes)
                        },
                    )
                }
                is ReaderViewModel.Dialog.PageActions -> {
                    ReaderPageActionsDialog(
                        onDismissRequest = onDismissRequest,
                        onSetAsCover = viewModel::setAsCover,
                        onShare = viewModel::shareImage,
                        onSave = viewModel::saveImage,
                        onKeepPanel = viewModel::keepPanel,
                    )
                }
                is ReaderViewModel.Dialog.Sound -> {
                    ReaderSoundSheet(
                        preferences = readerPreferences,
                        soundtrack = soundtrack,
                        sleepTimer = sleepTimer,
                        onDismissRequest = onDismissRequest,
                        onAmbiencePicked = { key -> state.manga?.id?.let { seriesAmbience.put(it, key) } },
                    )
                }
                is ReaderViewModel.Dialog.ChapterList -> {
                    kurotoon.ui.reader.ReaderChapterListSheet(
                        chapters = dialog.chapters,
                        currentChapterId = state.currentChapter?.chapter?.id,
                        onDismissRequest = onDismissRequest,
                        onChapterClick = { chapter ->
                            lifecycleScope.launch { viewModel.loadChapterById(chapter.id) }
                        },
                    )
                }
                null -> {}
            }
        }

        if (!viewModel.hasValidArgs) {
            finish()
            return
        }

        NotificationReceiver.dismissNotification(
            this,
            viewModel.mangaId.hashCode(),
            Notifications.ID_NEW_CHAPTERS,
        )

        setMenuVisibility(viewModel.state.value.menuVisible)

        // Finish when incognito mode is disabled
        preferences.incognitoMode.changes()
            .drop(1)
            .onEach { if (!it) finish() }
            .launchIn(lifecycleScope)

        viewModel.state
            .map { it.initError }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach(::setInitialChapterError)
            .launchIn(lifecycleScope)

        viewModel.state
            .map { it.manga }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach { updateViewer() }
            .launchIn(lifecycleScope)

        viewModel.state
            .map { it.viewerChapters }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach(::setChapters)
            .launchIn(lifecycleScope)

        viewModel.eventFlow
            .onEach { event ->
                when (event) {
                    ReaderViewModel.Event.ReloadViewerChapters -> {
                        viewModel.state.value.viewerChapters?.let(::setChapters)
                    }
                    ReaderViewModel.Event.PageChanged -> {
                        displayRefreshHost.flash()
                        if (viewer is PagerViewer && readerPreferences.soundPageTurn.get()) {
                            soundtrack.playPageTurn(readerPreferences.soundEffectsVolume.get() / 100f)
                        }
                    }
                    is ReaderViewModel.Event.SetOrientation -> {
                        setOrientation(event.orientation)
                    }
                    is ReaderViewModel.Event.SavedImage -> {
                        onSaveImageResult(event.result)
                    }
                    is ReaderViewModel.Event.ShareImage -> {
                        onShareImageResult(event.uri, event.page)
                    }
                    is ReaderViewModel.Event.PanelKept -> {
                        hint(if (event.ok) MR.strings.kurotoon_panels_kept else MR.strings.kurotoon_panels_failed)
                    }
                    is ReaderViewModel.Event.CopyImage -> {
                        onCopyImageResult(event.uri)
                    }
                    is ReaderViewModel.Event.SetCoverResult -> {
                        onSetAsCoverResult(event.result)
                    }
                    is ReaderViewModel.Event.ChapterCompleted -> {
                        if (sleepTimer.endOfChapter) {
                            sleepTimer.fire()
                            onSleepTimer()
                        } else if (readerPreferences.celebrateChapters.get()) {
                            chapterComplete.show(event.chapterName)
                        }
                    }
                    is ReaderViewModel.Event.NextElsewhere -> {
                        nextElsewhere.show(
                            kurotoon.reader.NextElsewhereState.Offer(
                                event.mangaId,
                                event.chapterId,
                                event.chapterName,
                                event.sourceName,
                            ),
                        )
                    }
                }
            }
            .launchIn(lifecycleScope)

        kurotoon.reader.PageAutoRetry.online(this)
            .flowWithLifecycle(lifecycle)
            .onEach { online ->
                if (online) {
                    kurotoon.reader.PageAutoRetry.retryFailed(viewModel.state.value.viewerChapters)
                    // Episode music that failed offline gets another go too
                    if (soundtrack.musicFailed || soundtrack.tracksFailed) soundtrack.retry()
                }
            }
            .launchIn(lifecycleScope)
    }

    /**
     * Called when the activity is destroyed. Cleans up the viewer and any view.
     */
    override fun onDestroy() {
        super.onDestroy()
        soundtrack.release()
        viewer?.destroy()
        menuToggleToast?.cancel()
        readingModeToast?.cancel()
    }

    private var silentChecked = false
    private var mediaVolumeHinted = false

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_SOUND_MUTED, soundtrack.muted)
        outState.putBoolean(STATE_VOLUME_HINTED, mediaVolumeHinted)
        outState.putBoolean(STATE_AUTO_SCROLL, autoScroll.enabled)
        sleepTimer.save(outState)
    }

    /**
     * Media audio plays even with the ringer off, so a reader opened on a silent phone starts muted
     * instead of surprising a quiet room. Checked once per visit; the mute badge brings sound back.
     */
    private fun muteIfPhoneSilent() {
        if (silentChecked) return
        silentChecked = true
        if (!readerPreferences.soundRespectSilent.get()) return
        val audio = getSystemService(android.media.AudioManager::class.java) ?: return
        if (audio.ringerMode != android.media.AudioManager.RINGER_MODE_NORMAL) {
            soundtrack.mute(true, fade = false)
        }
    }

    override fun onPause() {
        viewModel.saveHistory()
        soundtrack.onPause()
        readerHint.releaseToasts()
        super.onPause()
    }

    /**
     * Set menu visibility again on activity resume to apply immersive mode again if needed.
     * Helps with rotations.
     */
    override fun onResume() {
        super.onResume()
        viewModel.restartReadTimer()
        muteIfPhoneSilent()
        soundtrack.onResume()
        setMenuVisibility(viewModel.state.value.menuVisible)
        readerHint.claimToasts(this)
    }

    /**
     * Called when the window focus changes. It sets the menu visibility to the last known state
     * to apply immersive mode again if needed.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setMenuVisibility(viewModel.state.value.menuVisible)
        }
    }

    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)
        assistUrl?.let { outContent.webUri = it.toUri() }
    }

    /**
     * Called when the user clicks the back key or the button on the toolbar. The call is
     * delegated to the presenter.
     */
    override fun finish() {
        viewModel.onActivityFinish()
        super.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                R.anim.shared_axis_x_pop_enter,
                R.anim.shared_axis_x_pop_exit,
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.shared_axis_x_pop_enter, R.anim.shared_axis_x_pop_exit)
        }
    }

    @Composable
    private fun ReaderViewer() {
        val viewer by viewerFlow.collectAsState()
        val general by settingsViewModel.general.collectAsState()
        val insets = when {
            !general.fullscreen -> WindowInsets.systemBars
            !general.drawUnderCutout -> WindowInsets.displayCutout
            else -> WindowInsets(0)
        }
        val colorFilter = remember(general.grayscale, general.invertedColors) {
            viewerColorFilter(general.grayscale, general.invertedColors)
        }

        // Each viewer filters its page images alone: transition, loading and error pages keep their colours.
        CompositionLocalProvider(LocalPageColorFilter provides colorFilter) {
            key(viewer) {
                viewer?.Content(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(insets),
                )
            }
        }
    }

    /**
     * Handles a key [event] before anything else in the reader. Movement happens on key up.
     */
    private var lastVolumeTurn = 0L

    private fun onKeyEvent(event: KeyEvent): Boolean {
        val viewer = viewer ?: return false
        val navigation = settingsViewModel.navigation.value ?: return false
        val isUp = event.type == KeyEventType.KeyUp
        if (autoScroll.enabled && (event.key == Key.VolumeUp || event.key == Key.VolumeDown)) {
            val general = settingsViewModel.general.value
            if (general.volumeKeysEnabled && !viewModel.state.value.menuVisible) {
                // While auto-scrolling, volume keys set the speed instead of turning pages
                if (event.type == KeyEventType.KeyDown) changeAutoScrollSpeed(if (event.key == Key.VolumeUp) 1 else -1)
                return true
            }
        }
        val action = when (event.key) {
            Key.VolumeDown, Key.VolumeUp -> {
                val general = settingsViewModel.general.value
                if (!general.volumeKeysEnabled || viewModel.state.value.menuVisible) return false
                if (isUp) {
                    val now = android.os.SystemClock.uptimeMillis()
                    if (now - lastVolumeTurn < readerPreferences.volumeKeyDelay.get()) return true
                    lastVolumeTurn = now
                }
                if ((event.key == Key.VolumeDown) != general.volumeKeysInverted) Action.FORWARD else Action.BACKWARD
            }
            Key.DirectionRight -> if (event.isCtrlPressed) {
                Action.FORWARD
            } else {
                navigation.actionOf(
                    NavigationRegion.RIGHT,
                )
            }
            Key.DirectionLeft -> if (event.isCtrlPressed) {
                Action.BACKWARD
            } else {
                navigation.actionOf(
                    NavigationRegion.LEFT,
                )
            }
            Key.DirectionDown, Key.PageDown -> Action.FORWARD
            Key.DirectionUp, Key.PageUp -> Action.BACKWARD
            Key.Menu -> Action.MENU
            Key.N -> {
                if (isUp) loadNextChapter()
                return true
            }
            Key.P -> {
                if (isUp) loadPreviousChapter()
                return true
            }
            else -> return false
        }
        if (isUp) {
            when (action) {
                Action.MENU -> toggleMenu()
                Action.FORWARD -> viewer.moveForward()
                Action.BACKWARD -> viewer.moveBackward()
            }
        }
        return true
    }

    /**
     * Called from the viewer when it's tapped at [pos], given as a fraction of its size.
     */
    fun onViewerTap(pos: Offset) {
        val action = settingsViewModel.navigation.value?.getAction(pos) ?: Action.MENU
        when (action) {
            Action.MENU -> toggleMenu()
            Action.FORWARD -> viewer?.moveForward()
            Action.BACKWARD -> viewer?.moveBackward()
        }
    }

    @Composable
    fun AppBars(state: ReaderViewModel.State) {
        val isHttpSource = state.source is HttpSource
        val readingMode = ReadingMode.fromPreference(viewModel.getMangaReadingMode())

        val cropBorderPaged by readerPreferences.cropBorders.collectAsState()
        val cropBorderWebtoon by readerPreferences.cropBordersWebtoon.collectAsState()
        val isPagerType = ReadingMode.isPagerType(viewModel.getMangaReadingMode())
        val cropEnabled = if (isPagerType) cropBorderPaged else cropBorderWebtoon

        val verticalNavigatorModes by readerPreferences.verticalNavigator.collectAsState()
        val verticalNavigator = verticalNavigatorModes.contains(readingMode)
        val verticalNavigatorOnLeft by readerPreferences.verticalNavigatorOnLeft.collectAsState()
        val verticalNavigatorHeight by readerPreferences.verticalNavigatorHeight.collectAsState()

        var editingNote by remember { mutableStateOf(false) }
        if (editingNote) {
            kurotoon.ui.reader.BookmarkNoteDialog(
                initial = viewModel.bookmarkNote().orEmpty(),
                onDismiss = { editingNote = false },
                onSave = {
                    editingNote = false
                    viewModel.setBookmarkNote(it)
                },
            )
        }

        ReaderAppBars(
            visible = state.menuVisible,

            mangaTitle = state.manga?.title,
            chapterTitle = state.currentChapter?.chapter?.name?.let(ChapterTitles::tidy)?.let { name ->
                if (state.chaptersLeft > 0) {
                    pluralStringResource(
                        MR.plurals.kurotoon_reader_chaptersLeft,
                        state.chaptersLeft,
                        name,
                        state.chaptersLeft,
                    )
                } else {
                    name
                }
            },
            navigateUp = onBackPressedDispatcher::onBackPressed,
            onClickTopAppBar = ::openMangaScreen,
            bookmarked = state.bookmarked,
            onToggleBookmarked = viewModel::toggleChapterBookmark,
            onOpenInWebView = ::openChapterInWebView.takeIf { isHttpSource },
            onOpenInBrowser = ::openChapterInBrowser.takeIf { isHttpSource },
            onShare = ::shareChapter.takeIf { isHttpSource },
            onBookmarkNote = { editingNote = true },

            chapterNavigatorType = if (!verticalNavigator) {
                if (readingMode == ReadingMode.RIGHT_TO_LEFT) {
                    ChapterNavigatorType.HORIZONTAL_RTL
                } else {
                    ChapterNavigatorType.HORIZONTAL_LTR
                }
            } else {
                if (verticalNavigatorOnLeft) {
                    ChapterNavigatorType.VERTICAL_LEFT
                } else {
                    ChapterNavigatorType.VERTICAL_RIGHT
                }
            },
            verticalNavigatorHeight = verticalNavigatorHeight / 100f,
            onNextChapter = ::loadNextChapter,
            enabledNext = state.viewerChapters?.nextChapter != null,
            onPreviousChapter = ::loadPreviousChapter,
            enabledPrevious = state.viewerChapters?.prevChapter != null,
            currentPage = state.currentPage,
            totalPages = state.totalPages,
            onPageIndexChange = {
                isScrollingThroughPages = true
                moveToPageIndex(it)
            },
            onPageIndexChangeFinished = {
                isScrollingThroughPages = false
            },

            readingMode = ReadingMode.fromPreference(
                viewModel.getMangaReadingMode(resolveDefault = false),
            ),
            onClickReadingMode = viewModel::openReadingModeSelectDialog,
            orientation = ReaderOrientation.fromPreference(
                viewModel.getMangaOrientation(resolveDefault = false),
            ),
            onClickOrientation = viewModel::openOrientationModeSelectDialog,
            cropEnabled = cropEnabled,
            onClickCropBorder = {
                val enabled = viewModel.toggleCropBorders()
                hint(if (enabled) MR.strings.kurotoon_reader_cropOn else MR.strings.kurotoon_reader_cropOff)
            },
            onClickSettings = viewModel::openSettingsDialog,
            autoScrolling = autoScroll.enabled,
            onClickAutoScroll = ::toggleAutoScroll,
            onClickChapterList = viewModel::openChapterList,
            soundActive = soundtrack.playing != null || soundtrack.activeScape != Soundscape.OFF,
            soundMuted = soundtrack.muted,
            onClickSound = viewModel::openSoundDialog,
        )
    }

    /** Keeps the soundtrack in step with the chapter, the page and the sound settings. */
    @Composable
    private fun ReaderSoundEffects(state: ReaderViewModel.State) {
        val episodeMusic by readerPreferences.soundEpisodeMusic.collectAsState()
        val ambience by readerPreferences.soundAmbience.collectAsState()
        val musicVolume by readerPreferences.soundMusicVolume.collectAsState()
        val ambienceVolume by readerPreferences.soundAmbienceVolume.collectAsState()
        val chapter = state.currentChapter
        val source = state.source as? HttpSource

        LaunchedEffect(musicVolume, ambienceVolume) {
            soundtrack.setVolumes(musicVolume / 100f, ambienceVolume / 100f)
        }
        val mixWithOthers by readerPreferences.soundMixWithOthers.collectAsState()
        LaunchedEffect(mixWithOthers) { soundtrack.setMixWithOthers(mixWithOthers) }
        LaunchedEffect(episodeMusic) { soundtrack.enableMusic(episodeMusic) }
        // Music "not working" is usually the media volume sitting at zero; say so once per visit
        val hearingMusic = soundtrack.playing != null && !soundtrack.muted
        LaunchedEffect(hearingMusic) {
            if (!hearingMusic || mediaVolumeHinted) return@LaunchedEffect
            val audio = getSystemService(android.media.AudioManager::class.java) ?: return@LaunchedEffect
            if (audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) == 0) {
                mediaVolumeHinted = true
                hint(MR.strings.kurotoon_sound_mediaVolumeOff)
            }
        }
        LaunchedEffect(chapter?.chapter?.id, chapter?.pages?.size, episodeMusic) {
            if (chapter?.pages == null) return@LaunchedEffect
            soundtrack.loadChapter(
                baseUrl = source?.baseUrl,
                chapterUrl = chapter.chapter.url,
                pageUrls = chapter.pages.orEmpty().map { it.imageUrl },
                client = source?.client,
            )
        }
        LaunchedEffect(state.currentPage) {
            if (state.currentPage > 0) soundtrack.onPage(state.currentPage - 1)
        }
        LaunchedEffect(state.manga?.id) {
            // This series' own ambience, if one was picked for it before
            val saved = state.manga?.id?.let(seriesAmbience::get) ?: return@LaunchedEffect
            if (saved != readerPreferences.soundAmbience.get()) readerPreferences.soundAmbience.set(saved)
        }
        LaunchedEffect(ambience, state.manga?.genre) {
            soundtrack.setSoundscape(Soundscape.of(ambience), state.manga?.genre)
        }
    }

    /** The sleep timer ran out: stop moving, go quiet and let the curtain ask what's next. */
    private fun onSleepTimer() {
        autoScroll.enabled = false
        soundtrack.mute(true)
        setMenuVisibility(false)
    }

    private fun changeAutoScrollSpeed(delta: Int) {
        val pref = readerPreferences.autoScrollSpeed
        val speed = (pref.get() + delta).coerceIn(1, 10)
        pref.set(speed)
        hint(stringResource(MR.strings.kurotoon_reader_autoScrollSpeedNow, speed))
    }

    private fun toggleAutoScroll() {
        autoScroll.enabled = !autoScroll.enabled
        menuToggleToast?.cancel()
        if (autoScroll.enabled) {
            val message = if (settingsViewModel.general.value.volumeKeysEnabled) {
                MR.strings.kurotoon_reader_autoScrollOnVolume
            } else {
                MR.strings.kurotoon_reader_autoScrollOn
            }
            hint(message)
            setMenuVisibility(false)
        }
    }

    /**
     * Sets the visibility of the menu according to [visible].
     */
    private fun setMenuVisibility(visible: Boolean) {
        viewModel.showMenus(visible)
        if (visible) {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        } else if (readerPreferences.fullscreen.get()) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    /**
     * Called from the presenter when a manga is ready. Used to instantiate the appropriate viewer.
     */
    private fun updateViewer() {
        val prevViewer = viewer
        val readingMode = viewModel.getMangaReadingMode()
        settingsViewModel.setViewerReadingMode(ReadingMode.fromPreference(readingMode))
        val newViewer = ReadingMode.toViewer(readingMode, this)

        setOrientation(viewModel.getMangaOrientation())

        // Destroy previous viewer if there was one
        prevViewer?.destroy()
        viewerFlow.value = newViewer

        if (readerPreferences.showReadingMode.get()) {
            showReadingModeToast(viewModel.getMangaReadingMode())
        }

        isLoadingViewer = true
    }

    private fun openMangaScreen() {
        viewModel.manga?.id?.let { id ->
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    action = Constants.SHORTCUT_MANGA
                    putExtra(Constants.MANGA_EXTRA, id)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                },
            )
        }
    }

    private fun openChapterInWebView() {
        val manga = viewModel.manga ?: return
        val source = viewModel.getSource() ?: return
        assistUrl?.let {
            val intent = WebViewActivity.newIntent(this@ReaderActivity, it, source.id, manga.title)
            startActivity(intent)
        }
    }

    private fun openChapterInBrowser() {
        assistUrl?.let {
            openInBrowser(it.toUri(), forceDefaultBrowser = false)
        }
    }

    private fun shareChapter() {
        assistUrl?.let {
            val intent = it.toUri().toShareIntent(this, type = "text/plain")
            startActivity(intent)
        }
    }

    /** A short message in the reader's own capsule rather than a system toast. */
    private fun hint(text: String?) {
        menuToggleToast?.cancel()
        readerHint.show(text)
    }

    private fun hint(res: dev.icerock.moko.resources.StringResource) = hint(stringResource(res))

    private fun showReadingModeToast(mode: Int) {
        try {
            hint(ReadingMode.fromPreference(mode).stringRes)
        } catch (_: ArrayIndexOutOfBoundsException) {
            logcat(LogPriority.ERROR) { "Unknown reading mode: $mode" }
        }
    }

    /**
     * Called from the presenter whenever a new [viewerChapters] have been set. It delegates the
     * method to the current viewer, but also set the subtitle on the toolbar, and
     * hides or disables the reader prev/next buttons if there's a prev or next chapter
     */
    private fun setChapters(viewerChapters: ViewerChapters) {
        isLoadingViewer = false
        viewer?.setChapters(viewerChapters)

        if (!resumeHintChecked) {
            resumeHintChecked = true
            val requested = viewerChapters.currChapter.requestedPage
            val total = viewerChapters.currChapter.pages?.size ?: 0
            if (requested in 1..<total) resumeHint.show(requested + 1)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.getChapterUrl()?.let { url ->
                assistUrl = url
            }
        }
    }

    /**
     * Called from the presenter if the initial load couldn't load the pages of the chapter. In
     * this case the activity is closed and a toast is shown to the user.
     */
    private fun setInitialChapterError(error: Throwable) {
        logcat(LogPriority.ERROR, error)
        finish()
        toast(error.message)
    }

    /**
     * Moves the viewer to the given page [index]. It does nothing if the viewer is null or the
     * page is not found.
     */
    private fun moveToPageIndex(index: Int) {
        val viewer = this.viewer ?: return
        val currentChapter = viewModel.state.value.currentChapter ?: return
        val page = currentChapter.pages?.getOrNull(index) ?: return
        viewer.moveToPage(page)
    }

    /**
     * Tells the presenter to load the next chapter and mark it as active. The progress dialog
     * should be automatically shown.
     */
    private fun loadNextChapter() {
        lifecycleScope.launch {
            viewModel.loadNextChapter()
            moveToPageIndex(0)
        }
    }

    /**
     * Tells the presenter to load the previous chapter and mark it as active. The progress dialog
     * should be automatically shown.
     */
    private fun loadPreviousChapter() {
        lifecycleScope.launch {
            viewModel.loadPreviousChapter()
            moveToPageIndex(0)
        }
    }

    /**
     * Called from the viewer whenever a [page] is marked as active. It updates the values of the
     * bottom menu and delegates the change to the presenter.
     */
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (ev.actionMasked == android.view.MotionEvent.ACTION_DOWN) wakefulness.poke()
        return super.dispatchTouchEvent(ev)
    }

    fun onPageSelected(page: ReaderPage) {
        wakefulness.poke()
        viewModel.onPageSelected(page)
    }

    /**
     * Called from the viewer whenever a [page] is long clicked. A bottom sheet with a list of
     * actions to perform is shown.
     */
    fun onPageLongTap(page: ReaderPage) {
        viewModel.openPageDialog(page)
    }

    /**
     * Called from the viewer when the given [chapter] should be preloaded. It should be called when
     * the viewer is reaching the beginning or end of a chapter or the transition page is active.
     */
    fun requestPreloadChapter(chapter: ReaderChapter) {
        lifecycleScope.launch(Dispatchers.IO) { viewModel.preload(chapter) }
    }

    /**
     * Called from the viewer to toggle the visibility of the menu. It's implemented on the
     * viewer because each one implements its own touch and key events.
     */
    fun toggleMenu() {
        setMenuVisibility(!viewModel.state.value.menuVisible)
    }

    /**
     * Called from the viewer to show the menu.
     */
    fun showMenu() {
        if (!viewModel.state.value.menuVisible) {
            setMenuVisibility(true)
        }
    }

    /**
     * Called from the viewer to hide the menu.
     */
    fun hideMenu() {
        if (viewModel.state.value.menuVisible) {
            setMenuVisibility(false)
        }
    }

    /**
     * Called from the presenter when a page is ready to be shared. It shows Android's default
     * sharing tool.
     */
    private fun onShareImageResult(uri: Uri, page: ReaderPage) {
        val manga = viewModel.manga ?: return
        val chapter = page.chapter.chapter

        val intent = uri.toShareIntent(
            context = applicationContext,
            message = stringResource(MR.strings.share_page_info, manga.title, chapter.name, page.number),
        )
        startActivity(intent)
    }

    private fun onCopyImageResult(uri: Uri) {
        val clipboardManager = applicationContext.getSystemService<ClipboardManager>() ?: return
        val clipData = ClipData.newUri(applicationContext.contentResolver, "", uri)
        clipboardManager.setPrimaryClip(clipData)
    }

    /**
     * Called from the presenter when a page is saved or fails. It shows a message or logs the
     * event depending on the [result].
     */
    private fun onSaveImageResult(result: ReaderViewModel.SaveImageResult) {
        when (result) {
            is ReaderViewModel.SaveImageResult.Success -> {
                hint(MR.strings.picture_saved)
            }
            is ReaderViewModel.SaveImageResult.Error -> {
                logcat(LogPriority.ERROR, result.error)
            }
        }
    }

    /**
     * Called from the presenter when a page is set as cover or fails. It shows a different message
     * depending on the [result].
     */
    private fun onSetAsCoverResult(result: ReaderViewModel.SetAsCoverResult) {
        hint(
            when (result) {
                Success -> MR.strings.cover_updated
                AddToLibraryFirst -> MR.strings.notification_first_add_to_library
                Error -> MR.strings.notification_cover_update_failed
            },
        )
    }

    /**
     * Forces the user preferred [orientation] on the activity.
     */
    private fun setOrientation(orientation: Int) {
        val newOrientation = ReaderOrientation.fromPreference(orientation)
        if (newOrientation.flag != requestedOrientation) {
            requestedOrientation = newOrientation.flag
        }
    }

    /**
     * Sets the brightness of the screen. Range is [-75, 100].
     * From -75 to -1 the minimum brightness is used, and the overlay darkens further.
     * From 1 to 100 it sets that value as brightness.
     * 0 sets system brightness.
     */
    private fun setWindowBrightness(value: Int) {
        // Calculate and set reader brightness.
        val readerBrightness = when {
            value > 0 -> {
                value / 100f
            }
            value < 0 -> {
                0.01f
            }
            else -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        window.attributes = window.attributes.apply { screenBrightness = readerBrightness }
    }
}

private fun viewerColorFilter(grayscale: Boolean, invertedColors: Boolean): ColorFilter? {
    if (!grayscale && !invertedColors) return null
    val matrix = ColorMatrix()
    if (invertedColors) {
        matrix *= ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    }
    if (grayscale) {
        matrix *= ColorMatrix().apply { setToSaturation(0f) }
    }
    return ColorFilter.colorMatrix(matrix)
}
