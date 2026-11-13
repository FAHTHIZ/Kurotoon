package kurotoon.ui.similar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kurotoon.app.di.appGraph
import kurotoon.icons.materialsymbols.MaterialSymbols
import kurotoon.icons.materialsymbols.rounded.Refresh
import kurotoon.similar.SimilarResult
import kurotoon.similar.SimilarTitle
import kurotoon.similar.SimilarTitles
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen

/**
 * "More like this" for a series: titles AniList readers recommend alongside it. Tapping one runs a
 * search across the user's sources so it can be opened from wherever they read.
 */
class SimilarScreen(private val title: String) : Screen() {

    private sealed interface Load {
        data object Loading : Load
        data class Done(val result: SimilarResult) : Load
        data object Failed : Load
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val finder = remember { SimilarTitles(context.appGraph.networkHelper.client) }
        var attempt by rememberSaveable { mutableIntStateOf(0) }
        var load by remember { mutableStateOf<Load>(Load.Loading) }

        LaunchedEffect(attempt) {
            load = Load.Loading
            load = try {
                val result = withContext(Dispatchers.IO) { finder.find(title) }
                if (result == null) Load.Done(SimilarResult("", emptyList())) else Load.Done(result)
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Similar titles lookup failed" }
                Load.Failed
            }
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.kurotoon_similar_title),
                    subtitle = title,
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            AnimatedContent(
                targetState = load,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "similar",
                modifier = Modifier.padding(padding),
            ) { state ->
                when (state) {
                    Load.Loading -> LoadingScreen()
                    Load.Failed -> EmptyScreen(
                        message = stringResource(MR.strings.kurotoon_similar_failed),
                        actions = listOf(
                            EmptyScreenAction(MR.strings.action_retry, MaterialSymbols.Rounded.Refresh) { attempt++ },
                        ),
                    )
                    is Load.Done -> if (state.result.titles.isEmpty()) {
                        EmptyScreen(message = stringResource(MR.strings.kurotoon_similar_none))
                    } else {
                        SimilarGrid(state.result) { navigator.push(GlobalSearchScreen(it.title)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimilarGrid(result: SimilarResult, onClick: (SimilarTitle) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (result.matchedTitle.isNotBlank()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(MR.strings.kurotoon_similar_basedOn, result.matchedTitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
        items(result.titles, key = { it.title }) { item ->
            SimilarCard(item, Modifier.animateItem()) { onClick(item) }
        }
    }
}

@Composable
private fun SimilarCard(item: SimilarTitle, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(modifier = modifier.clip(shape).clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            AsyncImage(
                model = item.coverUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            val label = listOfNotNull(kindLabel(item), item.score?.let { "$it%" }).joinToString(" · ")
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                        .padding(start = 8.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
                )
            }
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 6.dp),
        )
    }
}

private fun kindLabel(item: SimilarTitle): String? = when (item.country) {
    "KR" -> "Manhwa"
    "CN", "TW" -> "Manhua"
    "JP" -> if (item.format == "NOVEL") "Novel" else "Manga"
    else -> null
}
