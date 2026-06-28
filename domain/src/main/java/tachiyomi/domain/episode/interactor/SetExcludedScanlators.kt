package tachiyomi.domain.episode.interactor

import tachiyomi.domain.episode.repository.EpisodeRepository

class SetExcludedScanlators(
    private val episodeRepository: EpisodeRepository,
) {

    suspend fun await(animeId: Long, excludedScanlators: Set<String>) {
        val currentExcluded = episodeRepository.getExcludedScanlatorsByAnimeId(animeId).toSet()

        val toExclude = excludedScanlators.subtract(currentExcluded)
        val toInclude = currentExcluded.subtract(excludedScanlators)

        toExclude.forEach {
            episodeRepository.insertExcludedScanlator(animeId, it)
        }

        if (toInclude.isNotEmpty()) {
            episodeRepository.removeExcludedScanlators(animeId, toInclude.toList())
        }
    }
}
