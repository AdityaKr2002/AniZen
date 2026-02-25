package tachiyomi.domain.history.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.history.model.ActivityLog
import java.util.Date

interface ActivityLogRepository {

    suspend fun insert(sourceId: Long, type: Int, timestamp: Date)

    suspend fun getActivityByPeriod(after: Date): List<ActivityLog>

    suspend fun getCountsByPeriod(after: Date): Map<Pair<Long, Int>, Long>

    fun subscribeByPeriod(after: Date): Flow<List<ActivityLog>>

    suspend fun removeOldActivity(before: Date)
}
