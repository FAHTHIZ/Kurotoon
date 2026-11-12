package eu.kanade.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import kurotoon.reader.caughtUpLabel
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.Info
import mihon.icons.materialsymbols.roundedfilled.CheckCircle
import mihon.icons.materialsymbols.roundedfilled.Warning
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.service.calculateChapterGap
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.secondaryItemAlpha

@Composable
fun ChapterTransition(
    transition: ChapterTransition,
    currChapterDownloaded: Boolean,
    goingToChapterDownloaded: Boolean,
) {
    val currChapter = transition.from.chapter
    val goingToChapter = transition.to?.chapter

    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
        when (transition) {
            is ChapterTransition.Prev -> {
                TransitionText(
                    topLabel = stringResource(MR.strings.transition_previous),
                    topChapter = goingToChapter,
                    topChapterDownloaded = goingToChapterDownloaded,
                    bottomLabel = stringResource(MR.strings.transition_current),
                    bottomChapter = currChapter,
                    bottomChapterDownloaded = currChapterDownloaded,
                    fallbackLabel = stringResource(MR.strings.transition_no_previous),
                    chapterGap = calculateChapterGap(currChapter, goingToChapter),
                    headingDown = false,
                )
            }
            is ChapterTransition.Next -> {
                TransitionText(
                    topLabel = stringResource(MR.strings.transition_finished),
                    topChapter = currChapter,
                    topChapterDownloaded = currChapterDownloaded,
                    bottomLabel = stringResource(MR.strings.transition_next),
                    bottomChapter = goingToChapter,
                    bottomChapterDownloaded = goingToChapterDownloaded,
                    fallbackLabel = if (goingToChapter == null) {
                        caughtUpLabel(currChapter.mangaId)
                    } else {
                        stringResource(MR.strings.transition_no_next)
                    },
                    chapterGap = calculateChapterGap(goingToChapter, currChapter),
                    headingDown = true,
                )
            }
        }
    }
}

@Composable
private fun TransitionText(
    topLabel: String,
    topChapter: Chapter?,
    topChapterDownloaded: Boolean,
    bottomLabel: String,
    bottomChapter: Chapter?,
    bottomChapterDownloaded: Boolean,
    fallbackLabel: String,
    chapterGap: Int,
    // The chapter you're heading to is the one that matters: Next highlights the bottom, Prev the top
    headingDown: Boolean,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (topChapter != null) {
            if (headingDown) {
                QuietChapter(label = topLabel, chapter = topChapter, downloaded = topChapterDownloaded, done = true)
            } else {
                DestinationCard(label = topLabel, chapter = topChapter, downloaded = topChapterDownloaded)
            }
        } else {
            NoChapterNotification(text = fallbackLabel)
        }

        if (bottomChapter != null && topChapter != null && chapterGap > 0) {
            ChapterGapWarning(gapCount = chapterGap, modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        if (bottomChapter != null) {
            if (headingDown) {
                DestinationCard(label = bottomLabel, chapter = bottomChapter, downloaded = bottomChapterDownloaded)
            } else {
                QuietChapter(label = bottomLabel, chapter = bottomChapter, downloaded = bottomChapterDownloaded)
            }
        } else if (topChapter != null) {
            NoChapterNotification(text = fallbackLabel)
        }
    }
}

/** The chapter you're leaving: small and dimmed, with a check once it's finished. */
@Composable
private fun QuietChapter(
    label: String,
    chapter: Chapter,
    downloaded: Boolean,
    done: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (done) MaterialSymbols.Rounded.Check else MaterialSymbols.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ChapterName(
                name = chapter.name,
                downloaded = downloaded,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The chapter you're heading to, as a tinted card with an accent label. */
@Composable
private fun DestinationCard(
    label: String,
    chapter: Chapter,
    downloaded: Boolean,
) {
    val shape = RoundedCornerShape(24.dp)
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.32f), shape)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .clip(CircleShape)
                .background(accent)
                .padding(horizontal = 10.dp, vertical = 3.dp),
        )
        Spacer(Modifier.height(10.dp))
        ChapterName(
            name = chapter.name,
            downloaded = downloaded,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            maxLines = 4,
            color = MaterialTheme.colorScheme.onSurface,
        )
        chapter.scanlator?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                modifier = Modifier
                    .secondaryItemAlpha()
                    .padding(top = 4.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ChapterName(
    name: String,
    downloaded: Boolean,
    style: TextStyle,
    maxLines: Int,
    color: Color,
) {
    Text(
        text = buildAnnotatedString {
            if (downloaded) {
                appendInlineContent(DOWNLOADED_ICON_ID)
                append(' ')
            }
            append(name)
        },
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = style,
        color = color,
        inlineContent = mapOf(
            DOWNLOADED_ICON_ID to InlineTextContent(
                Placeholder(
                    width = style.fontSize * 1.05f,
                    height = style.fontSize * 1.05f,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                ),
            ) {
                Icon(
                    imageVector = MaterialSymbols.RoundedFilled.CheckCircle,
                    contentDescription = stringResource(MR.strings.label_downloaded),
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
        ),
    )
}

/** Caught up, or nothing before this one: a calm card rather than an alert. */
@Composable
private fun NoChapterNotification(
    text: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MaterialSymbols.RoundedFilled.CheckCircle,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ChapterGapWarning(
    gapCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.14f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = MaterialSymbols.RoundedFilled.Warning,
            tint = MaterialTheme.colorScheme.error,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = pluralStringResource(MR.plurals.missing_chapters_warning, count = gapCount, gapCount),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

private const val DOWNLOADED_ICON_ID = "downloaded"

private fun previewChapter(name: String, scanlator: String, chapterNumber: Double) = Chapter.create().copy(
    id = 0L,
    mangaId = 0L,
    url = "",
    name = name,
    scanlator = scanlator,
    chapterNumber = chapterNumber,
)
private val FakeChapter = previewChapter(
    name = "Vol.1, Ch.1 - Fake Chapter Title",
    scanlator = "Scanlator Name",
    chapterNumber = 1.0,
)
private val FakeGapChapter = previewChapter(
    name = "Vol.5, Ch.44 - Fake Gap Chapter Title",
    scanlator = "Scanlator Name",
    chapterNumber = 44.0,
)
private val FakeChapterLongTitle = previewChapter(
    name = "Vol.1, Ch.0 - The Mundane Musings of a Metafictional Manga: A Chapter About a Chapter, Featuring" +
        " an Absurdly Long Title and a Surprisingly Normal Day in the Lives of Our Heroes, as They Grapple with the " +
        "Daily Challenges of Existence, from Paying Rent to Finding Love, All While Navigating the Strange World of " +
        "Fictional Realities and Reality-Bending Fiction, Where the Fourth Wall is Always in Danger of Being Broken " +
        "and the Line Between Author and Character is Forever Blurred.",
    scanlator = "Long Long Funny Scanlator Sniper Group Name Reborn",
    chapterNumber = 1.0,
)

@PreviewLightDark
@Composable
private fun TransitionTextPreview() {
    TachiyomiPreviewTheme {
        Surface(modifier = Modifier.padding(48.dp)) {
            ChapterTransition(
                transition = ChapterTransition.Next(ReaderChapter(FakeChapter), ReaderChapter(FakeChapter)),
                currChapterDownloaded = false,
                goingToChapterDownloaded = true,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TransitionTextLongTitlePreview() {
    TachiyomiPreviewTheme {
        Surface(modifier = Modifier.padding(48.dp)) {
            ChapterTransition(
                transition = ChapterTransition.Next(ReaderChapter(FakeChapterLongTitle), ReaderChapter(FakeChapter)),
                currChapterDownloaded = true,
                goingToChapterDownloaded = true,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TransitionTextWithGapPreview() {
    TachiyomiPreviewTheme {
        Surface(modifier = Modifier.padding(48.dp)) {
            ChapterTransition(
                transition = ChapterTransition.Next(ReaderChapter(FakeChapter), ReaderChapter(FakeGapChapter)),
                currChapterDownloaded = true,
                goingToChapterDownloaded = false,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TransitionTextNoNextPreview() {
    TachiyomiPreviewTheme {
        Surface(modifier = Modifier.padding(48.dp)) {
            ChapterTransition(
                transition = ChapterTransition.Next(ReaderChapter(FakeChapter), null),
                currChapterDownloaded = true,
                goingToChapterDownloaded = false,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TransitionTextNoPreviousPreview() {
    TachiyomiPreviewTheme {
        Surface(modifier = Modifier.padding(48.dp)) {
            ChapterTransition(
                transition = ChapterTransition.Prev(ReaderChapter(FakeChapter), null),
                currChapterDownloaded = true,
                goingToChapterDownloaded = false,
            )
        }
    }
}
