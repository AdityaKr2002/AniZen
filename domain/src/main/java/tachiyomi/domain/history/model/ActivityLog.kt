package tachiyomi.domain.history.model

import java.util.Date

data class ActivityLog(
    val id: Long,
    val sourceId: Long,
    val eventType: Int,
    val timestamp: Date,
) {
    companion object {
        const val TYPE_FETCH = 1
        const val TYPE_OPEN = 2
        const val TYPE_PLAY = 3
        const val TYPE_COMPLETE = 4
    }
}
