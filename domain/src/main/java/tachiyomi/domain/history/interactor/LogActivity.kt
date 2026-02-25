package tachiyomi.domain.history.interactor

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.history.repository.ActivityLogRepository
import java.util.Date

class LogActivity(
    private val repository: ActivityLogRepository,
) {

    suspend fun await(sourceId: Long, type: Int, timestamp: Date = Date()) {
        repository.insert(sourceId, type, timestamp)
    }

    suspend fun awaitIO(sourceId: Long, type: Int, timestamp: Date = Date()) {
        withIOContext {
            await(sourceId, type, timestamp)
        }
    }

    suspend fun awaitNonCancellable(sourceId: Long, type: Int, timestamp: Date = Date()) {
        withContext(NonCancellable) {
            awaitIO(sourceId, type, timestamp)
        }
    }
}
