// Season Grouping Logic Refinement
package eu.kanade.tachiyomi.ui.anime

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Immutable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.util.addOrRemove
import eu.kanade.core.util.insertSeparators
import eu.kanade.domain.anime.interactor.SetAnimeViewerFlags
import eu.kanade.domain.anime.interactor.UpdateAnime
import eu.kanade.domain.anime.model.downloadedFilter
import eu.kanade.domain.anime.model.episodesFiltered
import tachiyomi.domain.anime.model.toSAnime
import tachiyomi.domain.anime.model.toDomainAnime
import eu.kanade.domain.episode.interactor.SetSeenStatus
import eu.kanade.domain.episode.interactor.SyncEpisodesWithSource
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.domain.track.interactor.TrackEpisode
import eu.kanade.domain.track.model.AutoTrackState
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.presentation.anime.DownloadAction
import eu.kanade.presentation.anime.components.EpisodeDownloadAction
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.torrentServer.service.TorrentServerService
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.isSourceForTorrents
import eu.kanade.tachiyomi.torrentServer.TorrentServerUtils
import eu.kanade.tachiyomi.ui.anime.track.TrackItem
import eu.kanade.tachiyomi.ui.player.loader.EpisodeLoader
import eu.kanade.tachiyomi.ui.player.loader.HosterLoader
import eu.kanade.tachiyomi.ui.player.settings.GesturePreferences
import eu.kanade.tachiyomi.ui.player.settings.PlayerPreferences
import eu.kanade.tachiyomi.util.AniChartApi
import eu.kanade.tachiyomi.util.episode.EpisodeSeasonUtils
import eu.kanade.tachiyomi.util.episode.getNextUnseen
import eu.kanade.tachiyomi.util.removeCovers
import eu.kanade.tachiyomi.util.system.toast
import exh.source.MERGED_SOURCE_ID
import exh.util.nullIfEmpty
import exh.util.trimOrNull
import java.util.Collections
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.domain.episode.interactor.FilterEpisodesForDownload
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.mapAsCheckboxState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.source.NoResultsException
import tachiyomi.domain.anime.interactor.GetAnimeWithEpisodes
import tachiyomi.domain.track.interactor.GetTracks
import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.domain.track.interactor.GetTracksPerAnime
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.track.model.Track
import tachiyomi.domain.anime.interactor.GetDuplicateLibraryAnime
import tachiyomi.domain.anime.interactor.SetAnimeEpisodeFlags
import tachiyomi.domain.anime.interactor.SetCustomAnimeInfo
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.AnimeUpdate
import tachiyomi.domain.anime.model.CustomAnimeInfo
import tachiyomi.domain.anime.model.MergedAnimeReference
import tachiyomi.domain.anime.model.Season
import tachiyomi.domain.anime.model.applyFilter
import tachiyomi.domain.anime.model.toAnimeUpdate
import tachiyomi.domain.anime.repository.AnimeRepository
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetAnimeCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.anime.interactor.FetchInterval
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.episode.interactor.SetAnimeDefaultEpisodeFlags
import tachiyomi.domain.episode.interactor.UpdateEpisode
import tachiyomi.domain.episode.model.Episode
import tachiyomi.domain.episode.model.EpisodeUpdate
import tachiyomi.domain.episode.service.calculateChapterGap
import tachiyomi.domain.episode.service.getEpisodeSort
import tachiyomi.domain.episode.service.missingEpisodesCount
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import tachiyomi.domain.anime.interactor.CalculateUserAffinity
import tachiyomi.domain.anime.interactor.GetLibraryAnime
import tachiyomi.domain.library.model.LibraryAnime
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.interactor.GetRelatedAnime
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.domain.track.interactor.DeleteTrack
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.source.localanime.LocalAnimeSource
import tachiyomi.source.localanime.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.Serializable
import java.util.Calendar
import kotlin.math.floor

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.domain.episode.model.applyFilters

class AnimeScreenModel(
    private val context: Context,
    private val lifecycle: Lifecycle,
    private val animeId: Long,
    private val isFromSource: Boolean,
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    private val downloadPreferences: DownloadPreferences = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val trackPreferences: TrackPreferences = Injekt.get(),
    internal val playerPreferences: PlayerPreferences = Injekt.get(),
    internal val gesturePreferences: GesturePreferences = Injekt.get(),
    private val trackerManager: TrackerManager = Injekt.get(),
    private val trackEpisode: TrackEpisode = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val downloadCache: DownloadCache = Injekt.get(),
    private val downloadProvider: eu.kanade.tachiyomi.data.download.DownloadProvider = Injekt.get(),
    private val getAnimeAndEpisodes: GetAnimeWithEpisodes = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val setCustomAnimeInfo: SetCustomAnimeInfo = Injekt.get(),
    private val getDuplicateLibraryAnime: GetDuplicateLibraryAnime = Injekt.get(),
    private val setAnimeEpisodeFlags: SetAnimeEpisodeFlags = Injekt.get(),
    private val setAnimeDefaultEpisodeFlags: SetAnimeDefaultEpisodeFlags = Injekt.get(),
    private val setSeenStatus: SetSeenStatus = Injekt.get(),
    private val updateEpisode: UpdateEpisode = Injekt.get(),
    private val updateAnime: UpdateAnime = Injekt.get(),
    private val syncEpisodesWithSource: SyncEpisodesWithSource = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val getTracks: GetTracks = Injekt.get(),
    private val insertTrack: InsertTrack = Injekt.get(),
    private val deleteTrack: DeleteTrack = Injekt.get(),
    private val addTracks: AddTracks = Injekt.get(),
    private val setAnimeCategories: SetAnimeCategories = Injekt.get(),
    private val animeRepository: AnimeRepository = Injekt.get(),
    private val deleteEpisodes: tachiyomi.domain.episode.interactor.DeleteEpisodes = Injekt.get(),
    private val filterEpisodesForDownload: FilterEpisodesForDownload = Injekt.get(),
    internal val setAnimeViewerFlags: SetAnimeViewerFlags = Injekt.get(),
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
    private val storagePreferences: StoragePreferences = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
    private val getRelatedAnime: GetRelatedAnime = Injekt.get(),
    private val calculateUserAffinity: CalculateUserAffinity = Injekt.get(),
    private val getLibraryAnime: GetLibraryAnime = Injekt.get(),
    private val getSeasonsByAnimeId: tachiyomi.domain.anime.interactor.GetSeasonsByAnimeId = Injekt.get(),
    private val discoverSeasons: tachiyomi.domain.anime.interactor.DiscoverSeasons = Injekt.get(),
    private val getMergedAnimeById: tachiyomi.domain.anime.interactor.GetMergedAnimeById = Injekt.get(),
    private val fetchInterval: FetchInterval = Injekt.get(),
    private val removeHistory: tachiyomi.domain.history.interactor.RemoveHistory = Injekt.get(),
    private val animeMergeRepository: tachiyomi.domain.anime.repository.AnimeMergeRepository = Injekt.get(),
) : StateScreenModel<AnimeScreenModel.State>(State.Loading) {

    private val successState: State.Success?
        get() = state.value as? State.Success

    val anime: Anime?
        get() = successState?.anime

    val source: Source?
        get() = successState?.source

    private val isFavorited: Boolean
        get() = anime?.favorite ?: false

    private val processedEpisodes: List<EpisodeList.Item>?
        get() = successState?.processedEpisodes

    val episodeSwipeStartAction = libraryPreferences.swipeEpisodeEndAction().get()
    val episodeSwipeEndAction = libraryPreferences.swipeEpisodeStartAction().get()
    var autoTrackState = trackPreferences.autoUpdateTrackOnMarkRead().get()

    val showNextEpisodeAirTime = trackPreferences.showNextEpisodeAiringTime().get()
    val alwaysUseExternalPlayer = playerPreferences.alwaysUseExternalPlayer().get()
    val useExternalDownloader = downloadPreferences.useExternalDownloader().get()

    val isUpdateIntervalEnabled =
        LibraryPreferences.ANIME_OUTSIDE_RELEASE_PERIOD in libraryPreferences.autoUpdateAnimeRestrictions.get()

    private val selectedPositions: Array<Int> = arrayOf(-1, -1)
    private val selectedEpisodeIds: HashSet<Long> = HashSet()

    internal var isFromChangeCategory: Boolean = false

    internal val autoOpenTrack: Boolean
        get() = successState?.trackingAvailable == true && trackPreferences.trackOnAddingToLibrary().get()

    val showFileSize = storagePreferences.showEpisodeFileSize().get()

    private var fetchSuggestionsJob: kotlinx.coroutines.Job? = null
    private val suggestionsDispatcher = kotlinx.coroutines.Dispatchers.IO.limitedParallelism(3)

    private fun State.Success.copySuccess(
        anime: Anime = this.anime,
        episodes: List<EpisodeList.Item> = this.episodes,
        trackItems: List<TrackItem> = this.trackItems,
        suggestionSections: ImmutableList<SuggestionSection> = this.suggestionSections,
        isSuggestionsLoading: Boolean = this.isSuggestionsLoading,
        dialog: Dialog? = this.dialog,
        isRefreshingData: Boolean = this.isRefreshingData,
        discoveryExpanded: Boolean = this.discoveryExpanded,
        mergedSources: ImmutableList<Source> = this.mergedSources,
        trackingCount: Int = this.trackingCount,
        hasLoggedInTrackers: Boolean = this.hasLoggedInTrackers,
        hasPromptedToAddBefore: Boolean = this.hasPromptedToAddBefore,
        suggestions: ImmutableList<Anime> = this.suggestions,
        seasons: ImmutableList<Season> = this.seasons,
        nextAiringEpisode: Pair<Int, Long> = this.nextAiringEpisode,
        selectedSeason: String? = this.selectedSeason,
        episodeToSeason: Map<Long, String> = this.episodeToSeason,
        showEpisodeSummary: Boolean = anime.showSummaries(),
        showEpisodeThumbnail: Boolean = anime.showPreviews(),
    ): State.Success {
        val episodesStatusHash = episodes.sumOf { it.episode.lastModifiedAt + if (it.episode.seen) 1 else 0 }
        val episodesChanged = episodes.size != this.episodes.size || 
                             episodesStatusHash != (this as? State.Success)?.let { success -> 
                                 success.episodes.sumOf { it.episode.lastModifiedAt + if (it.episode.seen) 1 else 0 } 
                             } ?: 0L ||
                             episodes.firstOrNull()?.episode?.id != this.episodes.firstOrNull()?.episode?.id

        val processedEpisodes = if (anime === this.anime && !episodesChanged) {
            this.processedEpisodes
        } else {
            episodes.applyFilters(anime).toImmutableList()
        }

        val missingEpisodeCount = if (processedEpisodes.size == this.processedEpisodes.size && !episodesChanged) {
            this.missingEpisodeCount
        } else {
            processedEpisodes.map { it.episode.episodeNumber }.missingEpisodesCount()
        }

        val (episodeListItems, availableSeasons, epToSeason) = if (
            anime.seasonGroupingMode == this.anime.seasonGroupingMode &&
            anime.sortDescending() == this.anime.sortDescending() &&
            !episodesChanged &&
            processedEpisodes.size == this.processedEpisodes.size
        ) {
            Triple(this.episodeListItems, this.availableSeasons, this.episodeToSeason)
        } else {
            val items = mutableListOf<EpisodeList>()
            val seasonsList = mutableListOf<String>()
            val mapping = mutableMapOf<Long, String>()
            
            val groupingMode = anime.seasonGroupingMode
            // Handle Seasons
            if (groupingMode != LibraryPreferences.SeasonGrouping.Disabled) {
                // Step 1: Detect if source provides episodes in descending order (newest first)
                val sourceOrdered = processedEpisodes.sortedBy { it.episode.sourceOrder }
                
                // Detect if sourceOrder is likely descending (newest first)
                val firstWithNumber = sourceOrdered.firstOrNull { it.episode.episodeNumber >= 0 }
                val lastWithNumber = sourceOrdered.lastOrNull { it.episode.episodeNumber >= 0 }
                val isSourceDescending = if (firstWithNumber != null && lastWithNumber != null && firstWithNumber !== lastWithNumber) {
                    firstWithNumber.episode.episodeNumber > lastWithNumber.episode.episodeNumber
                } else {
                    false
                }
                
                // Step 2: Process episodes in chronological sequence (oldest to newest) to find blocks
                val chronological = if (isSourceDescending) sourceOrdered.reversed() else sourceOrdered
                
                data class EpisodeBlock(
                    val episodes: MutableList<EpisodeList.Item> = mutableListOf(),
                    var year: Int? = null
                )
                val blocks = mutableListOf<EpisodeBlock>()
                var currentBlock = EpisodeBlock()
                val cal = Calendar.getInstance()
                
                for (index in chronological.indices) {
                    val item = chronological[index]
                    val prevItem = chronological.getOrNull(index - 1)
                    
                    val itemYear = if (item.episode.dateUpload > 0) {
                        cal.timeInMillis = item.episode.dateUpload
                        cal.get(Calendar.YEAR)
                    } else null

                    val currentExplicit = EpisodeSeasonUtils.getSeasonName(item.episode)
                    val prevExplicit = prevItem?.let { EpisodeSeasonUtils.getSeasonName(it.episode) }
                    val currentIsSpecial = EpisodeSeasonUtils.isSpecial(item.episode)
                    val prevIsSpecial = prevItem?.let { EpisodeSeasonUtils.isSpecial(it.episode) }

                    val isNewBlock = if (prevItem == null) {
                        true
                    } else if (currentIsSpecial != prevIsSpecial) {
                        // Split when switching between special and regular content
                        true
                    } else if (currentExplicit != null || prevExplicit != null) {
                        // If titles explicitly mention seasons, split whenever they change
                        currentExplicit != prevExplicit
                    } else {
                        // Fallback for episodes without "S1/S2" in title
                        val numRestart = item.episode.episodeNumber >= 0 && prevItem.episode.episodeNumber >= 0 && 
                                        item.episode.episodeNumber < prevItem.episode.episodeNumber
                        
                        val timeJump = item.episode.dateUpload > 0 && prevItem.episode.dateUpload > 0 && 
                            (item.episode.dateUpload - prevItem.episode.dateUpload) > 1000L * 60 * 60 * 24 * 60 // 60 days
                        
                        // Fallback: If date is same or missing (0), use sourceOrder + number restart as a strong signal
                        val sameDateRestart = (item.episode.dateUpload == prevItem.episode.dateUpload || item.episode.dateUpload <= 0) && numRestart

                        val prevYear = if (prevItem.episode.dateUpload > 0) {
                            cal.timeInMillis = prevItem.episode.dateUpload
                            cal.get(Calendar.YEAR)
                        } else null
                        
                        val yearChange = itemYear != null && prevYear != null && itemYear > prevYear
                        
                        if (currentIsSpecial) {
                            numRestart
                        } else {
                            numRestart || timeJump || yearChange || sameDateRestart
                        }
                    }

                    if (isNewBlock && currentBlock.episodes.isNotEmpty()) {
                        blocks.add(currentBlock)
                        currentBlock = EpisodeBlock()
                    }
                    currentBlock.episodes.add(item)
                    if (currentBlock.year == null) currentBlock.year = itemYear
                }
                if (currentBlock.episodes.isNotEmpty()) blocks.add(currentBlock)

                // Step 3: Assign season names to blocks
                var implicitSeasonCount = 0
                blocks.forEach { block ->
                    var explicitSeasonName: String? = null
                    var hasSpecials = false
                    for (item in block.episodes) {
                        if (EpisodeSeasonUtils.hasSpecialKeywords(item.episode) || EpisodeSeasonUtils.isSeasonZero(item.episode)) {
                            hasSpecials = true
                        }
                        if (explicitSeasonName == null) {
                            val name = EpisodeSeasonUtils.getSeasonName(item.episode)
                            if (name != "Season 0") explicitSeasonName = name
                        }
                    }
                    
                    val seasonName = if (hasSpecials) {
                        "Specials"
                    } else if (explicitSeasonName != null) {
                        explicitSeasonName
                    } else if (block.episodes.all { EpisodeSeasonUtils.isSpecial(it.episode) }) {
                        "Extras"
                    } else {
                        implicitSeasonCount++
                        if (block.year != null) {
                            "Season $implicitSeasonCount (${block.year})"
                        } else {
                            "Season $implicitSeasonCount"
                        }
                    }
                    
                    block.episodes.forEach { item ->
                        mapping[item.episode.id] = seasonName
                    }
                }

                // Step 4: Populate final list (Ordered based on UI sort preference)
                var lastSeasonHeader: String? = null
                for (i in 0..processedEpisodes.lastIndex) {
                    val item = processedEpisodes[i]
                    
                    // 1. Season Header (Must be BEFORE the item)
                    val seasonName = mapping[item.episode.id]
                    if (seasonName != null && seasonName != lastSeasonHeader) {
                        items.add(EpisodeList.Season(seasonName))
                        if (!seasonsList.contains(seasonName)) {
                            seasonsList.add(seasonName)
                        }
                        lastSeasonHeader = seasonName
                    }

                    // 2. Missing count at series start (only for ascending)
                    if (i == 0 && !anime.sortDescending()) {
                        val gap = floor(item.episode.episodeNumber).toInt().minus(1).coerceAtLeast(0)
                        if (gap > 0) {
                            items.add(EpisodeList.MissingCount("start-${item.id}", gap))
                        }
                    }

                    // 3. Add Item
                    items.add(item)

                    // 4. Missing count between items
                    val next = processedEpisodes.getOrNull(i + 1)
                    if (next != null) {
                        val higher = if (anime.sortDescending()) item else next
                        val lower = if (anime.sortDescending()) next else item
                        val gap = calculateChapterGap(higher.episode, lower.episode)
                        if (gap > 0) {
                            items.add(EpisodeList.MissingCount("${lower.id}-${higher.id}", gap))
                        }
                    }
                }
            } else {
                // Original logic for non-grouped episodes
                for (i in 0..processedEpisodes.lastIndex) {
                    val item = processedEpisodes[i]

                    // Missing count at series start (only for ascending)
                    if (i == 0 && !anime.sortDescending()) {
                        val gap = floor(item.episode.episodeNumber).toInt().minus(1).coerceAtLeast(0)
                        if (gap > 0) {
                            items.add(EpisodeList.MissingCount("start-${item.id}", gap))
                        }
                    }

                    items.add(item)

                    // Missing count between items
                    val next = processedEpisodes.getOrNull(i + 1)
                    if (next != null) {
                        val higher = if (anime.sortDescending()) item else next
                        val lower = if (anime.sortDescending()) next else item
                        val gap = calculateChapterGap(higher.episode, lower.episode)
                        if (gap > 0) {
                            items.add(EpisodeList.MissingCount("${lower.id}-${higher.id}", gap))
                        }
                    }
                }
            }
            Triple(items.toImmutableList(), seasonsList.sortedWith(EpisodeSeasonUtils.SeasonComparator).toImmutableList(), mapping)
        }

        val groupingMode = anime.seasonGroupingMode
        val availableSeasonsList = availableSeasons
        val episodeToSeason = epToSeason

        // Default to first season if none selected and grouping is in Tabs mode
        val sortedSeasons = availableSeasonsList
        var finalSelectedSeason = if (selectedSeason == null && groupingMode == LibraryPreferences.SeasonGrouping.Tabs) {
            sortedSeasons.firstOrNull()
        } else {
            selectedSeason
        }

        // Ensure selected season actually exists in the current list
        if (finalSelectedSeason != null && !availableSeasonsList.contains(finalSelectedSeason)) {
            finalSelectedSeason = sortedSeasons.firstOrNull()
        }

        return this.copy(
            anime = anime,
            episodes = episodes.toImmutableList(),
            processedEpisodes = processedEpisodes,
            episodeListItems = episodeListItems,
            missingEpisodeCount = missingEpisodeCount,
            trackItems = trackItems.toImmutableList(),
            suggestionSections = suggestionSections,
            dialog = dialog,
            isRefreshingData = isRefreshingData,
            discoveryExpanded = discoveryExpanded,
            mergedSources = mergedSources,
            trackingCount = trackingCount,
            hasLoggedInTrackers = hasLoggedInTrackers,
            hasPromptedToAddBefore = hasPromptedToAddBefore,
            suggestions = suggestions,
            isSuggestionsLoading = isSuggestionsLoading,
            seasons = seasons,
            nextAiringEpisode = nextAiringEpisode,
            availableSeasons = availableSeasons,
            selectedSeason = finalSelectedSeason,
            episodeToSeason = episodeToSeason,
            showEpisodeSummary = showEpisodeSummary,
            showEpisodeThumbnail = showEpisodeThumbnail,
        )
    }

    fun onSeasonSelected(season: String?) {
        updateSuccessState { it.copySuccess(selectedSeason = season) }
        if (season == null) {
            libraryPreferences.lastSelectedSeason(animeId).delete()
        } else {
            libraryPreferences.lastSelectedSeason(animeId).set(season)
        }
    }

    private inline fun updateSuccessState(func: (State.Success) -> State.Success) {
        mutableState.update {
            when (it) {
                State.Loading -> it
                is State.Success -> func(it)
            }
        }
    }

    init {
        screenModelScope.launchIO {
            val initialAnime = getAnimeAndEpisodes.awaitManga(animeId)
            val initialEpisodes = getAnimeAndEpisodes.awaitChapters(animeId).toEpisodeListItems(initialAnime)

            if (!initialAnime.favorite) {
                setAnimeDefaultEpisodeFlags.await(initialAnime)
            }

            val animeSource = Injekt.get<SourceManager>().getOrStub(initialAnime.source)
            if (animeSource.isSourceForTorrents()) {
                TorrentServerService.start()
                TorrentServerService.wait(10)
                TorrentServerUtils.setTrackersList()
            }

            // Set initial state from database
            val savedSeason = libraryPreferences.lastSelectedSeason(animeId).get().takeIf { it.isNotEmpty() }
            mutableState.update {
                State.Success.create(
                    anime = initialAnime,
                    source = animeSource,
                    isFromSource = isFromSource,
                    episodes = initialEpisodes,
                    isRefreshingData = !initialAnime.initialized || initialEpisodes.isEmpty(),
                    dialog = null,
                    selectedSeason = savedSeason,
                )
            }

            // Reactive stream for all subsequent updates (DB changes, downloads, etc.)
            combine(
                getAnimeAndEpisodes.subscribe(animeId).distinctUntilChanged(),
                downloadCache.changes,
                downloadManager.queueState,
            ) { animeAndEpisodes, _, _ -> animeAndEpisodes }
                .onEach { (anime, episodes) ->
                    val oldAnime = successState?.anime
                    updateSuccessState {
                        it.copySuccess(
                            anime = anime,
                            episodes = episodes.toEpisodeListItems(anime),
                        )
                    }
                    // If details were just loaded (genre added), retry suggestions
                    if (oldAnime?.genre.isNullOrEmpty() && !anime.genre.isNullOrEmpty() && successState?.suggestionSections.isNullOrEmpty()) {
                        fetchSuggestions(anime)
                    }
                }
                .launchIn(this)
            
            observeDownloads()
            observeTrackers()
            observeSeasons()
            observeMergedAnime()

            screenModelScope.launchIO {
                fetchTriggerFlow.filter { it == animeId }.collect {
                    successState?.let { state -> fetchSuggestions(state.anime, manualFetch = true) }
                }
            }

            if (isActive) {
                val isLocal = initialAnime.isLocal()
                val needRefreshInfo = !initialAnime.initialized
                val needRefreshEpisode = initialEpisodes.isEmpty() || isLocal

                if (needRefreshInfo || needRefreshEpisode) {
                    val fetchFromSourceTasks = listOf(
                        async { fetchAnimeFromSource() },
                        async { fetchEpisodesFromSource() },
                    )
                    fetchFromSourceTasks.awaitAll()
                }
            }
            updateSuccessState { it.copySuccess(isRefreshingData = false) }
            if (initialAnime.initialized) {
                fetchSuggestions(initialAnime)
            }
        }
    }

    fun fetchAllFromSource(manualFetch: Boolean = true) {
        screenModelScope.launch {
            updateSuccessState { it.copySuccess(isRefreshingData = true) }
            val fetchFromSourceTasks = listOf(
                async { fetchAnimeFromSource(manualFetch) },
                async { fetchEpisodesFromSource(manualFetch) },
            )
            fetchFromSourceTasks.awaitAll()
            updateSuccessState { it.copySuccess(isRefreshingData = false) }
            successState?.let { updateAiringTime(it.anime, it.trackItems, manualFetch) }
        }
    }

    private suspend fun fetchAnimeFromSource(manualFetch: Boolean = false) {
        val state = successState ?: return
        try {
            withIOContext {
                val networkAnime = state.source.getAnimeDetails(state.anime.toSAnime())
                updateAnime.awaitUpdateFromSource(state.anime, networkAnime, manualFetch)
            }
        } catch (e: Throwable) {
            if (e is HttpException && e.code == 103) return
            logcat(LogPriority.ERROR, e)
            screenModelScope.launch {
                snackbarHostState.showSnackbar(message = with(context) { e.formattedMessage })
            }
        }
    }

    internal data class CachedSuggestions(
        val sections: ImmutableList<SuggestionSection>,
        val timestamp: Long,
    )

    internal companion object {
        // Limit to 50 anime to prevent OOM, LruCache is thread-safe
        val suggestionsCache = android.util.LruCache<Long, CachedSuggestions>(50)
        private const val CACHE_TTL = 60 * 60 * 1000L // 1 hour

        private val _suggestionsUpdateFlow = kotlinx.coroutines.flow.MutableSharedFlow<Long>(extraBufferCapacity = 1)
        val suggestionsUpdateFlow = _suggestionsUpdateFlow.asSharedFlow()

        private val _fetchTriggerFlow = kotlinx.coroutines.flow.MutableSharedFlow<Long>(extraBufferCapacity = 1)
        val fetchTriggerFlow = _fetchTriggerFlow.asSharedFlow()

        fun triggerFetch(animeId: Long) {
            _fetchTriggerFlow.tryEmit(animeId)
        }
    }

    private fun fetchSuggestions(anime: Anime, manualFetch: Boolean = false) {
        val now = System.currentTimeMillis()
        val cached = suggestionsCache.get(anime.id)
        if (cached != null && (now - cached.timestamp) < CACHE_TTL) {
            updateSuccessState { it.copySuccess(suggestionSections = cached.sections, isSuggestionsLoading = false) }
            return
        }

        if (!manualFetch) {
            val showSuggestions = sourcePreferences.relatedAnimeShowSource().get()
            val expandSuggestions = sourcePreferences.relatedAnimeExpand().get()
            val inOverflow = sourcePreferences.relatedAnimeInOverflow().get()

            if (!showSuggestions || !expandSuggestions || inOverflow) {
                updateSuccessState { it.copySuccess(isSuggestionsLoading = false) }
                return
            }
        }

        updateSuccessState { it.copySuccess(isSuggestionsLoading = true) }

        fetchSuggestionsJob?.cancel()
        fetchSuggestionsJob = screenModelScope.launch(suggestionsDispatcher) {
            if (!manualFetch) delay(500) // Wait for entry animation to finish
            try {
                // Update affinity vector in background if needed
                calculateUserAffinity.await()

                val source = sourceManager.get(anime.source) as? AnimeCatalogueSource ?: run {
                    updateSuccessState { it.copySuccess(isSuggestionsLoading = false) }
                    return@launch
                }
                val library = getLibraryAnime.await()

                val affinityMap = try {
                    val json = Json.parseToJsonElement(libraryPreferences.userAffinityMap().get()).jsonObject
                    json.mapValues { it.value.jsonPrimitive.float }
                } catch (e: Exception) { emptyMap<String, Float>() }

                // Only deduplicate against the current anime itself to keep density high as requested
                val initialSections = SuggestionSection.Type.entries.map { type ->
                    SuggestionSection(
                        title = when (type) {
                            SuggestionSection.Type.Franchise -> "Series & Sequels"
                            SuggestionSection.Type.Similarity -> "Similar Media"
                            SuggestionSection.Type.Source -> "Recommended"
                            SuggestionSection.Type.Tag -> "You Might Like"
                            else -> "Other"
                        },
                        items = persistentListOf(),
                        type = type
                    )
                }.toMutableList()

                fun rankAndSortItems(items: List<Anime>, currentAnime: Anime, type: SuggestionSection.Type): List<Anime> {
                    val currentClean = eu.kanade.tachiyomi.util.lang.StringSimilarity.cleanTitle(currentAnime.title)
                    return items.distinctBy { it.id to it.url }
                        .filter { it.id != currentAnime.id && it.url != currentAnime.url }
                        .map { candidate ->
                            val candClean = eu.kanade.tachiyomi.util.lang.StringSimilarity.cleanTitle(candidate.title)

                            // 1. Metadata Similarity (Order independent)
                            val titleSim = eu.kanade.tachiyomi.util.lang.StringSimilarity.tokenSortRatio(currentClean, candClean)

                            // 2. User Affinity Score
                            var affinityScore = 0f
                            candidate.genre?.forEach { tag ->
                                affinityScore += affinityMap[tag.trim().lowercase()] ?: 0f
                            }

                            // 3. Franchise Context
                            val isFranchise = library.any { lib ->
                                eu.kanade.tachiyomi.util.lang.StringSimilarity.tokenSortRatio(candClean, eu.kanade.tachiyomi.util.lang.StringSimilarity.cleanTitle(lib.anime.title)) > 85
                            }

                            val franchiseWeight = if (type == SuggestionSection.Type.Franchise) 3.0f else if (isFranchise) 0.4f else 1.0f
                            val baseScore = 1.0f

                            candidate to ((baseScore + affinityScore) * (0.3f + titleSim.toFloat()) * franchiseWeight)
                        }
                        .sortedByDescending { it.second }
                        .map { it.first }
                }

                fun updateSection(type: SuggestionSection.Type, items: List<Anime>) {
                    val currentSuccess = successState ?: return
                    val rankedItems = rankAndSortItems(items, currentSuccess.anime, type).toImmutableList()
                    
                    updateSuccessState { state ->
                        val index = initialSections.indexOfFirst { it.type == type }
                        if (index != -1) {
                            initialSections[index] = initialSections[index].copy(items = rankedItems)

                            // Fallback: If Recommended (Source) is empty but Franchise has items, mirror them to Recommended
                            if (type == SuggestionSection.Type.Franchise && initialSections.find { it.type == SuggestionSection.Type.Source }?.items.isNullOrEmpty()) {
                                val sourceIndex = initialSections.indexOfFirst { it.type == SuggestionSection.Type.Source }
                                if (sourceIndex != -1) {
                                    initialSections[sourceIndex] = initialSections[sourceIndex].copy(items = rankedItems.take(10).toImmutableList())
                                }
                            }
                        }
                        val finalSections = initialSections
                            .sortedBy { it.type }
                            .toImmutableList()
                        suggestionsCache.put(anime.id, CachedSuggestions(finalSections, System.currentTimeMillis()))
                        _suggestionsUpdateFlow.tryEmit(anime.id)
                        state.copySuccess(suggestionSections = finalSections)
                    }
                }

                // Discovery Load
                kotlinx.coroutines.withTimeoutOrNull(20000L) {
                    kotlinx.coroutines.coroutineScope {
                        // 0. Franchise & Sequels (Strict Verification)
                        launch {
                            try {
                                val rawVirtualSeasons = discoverSeasons.await(anime)
                                if (rawVirtualSeasons.isNotEmpty()) {
                                    val validSeasons = rawVirtualSeasons
                                        .map { async { networkToLocalAnime.await(it) } }
                                        .awaitAll()
                                        .mapNotNull { getAnime.await(it.id) }

                                    if (validSeasons.isNotEmpty()) {
                                        updateSection(SuggestionSection.Type.Franchise, validSeasons)
                                    }
                                }
                            } catch (_: Exception) {}
                        }

                        // 1. Similar Media (Broad Search Probe)
                        launch {
                            val keywords = eu.kanade.tachiyomi.util.lang.StringSimilarity.getSearchKeywords(anime.title)
                            try {
                                val searchResult = source.getSearchAnime(1, keywords, source.getFilterList())
                                val domainAnimes = searchResult.animes
                                    .map { async { networkToLocalAnime.await(it.toDomainAnime(anime.source)) } }
                                    .awaitAll()
                                    .mapNotNull { getAnime.await(it.id) }
                                if (domainAnimes.isNotEmpty()) updateSection(SuggestionSection.Type.Similarity, domainAnimes)
                            } catch (_: Exception) {}
                        }

                        // 3. Official Related (Source Provided)
                        launch {
                            try {
                                getRelatedAnime.subscribe(anime).collect { (_, animes) ->
                                    if (animes.isNotEmpty()) {
                                        kotlinx.coroutines.coroutineScope {
                                            val domainAnimes = animes
                                                .map { async { networkToLocalAnime.await(it.toDomainAnime(anime.source)) } }
                                                .awaitAll()
                                                .mapNotNull { getAnime.await(it.id) }
                                            updateSection(SuggestionSection.Type.Source, domainAnimes)
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                        }

                        // 4. Smart Recommendations (Parallel Tag Search)
                        launch {
                            if (!sourcePreferences.relatedAnimeShowSmart().get()) {
                                updateSection(SuggestionSection.Type.Tag, emptyList())
                                return@launch
                            }
                            kotlinx.coroutines.withTimeoutOrNull(15000L) {
                                kotlinx.coroutines.coroutineScope {
                                    val tags = anime.genre?.take(3) ?: emptyList()
                                    val results = tags.map { tag ->
                                        async {
                                            try {
                                                val filterList = source.getFilterList()
                                                var query = tag

                                                // Pro-Level: Try to find and apply the actual Genre/Tag filter from the extension
                                                val genreFilter = filterList.find { it.name.contains("Genre", true) || it.name.contains("Tag", true) }
                                                if (genreFilter != null) {
                                                    when (genreFilter) {
                                                        is AnimeFilter.Select<*> -> {
                                                            val select = genreFilter as AnimeFilter.Select<Any>
                                                            val index = select.values.indexOfFirst { it.toString().contains(tag, true) }
                                                            if (index != -1) {
                                                                select.state = index
                                                                query = "" // Clear query to use filter search
                                                            }
                                                        }
                                                        is AnimeFilter.Group<*> -> {
                                                            val subFilters = genreFilter.state as? List<*>
                                                            val subFilter = subFilters?.find { (it as? AnimeFilter<*>)?.name?.contains(tag, true) == true }
                                                            if (subFilter is AnimeFilter.CheckBox) {
                                                                subFilter.state = true
                                                                query = "" // Clear query to use filter search
                                                            } else if (subFilter is AnimeFilter.TriState) {
                                                                subFilter.state = AnimeFilter.TriState.STATE_INCLUDE
                                                                query = "" // Clear query to use filter search
                                                            }
                                                        }
                                                        else -> {}
                                                    }
                                                }

                                                val searchResult = source.getSearchAnime(1, query, filterList)
                                                searchResult.animes
                                                    .map { async { networkToLocalAnime.await(it.toDomainAnime(anime.source)) } }
                                                    .awaitAll()
                                                    .mapNotNull { getAnime.await(it.id) }
                                            } catch (_: Exception) {
                                                emptyList()
                                            }
                                        }
                                    }.awaitAll().flatten().distinctBy { it.id }.filter { it.id != anime.id }

                                    updateSection(SuggestionSection.Type.Tag, results)
                                }
                            } ?: updateSection(SuggestionSection.Type.Tag, emptyList())
                        }
                    }
                } ?: updateSuccessState { it.copySuccess(isSuggestionsLoading = false) }
            } catch (e: Exception) {
                // Log error if needed
            } finally {
                updateSuccessState { it.copySuccess(isSuggestionsLoading = false) }
            }
        }
    }
    fun setLocalTrack(score: Double, status: Long) {
        val state = successState ?: return
        val anime = state.anime
        screenModelScope.launchIO {
            val tracks = getTracks.await(anime.id)
            val existingTrack = tracks.find { it.trackerId == TrackerManager.LOCAL }
            
            if (existingTrack != null) {
                insertTrack.await(existingTrack.copy(status = status, score = score))
            } else {
                val dbTrack = eu.kanade.tachiyomi.data.database.models.Track.create(TrackerManager.LOCAL).apply {
                    anime_id = anime.id
                    this.title = anime.title.ifBlank { anime.ogTitle }
                    this.status = status
                    this.score = score
                }
                insertTrack.await(dbTrack.toDomainTrack(idRequired = false)!!)
            }
        }
    }

    fun updateAnimeInfo(
        title: String?,
        author: String?,
        artist: String?,
        thumbnailUrl: String?,
        description: String?,
        tags: List<String>?,
        status: Long?,
        score: Double?,
        note: String?,
    ) {
        val state = successState ?: return
        var anime = state.anime

        if (state.anime.isLocal()) {
            val newTitle = if (title.isNullOrBlank()) anime.url else title.trim()
            val newAuthor = author?.trimOrNull()
            val newArtist = artist?.trimOrNull()
            val newDesc = description?.trimOrNull()
            anime = anime.copy(
                ogTitle = newTitle,
                ogAuthor = author?.trimOrNull(),
                ogArtist = artist?.trimOrNull(),
                ogDescription = description?.trimOrNull(),
                ogGenre = tags?.nullIfEmpty(),
                ogStatus = status ?: 0,
                lastUpdate = anime.lastUpdate + 1,
            )
            (sourceManager.get(LocalAnimeSource.ID) as LocalAnimeSource).updateAnimeInfo(anime.toSAnime())
            screenModelScope.launchNonCancellable {
                updateAnime.await(
                    AnimeUpdate(
                        anime.id,
                        title = newTitle,
                        author = newAuthor,
                        artist = newArtist,
                        description = newDesc,
                        genre = tags,
                        status = status,
                    ),
                )
            }
        } else {
            val genre = if (!tags.isNullOrEmpty() && tags != state.anime.ogGenre) tags else null
            setCustomAnimeInfo.set(
                CustomAnimeInfo(
                    state.anime.id,
                    title?.trimOrNull(),
                    author?.trimOrNull(),
                    artist?.trimOrNull(),
                    thumbnailUrl?.trimOrNull(),
                    description?.trimOrNull(),
                    genre,
                    status.takeUnless { it == state.anime.ogStatus },
                    score,
                    note?.trimOrNull(),
                ),
            )
        }
    }

    fun toggleFavorite(checkDuplicate: Boolean = true) {
        toggleFavorite(
            onRemoved = {
                screenModelScope.launch {
                    if (!hasDownloads()) return@launch
                    val result = snackbarHostState.showSnackbar(
                        message = context.stringResource(MR.strings.delete_downloads_for_anime),
                        actionLabel = context.stringResource(MR.strings.action_delete),
                        withDismissAction = true,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        deleteDownloads()
                    }
                }
            },
            checkDuplicate = checkDuplicate,
        )
    }

    fun toggleFavorite(onRemoved: () -> Unit, checkDuplicate: Boolean = true) {
        val state = successState ?: return
        screenModelScope.launchIO {
            val anime = state.anime
            if (isFavorited) {
                if (updateAnime.awaitUpdateFavorite(anime.id, false)) {
                    if (trackPreferences.autoTrackWhenWatching().get()) {
                        val tracks = getTracks.await(anime.id)
                        val localTrack = tracks.find { it.trackerId == TrackerManager.LOCAL }
                        if (localTrack != null) {
                            when {
                                // If never started, delete track to keep history clean
                                localTrack.lastEpisodeSeen == 0.0 -> {
                                    deleteTrack.await(anime.id, TrackerManager.LOCAL)
                                }
                                // If already completed, leave it as completed
                                localTrack.status == eu.kanade.tachiyomi.data.track.local.LocalTracker.COMPLETED -> {}
                                // Otherwise, mark as dropped (including movies with progress)
                                else -> insertTrack.await(localTrack.copy(status = eu.kanade.tachiyomi.data.track.local.LocalTracker.DROPPED))
                            }
                        }
                    }
                    if (anime.removeCovers() != anime) {
                        updateAnime.awaitUpdateCoverLastModified(anime.id)
                    }
                    withUIContext { onRemoved() }
                }
            } else {
                if (checkDuplicate) {
                    val duplicate = getDuplicateLibraryAnime.await(anime).getOrNull(0)
                    if (duplicate != null) {
                        updateSuccessState { it.copySuccess(dialog = Dialog.DuplicateAnime(anime, duplicate)) }
                        return@launchIO
                    }
                }
                val categories = getCategories()
                val defaultCategoryId = libraryPreferences.defaultCategory().get().toLong()
                val defaultCategory = categories.find { it.id == defaultCategoryId }
                when {
                    defaultCategory != null -> {
                        if (updateAnime.awaitUpdateFavorite(anime.id, true)) moveAnimeToCategory(defaultCategory)
                    }
                    defaultCategoryId == 0L || categories.isEmpty() -> {
                        if (updateAnime.awaitUpdateFavorite(anime.id, true)) moveAnimeToCategory(null)
                    }
                    else -> {
                        isFromChangeCategory = true
                        showChangeCategoryDialog()
                    }
                }

                if (trackPreferences.autoAddTrack().get()) {
                    addTracks.bindEnhancedTrackers(anime, state.source)

                    val tracks = getTracks.await(anime.id)
                    var localTrack = tracks.find { it.trackerId == TrackerManager.LOCAL }
                    if (localTrack == null) {
                        val episodes = getAnimeAndEpisodes.awaitChapters(anime.id)
                        val seenCount = episodes.count { it.seen }
                        val dbTrack = eu.kanade.tachiyomi.data.database.models.Track.create(TrackerManager.LOCAL).apply {
                            this.anime_id = anime.id
                            this.title = anime.title
                            this.last_episode_seen = seenCount.toDouble()
                            this.total_episodes = episodes.size.toLong()
                            this.status = when {
                                episodes.isNotEmpty() && (seenCount == episodes.size) -> eu.kanade.tachiyomi.data.track.local.LocalTracker.COMPLETED
                                seenCount > 0 -> eu.kanade.tachiyomi.data.track.local.LocalTracker.WATCHING
                                else -> eu.kanade.tachiyomi.data.track.local.LocalTracker.PLAN_TO_WATCH
                            }
                        }
                        localTrack = dbTrack.toDomainTrack(idRequired = false)
                    } else if (localTrack.status == eu.kanade.tachiyomi.data.track.local.LocalTracker.DROPPED) {
                        localTrack = localTrack.copy(status = eu.kanade.tachiyomi.data.track.local.LocalTracker.PLAN_TO_WATCH)
                    }
                    localTrack?.let { insertTrack.await(it) }
                }

                syncAnimeOnAdd(anime)
            }
        }
    }

    private suspend fun syncAnimeOnAdd(anime: Anime) {
        if (libraryPreferences.syncOnAdd().get()) {
            val fetchWindow = fetchInterval.getWindow(java.time.ZonedDateTime.now())
            try {
                val source = sourceManager.getOrStub(anime.source)
                val episodes = source.getEpisodeList(anime.toSAnime())
                syncEpisodesWithSource.await(episodes, anime, source, false, fetchWindow)
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
            }
        }
    }

    fun showChangeCategoryDialog() {
        val anime = successState?.anime ?: return
        screenModelScope.launch {
            val categories = getCategories()
            val selection = getAnimeCategoryIds(anime)
            updateSuccessState { it.copySuccess(
                dialog = Dialog.ChangeCategory(
                    anime = anime,
                    initialSelection = categories.mapAsCheckboxState { it.id in selection }.toImmutableList(),
                ),
            )}
        }
    }

    private fun hasDownloads(): Boolean {
        val anime = successState?.anime ?: return false
        return downloadManager.getDownloadCount(anime) > 0
    }

    private fun deleteDownloads() {
        val state = successState ?: return
        downloadManager.deleteAnime(state.anime, state.source)
    }

    suspend fun getCategories(): List<Category> {
        return getCategories.await().filterNot { it.isSystemCategory }
    }

    private suspend fun getAnimeCategoryIds(anime: Anime): List<Long> {
        return getCategories.await(anime.id).map { it.id }
    }

    fun moveAnimeToCategoriesAndAddToLibrary(anime: Anime, categories: List<Long>) {
        moveAnimeToCategory(categories)
        if (anime.favorite) return
        screenModelScope.launchIO { updateAnime.awaitUpdateFavorite(anime.id, true) }
    }

    private fun moveAnimeToCategory(categoryIds: List<Long>) {
        screenModelScope.launchIO { setAnimeCategories.await(animeId, categoryIds) }
    }

    private fun moveAnimeToCategory(category: Category?) {
        moveAnimeToCategory(listOfNotNull(category?.id))
    }

    private fun observeDownloads() {
        screenModelScope.launchIO {
            downloadManager.statusFlow()
                .filter { it.anime.id == successState?.anime?.id }
                .catch { error -> logcat(LogPriority.ERROR, error) }
                .flowWithLifecycle(lifecycle)
                .collect { withUIContext { updateDownloadState(it) } }
        }
        screenModelScope.launchIO {
            downloadManager.progressFlow()
                .filter { it.anime.id == successState?.anime?.id }
                .catch { error -> logcat(LogPriority.ERROR, error) }
                .flowWithLifecycle(lifecycle)
                .collect { withUIContext { updateDownloadState(it) } }
        }
    }

    private fun updateDownloadState(download: Download) {
        updateSuccessState { successState ->
            val modifiedIndex = successState.episodes.indexOfFirst { it.id == download.episode.id }
            if (modifiedIndex < 0) return@updateSuccessState successState
            val newEpisodes = successState.episodes.toMutableList().apply {
                val item = removeAt(modifiedIndex).copy(downloadState = download.status, downloadProgress = download.progress)
                add(modifiedIndex, item)
            }.toImmutableList()
            successState.copySuccess(episodes = newEpisodes)
        }
    }

    private fun List<Episode>.toEpisodeListItems(anime: Anime): List<EpisodeList.Item> {
        val isLocal = anime.isLocal()
        val downloadedEpisodeDirs = if (isLocal) emptySet() else downloadManager.getDownloadedEpisodeDirs(anime)
        return map { episode ->
            val activeDownload = if (isLocal) null else downloadManager.getQueuedDownloadOrNull(episode.id)
            val downloaded = if (isLocal) {
                true
            } else if (downloadedEpisodeDirs.isNotEmpty()) {
                downloadProvider.getValidEpisodeDirNames(episode.name, episode.scanlator).any { it in downloadedEpisodeDirs }
            } else false
            val downloadState = when {
                activeDownload != null -> activeDownload.status
                downloaded -> Download.State.DOWNLOADED
                else -> Download.State.NOT_DOWNLOADED
            }
            EpisodeList.Item(episode = episode, downloadState = downloadState, downloadProgress = activeDownload?.progress ?: 0, selected = episode.id in selectedEpisodeIds)
        }
    }

    private suspend fun fetchEpisodesFromSource(manualFetch: Boolean = false) {
        val state = successState ?: return
        try {
            withIOContext {
                val episodes = state.source.getEpisodeList(state.anime.toSAnime())
                val newEpisodes = syncEpisodesWithSource.await(episodes, state.anime, state.source, manualFetch)
                if (manualFetch) downloadNewEpisodes(newEpisodes)
            }
        } catch (e: Throwable) {
            val message = if (e is NoResultsException) context.stringResource(MR.strings.no_episodes_error) else {
                logcat(LogPriority.ERROR, e)
                with(context) { e.formattedMessage }
            }
            screenModelScope.launch { snackbarHostState.showSnackbar(message = message) }
            val newAnime = animeRepository.getAnimeById(animeId)
            updateSuccessState { it.copySuccess(anime = newAnime, isRefreshingData = false) }
        }
    }

    fun episodeSwipe(episodeItem: EpisodeList.Item, swipeAction: LibraryPreferences.EpisodeSwipeAction) {
        screenModelScope.launch { executeEpisodeSwipeAction(episodeItem, swipeAction) }
    }

    private fun executeEpisodeSwipeAction(episodeItem: EpisodeList.Item, swipeAction: LibraryPreferences.EpisodeSwipeAction) {
        val episode = episodeItem.episode
        when (swipeAction) {
            LibraryPreferences.EpisodeSwipeAction.ToggleSeen -> markEpisodesSeen(listOf(episode), !episode.seen)
            LibraryPreferences.EpisodeSwipeAction.ToggleBookmark -> bookmarkEpisodes(listOf(episode), !episode.bookmark)
            LibraryPreferences.EpisodeSwipeAction.ToggleFillermark -> fillermarkEpisodes(listOf(episode), !episode.fillermark)
            LibraryPreferences.EpisodeSwipeAction.Download -> {
                val downloadAction: EpisodeDownloadAction = when (episodeItem.downloadState) {
                    Download.State.ERROR, Download.State.NOT_DOWNLOADED, Download.State.PAUSED -> EpisodeDownloadAction.START_NOW
                    Download.State.QUEUE, Download.State.DOWNLOADING, Download.State.MERGING, Download.State.DECRYPTING, Download.State.FINALIZING -> EpisodeDownloadAction.CANCEL
                    Download.State.DOWNLOADED -> EpisodeDownloadAction.DELETE
                }
                runEpisodeDownloadActions(items = listOf(episodeItem), action = downloadAction)
            }
            LibraryPreferences.EpisodeSwipeAction.Disabled -> throw IllegalStateException()
        }
    }

    fun getNextUnseenEpisode(): Episode? {
        val successState = successState ?: return null
        return successState.episodes.getNextUnseen(
            anime = successState.anime,
            seasonName = successState.selectedSeason.takeIf { successState.anime.seasonGroupingMode == LibraryPreferences.SeasonGrouping.Tabs },
            episodeToSeason = successState.episodeToSeason,
        )
    }

    private fun getUnseenEpisodes(): List<Episode> {
        return successState?.processedEpisodes?.filter { (episode, dlStatus) -> !episode.seen && dlStatus == Download.State.NOT_DOWNLOADED }?.map { it.episode }?.toList() ?: emptyList()
    }

    private fun getUnseenEpisodesSorted(): List<Episode> {
        val anime = successState?.anime ?: return emptyList()
        val episodes = getUnseenEpisodes().sortedWith(getEpisodeSort(anime))
        return if (anime.sortDescending()) episodes.reversed() else episodes
    }

    private fun startDownload(episodes: List<Episode>, startNow: Boolean, video: Video? = null) {
        val successState = successState ?: return
        screenModelScope.launchNonCancellable {
            if (startNow) {
                val episodeId = episodes.singleOrNull()?.id ?: return@launchNonCancellable
                downloadManager.startDownloadNow(episodeId)
            } else {
                downloadEpisodes(episodes, useExternalDownloader, video)
            }
            if (!isFavorited && !successState.hasPromptedToAddBefore) {
                updateSuccessState { it.copySuccess(hasPromptedToAddBefore = true) }
                val result = snackbarHostState.showSnackbar(message = context.stringResource(MR.strings.snack_add_to_anime_library), actionLabel = context.stringResource(MR.strings.action_add), withDismissAction = true)
                if (result == SnackbarResult.ActionPerformed && !isFavorited) toggleFavorite()
            }
        }
    }

    fun runEpisodeDownloadActions(items: List<EpisodeList.Item>, action: EpisodeDownloadAction) {
        when (action) {
            EpisodeDownloadAction.START -> {
                startDownload(items.map { it.episode }, false)
                if (items.any { it.downloadState == Download.State.ERROR }) downloadManager.startDownloads()
            }
            EpisodeDownloadAction.START_NOW -> startDownload(listOf(items.singleOrNull()?.episode ?: return), true)
            EpisodeDownloadAction.CANCEL -> cancelDownload(items.singleOrNull()?.id ?: return)
            EpisodeDownloadAction.DELETE -> deleteEpisodes(items.map { it.episode })
            EpisodeDownloadAction.SHOW_QUALITIES -> showQualitiesDialog(items.singleOrNull()?.episode ?: return)
        }
    }

    fun runDownloadAction(action: DownloadAction) {
        val episodesToDownload = when (action) {
            DownloadAction.NEXT_1_EPISODE -> getUnseenEpisodesSorted().take(1)
            DownloadAction.NEXT_5_EPISODES -> getUnseenEpisodesSorted().take(5)
            DownloadAction.NEXT_10_EPISODES -> getUnseenEpisodesSorted().take(10)
            DownloadAction.NEXT_25_EPISODES -> getUnseenEpisodesSorted().take(25)
            DownloadAction.UNSEEN_EPISODES -> getUnseenEpisodes()
        }
        if (episodesToDownload.isNotEmpty()) startDownload(episodesToDownload, false)
    }

    private fun cancelDownload(episodeId: Long) {
        val activeDownload = downloadManager.getQueuedDownloadOrNull(episodeId) ?: return
        downloadManager.cancelQueuedDownloads(listOf(activeDownload))
        updateDownloadState(activeDownload.apply { status = Download.State.NOT_DOWNLOADED })
    }

    fun markPreviousEpisodeSeen(pointer: Episode) {
        val anime = successState?.anime ?: return
        val episodes = processedEpisodes.orEmpty().map { it.episode }.toList()
        val prevEpisodes = if (anime.sortDescending()) episodes.asReversed() else episodes
        val pointerPos = prevEpisodes.indexOf(pointer)
        if (pointerPos != -1) markEpisodesSeen(prevEpisodes.take(pointerPos), true)
    }

    fun markEpisodesSeen(episodes: List<Episode>, seen: Boolean) {
        val anime = successState?.anime ?: return
        toggleAllSelection(false)
        screenModelScope.launchIO {
            setSeenStatus.await(seen = seen, episodes = episodes.toTypedArray())

            if (seen) {
                val removeAfterSeenSlots = downloadPreferences.removeAfterReadSlots().get()
                if (removeAfterSeenSlots != -1) {
                    val allEpisodes = successState?.episodes?.map { it.episode }.orEmpty()
                    val sortedEpisodes = allEpisodes.sortedWith(getEpisodeSort(anime)).let {
                        if (anime.sortDescending()) it.reversed() else it
                    }

                    episodes.forEach { chosenEpisode ->
                        val currentEpisodePosition = sortedEpisodes.indexOfFirst { it.id == chosenEpisode.id }
                        if (currentEpisodePosition != -1) {
                            val episodeToDelete = sortedEpisodes.getOrNull(currentEpisodePosition - removeAfterSeenSlots)
                            if (episodeToDelete != null && episodeToDelete.seen) {
                                downloadManager.enqueueEpisodesToDelete(listOf(episodeToDelete), anime)
                            }
                        }
                    }
                }

                if (downloadPreferences.removeAfterMarkedAsSeen().get()) {
                    downloadManager.enqueueEpisodesToDelete(episodes, anime)
                }

                downloadManager.deletePendingEpisodes()
            }

            if (!seen || successState?.hasLoggedInTrackers == false || autoTrackState == AutoTrackState.NEVER) return@launchIO
            val tracks = getTracks.await(animeId)
            val maxEpisodeNumber = episodes.maxOf { it.episodeNumber }
            if (tracks.none { it.lastEpisodeSeen < maxEpisodeNumber }) return@launchIO
            if (autoTrackState == AutoTrackState.ALWAYS) {
                trackEpisode.await(context, animeId, maxEpisodeNumber)
                withUIContext { context.toast(context.stringResource(MR.strings.trackers_updated_summary_anime, maxEpisodeNumber.toInt())) }
                return@launchIO
            }
            val result = snackbarHostState.showSnackbar(message = context.stringResource(MR.strings.confirm_tracker_update_anime, maxEpisodeNumber.toInt()), actionLabel = context.stringResource(MR.strings.action_ok), duration = SnackbarDuration.Short, withDismissAction = true)
            if (result == SnackbarResult.ActionPerformed) trackEpisode.await(context, animeId, maxEpisodeNumber)
        }
    }

    private fun downloadEpisodes(episodes: List<Episode>, alt: Boolean = false, video: Video? = null) {
        val anime = successState?.anime ?: return
        downloadManager.downloadEpisodes(anime, episodes, true, alt, video)
        toggleAllSelection(false)
    }

    fun bookmarkEpisodes(episodes: List<Episode>, bookmarked: Boolean) {
        screenModelScope.launchIO {
            episodes.filterNot { it.bookmark == bookmarked }.map { EpisodeUpdate(id = it.id, bookmark = bookmarked) }.let { updateEpisode.awaitAll(it) }
        }
        toggleAllSelection(false)
    }

    fun fillermarkEpisodes(episodes: List<Episode>, fillermarked: Boolean) {
        screenModelScope.launchIO {
            episodes.filterNot { it.fillermark == fillermarked }.map { EpisodeUpdate(id = it.id, fillermark = fillermarked) }.let { updateEpisode.awaitAll(it) }
        }
        toggleAllSelection(false)
    }

    fun deleteEpisodes(episodes: List<Episode>) {
        screenModelScope.launchNonCancellable {
            try {
                successState?.let { state -> downloadManager.deleteEpisodes(episodes, state.anime, state.source, isManual = true) }
            } catch (e: Throwable) { logcat(LogPriority.ERROR, e) }
        }
    }

    private fun downloadNewEpisodes(episodes: List<Episode>) {
        screenModelScope.launchNonCancellable {
            val anime = successState?.anime ?: return@launchNonCancellable
            val episodesToDownload = filterEpisodesForDownload.await(anime, episodes)
            if (episodesToDownload.isNotEmpty()) downloadEpisodes(episodesToDownload)
        }
    }

    fun setUnseenFilter(state: TriState) {
        val anime = successState?.anime ?: return
        val flag = when (state) {
            TriState.DISABLED -> Anime.SHOW_ALL
            TriState.ENABLED_IS -> Anime.EPISODE_SHOW_UNSEEN
            TriState.ENABLED_NOT -> Anime.EPISODE_SHOW_SEEN
        }
        screenModelScope.launchNonCancellable { setAnimeEpisodeFlags.awaitSetUnreadFilter(anime, flag) }
    }

    fun setDownloadedFilter(state: TriState) {
        val anime = successState?.anime ?: return
        val flag = when (state) {
            TriState.DISABLED -> Anime.SHOW_ALL
            TriState.ENABLED_IS -> Anime.EPISODE_SHOW_DOWNLOADED
            TriState.ENABLED_NOT -> Anime.EPISODE_SHOW_NOT_DOWNLOADED
        }
        screenModelScope.launchNonCancellable { setAnimeEpisodeFlags.awaitSetDownloadedFilter(anime, flag) }
    }

    fun setBookmarkedFilter(state: TriState) {
        val anime = successState?.anime ?: return
        val flag = when (state) {
            TriState.DISABLED -> Anime.SHOW_ALL
            TriState.ENABLED_IS -> Anime.EPISODE_SHOW_BOOKMARKED
            TriState.ENABLED_NOT -> Anime.EPISODE_SHOW_NOT_BOOKMARKED
        }
        screenModelScope.launchNonCancellable { setAnimeEpisodeFlags.awaitSetBookmarkFilter(anime, flag) }
    }

    fun setFillermarkedFilter(state: TriState) {
        val anime = successState?.anime ?: return
        val flag = when (state) {
            TriState.DISABLED -> Anime.SHOW_ALL
            TriState.ENABLED_IS -> Anime.EPISODE_SHOW_FILLERMARKED
            TriState.ENABLED_NOT -> Anime.EPISODE_SHOW_NOT_FILLERMARKED
        }
        screenModelScope.launchNonCancellable { setAnimeEpisodeFlags.awaitSetFillermarkFilter(anime, flag) }
    }

    fun setDisplayMode(mode: Long) {
        val anime = successState?.anime ?: return
        if (mode and 0x10000000L != 0L) {
            val flag = mode and 0x10000000L.inv()
            screenModelScope.launchNonCancellable {
                libraryPreferences.lastSelectedSeason(animeId).delete()
                setAnimeEpisodeFlags.awaitSetSeasonGroupingRaw(anime, flag)
            }
            return
        }
        screenModelScope.launchNonCancellable { setAnimeEpisodeFlags.awaitSetDisplayMode(anime, mode) }
    }

    fun setSorting(sort: Long) {
        val anime = successState?.anime ?: return
        screenModelScope.launchNonCancellable { setAnimeEpisodeFlags.awaitSetSortingModeOrFlipOrder(anime, sort) }
    }

    fun setShowEpisodeSummary(flag: Long) {
        val anime = successState?.anime ?: return
        screenModelScope.launchNonCancellable { setAnimeEpisodeFlags.awaitSetShowSummaries(anime, flag) }
    }

    fun setShowEpisodeThumbnail(flag: Long) {
        val anime = successState?.anime ?: return
        screenModelScope.launchNonCancellable { setAnimeEpisodeFlags.awaitSetShowPreviews(anime, flag) }
    }

    fun setCurrentSettingsAsDefault(applyToExisting: Boolean) {
        val anime = successState?.anime ?: return
        screenModelScope.launchNonCancellable {
            libraryPreferences.setEpisodeSettingsDefault(anime)
            if (applyToExisting) {
                setAnimeEpisodeFlags.awaitSetAllAnimeFlags(
                    unseenFilter = anime.unseenFilterRaw,
                    downloadedFilter = anime.downloadedFilterRaw,
                    bookmarkedFilter = anime.bookmarkedFilterRaw,
                    fillermarkedFilter = anime.fillermarkedFilterRaw,
                    sortingMode = anime.sorting,
                    displayMode = anime.displayMode,
                    sortingDirection = if (anime.sortDescending()) Anime.EPISODE_SORT_DESC else Anime.EPISODE_SORT_ASC,
                    seasonGrouping = anime.episodeFlags and Anime.EPISODE_SEASON_GROUP_MASK,
                    showPreviews = anime.episodeFlags and Anime.EPISODE_PREVIEWS_MASK,
                    showSummaries = anime.episodeFlags and Anime.EPISODE_SUMMARIES_MASK,
                )
            }
            snackbarHostState.showSnackbar(message = context.stringResource(MR.strings.episode_settings_updated))
        }
    }

    fun toggleSelection(item: EpisodeList.Item, selected: Boolean, userSelected: Boolean = false, fromLongPress: Boolean = false) {
        updateSuccessState { successState ->
            val newEpisodes = successState.processedEpisodes.toMutableList().apply {
                val selectedIndex = successState.processedEpisodes.indexOfFirst { it.id == item.episode.id }
                if (selectedIndex < 0) return@apply
                val selectedItem = get(selectedIndex)
                if ((selectedItem.selected && selected) || (!selectedItem.selected && !selected)) return@apply
                val firstSelection = none { it.selected }
                set(selectedIndex, selectedItem.copy(selected = selected))
                selectedEpisodeIds.addOrRemove(item.id, selected)
                if (selected && userSelected && fromLongPress) {
                    if (firstSelection) {
                        selectedPositions[0] = selectedIndex
                        selectedPositions[1] = selectedIndex
                    } else {
                        val range = if (selectedIndex < selectedPositions[0]) {
                            val r = selectedIndex + 1..<selectedPositions[0]
                            selectedPositions[0] = selectedIndex
                            r
                        } else if (selectedIndex > selectedPositions[1]) {
                            val r = (selectedPositions[1] + 1)..<selectedIndex
                            selectedPositions[1] = selectedIndex
                            r
                        } else IntRange.EMPTY
                        range.forEach {
                            val inbetweenItem = get(it)
                            if (!inbetweenItem.selected) {
                                selectedEpisodeIds.add(inbetweenItem.id)
                                set(it, inbetweenItem.copy(selected = true))
                            }
                        }
                    }
                } else if (userSelected && !fromLongPress) {
                    if (!selected) {
                        if (selectedIndex == selectedPositions[0]) selectedPositions[0] = indexOfFirst { it.selected }
                        else if (selectedIndex == selectedPositions[1]) selectedPositions[1] = indexOfLast { it.selected }
                    } else {
                        if (selectedIndex < selectedPositions[0]) selectedPositions[0] = selectedIndex
                        else if (selectedIndex > selectedPositions[1]) selectedPositions[1] = selectedIndex
                    }
                }
            }.toImmutableList()
            successState.copySuccess(episodes = newEpisodes)
        }
    }

    fun toggleAllSelection(selected: Boolean) {
        updateSuccessState { successState ->
            val newEpisodes = successState.episodes.map {
                selectedEpisodeIds.addOrRemove(it.id, selected)
                it.copy(selected = selected)
            }.toImmutableList()
            selectedPositions[0] = -1
            selectedPositions[1] = -1
            successState.copySuccess(episodes = newEpisodes)
        }
    }

    fun invertSelection() {
        updateSuccessState { successState ->
            val newEpisodes = successState.episodes.map {
                selectedEpisodeIds.addOrRemove(it.id, !it.selected)
                it.copy(selected = !it.selected)
            }.toImmutableList()
            selectedPositions[0] = -1
            selectedPositions[1] = -1
            successState.copySuccess(episodes = newEpisodes)
        }
    }

    private fun observeTrackers() {
        val anime = successState?.anime ?: return
        screenModelScope.launchIO {
            combine(getTracks.subscribe(anime.id).catch { logcat(LogPriority.ERROR, it) }, trackerManager.loggedInTrackersFlow()) { animeTracks, loggedInTrackers ->
                val supportedTrackers = loggedInTrackers.filter { (it as? EnhancedTracker)?.accept(source!!) ?: true }
                val supportedTrackerIds = supportedTrackers.map { it.id }.toHashSet()
                val supportedTrackerTracks = animeTracks.filter { it.trackerId in supportedTrackerIds }
                supportedTrackerTracks.size to supportedTrackers.isNotEmpty()
            }.flowWithLifecycle(lifecycle).distinctUntilChanged().collectLatest { (trackingCount, hasLoggedInTrackers) ->
                updateSuccessState { it.copySuccess(trackingCount = trackingCount, hasLoggedInTrackers = hasLoggedInTrackers) }
            }
        }
        screenModelScope.launchIO {
            combine(getTracks.subscribe(anime.id).catch { logcat(LogPriority.ERROR, it) }, trackerManager.loggedInTrackersFlow()) { animeTracks, loggedInTrackers ->
                loggedInTrackers.map { service -> TrackItem(animeTracks.find { it.trackerId == service.id }, service) }
            }.distinctUntilChanged().collectLatest { trackItems -> 
                updateSuccessState { it.copySuccess(trackItems = trackItems) }
                updateAiringTime(anime, trackItems, manualFetch = false) 
            }
        }
    }

    private fun observeSeasons() {
        val virtualSeasonsFlow = state.map { successState ->
            (successState as? State.Success)?.suggestionSections
                ?.find { it.type == SuggestionSection.Type.Franchise }
                ?.items.orEmpty()
        }.distinctUntilChanged()

        getSeasonsByAnimeId.subscribe(animeId, virtualSeasonsFlow)
            .onEach { seasons ->
                updateSuccessState { it.copySuccess(seasons = seasons.toImmutableList()) }
            }
            .launchIn(screenModelScope)
    }

    private fun observeMergedAnime() {
        screenModelScope.launchIO {
            getMergedAnimeById.subscribe(animeId)
                .onEach { mergedAnime ->
                    val sources = mergedAnime.map { sourceManager.getOrStub(it.source) }
                    updateSuccessState { it.copySuccess(mergedSources = sources.toImmutableList()) }
                }
                .launchIn(this)
        }
    }

    private suspend fun updateAiringTime(anime: Anime, trackItems: List<TrackItem>, manualFetch: Boolean) {
        val airingEpisodeData = AniChartApi().loadAiringTime(anime, trackItems, manualFetch)
        setAnimeViewerFlags.awaitSetNextEpisodeAiring(anime.id, airingEpisodeData)
        updateSuccessState { it.copySuccess(nextAiringEpisode = airingEpisodeData) }
    }

    data class MergedAnimeData(val anime: Map<Long, Anime>, val references: List<MergedAnimeReference>)

    sealed interface Dialog {
        data class ChangeCategory(val anime: Anime, val initialSelection: ImmutableList<CheckboxState<Category>>) : Dialog
        data class DeleteEpisodes(val episodes: List<Episode>) : Dialog
        data class DuplicateAnime(val anime: Anime, val duplicate: Anime) : Dialog
        data class Migrate(val newAnime: Anime, val oldAnime: Anime) : Dialog
        data class SetAnimeFetchInterval(val anime: Anime) : Dialog
        data class ShowQualities(val episode: Episode, val anime: Anime, val source: Source) : Dialog
        data class EditAnimeInfo(val anime: Anime) : Dialog
        data class LocalScorePicker(val score: Double, val status: Long) : Dialog
        data class EditMergedAnimeSettings(val data: MergedAnimeData) : Dialog
        data object ChangeAnimeSkipIntro : Dialog
        data object ClearAnime : Dialog
        data object SettingsSheet : Dialog
        data object TrackSheet : Dialog
        data object FullCover : Dialog
    }

    fun toggleDiscoveryExpansion() {
        updateSuccessState { it.copySuccess(discoveryExpanded = !it.discoveryExpanded) }
    }

    fun triggerFetchSuggestions() {
        successState?.let { fetchSuggestions(it.anime, manualFetch = true) }
    }

    fun dismissDialog() = updateSuccessState { it.copySuccess(dialog = null) }
    fun showDeleteEpisodeDialog(episodes: List<Episode>) = updateSuccessState { it.copySuccess(dialog = Dialog.DeleteEpisodes(episodes)) }
    fun showSettingsDialog() = updateSuccessState { it.copySuccess(dialog = Dialog.SettingsSheet) }
    fun showTrackDialog() = updateSuccessState { it.copySuccess(dialog = Dialog.TrackSheet) }
    fun showCoverDialog() = updateSuccessState { it.copySuccess(dialog = Dialog.FullCover) }
    fun showEditAnimeInfoDialog() = updateSuccessState { it.copySuccess(dialog = Dialog.EditAnimeInfo(it.anime)) }

    fun showEditMergedSettings() {
        val state = successState ?: return
        screenModelScope.launchIO {
            val mergedAnimes = animeMergeRepository.getMergedAnimeById(animeId)
            val references = animeMergeRepository.getReferencesById(animeId)
            updateSuccessState {
                it.copySuccess(dialog = Dialog.EditMergedAnimeSettings(MergedAnimeData(mergedAnimes.associateBy { it.id }, references)))
            }
        }
    }

    fun updateMergedSettings(references: List<MergedAnimeReference>) {
        screenModelScope.launchIO {
            animeMergeRepository.updateAllSettings(references.map { it.toMergeAnimeSettingsUpdate() })
        }
    }

    private fun MergedAnimeReference.toMergeAnimeSettingsUpdate() = tachiyomi.domain.anime.model.MergeAnimeSettingsUpdate(
        id = id,
        isInfoAnime = isInfoAnime,
        getEpisodeUpdates = getEpisodeUpdates,
        episodeSortMode = episodeSortMode,
        episodePriority = episodePriority,
        downloadEpisodes = downloadEpisodes,
    )

    fun deleteMergedEntry(reference: MergedAnimeReference) {
        screenModelScope.launchIO {
            animeMergeRepository.deleteById(reference.id)
        }
    }

    fun showLocalScoreDialog() {
        val state = successState ?: return
        val localTrack = state.trackItems.find { it.tracker.id == TrackerManager.LOCAL }?.track
        val currentScore = localTrack?.score ?: 0.0
        val currentStatus = localTrack?.status ?: eu.kanade.tachiyomi.data.track.local.LocalTracker.WATCHING
        updateSuccessState { it.copySuccess(dialog = Dialog.LocalScorePicker(currentScore, currentStatus)) }
    }
    fun showMigrateDialog(duplicate: Anime) = updateSuccessState { it.copySuccess(dialog = Dialog.Migrate(newAnime = it.anime, oldAnime = duplicate)) }
    fun showAnimeSkipIntroDialog() = updateSuccessState { it.copySuccess(dialog = Dialog.ChangeAnimeSkipIntro) }
    fun showClearAnimeDialog() = updateSuccessState { it.copySuccess(dialog = Dialog.ClearAnime) }

    fun showSetAnimeFetchIntervalDialog() {
        val anime = successState?.anime ?: return
        updateSuccessState { it.copySuccess(dialog = Dialog.SetAnimeFetchInterval(anime)) }
    }

    fun setFetchInterval(anime: Anime, interval: Int) {
        screenModelScope.launchIO {
            if (updateAnime.awaitUpdateFetchInterval(anime.copy(fetchInterval = interval, nextUpdate = 0L))) {
                val updatedAnime = animeRepository.getAnimeById(anime.id)
                updateSuccessState { it.copySuccess(anime = updatedAnime) }
            }
        }
    }

    fun clearAnime(deleteDownloads: Boolean, deleteFromDatabase: Boolean) {
        val state = successState ?: return
        screenModelScope.launchIO {
            if (deleteDownloads) {
                val episodes = getAnimeAndEpisodes.awaitChapters(animeId)
                deleteEpisodes(episodes)
            }

            if (deleteFromDatabase) {
                removeHistory.await(animeId)
                val episodes = getAnimeAndEpisodes.awaitChapters(animeId)
                setSeenStatus.await(seen = false, episodes = episodes.toTypedArray())
            }

            // Remove custom info
            setCustomAnimeInfo.set(CustomAnimeInfo(animeId, null))

            // Reset favorite
            if (state.anime.favorite) {
                toggleFavorite()
            }

            dismissDialog()
        }
    }

    private fun showQualitiesDialog(episode: Episode) = updateSuccessState { it.copySuccess(dialog = Dialog.ShowQualities(episode, it.anime, it.source)) }

    sealed interface State {
        @Immutable data object Loading : State
        @Immutable data class Success(
            val anime: Anime,
            val source: Source,
            val isFromSource: Boolean,
            val episodes: ImmutableList<EpisodeList.Item>,
            val processedEpisodes: ImmutableList<EpisodeList.Item>,
            val episodeListItems: ImmutableList<EpisodeList>,
            val missingEpisodeCount: Int = 0,
            val trackingCount: Int = 0,
            val hasLoggedInTrackers: Boolean = false,
            val isRefreshingData: Boolean = false,
            val dialog: Dialog? = null,
            val hasPromptedToAddBefore: Boolean = false,
            val trackItems: ImmutableList<TrackItem> = persistentListOf(),
            val nextAiringEpisode: Pair<Int, Long> = Pair(anime.nextEpisodeToAir, anime.nextEpisodeAiringAt),
            val suggestions: ImmutableList<Anime> = persistentListOf(),
            val isSuggestionsLoading: Boolean = true,
            val suggestionSections: ImmutableList<SuggestionSection> = persistentListOf(),
            val seasons: ImmutableList<Season> = persistentListOf(),
            val availableSeasons: ImmutableList<String> = persistentListOf(),
            val selectedSeason: String? = null,
            val discoveryExpanded: Boolean = false,
            val mergedSources: ImmutableList<Source> = persistentListOf(),
            val episodeToSeason: Map<Long, String> = emptyMap(),
            val showEpisodeSummary: Boolean = true,
            val showEpisodeThumbnail: Boolean = true,
        ) : State {
            companion object {
                fun create(
                    anime: Anime,
                    source: Source,
                    isFromSource: Boolean,
                    episodes: List<EpisodeList.Item>,
                    isRefreshingData: Boolean,
                    dialog: Dialog?,
                    selectedSeason: String? = null,
                ): Success {
                    val processedEpisodes = episodes.applyFilters(anime).toImmutableList()
                    val missingEpisodeCount = processedEpisodes.map { it.episode.episodeNumber }.missingEpisodesCount()
                    
                    val episodeListItems = mutableListOf<EpisodeList>()
                    val availableSeasonsList = mutableListOf<String>()
                    val episodeToSeason = mutableMapOf<Long, String>()
                    
                    val groupingMode = anime.seasonGroupingMode
                    // Handle Seasons
                    if (groupingMode != LibraryPreferences.SeasonGrouping.Disabled) {
                        // Step 1: Detect if source provides episodes in descending order (newest first)
                        val sourceOrdered = processedEpisodes.sortedBy { it.episode.sourceOrder }
                        
                        // Detect if sourceOrder is likely descending (newest first)
                        val firstWithNumber = sourceOrdered.firstOrNull { it.episode.episodeNumber >= 0 }
                        val lastWithNumber = sourceOrdered.lastOrNull { it.episode.episodeNumber >= 0 }
                        val isSourceDescending = if (firstWithNumber != null && lastWithNumber != null && firstWithNumber !== lastWithNumber) {
                            firstWithNumber.episode.episodeNumber > lastWithNumber.episode.episodeNumber
                        } else {
                            false
                        }
                        
                        // Step 2: Process episodes in chronological sequence (oldest to newest) to find blocks
                        val chronological = if (isSourceDescending) sourceOrdered.reversed() else sourceOrdered
                        
                        data class EpisodeBlock(
                            val episodes: MutableList<EpisodeList.Item> = mutableListOf(),
                            var year: Int? = null
                        )
                        val blocks = mutableListOf<EpisodeBlock>()
                        var currentBlock = EpisodeBlock()
                        val cal = Calendar.getInstance()
                        
                        for (index in chronological.indices) {
                            val item = chronological[index]
                            val prevItem = chronological.getOrNull(index - 1)
                            
                            val itemYear = if (item.episode.dateUpload > 0) {
                                cal.timeInMillis = item.episode.dateUpload
                                cal.get(Calendar.YEAR)
                            } else null

                            val currentExplicit = EpisodeSeasonUtils.getSeasonName(item.episode)
                            val prevExplicit = prevItem?.let { EpisodeSeasonUtils.getSeasonName(it.episode) }
                            val currentIsSpecial = EpisodeSeasonUtils.isSpecial(item.episode)
                            val prevIsSpecial = prevItem?.let { EpisodeSeasonUtils.isSpecial(it.episode) }

                            val isNewBlock = if (prevItem == null) {
                                true
                            } else if (currentIsSpecial != prevIsSpecial) {
                                // Split when switching between special and regular content
                                true
                            } else if (currentExplicit != null || prevExplicit != null) {
                                // If titles explicitly mention seasons, split whenever they change
                                currentExplicit != prevExplicit
                            } else {
                                // Fallback for episodes without "S1/S2" in title
                                val numRestart = item.episode.episodeNumber >= 0 && prevItem.episode.episodeNumber >= 0 && 
                                                item.episode.episodeNumber < prevItem.episode.episodeNumber
                                
                                val timeJump = item.episode.dateUpload > 0 && prevItem.episode.dateUpload > 0 && 
                                    (item.episode.dateUpload - prevItem.episode.dateUpload) > 1000L * 60 * 60 * 24 * 60 // 60 days

                                val sameDateRestart = (item.episode.dateUpload == prevItem.episode.dateUpload || item.episode.dateUpload <= 0) && numRestart
                                
                                val prevYear = if (prevItem.episode.dateUpload > 0) {
                                    cal.timeInMillis = prevItem.episode.dateUpload
                                    cal.get(Calendar.YEAR)
                                } else null
                                
                                val yearChange = itemYear != null && prevYear != null && itemYear > prevYear
                                
                                if (currentIsSpecial) {
                                    numRestart
                                } else {
                                    numRestart || timeJump || yearChange || sameDateRestart
                                }
                            }

                            if (isNewBlock && currentBlock.episodes.isNotEmpty()) {
                                blocks.add(currentBlock)
                                currentBlock = EpisodeBlock()
                            }
                            currentBlock.episodes.add(item)
                            if (currentBlock.year == null) currentBlock.year = itemYear
                        }
                        if (currentBlock.episodes.isNotEmpty()) blocks.add(currentBlock)

                        // Step 3: Assign season names to blocks
                        var implicitSeasonCount = 0
                        blocks.forEach { block ->
                            var explicitSeasonName: String? = null
                            var hasSpecials = false
                            for (item in block.episodes) {
                                if (EpisodeSeasonUtils.hasSpecialKeywords(item.episode) || EpisodeSeasonUtils.isSeasonZero(item.episode)) {
                                    hasSpecials = true
                                }
                                if (explicitSeasonName == null) {
                                    val name = EpisodeSeasonUtils.getSeasonName(item.episode)
                                    if (name != "Season 0") explicitSeasonName = name
                                }
                            }
                            
                            val seasonName = if (hasSpecials) {
                                "Specials"
                            } else if (explicitSeasonName != null) {
                                explicitSeasonName
                            } else if (block.episodes.all { EpisodeSeasonUtils.isSpecial(it.episode) }) {
                                "Extras"
                            } else {
                                implicitSeasonCount++
                                if (block.year != null) {
                                    "Season $implicitSeasonCount (${block.year})"
                                } else {
                                    "Season $implicitSeasonCount"
                                }
                            }
                            
                            block.episodes.forEach { item ->
                                episodeToSeason[item.episode.id] = seasonName
                            }
                        }

                        // Step 4: Populate final list (Ordered based on UI sort preference)
                        var lastSeasonHeader: String? = null
                        for (i in 0..processedEpisodes.lastIndex) {
                            val item = processedEpisodes[i]
                            
                            // 1. Season Header (Must be BEFORE the item)
                            val seasonName = episodeToSeason[item.episode.id]
                            if (seasonName != null && seasonName != lastSeasonHeader) {
                                episodeListItems.add(EpisodeList.Season(seasonName))
                                if (!availableSeasonsList.contains(seasonName)) {
                                    availableSeasonsList.add(seasonName)
                                }
                                lastSeasonHeader = seasonName
                            }

                            // 2. Missing count at series start (only for ascending)
                            if (i == 0 && !anime.sortDescending()) {
                                val gap = floor(item.episode.episodeNumber).toInt().minus(1).coerceAtLeast(0)
                                if (gap > 0) {
                                    episodeListItems.add(EpisodeList.MissingCount("start-${item.id}", gap))
                                }
                            }

                            // 3. Add Item
                            episodeListItems.add(item)

                            // 4. Missing count between items
                            val next = processedEpisodes.getOrNull(i + 1)
                            if (next != null) {
                                val higher = if (anime.sortDescending()) item else next
                                val lower = if (anime.sortDescending()) next else item
                                val gap = calculateChapterGap(higher.episode, lower.episode)
                                if (gap > 0) {
                                    episodeListItems.add(EpisodeList.MissingCount("${lower.id}-${higher.id}", gap))
                                }
                            }
                        }
                    } else {
                        // Original logic for non-grouped episodes
                        for (i in 0..processedEpisodes.lastIndex) {
                            val item = processedEpisodes[i]

                            // Missing count at series start (only for ascending)
                            if (i == 0 && !anime.sortDescending()) {
                                val gap = floor(item.episode.episodeNumber).toInt().minus(1).coerceAtLeast(0)
                                if (gap > 0) {
                                    episodeListItems.add(EpisodeList.MissingCount("start-${item.id}", gap))
                                }
                            }

                            episodeListItems.add(item)

                            // Missing count between items
                            val next = processedEpisodes.getOrNull(i + 1)
                            if (next != null) {
                                val higher = if (anime.sortDescending()) item else next
                                val lower = if (anime.sortDescending()) next else item
                                val gap = calculateChapterGap(higher.episode, lower.episode)
                                if (gap > 0) {
                                    episodeListItems.add(EpisodeList.MissingCount("${lower.id}-${higher.id}", gap))
                                }
                            }
                        }
                    }

                    // Default to first season if none selected and grouping is in Tabs mode
                    val sortedSeasons = availableSeasonsList.sortedWith(EpisodeSeasonUtils.SeasonComparator)
                    var finalSelectedSeason = if (selectedSeason == null && groupingMode == LibraryPreferences.SeasonGrouping.Tabs) {
                        sortedSeasons.firstOrNull()
                    } else {
                        selectedSeason
                    }

                    // Ensure selected season actually exists in the current list
                    if (finalSelectedSeason != null && !availableSeasonsList.contains(finalSelectedSeason)) {
                        finalSelectedSeason = sortedSeasons.firstOrNull()
                    }

                    return Success(
                        anime = anime,
                        source = source,
                        isFromSource = isFromSource,
                        episodes = episodes.toImmutableList(),
                        processedEpisodes = processedEpisodes,
                        episodeListItems = episodeListItems.toImmutableList(),
                        missingEpisodeCount = missingEpisodeCount,
                        isRefreshingData = isRefreshingData,
                        dialog = dialog,
                        availableSeasons = sortedSeasons.toImmutableList(),
                        selectedSeason = finalSelectedSeason,
                        episodeToSeason = episodeToSeason,
                        showEpisodeSummary = anime.showSummaries(),
                        showEpisodeThumbnail = anime.showPreviews(),
                    )
                }
            }
            val totalScore: Double? by lazy {
                val localTrackScore = trackItems.find { it.tracker.id == 999L }?.track?.score?.takeIf { it > 0 }
                localTrackScore ?: anime.score ?: trackItems.mapNotNull { item ->
                    item.track?.let { item.tracker.animeService.get10PointScore(it) }
                }.filter { it > 0 }.average().takeIf { !it.isNaN() }
            }

            val trackingAvailable: Boolean get() = trackItems.isNotEmpty()
            val airingEpisodeNumber: Double get() = nextAiringEpisode.first.toDouble()
            val airingTime: Long get() = nextAiringEpisode.second.times(1000L).minus(Calendar.getInstance().timeInMillis)
            val filterActive: Boolean get() = anime.episodesFiltered()
        }
    }
}
