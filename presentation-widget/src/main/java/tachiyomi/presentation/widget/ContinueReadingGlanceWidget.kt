package tachiyomi.presentation.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Scale
import dev.zacsweers.metro.HasMemberInjections
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.util.system.dpToPx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kurotoon.core.metro.metroGraph
import kurotoon.presentation.widget.di.PresentationWidgetGraph
import tachiyomi.core.common.Constants
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.widget.components.CoverHeight
import tachiyomi.presentation.widget.components.CoverWidth
import tachiyomi.presentation.widget.components.LockedWidget
import tachiyomi.presentation.widget.components.UpdatesMangaCover
import tachiyomi.presentation.widget.util.appWidgetBackgroundRadius

/** One title on the Continue reading widget. */
data class ContinueItem(val mangaId: Long, val chapterId: Long, val label: String, val cover: Bitmap?)

/**
 * A home-screen shelf of what you were last reading, the "Continue reading" row from WEBTOON and Kotatsu.
 * Tapping a cover drops you back into the chapter where you left off.
 */
@HasMemberInjections
class ContinueReadingGlanceWidget : GlanceAppWidget() {

    @Inject private lateinit var getHistory: GetHistory

    @Inject private lateinit var preferences: SecurityPreferences

    override val sizeMode = SizeMode.Exact

    @SuppressLint("RestrictedApi")
    private val foreground = ColorProvider(R.color.appwidget_on_secondary_container)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        context.metroGraph<PresentationWidgetGraph>().inject(this)
        val locked = preferences.useAuthenticator.get()
        val modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.appwidget_background))
            .appWidgetBackground()
            .appWidgetBackgroundRadius()

        provideContent {
            if (locked) {
                LockedWidget(foreground = foreground, modifier = modifier)
                return@provideContent
            }
            val flow = remember {
                getHistory.subscribe("").map { it.prepare(context) }
            }
            val data by flow.collectAsState(initial = null)
            ContinueReading(data, foreground, modifier)
        }
    }

    private suspend fun List<HistoryWithRelations>.prepare(context: Context): List<ContinueItem> {
        val widthPx = CoverWidth.value.toInt().dpToPx
        val heightPx = CoverHeight.value.toInt().dpToPx
        return withContext(Dispatchers.Default) {
            distinctBy { it.mangaId }.take(MAX_ITEMS).map { h ->
                val request = ImageRequest.Builder(context)
                    .data(h.coverData)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .precision(Precision.EXACT)
                    .size(widthPx, heightPx)
                    .scale(Scale.FILL)
                    .build()
                val bitmap = context.imageLoader.execute(request).image?.asDrawable(context.resources)?.toBitmap()
                val number = h.chapterNumber
                val label = if (number >= 0) {
                    context.stringResource(
                        MR.strings.kurotoon_widget_chapter,
                        if (number % 1.0 == 0.0) number.toLong().toString() else number.toString(),
                    )
                } else {
                    h.title
                }
                ContinueItem(h.mangaId, h.chapterId, label, bitmap)
            }
        }
    }

    companion object {
        private const val MAX_ITEMS = 10
    }
}

@Composable
private fun ContinueReading(data: List<ContinueItem>?, color: ColorProvider, modifier: GlanceModifier) {
    val context = LocalContext.current
    Column(modifier = modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(
            text = context.stringResource(MR.strings.kurotoon_widget_continue),
            style = TextStyle(color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp),
            modifier = GlanceModifier.padding(start = 4.dp, bottom = 6.dp),
        )
        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                data == null -> CircularProgressIndicator(color = color)
                data.isEmpty() -> Text(
                    text = context.stringResource(MR.strings.kurotoon_widget_empty),
                    style = TextStyle(color = color),
                )
                else -> {
                    val columns = (LocalSize.current.width.value / 66).toInt().coerceIn(1, data.size)
                    Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        data.take(columns).forEach { item ->
                            val intent = Intent().apply {
                                setClassName(context, READER_ACTIVITY)
                                putExtra(Constants.MANGA_EXTRA, item.mangaId)
                                putExtra("chapter", item.chapterId)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                // https://issuetracker.google.com/issues/238793260
                                addCategory(item.chapterId.toString())
                            }
                            Column(
                                modifier = GlanceModifier
                                    .padding(horizontal = 3.dp)
                                    .clickable(actionStartActivity(intent)),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                UpdatesMangaCover(cover = item.cover)
                                Text(
                                    text = item.label,
                                    maxLines = 1,
                                    style = TextStyle(color = color, fontSize = 11.sp),
                                    modifier = GlanceModifier.width(CoverWidth).padding(top = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val READER_ACTIVITY = "eu.kanade.tachiyomi.ui.reader.ReaderActivity"

class ContinueReadingGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = ContinueReadingGlanceWidget()
}
