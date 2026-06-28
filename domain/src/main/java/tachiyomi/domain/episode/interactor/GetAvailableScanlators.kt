package tachiyomi.domain.episode.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.episode.repository.EpisodeRepository

class GetAvailableScanlators(
    private val episodeRepository: EpisodeRepository,
) {

    fun subscribe(animeId: Long): Flow<List<String>> {
        return episodeRepository.getScanlatorsByAnimeIdAsFlow(animeId)
    }

    suspend fun await(animeId: Long): List<String> {
        return episodeRepository.getScanlatorsByAnimeId(animeId)
    }
}
