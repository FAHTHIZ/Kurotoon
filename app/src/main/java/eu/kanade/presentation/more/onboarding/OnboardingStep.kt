package eu.kanade.presentation.more.onboarding

import androidx.compose.runtime.Composable
import dev.icerock.moko.resources.StringResource

internal interface OnboardingStep {

    /** Big heading shown above the step. */
    val title: StringResource

    /** One line under the heading that says why the step is there. */
    val subtitle: StringResource

    val isComplete: Boolean

    /** Called when Next is pressed before the step is done, to take the user straight to what's missing. */
    fun onIncompleteAccept() {}

    @Composable
    fun Content()
}
