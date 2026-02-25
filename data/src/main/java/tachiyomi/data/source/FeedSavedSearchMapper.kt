package tachiyomi.data.source

import tachiyomi.domain.source.model.FeedSavedSearch

object FeedSavedSearchMapper {
    fun map(
        id: Long,
        source: Long,
        savedSearch: Long?,
        global: Boolean,
        feedOrder: Long,
        searchType: Long,
        category: Long,
        last_top_url: String?,
    ): FeedSavedSearch {
        return FeedSavedSearch(
            id = id,
            source = source,
            savedSearch = savedSearch,
            global = global,
            feedOrder = feedOrder,
            type = searchType.toInt(),
            category = category,
            lastTopUrl = last_top_url,
        )
    }
}
