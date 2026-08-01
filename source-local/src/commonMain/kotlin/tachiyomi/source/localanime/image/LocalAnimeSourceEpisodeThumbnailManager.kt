package tachiyomi.source.localanime.image

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.SAnime
import eu.kanade.tachiyomi.source.model.SEpisode
import java.io.InputStream

expect class LocalAnimeSourceEpisodeThumbnailManager {

    fun find(animeUrl: String, episodeName: String): UniFile?

    fun update(anime: SAnime, episode: SEpisode, inputStream: InputStream): UniFile?
}
