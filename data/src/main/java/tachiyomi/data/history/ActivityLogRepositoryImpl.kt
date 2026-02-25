package tachiyomi.data.history

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.history.model.ActivityLog
import tachiyomi.domain.history.repository.ActivityLogRepository
import java.util.Date

class ActivityLogRepositoryImpl(
    private val handler: DatabaseHandler,
) : ActivityLogRepository {

    override suspend fun insert(sourceId: Long, type: Int, timestamp: Date) {
        handler.await {
            activity_logQueries.insert(sourceId, type.toLong(), timestamp)
        }
    }

    override suspend fun getActivityByPeriod(after: Date): List<ActivityLog> {
        return handler.awaitList {
            activity_logQueries.getActivityByPeriod(after) { id, sourceId, type, ts ->
                ActivityLog(id, sourceId, type.toInt(), ts)
            }
        }
    }

    override suspend fun getCountsByPeriod(after: Date): Map<Pair<Long, Int>, Long> {
        return handler.awaitList {
            activity_logQueries.getCountsByPeriod(after)
        }.associate { (source_id, type, count) ->
            Pair(source_id, type.toInt()) to count
        }
    }

    override fun subscribeByPeriod(after: Date): Flow<List<ActivityLog>> {
        return handler.subscribeToList {
            activity_logQueries.getActivityByPeriod(after) { id, sourceId, type, ts ->
                ActivityLog(id, sourceId, type.toInt(), ts)
            }
        }
    }

    override suspend fun removeOldActivity(before: Date) {
        handler.await {
            activity_logQueries.removeOldActivity(before)
        }
    }
}
