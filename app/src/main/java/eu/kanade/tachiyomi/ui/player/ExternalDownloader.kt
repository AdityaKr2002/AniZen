package eu.kanade.tachiyomi.ui.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.episode.model.Episode

class ExternalDownloader {

    fun getDownloadIntent(
        context: Context,
        anime: Anime,
        episode: Episode,
        source: Source,
        video: Video,
    ): Intent {
        val uri = Uri.parse(video.videoUrl)
        val filename = "${anime.title} - ${episode.name}"
        val targetPackage = resolveTargetPackage(context.packageManager) ?: return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return Intent(Intent.ACTION_VIEW).apply {
            val componentName = ComponentName(targetPackage, "idm.internet.download.manager.Downloader")
            if (isActivityAvailable(context, componentName)) {
                component = componentName
            }
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // 1DM Specific Extras
            putExtra("extra_filename", filename)

            val headers = video.headers ?: (source as? HttpSource)?.headers
            if (headers != null) {
                // Standard 1DM headers extra
                val headersBundle = android.os.Bundle()
                var headersArray = arrayOf<String>()
                for (header in headers) {
                    headersBundle.putString(header.first, header.second)
                    headersArray += arrayOf(header.first, header.second)
                    
                    // Special handling for common headers
                    if (header.first.equals("User-Agent", ignoreCase = true)) {
                        putExtra("extra_useragent", header.second)
                    }
                    if (header.first.equals("Referer", ignoreCase = true)) {
                        putExtra("extra_referer", header.second)
                    }
                    if (header.first.equals("Cookie", ignoreCase = true)) {
                        putExtra("extra_cookies", header.second)
                    }
                }
                putExtra("android.media.intent.extra.HTTP_HEADERS", headersBundle)
                putExtra("headers", headersArray) // For some other downloaders
            }
        }
    }

    private fun isActivityAvailable(context: Context, componentName: ComponentName): Boolean {
        return try {
            context.packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun resolveTargetPackage(pm: PackageManager): String? {
        val candidates = listOf(
            IDM_PLUS,
            IDM_FREE,
            IDM_LITE,
        )
        for (pkg in candidates) {
            try {
                pm.getPackageInfo(pkg, 0)
                return pkg
            } catch (_: Exception) {
                // ignore
            }
        }
        return null
    }

    companion object {
        const val IDM_PLUS = "idm.internet.download.manager.plus"
        const val IDM_FREE = "idm.internet.download.manager"
        const val IDM_LITE = "idm.internet.download.manager.adm.lite"
    }
}
