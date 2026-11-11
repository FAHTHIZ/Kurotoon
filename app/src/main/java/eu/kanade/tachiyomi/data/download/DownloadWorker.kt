package eu.kanade.tachiyomi.data.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.lifecycle.asFlow
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.activeNetworkState
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mihon.app.di.AppGraph
import mihon.core.metro.metroGraph
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.i18n.R
import kotlin.time.Duration.Companion.seconds

/**
 * This worker owns the lifecycle of the downloader: it starts the downloader and stops it when the
 * worker is stopped by the system or there's no suitable network available.
 */
class DownloadWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    private val graph: AppGraph = context.metroGraph()

    @Inject private lateinit var downloader: Downloader

    @Inject private lateinit var downloadPreferences: DownloadPreferences

    init {
        graph.inject(this)
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = applicationContext.notificationBuilder(Notifications.CHANNEL_DOWNLOADER_PROGRESS) {
            setContentTitle(applicationContext.getString(R.string.download_notifier_downloader_title))
            setSmallIcon(android.R.drawable.stat_sys_download)
        }.build()
        return ForegroundInfo(
            Notifications.ID_DOWNLOAD_CHAPTER_PROGRESS,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    override suspend fun doWork(): Result {
        var networkIssue = networkIssue()
        if (networkIssue != null) {
            downloader.stop(networkIssue)
            scheduleResume(applicationContext, downloadPreferences.downloadOnlyOverWifi.get())
            return Result.failure()
        }

        if (!downloader.start()) return Result.failure()

        try {
            setForegroundSafely()

            while (networkIssue == null && downloader.isRunning) {
                delay(1.seconds)
                networkIssue = networkIssue()
            }
        } finally {
            // Stopped by WorkManager means already canceled, and taking the lock would then throw instead of stopping
            if (downloader.isRunning && (networkIssue != null || isStopped)) {
                withContext(NonCancellable) { downloader.stop(networkIssue) }
            }
        }
        if (networkIssue != null && downloader.queueState.value.isNotEmpty()) {
            scheduleResume(applicationContext, downloadPreferences.downloadOnlyOverWifi.get())
        }

        return Result.success()
    }

    private fun networkIssue(): String? {
        val state = applicationContext.activeNetworkState()
        return when {
            !state.isOnline -> applicationContext.getString(R.string.download_notifier_no_network)
            downloadPreferences.downloadOnlyOverWifi.get() && !state.isWifi ->
                applicationContext.getString(R.string.download_notifier_text_only_wifi)
            else -> null
        }
    }

    companion object {
        private const val TAG = "Downloader"

        fun start(context: Context) {
            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .addTag(TAG)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }

        private const val RESUME_TAG = "DownloaderResume"

        /**
         * Picks the queue back up by itself once a suitable network returns, instead of leaving it
         * stalled until the user opens the app and taps resume (mihon#1053).
         */
        fun scheduleResume(context: Context, wifiOnly: Boolean) {
            val network = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
            val request = OneTimeWorkRequestBuilder<DownloadResumeWorker>()
                .addTag(RESUME_TAG)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(network).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(RESUME_TAG, ExistingWorkPolicy.REPLACE, request)
        }

        fun isRunningFlow(context: Context): Flow<Boolean> {
            return WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData(TAG)
                .asFlow()
                .map { list -> list.count { it.state == WorkInfo.State.RUNNING } == 1 }
        }
    }
}

/** Runs once the network is back and restarts the downloader if chapters are still waiting. */
class DownloadResumeWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    @Inject private lateinit var downloader: Downloader

    init {
        context.metroGraph<AppGraph>().inject(this)
    }

    override suspend fun doWork(): Result {
        val waiting = downloader.queueState.value.any { it.status != Download.State.DOWNLOADED }
        if (waiting && !downloader.isRunning && !downloader.isPaused) DownloadWorker.start(applicationContext)
        return Result.success()
    }
}
