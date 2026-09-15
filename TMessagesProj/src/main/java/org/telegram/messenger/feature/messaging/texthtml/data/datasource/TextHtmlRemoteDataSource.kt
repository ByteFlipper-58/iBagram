package org.telegram.messenger.feature.messaging.texthtml.data.datasource

import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.core.result.Result

/**
 * Удаленный источник данных для валидации форматирования и кастомных эмодзи в HTML.
 */
class TextHtmlRemoteDataSource(
    private val currentAccount: Int = 0
) {

    private val isLegacyAvailable: Boolean
        get() = try {
            ApplicationLoader.applicationContext != null
        } catch (_: Throwable) {
            false
        }

    suspend fun validateCustomEmoji(documentIds: List<Long>): Result<Boolean> {
        if (!isLegacyAvailable) return Result.success(true)
        return try {
            Result.success(documentIds.all { it > 0 })
        } catch (e: Throwable) {
            Result.failure(org.telegram.messenger.core.result.AppError.Generic(e.message ?: "Failed to validate custom emoji", e))
        }
    }
}
