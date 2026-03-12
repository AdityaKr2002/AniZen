package eu.kanade.tachiyomi.data.coil

import android.graphics.BitmapFactory
import androidx.palette.graphics.Palette
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.ui.anime.AnimeScreenModel
import okio.BufferedSource
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.anime.model.AnimeCover
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Object that holds info about a covers size ratio + dominant colors
 */
object AnimeCoverMetadata {
    private val preferences by injectLazy<LibraryPreferences>()
    private val coverCache by injectLazy<CoverCache>()

    fun load() {
        val ratios = preferences.coverRatios().get()
        AnimeCover.coverRatioMap = ConcurrentHashMap(
            ratios.mapNotNull {
                val splits = it.split("|")
                val id = splits.firstOrNull()?.toLongOrNull()
                val ratio = splits.lastOrNull()?.toFloatOrNull()
                if (id != null && ratio != null) {
                    id to ratio
                } else {
                    null
                }
            }.toMap(),
        )
        val colors = preferences.coverColors().get()
        AnimeCover.dominantCoverColorMap = ConcurrentHashMap(
            colors.mapNotNull {
                val splits = it.split("|")
                val id = splits.firstOrNull()?.toLongOrNull()
                val color = splits.getOrNull(1)?.toIntOrNull()
                val textColor = splits.getOrNull(2)?.toIntOrNull()
                if (id != null && color != null) {
                    id to (color to (textColor ?: 0))
                } else {
                    null
                }
            }.toMap(),
        )
    }

    /**
     * Set ratio and colors for a cover
     */
    fun setRatioAndColors(
        animeCover: AnimeCover,
        bufferedSource: BufferedSource? = null,
        ogFile: File? = null,
        onlyDominantColor: Boolean = true,
        force: Boolean = false,
    ) {
        if (!animeCover.isAnimeFavorite) {
            animeCover.remove()
            if (animeCover.vibrantCoverColor != null) return
        }

        if (animeCover.isAnimeFavorite && onlyDominantColor && animeCover.dominantCoverColors != null) return

        val options = BitmapFactory.Options()

        val updateColors =
            (animeCover.isAnimeFavorite && animeCover.dominantCoverColors == null) ||
                (!onlyDominantColor && animeCover.vibrantCoverColor == null) ||
                force

        if (updateColors) {
            options.inSampleSize = SUB_SAMPLE
        } else {
            options.inJustDecodeBounds = true
            return
        }

        val file = ogFile
            ?: coverCache.getCustomCoverFile(animeCover.animeId).takeIf { it.exists() }
            ?: coverCache.getCoverFile(animeCover.url)

        val bitmap = when {
            bufferedSource != null -> BitmapFactory.decodeStream(bufferedSource.inputStream(), null, options)
            file?.exists() == true -> BitmapFactory.decodeFile(file.path, options)
            else -> null
        }

        if (bitmap != null) {
            Palette.from(bitmap).generate {
                if (it == null) return@generate
                if (animeCover.isAnimeFavorite) {
                    it.dominantSwatch?.let { swatch ->
                        animeCover.dominantCoverColors = swatch.rgb to swatch.titleTextColor
                    }
                }
                val color = getBestColor(it) ?: return@generate
                animeCover.vibrantCoverColor = color
            }
        }
        if (animeCover.isAnimeFavorite && options.outWidth != -1 && options.outHeight != -1) {
            animeCover.ratio = options.outWidth / options.outHeight.toFloat()
        }
    }

    private fun getBestColor(palette: Palette): Int? {
        return palette.vibrantSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
            ?: palette.dominantSwatch?.rgb
    }

    fun AnimeCover.remove() {
        AnimeCover.coverRatioMap.remove(animeId)
        AnimeCover.dominantCoverColorMap.remove(animeId)
    }

    fun savePrefs() {
        val mapCopy = AnimeCover.coverRatioMap.toMap()
        preferences.coverRatios().set(mapCopy.map { "${it.key}|${it.value}" }.toSet())
        val mapColorCopy = AnimeCover.dominantCoverColorMap.toMap()
        preferences.coverColors().set(mapColorCopy.map { "${it.key}|${it.value.first}|${it.value.second}" }.toSet())
    }

    private const val SUB_SAMPLE = 4
}
