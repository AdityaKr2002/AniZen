package tachiyomi.domain.anime.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.CustomAnimeInfo
import tachiyomi.domain.anime.repository.AnimeRepository
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.episode.repository.EpisodeRepository

class GetAnimeWithEpisodes(
    private val animeRepository: AnimeRepository,
    private val episodeRepository: EpisodeRepository,
    private val getCustomAnimeInfo: GetCustomAnimeInfo,
) {

    suspend fun subscribe(id: Long, applyScanlatorFilter: Boolean = false): Flow<Pair<Anime, List<Episode>>> {
        return combine(
            animeRepository.getAnimeByIdAsFlow(id),
            episodeRepository.getEpisodeByAnimeIdAsFlow(id, applyScanlatorFilter),
            getCustomAnimeInfo.subscribe(id),
        ) { manga, chapters, customInfo ->
            Pair(manga.reflectCustomInfo(customInfo), chapters)
        }
    }

    private fun Anime.reflectCustomInfo(customInfo: CustomAnimeInfo?): Anime {
        if (customInfo == null) return this
        return this.copy(
            lastUpdate = this.lastUpdate // This is just to ensure a new instance is created if needed, but copy() does that anyway.
        ).apply {
            // Since customAnimeInfo is a private lazy property in Anime.kt, 
            // we can't set it directly. However, by returning a NEW Anime instance,
            // its internal 'customAnimeInfo' lazy property will be re-evaluated
            // using the latest data from the repository when accessed.
        }
    }

    suspend fun awaitManga(id: Long): Anime {
        return animeRepository.getAnimeById(id)
    }

    suspend fun awaitChapters(id: Long, applyScanlatorFilter: Boolean = false): List<Episode> {
        return episodeRepository.getEpisodeByAnimeId(id, applyScanlatorFilter)
    }
}
