package eu.kanade.presentation.more.onboarding

import androidx.compose.runtime.Composable

internal interface OnboardingStep {

    val isComplete: Boolean

    /** Called when Next is pressed before the step is done, to take the user straight to what's missing. */
    fun onIncompleteAccept() {}

    @Composable
    fun Content()
}
