package org.telegram.messenger.core.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result

/**
 * Base class for executing safe local storage operations against MessagesStorage / SQLite
 * and disk cache using an IO dispatcher.
 */
abstract class BaseLocalDataSource(
    protected val currentAccount: Int,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    protected val messagesStorage: MessagesStorage
        get() = MessagesStorage.getInstance(currentAccount)

    /**
     * Executes a database block safely on the IO dispatcher, wrapping any caught exceptions into Result.Failure.
     */
    protected suspend fun <T> runOnDb(block: suspend (storage: MessagesStorage) -> T): Result<T> =
        withContext(ioDispatcher) {
            try {
                Result.success(block(messagesStorage))
            } catch (t: Throwable) {
                Result.failure(AppError.Database(t.message ?: "Database operation failed", t))
            }
        }
}
