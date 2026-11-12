package eu.kanade.presentation.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.DisabledNavigation
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The tap-zone guide shown when a reading mode or tap layout changes: each zone as a rounded glass
 * panel, the zones that go forward in the accent, back in a light tint and the menu zone dark, each
 * labelled with a pill. Fades away on the first touch.
 */
@Composable
fun ReaderNavigationOverlay(
    state: ReaderNavigationOverlayState,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = state.visible,
        modifier = modifier,
        enter = fadeIn(tween(FADE_DURATION)),
        exit = fadeOut(tween(FADE_DURATION)),
    ) {
        val regions = state.regions
        val navigation = state.current
        val accent = MaterialTheme.colorScheme.primary
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f))) {
            regions.forEach { region ->
                val rect = region.rect
                val action = navigation?.actionOf(region.type) ?: ViewerNavigation.Action.MENU
                val (fill, edge) = when (action) {
                    ViewerNavigation.Action.FORWARD -> accent.copy(alpha = 0.42f) to accent.copy(alpha = 0.9f)
                    ViewerNavigation.Action.BACKWARD -> Color.White.copy(alpha = 0.18f) to
                        Color.White.copy(alpha = 0.5f)
                    ViewerNavigation.Action.MENU -> Color.Black.copy(alpha = 0.35f) to Color.White.copy(alpha = 0.16f)
                }
                val shape = RoundedCornerShape(24.dp)
                Box(
                    modifier = Modifier
                        .offset(x = maxWidth * rect.left, y = maxHeight * rect.top)
                        .size(width = maxWidth * rect.width, height = maxHeight * rect.height)
                        .padding(4.dp)
                        .background(fill, shape)
                        .border(1.5.dp, edge, shape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(region.type.nameRes),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (action == ViewerNavigation.Action.FORWARD) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            Color.White
                        },
                        modifier = Modifier
                            .background(
                                if (action ==
                                    ViewerNavigation.Action.FORWARD
                                ) {
                                    accent
                                } else {
                                    Color.Black.copy(alpha = 0.6f)
                                },
                                CircleShape,
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Stable
class ReaderNavigationOverlayState {

    internal var regions by mutableStateOf(emptyList<ViewerNavigation.Region>())
    internal var visible by mutableStateOf(false)
    internal var current by mutableStateOf<ViewerNavigation?>(null)
        private set

    private var navigation: ViewerNavigation? = null

    fun setNavigation(navigation: ViewerNavigation, showOnStart: Boolean) {
        val firstLaunch = this.navigation == null
        this.navigation = navigation
        current = navigation
        regions = navigation.getRegions()

        if (visible || (!showOnStart && firstLaunch) || navigation is DisabledNavigation) {
            return
        }
        visible = true
    }

    fun hide() {
        visible = false
    }
}

private const val FADE_DURATION = 600
