package tachiyomi.domain.history.interactor

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.history.repository.ActivityLogRepository
import java.util.Date

class LogActivity(
    private val repository: ActivityLogRepository,
) {

    suspend fun await(sourceId: Long, eventType: Int, timestamp: Date = Date()) {
        repository.insert(sourceId, eventType, timestamp)
    }

    suspend fun awaitIO(sourceId: Long, eventType: Int, timestamp: Date = Date()) {
        withIOContext {
            await(sourceId, eventType, timestamp)
        }
    }

    suspend fun awaitNonCancellable(sourceId: Long, eventType: Int, timestamp: Date = Date()) {
        withContext(NonCancellable) {
            awaitIO(sourceId, eventType, timestamp)
        }
    }
}
