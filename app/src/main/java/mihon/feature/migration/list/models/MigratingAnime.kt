package mihon.feature.migration.list.models

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import mihon.feature.migration.list.MigrationListScreenModel.EpisodeInfo
import tachiyomi.domain.anime.model.Anime
import kotlin.coroutines.CoroutineContext

class MigratingAnime(
    val anime: Anime,
    val episodeCount: Int,
    val latestEpisode: Double?,
    val source: String,
    parentContext: CoroutineContext,
) {
    val migrationScope = CoroutineScope(parentContext + SupervisorJob() + Dispatchers.Default)

    var searchingJob: Deferred<Pair<Anime, EpisodeInfo>?>? = null

    val searchResult = MutableStateFlow<SearchResult>(SearchResult.Searching)

    sealed interface SearchResult {
        data object Searching : SearchResult
        data object NotFound : SearchResult
        data class Success(
            val anime: Anime,
            val episodeCount: Int,
            val latestEpisode: Double?,
            val source: String,
        ) : SearchResult
    }
}
