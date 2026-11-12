package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.domain.ui.model.ThemeMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private val options = mapOf(
    ThemeMode.SYSTEM to MR.strings.theme_system,
    ThemeMode.LIGHT to MR.strings.theme_light,
    ThemeMode.DARK to MR.strings.theme_dark,
)

@Composable
internal fun AppThemeModePreferenceWidget(
    value: ThemeMode,
    onItemClick: (ThemeMode) -> Unit,
) {
    BasePreferenceWidget(
        subcomponent = {
            val modes = options.keys.toList()
            kurotoon.ui.KuroSegmentedChoice(
                titles = options.values.map { stringResource(it) },
                selected = modes.indexOf(value),
                onSelect = { onItemClick(modes[it]) },
                modifier = Modifier.padding(horizontal = PrefsHorizontalPadding),
            )
        },
    )
}
