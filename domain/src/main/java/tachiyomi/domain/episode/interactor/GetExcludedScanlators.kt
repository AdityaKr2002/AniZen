package tachiyomi.domain.episode.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.episode.repository.EpisodeRepository

class GetExcludedScanlators(
    private val episodeRepository: EpisodeRepository,
) {

    fun subscribe(animeId: Long): Flow<Set<String>> {
        return episodeRepository.getExcludedScanlatorsByAnimeIdAsFlow(animeId)
            .map { it.toSet() }
    }

    suspend fun await(animeId: Long): Set<String> {
        return episodeRepository.getExcludedScanlatorsByAnimeId(animeId).toSet()
    }
}
