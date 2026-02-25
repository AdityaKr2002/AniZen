package tachiyomi.domain.history.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.history.model.ActivityLog
import tachiyomi.domain.history.repository.ActivityLogRepository
import java.util.Date

class GetActivityLog(
    private val repository: ActivityLogRepository,
) {

    suspend fun awaitByPeriod(after: Date): List<ActivityLog> {
        return repository.getActivityByPeriod(after)
    }

    suspend fun awaitCountsByPeriod(after: Date): Map<Pair<Long, Int>, Long> {
        return repository.getCountsByPeriod(after)
    }

    fun subscribeByPeriod(after: Date): Flow<List<ActivityLog>> {
        return repository.subscribeByPeriod(after)
    }
}
