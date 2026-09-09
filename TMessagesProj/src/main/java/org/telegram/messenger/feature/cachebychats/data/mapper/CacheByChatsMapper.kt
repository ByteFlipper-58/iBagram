package org.telegram.messenger.feature.cachebychats.data.mapper

import org.telegram.messenger.CacheByChatsController
import org.telegram.messenger.feature.cachebychats.domain.model.CacheChatType
import org.telegram.messenger.feature.cachebychats.domain.model.KeepMediaDuration
import org.telegram.messenger.feature.cachebychats.domain.model.KeepMediaExceptionModel

/**
 * Pure mapping functions translating legacy CacheByChatsController types and exceptions into clean domain models.
 */
object CacheByChatsMapper {

    fun toDomainException(
        exception: CacheByChatsController.KeepMediaException,
        type: CacheChatType
    ): KeepMediaExceptionModel {
        return KeepMediaExceptionModel(
            dialogId = exception.dialogId,
            type = type,
            duration = KeepMediaDuration.fromRaw(exception.keepMedia)
        )
    }

    fun toLegacyException(model: KeepMediaExceptionModel): CacheByChatsController.KeepMediaException {
        return CacheByChatsController.KeepMediaException(
            model.dialogId,
            model.duration.rawValue
        )
    }
}
