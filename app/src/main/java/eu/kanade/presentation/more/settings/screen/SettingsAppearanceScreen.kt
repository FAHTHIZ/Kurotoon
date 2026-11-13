package eu.kanade.presentation.more.settings.screen

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.core.app.ActivityCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.domain.ui.model.TimeFormat
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.appearance.AppLanguageScreen
import eu.kanade.presentation.more.settings.widget.AppThemeModePreferenceWidget
import eu.kanade.presentation.more.settings.widget.AppThemePreferenceWidget
import eu.kanade.presentation.util.rememberIs24HourSystem
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import kurotoon.app.di.appGraph
import kurotoon.ui.home.HomeSection
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import kotlin.time.Clock

object SettingsAppearanceScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_appearance

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val uiPreferences = remember { context.appGraph.uiPreferences }

        return listOf(
            getThemeGroup(uiPreferences = uiPreferences),
            getDisplayGroup(uiPreferences = uiPreferences),
        )
    }

    @Composable
    private fun getThemeGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current

        val themeModePref = uiPreferences.themeMode
        val themeMode by themeModePref.collectAsState()

        val appThemePref = uiPreferences.appTheme
        val appTheme by appThemePref.collectAsState()

        val amoledPref = uiPreferences.themeDarkAmoled
        val amoled by amoledPref.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_theme),
            preferenceItems = listOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_app_theme),
                ) {
                    Column {
                        AppThemeModePreferenceWidget(
                            value = themeMode,
                            onItemClick = {
                                themeModePref.set(it)
                                setAppCompatDelegateThemeMode(it)
                            },
                        )

                        AppThemePreferenceWidget(
                            value = appTheme,
                            amoled = amoled,
                            onItemClick = { appThemePref.set(it) },
                        )
                    }
                },
                Preference.PreferenceItem.SwitchPreference(
                    preference = amoledPref,
                    title = stringResource(MR.strings.pref_dark_theme_pure_black),
                    visible = themeMode != ThemeMode.LIGHT,
                    onValueChanged = {
                        (context as? Activity)?.let { ActivityCompat.recreate(it) }
                        true
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getDisplayGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val locale = LocalLocale.current
        val is24HourSystem = rememberIs24HourSystem()

        val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime() }

        val dateFormat by uiPreferences.dateFormat.collectAsState()
        val reminderOn by context.appGraph.kuroPreferences.readingReminder.collectAsState()
        val formattedDateNow = remember(dateFormat) {
            UiPreferences.dateFormat(dateFormat, locale.platformLocale).format(now)
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_display),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.pref_app_language),
                    onClick = { navigator.push(AppLanguageScreen()) },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.tabletUiMode,
                    entries = TabletUiMode.entries
                        .associateWith { stringResource(it.titleRes) },
                    title = stringResource(MR.strings.pref_tablet_ui_mode),
                    onValueChanged = {
                        context.toast(MR.strings.requires_app_restart)
                        true
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.dateFormat,
                    entries = DateFormats
                        .associateWith {
                            val formattedDate = UiPreferences.dateFormat(it, locale.platformLocale).format(now)
                            "${it.ifEmpty { stringResource(MR.strings.label_default) }} ($formattedDate)"
                        },
                    title = stringResource(MR.strings.pref_date_format),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.timeFormat,
                    entries = TimeFormat.entries
                        .associateWith {
                            val formattedTime = UiPreferences.timeFormat(
                                it,
                                locale.platformLocale,
                                is24HourSystem,
                            ).format(now)
                            "${stringResource(it.titleRes)} ($formattedTime)"
                        },
                    title = stringResource(MR.strings.pref_time_format),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.relativeTime,
                    title = stringResource(MR.strings.pref_relative_format),
                    subtitle = stringResource(
                        MR.strings.pref_relative_format_summary,
                        stringResource(MR.strings.relative_time_today),
                        formattedDateNow,
                    ),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.imagesInDescription,
                    title = stringResource(MR.strings.pref_display_images_description),
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = context.appGraph.kuroPreferences.homeHiddenSections,
                    entries = mapOf(
                        HomeSection.HERO.key to stringResource(MR.strings.kurotoon_section_hero),
                        HomeSection.SHORTCUTS.key to stringResource(MR.strings.kurotoon_section_shortcuts),
                        HomeSection.CONTINUE.key to stringResource(MR.strings.kurotoon_home_continueReading),
                        HomeSection.NEW_CHAPTERS.key to stringResource(MR.strings.kurotoon_home_newChapters),
                        HomeSection.SCHEDULE.key to stringResource(MR.strings.kurotoon_section_schedule),
                        HomeSection.STALLED.key to stringResource(MR.strings.kurotoon_home_stalled),
                        HomeSection.BINGE.key to stringResource(MR.strings.kurotoon_home_binge),
                        HomeSection.RECENTLY_ADDED.key to stringResource(MR.strings.kurotoon_home_recentlyAdded),
                        HomeSection.PANELS.key to stringResource(MR.strings.kurotoon_home_panels),
                        HomeSection.PICKS.key to stringResource(MR.strings.kurotoon_section_picks),
                        HomeSection.SAVED.key to stringResource(MR.strings.kurotoon_section_saved),
                        HomeSection.SOURCES.key to stringResource(MR.strings.kurotoon_section_sources),
                    ),
                    title = stringResource(MR.strings.kurotoon_settings_homeHidden),
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = context.appGraph.kuroPreferences.navTabs,
                    entries = mapOf(
                        "browse" to stringResource(MR.strings.browse),
                        "library" to stringResource(MR.strings.label_library),
                        "updates" to stringResource(MR.strings.label_recent_updates),
                        "history" to stringResource(MR.strings.label_recent_manga),
                    ),
                    title = stringResource(MR.strings.kurotoon_settings_navTabs),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = context.appGraph.kuroPreferences.startTab,
                    entries = mapOf(
                        "home" to stringResource(MR.strings.kurotoon_tab_home),
                        "browse" to stringResource(MR.strings.browse),
                        "library" to stringResource(MR.strings.label_library),
                        "updates" to stringResource(MR.strings.label_recent_updates),
                        "history" to stringResource(MR.strings.label_recent_manga),
                    ),
                    title = stringResource(MR.strings.kurotoon_settings_startTab),
                    subtitle = stringResource(MR.strings.kurotoon_settings_startTabSummary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = context.appGraph.kuroPreferences.particles,
                    title = stringResource(MR.strings.kurotoon_settings_particles),
                    subtitle = stringResource(MR.strings.kurotoon_settings_particlesSummary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = context.appGraph.kuroPreferences.ambientEmbers,
                    title = stringResource(MR.strings.kurotoon_settings_embers),
                    subtitle = stringResource(MR.strings.kurotoon_settings_embersSummary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = context.appGraph.kuroPreferences.coverColors,
                    title = stringResource(MR.strings.kurotoon_settings_coverColors),
                    subtitle = stringResource(MR.strings.kurotoon_settings_coverColorsSummary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = context.appGraph.kuroPreferences.coverProgress,
                    title = stringResource(MR.strings.kurotoon_settings_coverProgress),
                    subtitle = stringResource(MR.strings.kurotoon_settings_coverProgressSummary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = context.appGraph.kuroPreferences.readingReminder,
                    title = stringResource(MR.strings.kurotoon_settings_reminder),
                    subtitle = stringResource(MR.strings.kurotoon_settings_reminderSummary),
                    onValueChanged = {
                        kurotoon.reminder.ReadingReminderWorker.setupTask(context, enabled = it)
                        true
                    },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = context.appGraph.kuroPreferences.readingReminderHour,
                    entries = (6..23).associateWith { "%02d:00".format(it) },
                    title = stringResource(MR.strings.kurotoon_settings_reminderHour),
                    visible = reminderOn,
                    onValueChanged = {
                        kurotoon.reminder.ReadingReminderWorker.setupTask(context, hour = it)
                        true
                    },
                ),
            ),
        )
    }
}

private val DateFormats = listOf(
    "", // Default
    "MM/dd/yy",
    "dd/MM/yy",
    "yyyy-MM-dd",
    "dd MMM yyyy",
    "MMM dd, yyyy",
)
