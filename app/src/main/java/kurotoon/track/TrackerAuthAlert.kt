package kurotoon.track

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.system.notify
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import java.util.concurrent.ConcurrentHashMap

/** A notification for a tracker whose sign-in ran out, shown once per tracker per app run. */
object TrackerAuthAlert {

    private val shown = ConcurrentHashMap.newKeySet<Long>()

    fun signedOut(context: Context, trackerId: Long, trackerName: String) {
        if (!shown.add(trackerId)) return
        val notificationId = Notifications.ID_TRACKER_SIGNED_OUT - trackerId.toInt()
        val intent = Intent(context, MainActivity::class.java)
            .setAction(MainActivity.INTENT_TRACKING_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        runCatching {
            context.notify(notificationId, Notifications.CHANNEL_COMMON) {
                setSmallIcon(R.drawable.ic_kurotoon)
                setContentTitle(context.stringResource(MR.strings.kurotoon_tracker_signedOut_title, trackerName))
                setContentText(context.stringResource(MR.strings.kurotoon_tracker_signedOut_text))
                setContentIntent(pending)
                setAutoCancel(true)
            }
        }
    }

    /** Signing in again allows a later expiry to be reported. */
    fun reset(trackerId: Long) {
        shown.remove(trackerId)
    }
}
