package kurotoon.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil3.compose.AsyncImage
import eu.kanade.presentation.manga.components.MangaCover
import kotlinx.coroutines.delay
import kurotoon.ui.KuroColors
import kurotoon.ui.kuroCard
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.AutoAwesome
import mihon.icons.materialsymbols.rounded.Bookmark
import mihon.icons.materialsymbols.rounded.KeyboardArrowRight
import mihon.icons.materialsymbols.rounded.LocalFireDepartment
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.absoluteValue

/** A source's top title, presented like the web app's hero banner. */
@Composable
internal fun HomeHero(
    sourceName: String,
    manga: Manga,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .kuroCard(shape)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = manga.asMangaCover(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .blur(24.dp)
                .alpha(0.32f),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(KuroColors.Crimson.copy(alpha = 0.14f), MaterialTheme.colorScheme.background.copy(0.6f)),
                    ),
                ),
        )
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MangaCover.Book(
                data = manga.asMangaCover(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.width(96.dp),
                contentDescription = manga.title,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(144.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Row(
                    modifier = Modifier
                        .background(KuroColors.Crimson.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, KuroColors.Crimson.copy(alpha = 0.25f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        MaterialSymbols.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = KuroColors.Crimson,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = stringResource(MR.strings.kurotoon_home_topOn, sourceName).uppercase(),
                        color = KuroColors.Crimson,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = manga.title,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
                val tags = manga.genre.orEmpty().take(4)
                if (tags.isNotEmpty()) {
                    Text(
                        text = tags.joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(KuroColors.Crimson, KuroColors.CrimsonDeep)))
                        .clickable(onClick = onClick)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(MR.strings.kurotoon_home_readNow),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/**
 * The top titles as a swipeable carousel that moves on by itself every few seconds. It holds
 * still while touched, and the pages beside the current one shrink and fade a little.
 */
@Composable
internal fun HomeHeroCarousel(
    heroes: List<Pair<KuroHomeViewModel.SourceRow, Manga>>,
    onClick: (Manga) -> Unit,
) {
    val pagerState = rememberPagerState { heroes.size }
    val dragged by pagerState.interactionSource.collectIsDraggedAsState()
    val pressed by pagerState.interactionSource.collectIsPressedAsState()
    if (heroes.size > 1) {
        LaunchedEffect(pagerState, dragged, pressed, heroes.size) {
            if (dragged || pressed) return@LaunchedEffect
            while (true) {
                delay(HERO_INTERVAL_MS)
                val next = (pagerState.currentPage + 1) % heroes.size
                pagerState.animateScrollToPage(next, animationSpec = tween(700, easing = FastOutSlowInEasing))
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            key = { heroes[it].second.url + heroes[it].first.sourceId },
        ) { page ->
            val (row, manga) = heroes[page]
            HomeHero(
                sourceName = row.name,
                manga = manga,
                onClick = { onClick(manga) },
                modifier = Modifier.graphicsLayer {
                    val offset = pagerState.getOffsetDistanceInPages(page).absoluteValue.coerceIn(0f, 1f)
                    val scale = lerp(1f, 0.92f, offset)
                    scaleX = scale
                    scaleY = scale
                    alpha = lerp(1f, 0.55f, offset)
                },
            )
        }
        if (heroes.size > 1) {
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(heroes.size) { i ->
                    val selected = pagerState.currentPage == i
                    val width by animateDpAsState(
                        targetValue = if (selected) 20.dp else 6.dp,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                        label = "heroDot",
                    )
                    val color by animateColorAsState(
                        targetValue = if (selected) KuroColors.Crimson else Color.White.copy(alpha = 0.22f),
                        label = "heroDotColor",
                    )
                    Box(
                        Modifier
                            .height(6.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(color),
                    )
                }
            }
        }
    }
}

private const val HERO_INTERVAL_MS = 5_000L

/** Popular / Latest switch next to a source row's title. */
@Composable
private fun ListingToggle(latest: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(start = 8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(2.dp),
    ) {
        listOf(false, true).forEach { option ->
            val selected = option == latest
            Text(
                text = stringResource(if (option) MR.strings.latest else MR.strings.popular),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected) KuroColors.Crimson else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

/** A source's popular or latest titles in a horizontal row. */
@Composable
internal fun HomeSourceRow(
    row: KuroHomeViewModel.SourceRow,
    onSeeAll: () -> Unit,
    onMangaClick: (Manga) -> Unit,
    onRetry: () -> Unit,
    onToggleLatest: () -> Unit = {},
) {
    // Keep an empty Latest row so its switch can bring Popular back
    if (row.items?.isEmpty() == true && !row.latest) return
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(KuroColors.Crimson.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .border(1.dp, KuroColors.Crimson.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (row.savedSearch != null) {
                        MaterialSymbols.Rounded.Bookmark
                    } else if (row.pickTag != null) {
                        MaterialSymbols.Rounded.AutoAwesome
                    } else {
                        MaterialSymbols.Rounded.LocalFireDepartment
                    },
                    contentDescription = null,
                    tint = KuroColors.Crimson,
                    modifier = Modifier.size(16.dp),
                )
            }
            val saved = row.savedSearch != null
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    // Saved searches and "Because you read X" take the whole line
                    .weight(1f, fill = saved || row.pickTag != null),
            ) {
                Text(
                    text = row.pickTag?.let { stringResource(MR.strings.kurotoon_home_becauseYouRead, it) } ?: row.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (saved) {
                    Text(
                        text = row.sourceName + row.freshUrls.size.takeIf { it > 0 }
                            ?.let { " · " + stringResource(MR.strings.kurotoon_home_newCount, it) }
                            .orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (row.pickTag == null && !saved) {
                Box(Modifier.weight(1f)) {
                    when {
                        row.supportsLatest -> ListingToggle(latest = row.latest, onClick = onToggleLatest)
                        else -> Text(
                            text = " · " + stringResource(MR.strings.popular),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onSeeAll)
                    .padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(MR.strings.kurotoon_home_seeAll),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    MaterialSymbols.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        when {
            row.failed -> Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .kuroCard()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(MR.strings.kurotoon_home_sourceUnavailable, row.name),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(MR.strings.action_retry),
                    style = MaterialTheme.typography.labelMedium,
                    color = KuroColors.Crimson,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onRetry)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            row.items == null -> LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false,
            ) {
                items(6) {
                    Box(
                        Modifier
                            .width(112.dp)
                            .height(168.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp)),
                    )
                }
            }
            else -> LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(row.items, key = { it.id }) { manga ->
                    Column(modifier = Modifier.width(112.dp)) {
                        Box {
                            MangaCover.Book(
                                data = manga.asMangaCover(),
                                shape = RoundedCornerShape(12.dp),
                                contentDescription = manga.title,
                                onClick = { onMangaClick(manga) },
                            )
                            if (manga.url in row.freshUrls) {
                                Text(
                                    text = stringResource(MR.strings.kurotoon_home_newBadge),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(6.dp)
                                        .clip(CircleShape)
                                        .background(KuroColors.Crimson)
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Text(
                            text = manga.title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
