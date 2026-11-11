package kurotoon.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private const val SHOW_MS = 4_500L

/** "Picked up on page 12 · Start over", shown once when a chapter opens part-way through. */
@Stable
class ResumeHintState {
    internal var page by mutableIntStateOf(0)
        private set
    internal var visible by mutableStateOf(false)

    fun show(page: Int) {
        this.page = page
        visible = true
    }

    fun dismiss() {
        visible = false
    }
}

@Composable
fun ResumeHintHost(
    state: ResumeHintState,
    menuVisible: Boolean,
    onStartOver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(state.visible, state.page) {
        if (!state.visible) return@LaunchedEffect
        delay(SHOW_MS)
        state.dismiss()
    }
    // The menu has its own slider; don't stack on top of it.
    LaunchedEffect(menuVisible) { if (menuVisible) state.dismiss() }

    Box(modifier) {
        AnimatedVisibility(
            visible = state.visible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 44.dp, start = 24.dp, end = 24.dp),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xE6141418))
                    .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(MR.strings.kurotoon_reader_resumedAt, state.page),
                    color = Color.White,
                    fontSize = 13.sp,
                )
                Text(
                    text = stringResource(MR.strings.kurotoon_reader_startOver),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            state.dismiss()
                            onStartOver()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}
