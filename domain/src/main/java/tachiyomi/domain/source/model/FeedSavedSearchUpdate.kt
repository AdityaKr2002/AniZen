package tachiyomi.domain.source.model

data class FeedSavedSearchUpdate(
    // Tag identifier, unique
    val id: Long,

    // Source for the saved search
    val source: Long? = null,

    // If null then get latest/popular, if set get the saved search
    val savedSearch: Long? = null,

    val deleteSavedSearch: Boolean = false,

    // If the feed is a global (FeedScreen) or source specific feed (SourceFeedScreen)
    val global: Boolean? = null,
    val feedOrder: Long? = null,
    val searchType: Long? = null,
    val category: Long? = null,
    val lastTopUrl: String? = null,
)
