package eu.kanade.tachiyomi.ui.anilistimport

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.tachiyomi.data.database.models.Track as DbTrack
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.AnilistApi
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
    WATCHING(MR.strings.watching, setOf("CURRENT", "REPEATING")),
    PLAN_TO_WATCH(MR.strings.plan_to_watch, setOf("PLANNING")),
    COMPLETED(MR.strings.completed, setOf("COMPLETED")),
    ON_HOLD(MR.strings.on_hold, setOf("PAUSED")),
}

@Immutable
data class AnilistImportScreenState(
    val isLoading: Boolean = true,
    val rawItems: List<AnilistImportItem> = emptyList(),
    val excludeLibraryMatches: Boolean = true,
    val selectedStatuses: Set<ImportStatusFilter> = ImportStatusFilter.entries.toSet(),
) {
    val items: List<AnilistImportItem>
        get() = rawItems.filter { item ->
            val matchingFilter = selectedStatuses.any { filter -> item.item.status in filter.apiStatuses }
            if (!matchingFilter) return@filter false
            if (excludeLibraryMatches && item.isLibraryMatch) return@filter false
            true
        }

    val selected = items.filter { it.selected }
    val selectionMode = selected.isNotEmpty()
}

@Immutable
data class AnilistImportItem(
    val item: eu.kanade.tachiyomi.data.track.anilist.dto.ALUserListItem,
    val selected: Boolean = false,
    val isLibraryMatch: Boolean = false,
)

class AnilistImportScreenModel(
    private val getLibraryAnime: GetLibraryAnime = Injekt.get(),
    private val getTracks: GetTracks = Injekt.get(),
    private val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
    private val insertTrack: InsertTrack = Injekt.get(),
    private val trackerManager: TrackerManager = Injekt.get(),
) : StateScreenModel<AnilistImportScreenState>(AnilistImportScreenState()) {

    init {
        loadItems()
        observeLibraryChanges()
    }

    private fun loadItems() {
        screenModelScope.launchIO {
            try {
                mutableState.update { it.copy(isLoading = true) }
                val anilist = trackerManager.aniList
                if (!anilist.isLoggedIn) {
                    mutableState.update { it.copy(isLoading = false, rawItems = emptyList()) }
                    return@launchIO
                }
                val currentUserId = try { anilist.getUsername() } catch (e: Exception) { "" }
                val now = System.currentTimeMillis()
                val remoteItems = if (cachedRemoteItems != null &&
                    cachedUserId == currentUserId &&
                    now - lastCacheTime < CACHE_DURATION
                ) {
                    cachedRemoteItems!!
                } else {
                    val fetched = anilist.getUserAnimeList()
                    cachedRemoteItems = fetched
                    cachedUserId = currentUserId
                    lastCacheTime = now
                    fetched
                }
                val localTracks = getTracks.await()
                val alreadyTrackedRemoteIds = localTracks
                    .filter { it.trackerId == TrackerManager.ANILIST }
                    .map { it.remoteId }
                    .toSet()

                val libraryAnime = getLibraryAnime.await()
                val libraryAnimeNormalizedTitles = libraryAnime
                    .map { it.anime.title.normalizeTitle() }
                    .toSet()


                val allItems = remoteItems.map { item ->
                    val isAlreadyTracked = item.media.id in alreadyTrackedRemoteIds
                    val title = item.media.title
                    val titlesToCompare = listOfNotNull(
                        title.userPreferred,
                        title.romaji,
                        title.english,
                        title.native
                    ).map { it.normalizeTitle() }
                    val hasTitleMatch = titlesToCompare.any { it in libraryAnimeNormalizedTitles }
                    val isLibraryMatch = isAlreadyTracked || hasTitleMatch
                    AnilistImportItem(
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

    fun toggleSelection(item: AnilistImportItem, selected: Boolean) {
        mutableState.update { state ->
            val newItems = state.rawItems.map {
                if (it.item.media.id == item.item.media.id) {
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
            val visibleIds = state.items.map { it.item.media.id }.toSet()
            val newItems = state.rawItems.map {
                if (it.item.media.id in visibleIds) {
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
            val visibleIds = state.items.map { it.item.media.id }.toSet()
            val newItems = state.rawItems.map {
                if (it.item.media.id in visibleIds) {
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
                        url = "/anime/anilist-import/${item.media.id}",
                        ogTitle = item.media.title.userPreferred,
                        ogThumbnailUrl = item.media.coverImage.large,
                        source = TrackerManager.ANILIST,
                        favorite = false,
                        dateAdded = System.currentTimeMillis()
                    )
                    val savedAnime = networkToLocalAnime.await(anime)
                    createdAnimeIds.add(savedAnime.id)

                    val dbTrack = DbTrack.create(TrackerManager.ANILIST).apply {
                        anime_id = savedAnime.id
                        remote_id = item.media.id
                        library_id = item.id
                        title = item.media.title.userPreferred
                        last_episode_seen = item.progress.toDouble()
                        total_episodes = item.media.episodes ?: 0
                        status = item.toALUserAnime().toTrack().status
                        score = item.scoreRaw.toDouble()
                        tracking_url = AnilistApi.animeUrl(item.media.id)
                        started_watching_date = item.startedAt.toEpochMilli()
                        finished_watching_date = item.completedAt.toEpochMilli()
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

    private fun observeLibraryChanges() {
        screenModelScope.launchIO {
            getLibraryAnime.subscribe().collectLatest { libraryAnime ->
                val libraryAnimeNormalizedTitles = libraryAnime
                    .map { it.anime.title.normalizeTitle() }
                    .toSet()

                val localTracks = getTracks.await()
                val alreadyTrackedRemoteIds = localTracks
                    .filter { it.trackerId == TrackerManager.ANILIST }
                    .map { it.remoteId }
                    .toSet()

                mutableState.update { state ->
                    val updatedItems = state.rawItems.map { item ->
                        val isAlreadyTracked = item.item.media.id in alreadyTrackedRemoteIds
                        val title = item.item.media.title
                        val titlesToCompare = listOfNotNull(
                            title.userPreferred,
                            title.romaji,
                            title.english,
                            title.native
                        ).map { it.normalizeTitle() }
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
        private var cachedRemoteItems: List<eu.kanade.tachiyomi.data.track.anilist.dto.ALUserListItem>? = null
        private var cachedUserId: String? = null
        private var lastCacheTime: Long = 0
        private const val CACHE_DURATION = 5 * 60 * 1000 // 5 minutes
    }
}
