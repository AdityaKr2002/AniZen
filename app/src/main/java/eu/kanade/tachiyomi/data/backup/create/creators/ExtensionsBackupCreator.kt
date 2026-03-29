package eu.kanade.tachiyomi.data.backup.create.creators

import android.content.Context
import android.content.pm.PackageManager
import eu.kanade.tachiyomi.data.backup.models.BackupExtension
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

class ExtensionsBackupCreator(
    private val context: Context,
    private val extensionManager: ExtensionManager = Injekt.get(),
) {

    operator fun invoke(): List<BackupExtension> {
        val installedExtensions = mutableListOf<BackupExtension>()
        extensionManager.installedExtensionsFlow.value.forEach { it ->
            val packageName = it.pkgName
            try {
                val apk = if (it.isShared) {
                    File(
                        context.packageManager
                            .getApplicationInfo(
                                packageName,
                                PackageManager.GET_META_DATA,
                            ).publicSourceDir,
                    ).readBytes()
                } else {
                    File(
                        ExtensionLoader.getPrivateExtensionDir(context),
                        "$packageName.${ExtensionLoader.PRIVATE_EXTENSION_EXTENSION}",
                    ).readBytes()
                }
                installedExtensions.add(
                    BackupExtension(packageName, apk),
                )
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to backup extension: $packageName" }
            }
        }
        return installedExtensions
    }
}
