package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import kurotoon.icons.materialsymbols.MaterialSymbols
import kurotoon.icons.materialsymbols.rounded.Extension
import kurotoon.icons.materialsymbols.rounded.History
import kurotoon.icons.materialsymbols.rounded.LocalLibrary
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** The last step: what to do first. Get started finishes; the cards are shortcuts for people who know already. */
internal class GuidesStep(
    private val onRestoreBackup: () -> Unit,
    private val onAddSources: () -> Unit,
) : OnboardingStep {

    override val title = MR.strings.onboarding_ready_title
    override val subtitle = MR.strings.onboarding_ready_subtitle

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val handler = LocalUriHandler.current

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OnboardingChoice(
                icon = MaterialSymbols.Rounded.Extension,
                title = stringResource(MR.strings.onboarding_ready_sources_title),
                body = stringResource(MR.strings.onboarding_ready_sources_body),
                selected = false,
                radio = false,
                onClick = onAddSources,
            )
            OnboardingChoice(
                icon = MaterialSymbols.Rounded.History,
                title = stringResource(MR.strings.onboarding_ready_restore_title),
                body = stringResource(MR.strings.onboarding_ready_restore_body),
                selected = false,
                radio = false,
                onClick = onRestoreBackup,
            )
            OnboardingChoice(
                icon = MaterialSymbols.Rounded.LocalLibrary,
                title = stringResource(MR.strings.getting_started_guide),
                body = stringResource(MR.strings.onboarding_ready_guide_body),
                selected = false,
                radio = false,
                onClick = { handler.openUri(GETTING_STARTED_URL) },
            )
        }
    }
}

const val GETTING_STARTED_URL = "https://github.com/FAHTHIZ/Kurotoon/blob/main/docs/getting-started.md"

@PreviewLightDark
@Composable
private fun GuidesStepPreview() {
    TachiyomiPreviewTheme {
        GuidesStep(
            onRestoreBackup = {},
            onAddSources = {},
        ).Content()
    }
}
