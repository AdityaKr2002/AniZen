package tachiyomi.core.common.storage

import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import java.io.File

class AndroidStorageFolderProvider(
    private val context: Context,
) : FolderProvider {

    override fun directory(): File {
        val appName = context.stringResource(MR.strings.app_name)
        val normalizedAppName = appName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(
            Environment.getExternalStorageDirectory().absolutePath + File.separator +
                normalizedAppName,
        )
    }

    override fun path(): String {
        return directory().toUri().toString()
    }
}
