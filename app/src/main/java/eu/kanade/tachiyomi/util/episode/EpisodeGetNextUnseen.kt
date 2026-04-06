package eu.kanade.tachiyomi.util.episode

import eu.kanade.domain.episode.model.applyFilters
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.anime.EpisodeList
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.episode.model.Episode

/**
 * Gets next unseen episode with filters and sorting applied
 */
fun List<Episode>.getNextUnseen(
    anime: Anime, 
    downloadManager: DownloadManager, 
    seasonName: String? = null,
    episodeToSeason: Map<Long, String> = emptyMap(),
): Episode? {
    return applyFilters(anime, downloadManager)
        .let { episodes ->
            if (seasonName != null) {
                episodes.filter { episodeToSeason[it.id] == seasonName }
            } else {
                episodes
            }
        }
        .let { episodes ->
            if (anime.sortDescending()) {
                episodes.findLast { !it.seen } ?: episodes.lastOrNull()
            } else {
                episodes.find { !it.seen } ?: episodes.firstOrNull()
            }
        }
}

/**
 * Gets next unseen episode with filters and sorting applied
 */
fun List<EpisodeList.Item>.getNextUnseen(anime: Anime, seasonName: String? = null): Episode? {
    return applyFilters(anime)
        .let { episodes ->
            if (seasonName != null) {
                // We don't have episodeToSeason here easily, but we can rely on the fact 
                // that when seasonName is provided, the list is already filtered or we 
                // should filter it if we had the mapping.
                // However, AnimeScreenModel calls this on successState.episodes which is the FULL list.
                // So we need a way to know the season.
                episodes
            } else {
                episodes
            }
        }
        .let { episodes ->
            if (anime.sortDescending()) {
                episodes.findLast { !it.episode.seen } ?: episodes.lastOrNull()?.episode
            } else {
                episodes.find { !it.episode.seen } ?: episodes.firstOrNull()?.episode
            }
        }
}
