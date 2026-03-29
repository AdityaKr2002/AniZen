package eu.kanade.tachiyomi.extension

import android.content.Context
import androidx.core.app.NotificationCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.notify
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR

class ExtensionInstallNotifier(private val context: Context) {

    val progressNotificationBuilder = context.notificationBuilder(Notifications.CHANNEL_EXTENSIONS_UPDATE) {
        setSmallIcon(R.drawable.ic_extension_24dp)
        setOngoing(true)
        setOnlyAlertOnce(true)
        showCustomTime(false)
    }

    fun showProgressNotification(progress: Int, max: Int) {
        context.notify(
            Notifications.ID_EXTENSION_PROGRESS,
            Notifications.CHANNEL_EXTENSIONS_UPDATE,
        ) {
            setContentTitle(context.stringResource(MR.strings.ext_installing))
            setProgress(max, progress, false)
            setSmallIcon(R.drawable.ic_extension_24dp)
            setOngoing(true)
            setOnlyAlertOnce(true)
        }
    }

    fun cancelProgressNotification() {
        context.notify(Notifications.ID_EXTENSION_PROGRESS, Notifications.CHANNEL_EXTENSIONS_UPDATE) {
            // Cancel handled by system or manual cancel
        }
    }
}
