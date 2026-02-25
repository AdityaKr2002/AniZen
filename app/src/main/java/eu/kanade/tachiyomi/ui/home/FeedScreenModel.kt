package eu.kanade.tachiyomi.ui.home

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.anime.interactor.NetworkToLocalAnime
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.toDomainAnime
import tachiyomi.domain.source.interactor.GetFeedSavedSearchCategories
import tachiyomi.domain.source.interactor.GetFeedSavedSearchGlobal
import tachiyomi.domain.source.interactor.GetSavedSearchGlobalFeed
import tachiyomi.domain.source.interactor.InsertFeedSavedSearchCategory
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.FeedSavedSearchCategory
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.history.interactor.LogActivity
import tachiyomi.domain.history.model.ActivityLog
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class FeedScreenModel(
    private val sourceManager: SourceManager = Injekt.get(),
    private val getFeedSavedSearchGlobal: GetFeedSavedSearchGlobal = Injekt.get(),
    private val getSavedSearchGlobalFeed: GetSavedSearchGlobalFeed = Injekt.get(),
    private val networkToLocalAnime: NetworkToLocalAnime = Injekt.get(),
    private val getAnime: GetAnime = Injekt.get(),
    private val getFeedSavedSearchCategories: GetFeedSavedSearchCategories = Injekt.get(),
    private val insertFeedSavedSearchCategory: InsertFeedSavedSearchCategory = Injekt.get(),
    private val logActivity: LogActivity = Injekt.get(),
) : StateScreenModel<FeedScreenModel.State>(State()) {

    private val feedJobs = java.util.concurrent.ConcurrentHashMap<Long, kotlinx.coroutines.Job>()
    private val lastTopUrls = java.util.concurrent.ConcurrentHashMap<Long, String>()

    init {
        screenModelScope.launchIO {
            var categories = getFeedSavedSearchCategories.await()
            if (categories.isEmpty()) {
                insertFeedSavedSearchCategory.await("Global")
                categories = getFeedSavedSearchCategories.await()
            }
            mutableState.update { it.copy(categories = categories.toImmutableList()) }
            setupFeedSubscriptions(categories)

            getFeedSavedSearchCategories.subscribe()
                .onEach { updatedCategories ->
                    mutableState.update { it.copy(categories = updatedCategories.toImmutableList()) }
                    setupFeedSubscriptions(updatedCategories)
                }
                .launchIn(screenModelScope)
        }
    }

    private fun setupFeedSubscriptions(categories: List<FeedSavedSearchCategory>) {
        // Cancel jobs for removed categories
        val categoryIds = categories.map { it.id }.toSet()
        val removedIds = feedJobs.keys - categoryIds
        removedIds.forEach { id ->
            feedJobs[id]?.cancel()
            feedJobs.remove(id)
        }

        // Start jobs for new categories or existing ones if not running
        categories.forEach { category ->
            if (!feedJobs.containsKey(category.id)) {
                feedJobs[category.id] = screenModelScope.launchIO {
                    combine(
                        getFeedSavedSearchGlobal.subscribe(category.id),
                        sourceManager.isInitialized,
                        ::Pair
                    ).collectLatest { (feedSavedSearches, isInitialized) ->
                        if (!isInitialized) return@collectLatest

                        // Fetch saved searches for the current category
                        val savedSearches = getSavedSearchGlobalFeed.await(category.id)
                        
                        // 1. Establish structural placeholders immediately
                        val initialItems = feedSavedSearches.mapNotNull { feed ->
                            val source = sourceManager.get(feed.source) as? AnimeCatalogueSource ?: return@mapNotNull null
                            FeedItem(
                                feed = feed,
                                source = source,
                                savedSearch = savedSearches.find { it.id == feed.savedSearch },
                                animeList = persistentListOf(),
                            )
                        }.toImmutableList()

                        mutableState.update { state ->
                            val newItems = state.items.toMutableMap()
                            newItems[category.id] = initialItems
                            state.copy(items = newItems.toImmutableMap())
                        }

                        // 2. Load content in parallel
                        coroutineScope {
                            feedSavedSearches.forEach { feed ->
                                launch {
                                    val source = sourceManager.get(feed.source) as? AnimeCatalogueSource
                                    if (source == null) return@launch

                                    var retryCount = 0
                                    var loadedAnime: ImmutableList<Anime>? = null

                                    while (retryCount < 3 && loadedAnime == null) {
                                        try {
                                            val results = when (FeedSavedSearch.Type.from(feed.type)) {
                                                FeedSavedSearch.Type.Latest -> {
                                                    try {
                                                        source.getLatestUpdates(1).animes
                                                    } catch (e: Exception) {
                                                        source.getPopularAnime(1).animes
                                                    }
                                                }
                                                FeedSavedSearch.Type.Popular -> source.getPopularAnime(1).animes
                                                FeedSavedSearch.Type.SavedSearch -> {
                                                    val savedSearch = savedSearches.find { it.id == feed.savedSearch }
                                                    if (savedSearch != null) {
                                                        val filters = source.getFilterList()
                                                        source.getSearchAnime(1, savedSearch.query ?: "", filters).animes
                                                    } else {
                                                        emptyList()
                                                    }
                                                }
                                            }

                                            val animeList = results.map {
                                                async {
                                                    val domainAnime = it.toDomainAnime(source.id)
                                                    networkToLocalAnime.await(domainAnime)
                                                }
                                            }.awaitAll().filterNotNull().distinctBy { it.id }.toImmutableList()

                                            // Delta tracking for stats
                                            val currentTopUrl = results.firstOrNull()?.url
                                            val previousTopUrl = lastTopUrls[feed.id]
                                            if (currentTopUrl != null && currentTopUrl != previousTopUrl) {
                                                val newItemsCount = if (previousTopUrl == null) {
                                                    results.size
                                                } else {
                                                    val index = results.indexOfFirst { it.url == previousTopUrl }
                                                    if (index == -1) results.size else index
                                                }
                                                
                                                if (newItemsCount > 0) {
                                                    logActivity.await(source.id, ActivityLog.TYPE_FEED_UPDATE, feedId = feed.id, count = newItemsCount.toLong())
                                                }
                                                lastTopUrls[feed.id] = currentTopUrl
                                            }

                                            loadedAnime = animeList
                                        } catch (e: Exception) {
                                            retryCount++
                                            if (retryCount < 3) kotlinx.coroutines.delay(1000L * retryCount)
                                        }
                                    }

                                    if (loadedAnime != null) {
                                        mutableState.update { state ->
                                            val currentCategoryItems = state.items[category.id] ?: initialItems
                                            val updatedItems = currentCategoryItems.map { item ->
                                                if (item.feed.id == feed.id) {
                                                    item.copy(animeList = loadedAnime!!)
                                                } else {
                                                    item
                                                }
                                            }.toImmutableList()
                                            
                                            val newItemsMap = state.items.toMutableMap()
                                            newItemsMap[category.id] = updatedItems
                                            state.copy(items = newItemsMap.toImmutableMap())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Immutable
    data class State(
        val items: ImmutableMap<Long, ImmutableList<FeedItem>> = persistentMapOf(),
        val categories: ImmutableList<FeedSavedSearchCategory> = persistentListOf(),
    ) {
        val isLoading: Boolean
            get() = items.isEmpty() && categories.isNotEmpty() // Simplified loading check
    }

    @Immutable
    data class FeedItem(
        val feed: FeedSavedSearch,
        val source: AnimeCatalogueSource,
        val savedSearch: SavedSearch?,
        val animeList: ImmutableList<Anime>,
    )
}
