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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private const val SHOW_MS = 12_000L

/** "Chapter 5 is on MangaDex · Continue", when this copy of a series has run out. */
@Stable
class NextElsewhereState {
    data class Offer(val mangaId: Long, val chapterId: Long, val chapterName: String, val sourceName: String)

    internal var offer by mutableStateOf<Offer?>(null)
        private set

    fun show(offer: Offer) {
        this.offer = offer
    }

    fun dismiss() {
        offer = null
    }
}

@Composable
fun NextElsewhereHost(
    state: NextElsewhereState,
    onContinue: (NextElsewhereState.Offer) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(state.offer) {
        if (state.offer == null) return@LaunchedEffect
        delay(SHOW_MS)
        state.dismiss()
    }
    var last by androidx.compose.runtime.remember { mutableStateOf<NextElsewhereState.Offer?>(null) }
    state.offer?.let { last = it }

    Box(modifier) {
        AnimatedVisibility(
            visible = state.offer != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp, start = 24.dp, end = 24.dp),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            val offer = last ?: return@AnimatedVisibility
            Row(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xF0141418))
                    .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Column(Modifier.weight(1f, fill = false)) {
                    Text(
                        text = offer.chapterName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(MR.strings.kurotoon_reader_nextElsewhere, offer.sourceName),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = stringResource(MR.strings.kurotoon_reader_nextElsewhereGo),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            state.dismiss()
                            onContinue(offer)
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }
    }
}
