package tachiyomi.domain.history.interactor

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.history.repository.ActivityLogRepository
import java.util.Date

class LogActivity(
    private val repository: ActivityLogRepository,
) {

    suspend fun await(sourceId: Long, eventType: Int, feedId: Long? = null, count: Long? = null, timestamp: Date = Date()) {
        repository.insert(sourceId, feedId, eventType, count, timestamp)
    }

    suspend fun awaitIO(sourceId: Long, eventType: Int, feedId: Long? = null, count: Long? = null, timestamp: Date = Date()) {
        withIOContext {
            await(sourceId, eventType, feedId, count, timestamp)
        }
    }

    suspend fun awaitNonCancellable(sourceId: Long, eventType: Int, feedId: Long? = null, count: Long? = null, timestamp: Date = Date()) {
        withContext(NonCancellable) {
            awaitIO(sourceId, eventType, feedId, count, timestamp)
        }
    }
}
