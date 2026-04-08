package eu.kanade.tachiyomi.util.system

import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.asDrawable
import coil3.compose.AsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tachiyomi.domain.anime.model.AnimeCover
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

import kotlin.math.abs

object CoverColorExtractor {

    suspend fun extract(
        cover: AnimeCover,
        state: AsyncImagePainter.State.Success,
        extractColor: Boolean = true,
    ) = withContext(Dispatchers.Default) {
        val context = Injekt.get<Application>()
        val image = state.result.image
        
        // Fast ratio extraction without bitmap conversion
        val ratio = image.width.toFloat() / image.height.toFloat()
        cover.ratio = ratio
        CoverColorObserver.updateRatio(cover.animeId, ratio)

        if (!extractColor || cover.vibrantCoverColor != null || CoverColorObserver.get(cover.animeId) != null) return@withContext

        val bitmap = when (image) {
            is BitmapImage -> image.bitmap
            else -> image.asDrawable(context.resources).toBitmap()
        }.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && it.config == Bitmap.Config.HARDWARE) {
                it.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                it
            }
        }

        val palette = Palette.from(bitmap).generate()
        val color = palette.getVibrantColor(palette.getMutedColor(0))
        if (color != 0) {
            cover.vibrantCoverColor = color
            CoverColorObserver.update(cover.animeId, color)
        }
    }
}
