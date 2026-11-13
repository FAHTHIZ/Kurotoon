package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoresScreen
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.authenticate
import kurotoon.app.di.appGraph
import kurotoon.catalogue.HiddenTags
import kurotoon.domain.extension.model.ContentWarning
import kurotoon.ui.browse.HiddenTagsScreen
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

object SettingsBrowseScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.browse

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val sourcePreferences = remember { context.appGraph.sourcePreferences }
        val getExtensionStoreCountAsFlow = remember { context.appGraph.getExtensionStoreCountAsFlow }

        val reposCount by getExtensionStoreCountAsFlow().collectAsState(0)
        val hiddenTags by remember { context.appGraph.kuroPreferences.hiddenTags.changes() }
            .collectAsState(context.appGraph.kuroPreferences.hiddenTags.get())

        val hiddenSeries by remember { context.appGraph.kuroPreferences.hiddenSeries.changes() }
            .collectAsState(context.appGraph.kuroPreferences.hiddenSeries.get())
        val hiddenSeriesCount = hiddenSeries.size

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.label_sources),
                preferenceItems = listOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = sourcePreferences.hideInLibraryItems,
                        title = stringResource(MR.strings.pref_hide_in_library_items),
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.kurotoon_browse_hiddenTags),
                        subtitle = HiddenTags.names(hiddenTags).joinToString().ifEmpty {
                            stringResource(MR.strings.kurotoon_browse_hiddenTagsSummary)
                        },
                        onClick = { navigator.push(HiddenTagsScreen()) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.kurotoon_browse_hiddenSeries),
                        subtitle = pluralStringResource(
                            MR.plurals.kurotoon_browse_hiddenSeriesCount,
                            hiddenSeriesCount,
                            hiddenSeriesCount,
                        ),
                        onClick = { navigator.push(kurotoon.ui.browse.HiddenSeriesScreen()) },
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.extensionStores),
                        subtitle = pluralStringResource(MR.plurals.num_repos, reposCount.toInt(), reposCount),
                        onClick = {
                            navigator.push(ExtensionStoresScreen())
                        },
                    ),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.pref_category_extensions),
                preferenceItems = listOf(
                    Preference.PreferenceItem.MultiSelectListPreference(
                        preference = sourcePreferences.enabledContentWarnings,
                        entries = mapOf(
                            ContentWarning.SAFE to stringResource(MR.strings.ext_content_warning_safe),
                            ContentWarning.MIXED to stringResource(MR.strings.ext_content_warning_mixed),
                            ContentWarning.NSFW to stringResource(MR.strings.ext_content_warning_nsfw),
                        ),
                        title = stringResource(MR.strings.pref_allowed_content_warnings),
                        subtitleProvider = { value, entries ->
                            remember(value, entries) {
                                entries.filterKeys { it in value }.values.joinToString()
                            }
                                .takeUnless { it.isBlank() }
                                ?: stringResource(MR.strings.none)
                        },
                        onValueChanged = { newValue ->
                            val added = newValue - sourcePreferences.enabledContentWarnings.get()
                            if (added.any { it != ContentWarning.SAFE }) {
                                (context as FragmentActivity).authenticate(
                                    title = context.stringResource(MR.strings.pref_allowed_content_warnings),
                                )
                            } else {
                                true
                            }
                        },
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = sourcePreferences.applyContentWarningsToInstalled,
                        title = stringResource(MR.strings.pref_apply_content_warnings_to_installed),
                        subtitle = stringResource(MR.strings.pref_apply_content_warnings_to_installed_summary),
                        onValueChanged = { newValue ->
                            if (newValue) {
                                true
                            } else {
                                (context as FragmentActivity).authenticate(
                                    title = context.stringResource(
                                        MR.strings.pref_apply_content_warnings_to_installed,
                                    ),
                                )
                            }
                        },
                    ),
                    Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.content_warnings_info)),
                ),
            ),
        )
    }
}
