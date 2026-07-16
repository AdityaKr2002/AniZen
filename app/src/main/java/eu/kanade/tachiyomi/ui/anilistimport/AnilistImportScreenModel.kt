package eu.kanade.tachiyomi.ui.anilistimport

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.tachiyomi.data.database.models.Track as DbTrack
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.AnilistApi
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
import java.util.Locale

@Immutable
data class AnilistImportScreenState(
    val isLoading: Boolean = true,
    val items: List<AnilistImportItem> = emptyList(),
) {
    val selected = items.filter { it.selected }
    val selectionMode = selected.isNotEmpty()
}

@Immutable
data class AnilistImportItem(
    val item: eu.kanade.tachiyomi.data.track.anilist.dto.ALUserListItem,
    val selected: Boolean = false,
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
    }

    private fun loadItems() {
        screenModelScope.launchIO {
            try {
                mutableState.update { it.copy(isLoading = true) }
                val anilist = trackerManager.aniList
                if (!anilist.isLoggedIn) {
                    mutableState.update { it.copy(isLoading = false, items = emptyList()) }
                    return@launchIO
                }
                val remoteItems = anilist.getUserAnimeList()
                val localTracks = getTracks.await()
                val alreadyTrackedRemoteIds = localTracks
                    .filter { it.trackerId == TrackerManager.ANILIST }
                    .map { it.remoteId }
                    .toSet()

                val libraryAnime = getLibraryAnime.await()
                val libraryAnimeNormalizedTitles = libraryAnime
                    .map { it.title.normalizeTitle() }
                    .toSet()

                val filteredItems = remoteItems.filter { item ->
                    if (item.media.id in alreadyTrackedRemoteIds) return@filter false
                    val title = item.media.title
                    val titlesToCompare = listOfNotNull(
                        title.userPreferred,
                        title.romaji,
                        title.english,
                        title.native
                    ).map { it.normalizeTitle() }
                    if (titlesToCompare.any { it in libraryAnimeNormalizedTitles }) return@filter false
                    true
                }.map { AnilistImportItem(it) }

                mutableState.update { it.copy(isLoading = false, items = filteredItems) }
            } catch (e: Exception) {
                mutableState.update { it.copy(isLoading = false, items = emptyList()) }
            }
        }
    }

    private fun String.normalizeTitle(): String {
        return this.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
    }

    fun toggleSelection(item: AnilistImportItem, selected: Boolean) {
        mutableState.update { state ->
            val newItems = state.items.map {
                if (it.item.media.id == item.item.media.id) {
                    it.copy(selected = selected)
                } else {
                    it
                }
            }
            state.copy(items = newItems)
        }
    }

    fun toggleAllSelection(selected: Boolean) {
        mutableState.update { state ->
            val newItems = state.items.map {
                it.copy(selected = selected)
            }
            state.copy(items = newItems)
        }
    }

    fun invertSelection() {
        mutableState.update { state ->
            val newItems = state.items.map {
                it.copy(selected = !it.selected)
            }
            state.copy(items = newItems)
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
                        favorite = true,
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
}
