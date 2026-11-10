package kurotoon.reminder

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.notify
import eu.kanade.tachiyomi.util.system.workManager
import mihon.app.di.appGraph
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlin.time.toJavaInstant

/**
 * Fires once a day at the chosen hour. Stays quiet when something was already read today,
 * when there is nothing to continue, or while incognito mode is on.
 */
class ReadingReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val graph = applicationContext.appGraph
        if (!graph.kuroPreferences.readingReminder.get()) return Result.success()
        if (graph.basePreferences.incognitoMode.get()) return Result.success()

        val zone = ZoneId.systemDefault()
        val last = graph.historyRepository.getLastHistory() ?: return Result.success()
        val readToday = last.readAt?.toJavaInstant()?.atZone(zone)?.toLocalDate() == LocalDate.now(zone)
        if (readToday) return Result.success()

        val next = graph.getNextChapters.await(onlyUnread = true).firstOrNull() ?: return Result.success()

        val rewards = graph.rewardsStore.current
        val yesterday = LocalDate.now(zone).minusDays(1).toString()
        val streak = if (rewards.lastDay == yesterday || rewards.lastDay == LocalDate.now(zone).toString()) {
            rewards.streak
        } else {
            0
        }

        val context = applicationContext
        val intent = ReaderActivity.newIntent(context, next.mangaId, next.id)
        val pending = PendingIntent.getActivity(
            context,
            Notifications.ID_READING_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        context.notify(Notifications.ID_READING_REMINDER, Notifications.CHANNEL_READING_REMINDER) {
            setSmallIcon(R.drawable.ic_kurotoon)
            setContentTitle(
                if (streak > 1) {
                    context.stringResource(MR.strings.kurotoon_reminder_titleStreak, streak)
                } else {
                    context.stringResource(MR.strings.kurotoon_reminder_title)
                },
            )
            setContentText(context.stringResource(MR.strings.kurotoon_reminder_text, last.title, next.name))
            setContentIntent(pending)
            addAction(0, context.stringResource(MR.strings.kurotoon_reminder_continue), pending)
            setAutoCancel(true)
            setCategory(NotificationCompat.CATEGORY_REMINDER)
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "KurotoonReadingReminder"

        /** Schedules or cancels the daily reminder to match the current preferences. */
        fun setupTask(
            context: Context,
            enabled: Boolean? = null,
            hour: Int? = null,
        ) {
            val prefs = context.appGraph.kuroPreferences
            val isEnabled = enabled ?: prefs.readingReminder.get()
            if (!isEnabled) {
                context.workManager.cancelUniqueWork(TAG)
                return
            }
            val delay = initialDelay(ZonedDateTime.now(), hour ?: prefs.readingReminderHour.get())
            val request = PeriodicWorkRequestBuilder<ReadingReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay.toMinutes(), TimeUnit.MINUTES)
                .addTag(TAG)
                .build()
            context.workManager.enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        /** Time until the next occurrence of [hour]:00 after [now]. */
        internal fun initialDelay(now: ZonedDateTime, hour: Int): Duration {
            var target = now.withHour(hour.coerceIn(0, 23)).withMinute(0).withSecond(0).withNano(0)
            if (!target.isAfter(now)) target = target.plusDays(1)
            return Duration.between(now, target)
        }
    }
}
