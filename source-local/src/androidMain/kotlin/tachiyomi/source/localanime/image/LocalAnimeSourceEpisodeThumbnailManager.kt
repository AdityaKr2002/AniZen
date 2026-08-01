package tachiyomi.source.localanime.image

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.SAnime
import eu.kanade.tachiyomi.source.model.SEpisode
import eu.kanade.tachiyomi.util.storage.DiskUtil
import tachiyomi.core.common.storage.nameWithoutExtension
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.source.localanime.io.LocalAnimeSourceFileSystem
import java.io.InputStream

private const val DEFAULT_THUMBNAIL_NAME = "thumbnail.jpg"

actual class LocalAnimeSourceEpisodeThumbnailManager(
    private val context: Context,
    private val fileSystem: LocalAnimeSourceFileSystem,
) {

    actual fun find(animeUrl: String, episodeName: String): UniFile? {
        val files = fileSystem.getFilesInAnimeDirectory(animeUrl)

        // 1. Match "<Episode Name>-thumbnail" (e.g., Episode 01-thumbnail.jpg)
        files.firstOrNull {
            it.isFile &&
                it.nameWithoutExtension.equals("$episodeName-thumbnail", ignoreCase = true) &&
                ImageUtil.isImage(it.name) { it.openInputStream() }
        }?.let { return it }

        // 2. Match "<Episode Name>" image file (e.g., Episode 01.jpg)
        files.firstOrNull {
            it.isFile &&
                it.nameWithoutExtension.equals(episodeName, ignoreCase = true) &&
                ImageUtil.isImage(it.name) { it.openInputStream() }
        }?.let { return it }

        // 3. Match "<Episode Name>-cover" (e.g., Episode 01-cover.jpg)
        files.firstOrNull {
            it.isFile &&
                it.nameWithoutExtension.equals("$episodeName-cover", ignoreCase = true) &&
                ImageUtil.isImage(it.name) { it.openInputStream() }
        }?.let { return it }

        return null
    }

    actual fun update(anime: SAnime, episode: SEpisode, inputStream: InputStream): UniFile? {
        val directory = fileSystem.getAnimeDirectory(anime.url)
        if (directory == null) {
            inputStream.close()
            return null
        }

        val fileName = "${episode.name}-$DEFAULT_THUMBNAIL_NAME"
        val targetFile = find(anime.url, episode.name) ?: directory.createFile(fileName)!!

        inputStream.use { input ->
            targetFile.openOutputStream().use { output ->
                input.copyTo(output)
            }
        }

        DiskUtil.createNoMediaFile(directory, context)

        episode.preview_url = targetFile.uri.toString()
        return targetFile
    }
}
