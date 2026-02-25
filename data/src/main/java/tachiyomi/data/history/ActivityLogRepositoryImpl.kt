package tachiyomi.data.history

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.history.model.ActivityLog
import tachiyomi.domain.history.repository.ActivityLogRepository
import java.util.Date

class ActivityLogRepositoryImpl(
    private val handler: DatabaseHandler,
) : ActivityLogRepository {

    override suspend fun insert(sourceId: Long, eventType: Int, timestamp: Date) {
        handler.await {
            activity_logQueries.insert(
                sourceId = sourceId,
                eventType = eventType.toLong(),
                timestamp = timestamp
            )
        }
    }

    override suspend fun getActivityByPeriod(after: Date): List<ActivityLog> {
        return handler.awaitList {
            activity_logQueries.getActivityByPeriod(after) { id, sourceId, event_type, ts ->
                ActivityLog(id, sourceId, event_type.toInt(), ts)
            }
        }
    }

    override suspend fun getCountsByPeriod(after: Date): Map<Pair<Long, Int>, Long> {
        return handler.awaitList {
            activity_logQueries.getCountsByPeriod(after)
        }.associate { (source_id, event_type, count) ->
            Pair(source_id, event_type.toInt()) to count
        }
    }

    override fun subscribeByPeriod(after: Date): Flow<List<ActivityLog>> {
        return handler.subscribeToList {
            activity_logQueries.getActivityByPeriod(after) { id, sourceId, event_type, ts ->
                ActivityLog(id, sourceId, event_type.toInt(), ts)
            }
        }
    }

    override suspend fun removeOldActivity(before: Date) {
        handler.await {
            activity_logQueries.removeOldActivity(before)
        }
    }
}
