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
                        
                        val feedItems = coroutineScope {
                            feedSavedSearches.map { feed ->
                                async {
                                    val source = sourceManager.get(feed.source) as? AnimeCatalogueSource
                                    if (source != null) {
                                        val results = try {
                                            when (FeedSavedSearch.Type.from(feed.type)) {
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
                                        } catch (e: Exception) {
                                            emptyList()
                                        }

                                        val animeList = results.map {
                                            async {
                                                val domainAnime = it.toDomainAnime(source.id)
                                                networkToLocalAnime.await(domainAnime)
                                            }
                                        }.awaitAll()

                                        if (animeList.isNotEmpty()) {
                                            logActivity.await(source.id, ActivityLog.TYPE_FEED_UPDATE, feedId = feed.id, count = animeList.size.toLong())
                                        }

                                        FeedItem(
                                            feed = feed,
                                            source = source,
                                            savedSearch = savedSearches.find { it.id == feed.savedSearch },
                                            animeList = animeList.distinctBy { it.id }.toImmutableList(),
                                        )
                                    } else {
                                        null
                                    }
                                }
                            }.awaitAll().filterNotNull()
                        }

                        mutableState.update { state ->
                            val newItems = state.items.toMutableMap()
                            newItems[category.id] = feedItems.toImmutableList()
                            state.copy(items = newItems.toImmutableMap())
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
