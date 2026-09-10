package org.telegram.messenger.feature.media.cachebychats.presentation

import org.telegram.messenger.feature.media.cachebychats.domain.model.CacheByChatsConfigModel
import org.telegram.messenger.feature.media.cachebychats.domain.model.CacheChatType
import org.telegram.messenger.feature.media.cachebychats.domain.model.KeepMediaDuration
import org.telegram.messenger.feature.media.cachebychats.domain.model.KeepMediaExceptionModel

/**
 * Immutable UI State for the Keep-Media Cache Retention settings screen.
 */
data class CacheByChatsUiState(
    val isLoading: Boolean = false,
    val config: CacheByChatsConfigModel = CacheByChatsConfigModel(),
    val selectedTab: CacheChatType = CacheChatType.USER,
    val infoMessage: String? = null
) {
    val currentTabDuration: KeepMediaDuration
        get() = config.getDurationForType(selectedTab)

    val currentTabExceptions: List<KeepMediaExceptionModel>
        get() = config.getExceptionsForType(selectedTab)

    val totalExceptionsCount: Int
        get() = config.exceptions.size
}
