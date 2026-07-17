package eu.kanade.tachiyomi.ui.malimport

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.tachiyomi.data.database.models.Track as DbTrack
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.anime.interactor.GetLibraryAnime
import tachiyomi.domain.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR
import java.util.Locale

enum class ImportStatusFilter(val titleRes: StringResource, val apiStatuses: Set<String>) {
    WATCHING(MR.strings.watching, setOf("watching")),
    PLAN_TO_WATCH(MR.strings.plan_to_watch, setOf("plan_to_watch")),
    COMPLETED(MR.strings.completed, setOf("completed")),
    ON_HOLD(MR.strings.on_hold, setOf("on_hold")),
}

@Immutable
data class MalImportScreenState(
    val isLoading: Boolean = true,
    val rawItems: List<MalImportItem> = emptyList(),
    val excludeLibraryMatches: Boolean = true,
    val selectedStatuses: Set<ImportStatusFilter> = ImportStatusFilter.entries.toSet(),
) {
    val items: List<MalImportItem>
        get() = rawItems.filter { item ->
            val matchingFilter = selectedStatuses.any { filter -> item.item.listStatus.status in filter.apiStatuses }
            if (!matchingFilter) return@filter false
            if (excludeLibraryMatches && item.isLibraryMatch) return@filter false
            true
        }

    val selected = items.filter { it.selected }
    val selectionMode = selected.isNotEmpty()
}

@Immutable
data class MalImportItem(
    val item: eu.kanade.tachiyomi.data.track.myanimelist.dto.MALUserAnimeListItem,
    val selected: Boolean = false,
    val isLibraryMatch: Boolean = false,
)

class MalImportScreenModel(
    private val getLibraryAnime: GetLibraryAnime = Injekt.get(),
    private val getTracks: GetTracks = Injekt.get(),
    private val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
    private val insertTrack: InsertTrack = Injekt.get(),
    private val trackerManager: TrackerManager = Injekt.get(),
) : StateScreenModel<MalImportScreenState>(MalImportScreenState()) {

    init {
        loadItems()
        observeLibraryChanges()
    }

    private fun loadItems() {
        screenModelScope.launchIO {
            try {
                mutableState.update { it.copy(isLoading = true) }
                val mal = trackerManager.myAnimeList
                if (!mal.isLoggedIn) {
                    mutableState.update { it.copy(isLoading = false, rawItems = emptyList()) }
                    return@launchIO
                }
                val currentUserId = try { mal.getUsername() } catch (e: Exception) { "" }
                val now = System.currentTimeMillis()
                val remoteItems = if (cachedRemoteItems != null &&
                    cachedUserId == currentUserId &&
                    now - lastCacheTime < CACHE_DURATION
                ) {
                    cachedRemoteItems!!
                } else {
                    val fetched = mal.getUserAnimeList()
                    cachedRemoteItems = fetched
                    cachedUserId = currentUserId
                    lastCacheTime = now
                    fetched
                }
                val localTracks = getTracks.await()
                val alreadyTrackedRemoteIds = localTracks
                    .filter { it.trackerId == 1L }
                    .map { it.remoteId }
                    .toSet()

                val libraryAnime = getLibraryAnime.await()
                val libraryAnimeNormalizedTitles = libraryAnime
                    .map { it.anime.title.normalizeTitle() }
                    .toSet()

                val allItems = remoteItems.map { item ->
                    val isAlreadyTracked = item.node.id in alreadyTrackedRemoteIds
                    val title = item.node.title
                    val titlesToCompare = listOf(title).map { it.normalizeTitle() }
                    val hasTitleMatch = titlesToCompare.any { it in libraryAnimeNormalizedTitles }
                    val isLibraryMatch = isAlreadyTracked || hasTitleMatch
                    MalImportItem(
                        item = item,
                        isLibraryMatch = isLibraryMatch
                    )
                }

                mutableState.update { it.copy(isLoading = false, rawItems = allItems) }
            } catch (e: Exception) {
                mutableState.update { it.copy(isLoading = false, rawItems = emptyList()) }
            }
        }
    }

    private fun String.normalizeTitle(): String {
        return this.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
    }

    fun setExcludeLibraryMatches(exclude: Boolean) {
        mutableState.update { it.copy(excludeLibraryMatches = exclude) }
    }

    fun toggleStatusFilter(filter: ImportStatusFilter) {
        mutableState.update { state ->
            val newStatuses = if (state.selectedStatuses.contains(filter)) {
                state.selectedStatuses - filter
            } else {
                state.selectedStatuses + filter
            }
            state.copy(selectedStatuses = newStatuses)
        }
    }

    fun toggleSelection(item: MalImportItem, selected: Boolean) {
        mutableState.update { state ->
            val newItems = state.rawItems.map {
                if (it.item.node.id == item.item.node.id) {
                    it.copy(selected = selected)
                } else {
                    it
                }
            }
            state.copy(rawItems = newItems)
        }
    }

    fun toggleAllSelection(selected: Boolean) {
        mutableState.update { state ->
            val visibleIds = state.items.map { it.item.node.id }.toSet()
            val newItems = state.rawItems.map {
                if (it.item.node.id in visibleIds) {
                    it.copy(selected = selected)
                } else {
                    it
                }
            }
            state.copy(rawItems = newItems)
        }
    }

    fun invertSelection() {
        mutableState.update { state ->
            val visibleIds = state.items.map { it.item.node.id }.toSet()
            val newItems = state.rawItems.map {
                if (it.item.node.id in visibleIds) {
                    it.copy(selected = !it.selected)
                } else {
                    it
                }
            }
            state.copy(rawItems = newItems)
        }
    }

    fun importSelected(onSuccess: (List<Long>) -> Unit, onFailure: (Throwable) -> Unit) {
        val selectedItems = state.value.selected
        if (selectedItems.isEmpty()) return

        screenModelScope.launchIO {
            try {
                val createdAnimeIds = mutableListOf<Long>()
                for (selected in selectedItems) {
                    val item = selected.item
                    val anime = Anime.create().copy(
                        url = "/anime/mal-import/${item.node.id}",
                        ogTitle = item.node.title,
                        ogThumbnailUrl = item.node.covers?.large ?: item.node.covers?.medium ?: "",
                        source = 1L,
                        favorite = false,
                        dateAdded = System.currentTimeMillis()
                    )
                    val savedAnime = networkToLocalAnime.await(anime)
                    createdAnimeIds.add(savedAnime.id)

                    val dbTrack = DbTrack.create(1L).apply {
                        anime_id = savedAnime.id
                        remote_id = item.node.id
                        library_id = item.node.id
                        title = item.node.title
                        last_episode_seen = item.listStatus.numEpisodesWatched
                        total_episodes = item.node.numEpisodes
                        status = item.toMALUserAnime().toTrack().status
                        score = item.listStatus.score.toDouble()
                        tracking_url = "https://myanimelist.net/anime/${item.node.id}"
                        started_watching_date = item.listStatus.startDate?.let { parseDate(it) } ?: 0L
                        finished_watching_date = item.listStatus.finishDate?.let { parseDate(it) } ?: 0L
                    }
                    val domainTrack = dbTrack.toDomainTrack(idRequired = false)!!
                    insertTrack.await(domainTrack)
                }
                onSuccess(createdAnimeIds)
            } catch (t: Throwable) {
                onFailure(t)
            }
        }
    }

    private fun parseDate(isoDate: String): Long {
        if (isoDate.isBlank()) return 0L
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(isoDate)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun observeLibraryChanges() {
        screenModelScope.launchIO {
            getLibraryAnime.subscribe().collectLatest { libraryAnime ->
                val libraryAnimeNormalizedTitles = libraryAnime
                    .map { it.anime.title.normalizeTitle() }
                    .toSet()

                val localTracks = getTracks.await()
                val alreadyTrackedRemoteIds = localTracks
                    .filter { it.trackerId == 1L }
                    .map { it.remoteId }
                    .toSet()

                mutableState.update { state ->
                    val updatedItems = state.rawItems.map { item ->
                        val isAlreadyTracked = item.item.node.id in alreadyTrackedRemoteIds
                        val title = item.item.node.title
                        val titlesToCompare = listOf(title).map { it.normalizeTitle() }
                        val hasTitleMatch = titlesToCompare.any { it in libraryAnimeNormalizedTitles }
                        val isLibraryMatch = isAlreadyTracked || hasTitleMatch
                        item.copy(
                            isLibraryMatch = isLibraryMatch,
                            selected = if (isLibraryMatch) false else item.selected
                        )
                    }
                    state.copy(rawItems = updatedItems)
                }
            }
        }
    }

    companion object {
        private var cachedRemoteItems: List<eu.kanade.tachiyomi.data.track.myanimelist.dto.MALUserAnimeListItem>? = null
        private var cachedUserId: String? = null
        private var lastCacheTime: Long = 0
        private const val CACHE_DURATION = 5 * 60 * 1000 // 5 minutes
    }
}
