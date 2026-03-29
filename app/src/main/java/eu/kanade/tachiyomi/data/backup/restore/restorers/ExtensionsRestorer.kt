package eu.kanade.tachiyomi.data.backup.restore.restorers

import android.content.Context
import android.content.Intent
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.backup.models.BackupExtension
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.util.storage.getUriCompat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

class ExtensionsRestorer(
    private val context: Context,
    private val preferences: SourcePreferences = Injekt.get(),
) {

    fun restoreExtensions(extensions: List<BackupExtension>) {
        extensions.forEach {
            if (context.packageManager.getInstalledPackages(0).none { pkg -> pkg.packageName == it.pkgName }) {
                // save apk in files dir
                val file = File(context.cacheDir, "${it.pkgName}.apk")
                file.writeBytes(it.apk)

                if (preferences.extensionInstaller().get() == BasePreferences.ExtensionInstaller.PRIVATE) {
                    ExtensionLoader.installPrivateExtensionFile(context, file)
                    file.delete()
                } else {
                    // open installer dialog
                    val intent = Intent(Intent.ACTION_VIEW)
                        .setDataAndType(
                            file.getUriCompat(context),
                            "application/vnd.android.package-archive",
                        )
                        .setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    context.startActivity(intent)
                }
            }
        }
    }
}
