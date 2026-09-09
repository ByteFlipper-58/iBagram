package org.telegram.messenger.feature.cachebychats.presentation

import org.telegram.messenger.feature.cachebychats.domain.model.CacheChatType
import org.telegram.messenger.feature.cachebychats.domain.model.KeepMediaDuration

/**
 * MVI intents for Cache by Chats retention configuration.
 */
sealed class CacheByChatsEvent {
    data class SelectTab(val tab: CacheChatType) : CacheByChatsEvent()
    data class SetDuration(val type: CacheChatType, val duration: KeepMediaDuration) : CacheByChatsEvent()
    data class SetException(val dialogId: Long, val type: CacheChatType, val duration: KeepMediaDuration) : CacheByChatsEvent()
    data class RemoveException(val dialogId: Long, val type: CacheChatType) : CacheByChatsEvent()
    data class ClearAllExceptions(val type: CacheChatType) : CacheByChatsEvent()
    object DismissInfo : CacheByChatsEvent()
}
