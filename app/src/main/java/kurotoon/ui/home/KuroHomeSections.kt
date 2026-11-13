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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.remember
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
import kurotoon.icons.materialsymbols.MaterialSymbols
import kurotoon.icons.materialsymbols.rounded.AutoAwesome
import kurotoon.icons.materialsymbols.rounded.Bookmark
import kurotoon.icons.materialsymbols.rounded.LocalFireDepartment
import kurotoon.icons.materialsymbols.roundedfilled.PlayArrow
import kurotoon.ui.KuroMetaLine
import kurotoon.ui.KuroPrimaryButton
import kurotoon.ui.KuroRankNumeral
import kurotoon.ui.KuroTag
import kurotoon.ui.kuroBottomScrim
import kurotoon.ui.kuroBounceClickable
import kurotoon.ui.kuroEntrance
import kurotoon.ui.kuroShimmer
import kurotoon.ui.onArtwork
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.absoluteValue

/**
 * A source's top title as a full-bleed artwork banner, the way WEBTOON and MANGA Plus open their home tab: the
 * cover fills the card, ink fades up from the bottom, and the title, tags and one Read button sit on the art.
 */
@Composable
internal fun HomeHero(
    sourceName: String,
    manga: Manga,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .kuroBounceClickable(onClick = onClick, pressed = 0.98f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        AsyncImage(
            model = manga.asMangaCover(),
            contentDescription = manga.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Box(Modifier.matchParentSize().kuroBottomScrim(start = 0.3f, strength = 0.96f))
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            KuroTag(
                text = stringResource(MR.strings.kurotoon_home_topOn, sourceName),
                container = Color.White.copy(alpha = 0.18f),
                content = Color.White,
            )
            Text(
                text = manga.title,
                style = onArtwork(MaterialTheme.typography.headlineMedium),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp),
            )
            val tags = manga.genre.orEmpty().take(3)
            if (tags.isNotEmpty()) {
                KuroMetaLine(
                    parts = tags,
                    color = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            KuroPrimaryButton(
                text = stringResource(MR.strings.kurotoon_home_readNow),
                icon = MaterialSymbols.RoundedFilled.PlayArrow,
                onClick = onClick,
                modifier = Modifier.padding(top = 16.dp),
            )
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
    // The same series can top two rows of one source (Popular and Latest); pager keys must be unique
    @Suppress("NAME_SHADOWING")
    val heroes = remember(heroes) { heroes.distinctBy { (row, manga) -> manga.url + row.sourceId } }
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
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 10.dp,
            key = { heroes[it].second.url + heroes[it].first.sourceId },
        ) { page ->
            val (row, manga) = heroes[page]
            HomeHero(
                sourceName = row.name,
                manga = manga,
                onClick = { onClick(manga) },
                modifier = Modifier.graphicsLayer {
                    val offset = pagerState.getOffsetDistanceInPages(page).absoluteValue.coerceIn(0f, 1f)
                    val scale = lerp(1f, 0.9f, offset)
                    scaleX = scale
                    scaleY = scale
                    alpha = lerp(1f, 0.45f, offset)
                },
            )
        }
        if (heroes.size > 1) {
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val primary = MaterialTheme.colorScheme.primary
                val idle = MaterialTheme.colorScheme.outlineVariant
                repeat(heroes.size) { i ->
                    val selected = pagerState.currentPage == i
                    val width by animateDpAsState(
                        targetValue = if (selected) 22.dp else 5.dp,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                        label = "heroDot",
                    )
                    val color by animateColorAsState(
                        targetValue = if (selected) primary else idle,
                        label = "heroDotColor",
                    )
                    Box(
                        Modifier
                            .height(5.dp)
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
                color = MaterialTheme.colorScheme.run { if (selected) onPrimary else onSurfaceVariant },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
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
    /** Long-press on a cover; null keeps covers tap-only. */
    onMangaLongClick: ((Manga) -> Unit)? = null,
    /** Off where the row's listing is fixed, e.g. one tag browsed on every source. */
    showListing: Boolean = true,
    modifier: Modifier = Modifier,
) {
    // Keep an empty Latest row so its switch can bring Popular back
    if (row.items?.isEmpty() == true && !row.latest) return
    Column(modifier) {
        val saved = row.savedSearch != null
        HomeShelfHeader(
            title = row.pickTag?.let { stringResource(MR.strings.kurotoon_home_becauseYouRead, it) } ?: row.name,
            icon = when {
                saved -> MaterialSymbols.Rounded.Bookmark
                row.pickTag != null -> MaterialSymbols.Rounded.AutoAwesome
                else -> MaterialSymbols.Rounded.LocalFireDepartment
            },
            subtitle = if (saved) {
                row.sourceName + row.freshUrls.size.takeIf { it > 0 }
                    ?.let { " · " + stringResource(MR.strings.kurotoon_home_newCount, it) }
                    .orEmpty()
            } else {
                null
            },
            onSeeAll = onSeeAll,
            extra = if (row.pickTag == null && !saved && showListing) {
                {
                    if (row.supportsLatest) {
                        ListingToggle(latest = row.latest, onClick = onToggleLatest)
                    } else {
                        Text(
                            text = stringResource(MR.strings.popular),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                null
            },
        )
        when {
            row.failed -> Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
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
                    color = MaterialTheme.colorScheme.primary,
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
                            .width(HomeCoverWidth)
                            .height(168.dp)
                            .kuroShimmer(RoundedCornerShape(12.dp), MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                }
            }
            else -> LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Popular rows read as a ranked Top list, like MANGA Plus' Hottest shelf
                val ranked = !row.latest && row.pickTag == null && !saved && showListing
                itemsIndexed(row.items.distinctBy { it.id }, key = { _, it -> it.id }) { index, manga ->
                    Column(modifier = Modifier.width(HomeCoverWidth).kuroEntrance(index)) {
                        Box(
                            Modifier.kuroBounceClickable(
                                onClick = { onMangaClick(manga) },
                                onLongClick = onMangaLongClick?.let { { it(manga) } },
                            ),
                        ) {
                            MangaCover.Book(
                                data = manga.asMangaCover(),
                                shape = RoundedCornerShape(12.dp),
                                contentDescription = manga.title,
                                modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                            )
                            if (ranked && index < 10) {
                                Box(
                                    Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .kuroBottomScrim(start = 0.55f, strength = 0.7f),
                                )
                                KuroRankNumeral(rank = index + 1, modifier = Modifier.padding(start = 6.dp))
                            }
                            if (manga.favorite) {
                                Icon(
                                    MaterialSymbols.Rounded.Bookmark,
                                    contentDescription = stringResource(MR.strings.in_library),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .padding(4.dp)
                                        .size(12.dp),
                                )
                            }
                            if (manga.url in row.freshUrls) {
                                Text(
                                    text = stringResource(MR.strings.kurotoon_home_newBadge),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
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
