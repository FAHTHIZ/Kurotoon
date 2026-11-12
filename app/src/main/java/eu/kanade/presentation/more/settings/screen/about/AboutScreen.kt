package eu.kanade.presentation.more.settings.screen.about

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.rememberIs24HourSystem
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.updater.RELEASE_URL
import eu.kanade.tachiyomi.ui.more.NewUpdateScreen
import eu.kanade.tachiyomi.util.lang.toDateTimestampString
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.isFossBuildType
import eu.kanade.tachiyomi.util.system.isNightlyBuildType
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.updaterEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kurotoon.ui.profile.MeGroup
import logcat.LogPriority
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Public
import mihon.icons.simpleicons.Discord
import mihon.icons.simpleicons.Facebook
import mihon.icons.simpleicons.Github
import mihon.icons.simpleicons.Reddit
import mihon.icons.simpleicons.SimpleIcons
import mihon.icons.simpleicons.X
import tachiyomi.core.common.Constants
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.release.interactor.GetApplicationRelease
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.KuroSpinner
import tachiyomi.presentation.core.components.LinkIcon
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import java.util.Locale
import kotlin.time.Instant

object AboutScreen : Screen() {

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val handleBack = LocalBackPress.current
        val navigator = LocalNavigator.currentOrThrow
        var isCheckingUpdates by remember { mutableStateOf(false) }
        val crashLogUtil = remember { context.appGraph.crashLogUtil }
        val is24HourSystem = rememberIs24HourSystem()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.pref_category_about),
                    navigateUp = if (handleBack != null) handleBack::invoke else null,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            ScrollbarLazyColumn(
                contentPadding = contentPadding,
            ) {
                item { KuroAboutHero(version = getVersionName(buildTime = null)) }

                item {
                    MeGroup(title = stringResource(MR.strings.pref_category_about)) {
                        TextPreferenceWidget(
                            title = stringResource(MR.strings.version),
                            // Keyed on the system 12/24h setting, which can change while the app is backgrounded.
                            // Date/time prefs only change in Appearance, which recreates this screen on return.
                            subtitle = remember(is24HourSystem) {
                                getVersionName(buildTime = getFormattedBuildTime(context))
                            },
                            onPreferenceClick = {
                                val deviceInfo = crashLogUtil.getDebugInfo()
                                context.copyToClipboard("Debug information", deviceInfo)
                            },
                        )

                        if (updaterEnabled) {
                            TextPreferenceWidget(
                                title = stringResource(MR.strings.check_for_updates),
                                widget = {
                                    AnimatedVisibility(visible = isCheckingUpdates) {
                                        KuroSpinner(
                                            size = 28.dp,
                                            strokeWidth = 3.dp,
                                        )
                                    }
                                },
                                onPreferenceClick = {
                                    if (!isCheckingUpdates) {
                                        scope.launch {
                                            isCheckingUpdates = true

                                            checkVersion(
                                                context = context,
                                                onAvailableUpdate = { result ->
                                                    val updateScreen = NewUpdateScreen(
                                                        versionName = result.release.version,
                                                        changelogInfo = result.release.info,
                                                        releaseLink = result.release.releaseLink,
                                                        downloadLink = result.release.downloadLink,
                                                    )
                                                    navigator.push(updateScreen)
                                                },
                                                onFinish = {
                                                    isCheckingUpdates = false
                                                },
                                            )
                                        }
                                    }
                                },
                            )
                        }

                        if (!BuildConfig.DEBUG) {
                            TextPreferenceWidget(
                                title = stringResource(MR.strings.whats_new),
                                onPreferenceClick = { uriHandler.openUri(RELEASE_URL) },
                            )
                        }

                        TextPreferenceWidget(
                            title = stringResource(MR.strings.licenses),
                            onPreferenceClick = { navigator.push(OpenSourceLicensesScreen()) },
                        )

                        TextPreferenceWidget(
                            title = stringResource(MR.strings.privacy_policy),
                            onPreferenceClick = {
                                uriHandler.openUri("https://github.com/FAHTHIZ/Kurotoon/blob/main/docs/privacy.md")
                            },
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        LinkIcon(
                            label = "GitHub",
                            icon = SimpleIcons.Github,
                            url = Constants.URL_REPO,
                        )
                    }
                }
            }
        }
    }

    /**
     * Checks version and shows a user prompt if an update is available.
     */
    private suspend fun checkVersion(
        context: Context,
        onAvailableUpdate: (GetApplicationRelease.Result.NewUpdate) -> Unit,
        onFinish: () -> Unit,
    ) {
        val updateChecker = context.appGraph.updateChecker
        withContext(Dispatchers.Main) {
            try {
                when (val result = updateChecker.checkForUpdate(forceCheck = true)) {
                    is GetApplicationRelease.Result.NewUpdate -> {
                        onAvailableUpdate(result)
                    }
                    is GetApplicationRelease.Result.NoNewUpdate -> {
                        context.toast(MR.strings.update_check_no_new_updates)
                    }
                    is GetApplicationRelease.Result.OsTooOld -> {
                        context.toast(MR.strings.update_check_eol)
                    }
                }
            } catch (e: Exception) {
                context.toast(e.message)
                logcat(LogPriority.ERROR, e)
            } finally {
                onFinish()
            }
        }
    }

    fun getVersionName(buildTime: String?): String {
        return when {
            BuildConfig.DEBUG -> {
                "Debug ${BuildConfig.COMMIT_SHA}".let {
                    if (buildTime != null) {
                        "$it ($buildTime)"
                    } else {
                        it
                    }
                }
            }
            isNightlyBuildType -> {
                "Nightly r${BuildConfig.COMMIT_COUNT}".let {
                    if (buildTime != null) {
                        "$it (${BuildConfig.COMMIT_SHA}, $buildTime)"
                    } else {
                        "$it (${BuildConfig.COMMIT_SHA})"
                    }
                }
            }
            else -> {
                val channel = if (isFossBuildType) "FOSS" else "Stable"
                "$channel v${BuildConfig.VERSION_NAME}".let {
                    if (buildTime != null) {
                        "$it ($buildTime)"
                    } else {
                        it
                    }
                }
            }
        }
    }

    internal fun getFormattedBuildTime(context: Context): String {
        return try {
            val uiPreferences = context.appGraph.uiPreferences
            Instant.parse(BuildConfig.BUILD_TIME)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .toDateTimestampString(
                    UiPreferences.dateTimeFormat(
                        uiPreferences.dateFormat.get(),
                        uiPreferences.timeFormat.get(),
                        Locale.getDefault(),
                        DateFormat.is24HourFormat(context),
                    ),
                )
        } catch (_: Exception) {
            BuildConfig.BUILD_TIME
        }
    }
}

/** KuroToon's About header: the logo, the name, the tagline and the version as a pill. */
@Composable
private fun KuroAboutHero(version: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        kurotoon.ui.KuroLogo(size = 72.dp, modifier = Modifier.padding(bottom = 8.dp))
        Text(
            text = stringResource(MR.strings.app_name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(MR.strings.kurotoon_about_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        Text(
            text = version,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}
