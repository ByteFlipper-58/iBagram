package org.telegram.messenger.feature.cachebychats.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.cachebychats.domain.model.CacheByChatsConfigModel
import org.telegram.messenger.feature.cachebychats.domain.model.CacheChatType
import org.telegram.messenger.feature.cachebychats.domain.model.KeepMediaDuration
import org.telegram.messenger.feature.cachebychats.domain.model.KeepMediaExceptionModel

/**
 * Clean domain contract for managing cache media retention periods and exceptions by chat types.
 */
interface CacheByChatsRepository {
    fun observeConfig(): Flow<CacheByChatsConfigModel>
    fun getConfig(): CacheByChatsConfigModel
    fun getDuration(type: CacheChatType): KeepMediaDuration
    fun setDuration(type: CacheChatType, duration: KeepMediaDuration)
    fun getExceptions(type: CacheChatType): List<KeepMediaExceptionModel>
    fun setException(dialogId: Long, type: CacheChatType, duration: KeepMediaDuration)
    fun removeException(dialogId: Long, type: CacheChatType)
    fun clearAllExceptions(type: CacheChatType)
}
