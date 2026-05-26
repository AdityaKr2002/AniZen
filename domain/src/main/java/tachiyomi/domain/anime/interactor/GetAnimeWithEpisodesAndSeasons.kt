package tachiyomi.domain.anime.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.CustomAnimeInfo
import tachiyomi.domain.anime.model.SeasonAnime
import tachiyomi.domain.anime.repository.AnimeRepository
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.episode.repository.EpisodeRepository

class GetAnimeWithEpisodesAndSeasons(
    private val animeRepository: AnimeRepository,
    private val episodeRepository: EpisodeRepository,
    private val getCustomAnimeInfo: GetCustomAnimeInfo,
    private val getSeasonsByAnimeId: GetSeasonsByAnimeId,
) {

    private fun isHierarchicalSupported(sourceId: Long): Boolean {
        return try {
            val extensionManager = uy.kohesive.injekt.Injekt.get<eu.kanade.tachiyomi.extension.ExtensionManager>()
            val installedExtensions = extensionManager.installedExtensionsFlow.value
            val extension = installedExtensions.find { ext -> ext.sources.any { it.id == sourceId } }
            (extension?.libVersion ?: 0.0) >= 16.0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun subscribe(
        id: Long,
        applyScanlatorFilter: Boolean = false,
        useHierarchicalSeasons: Boolean = true,
        virtualSeasonsFlow: Flow<List<Anime>>? = null,
    ): Flow<Triple<Anime, List<Episode>, List<SeasonAnime>>> {
        return animeRepository.getAnimeByIdAsFlow(id).flatMapLatest { anime ->
            val isHierarchicalSupported = isHierarchicalSupported(anime.source)
            val effectiveHierarchical = useHierarchicalSeasons && isHierarchicalSupported
            
            val parentId = if (effectiveHierarchical) (anime.parentId ?: id) else id
            
            val dbSeasonsFlow = animeRepository.getAnimeSeasonsByIdAsFlow(parentId)
            val seasonsListFlow = getSeasonsByAnimeId.subscribe(id, virtualSeasonsFlow, effectiveHierarchical)

            val combinedSeasonsFlow = combine(dbSeasonsFlow, seasonsListFlow) { dbSeasons, seasonsList ->
                seasonsList.map { season ->
                    val dbSeason = dbSeasons.find { it.anime.id == season.anime.id }
                    if (dbSeason != null) {
                        dbSeason
                    } else {
                        SeasonAnime(
                            anime = season.anime,
                            totalCount = 0,
                            seenCount = 0,
                            bookmarkCount = 0,
                            fillermarkCount = 0,
                            latestUpload = 0,
                            fetchedAt = 0,
                            lastSeen = 0,
                        )
                    }
                }
            }

            combine(
                episodeRepository.getEpisodeByAnimeIdAsFlow(id, applyScanlatorFilter),
                combinedSeasonsFlow,
                getCustomAnimeInfo.subscribe(id),
            ) { episodes, seasons, customInfo ->
                Triple(anime.copy(customAnimeInfo = customInfo), episodes, seasons)
            }
        }
    }

    suspend fun awaitAnime(id: Long): Anime {
        return animeRepository.getAnimeById(id)
    }

    suspend fun awaitEpisodes(id: Long, applyScanlatorFilter: Boolean = false): List<Episode> {
        return episodeRepository.getEpisodeByAnimeId(id, applyScanlatorFilter)
    }

    suspend fun awaitSeasons(id: Long, useHierarchicalSeasons: Boolean = true): List<SeasonAnime> {
        val anime = animeRepository.getAnimeById(id)
        val isHierarchicalSupported = isHierarchicalSupported(anime.source)
        val effectiveHierarchical = useHierarchicalSeasons && isHierarchicalSupported
        
        val parentId = if (effectiveHierarchical) (anime.parentId ?: id) else id
        val dbSeasons = animeRepository.getAnimeSeasonsById(parentId)
        val seasonsList = getSeasonsByAnimeId.await(id, emptyList(), effectiveHierarchical)

        return seasonsList.map { season ->
            val dbSeason = dbSeasons.find { it.anime.id == season.anime.id }
            if (dbSeason != null) {
                dbSeason
            } else {
                SeasonAnime(
                    anime = season.anime,
                    totalCount = 0,
                    seenCount = 0,
                    bookmarkCount = 0,
                    fillermarkCount = 0,
                    latestUpload = 0,
                    fetchedAt = 0,
                    lastSeen = 0,
                )
            }
        }
    }
}
