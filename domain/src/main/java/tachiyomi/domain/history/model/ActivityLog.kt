package tachiyomi.domain.history.model

import java.util.Date

data class ActivityLog(
    val id: Long,
    val sourceId: Long,
    val feedId: Long?,
    val animeId: Long?,
    val eventType: Int,
    val count: Long?,
    val timestamp: Date,
) {
    companion object {
        const val TYPE_FETCH = 1
        const val TYPE_OPEN = 2
        const val TYPE_PLAY = 3
        const val TYPE_COMPLETE = 4
        const val TYPE_FEED_UPDATE = 5
    }
}
