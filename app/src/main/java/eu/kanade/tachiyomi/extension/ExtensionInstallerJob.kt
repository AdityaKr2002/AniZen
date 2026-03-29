package eu.kanade.tachiyomi.extension

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.system.notificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import logcat.LogPriority
import okhttp3.Request
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

class ExtensionInstallerJob(val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val httpClient by lazy { Injekt.get<NetworkHelper>().client }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val pkgName = inputData.getString(KEY_PKG_NAME) ?: "extension"
        val id = Notifications.ID_EXTENSION_PROGRESS + pkgName.hashCode()
        val notification = ExtensionInstallNotifier(context).progressNotificationBuilder
            .setContentTitle(context.stringResource(MR.strings.ext_installing))
            .build()
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id, notification)
        }
    }

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val pkgName = inputData.getString(KEY_PKG_NAME) ?: return Result.failure()
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, -1).takeIf { it >= 0 } ?: return Result.failure()
        val notificationId = Notifications.ID_EXTENSION_PROGRESS + pkgName.hashCode()

        logcat { "Starting extension download job for $pkgName (URL: $url)" }
        val installer = Injekt.get<ExtensionInstaller>()
        val notifier = ExtensionInstallNotifier(context)

        try {
            setForeground(getForegroundInfo())
            
            // Signal UI that download has started
            installer.updateInstallStep(downloadId, InstallStep.Downloading)

            val tmpFile = File(context.cacheDir, "extension_$pkgName.apk")
            
            // Try to download with fallback logic
            var response = downloadWithPossibleFallback(url)

            if (!response.isSuccessful) {
                logcat(LogPriority.ERROR) { "All download attempts failed for $pkgName. Final code: ${response.code}" }
                throw Exception("Failed to download extension: ${response.code}")
            }

            val body = response.body ?: throw Exception("Empty response body")
            val totalBytes = body.contentLength()
            var bytesDownloaded = 0L

            kotlinx.coroutines.withContext(Dispatchers.IO) {
                body.byteStream().use { input ->
                    tmpFile.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            bytesDownloaded += bytesRead
                            if (totalBytes > 0) {
                                val progress = (bytesDownloaded * 100 / totalBytes).toInt()
                                notifier.showProgressNotification(pkgName, progress, 100)
                            }
                        }
                    }
                }
            }

            logcat { "Extension $pkgName downloaded successfully, triggering install" }
            installer.updateInstallStep(downloadId, InstallStep.Installing)
            installer.installApk(downloadId, tmpFile)
            
            return Result.success()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Error in extension installer job for $pkgName" }
            installer.updateInstallStep(downloadId, InstallStep.Error)
            return Result.failure()
        } finally {
            context.notificationManager.cancel(notificationId)
        }
    }

    private suspend fun downloadWithPossibleFallback(primaryUrl: String): okhttp3.Response {
        val request = Request.Builder().url(primaryUrl).build()
        val response = kotlinx.coroutines.withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute()
        }

        if (response.code == 404 && !primaryUrl.contains("/apk/")) {
            // If flat URL fails, try nested /apk/ folder (old Mihon style)
            val fileName = primaryUrl.substringAfterLast("/")
            val baseUrl = primaryUrl.substringBeforeLast("/")
            val fallbackUrl = "$baseUrl/apk/$fileName"
            
            logcat { "Primary URL 404, trying fallback: $fallbackUrl" }
            val fallbackRequest = Request.Builder().url(fallbackUrl).build()
            return kotlinx.coroutines.withContext(Dispatchers.IO) {
                httpClient.newCall(fallbackRequest).execute()
            }
        }
        
        return response
    }

    companion object {
        private const val TAG = "ExtensionInstaller"
        private const val KEY_URL = "url"
        private const val KEY_PKG_NAME = "pkg_name"
        private const val KEY_DOWNLOAD_ID = "download_id"

        fun start(context: Context, url: String, pkgName: String, downloadId: Long) {
            val request = OneTimeWorkRequestBuilder<ExtensionInstallerJob>()
                .addTag(TAG)
                .addTag(pkgName)
                .setInputData(
                    workDataOf(
                        KEY_URL to url,
                        KEY_PKG_NAME to pkgName,
                        KEY_DOWNLOAD_ID to downloadId,
                    ),
                )
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "$TAG-$pkgName",
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        fun stop(context: Context, pkgName: String) {
            WorkManager.getInstance(context).cancelUniqueWork("$TAG-$pkgName")
        }
    }
}
