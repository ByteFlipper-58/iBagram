package org.telegram.messenger.feature.messaging.chatmeta.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.chatmeta.domain.model.ChatMetadataBatchResult
import org.telegram.messenger.feature.messaging.chatmeta.domain.model.ChatMetadataStatsModel
import org.telegram.messenger.feature.messaging.chatmeta.domain.model.MessageMetadataCheckItem

/**
 * Domain repository contract for checking and loading visible messages metadata.
 */
interface ChatMessagesMetadataRepository {
    fun observeStats(): Flow<ChatMetadataStatsModel>
    fun getStats(): ChatMetadataStatsModel
    fun checkMessages(
        dialogId: Long,
        items: List<MessageMetadataCheckItem>,
        currentTime: Long
    ): Result<ChatMetadataBatchResult>
    fun loadReactions(dialogId: Long, messageIds: List<Int>): Result<Unit>
    fun loadExtendedMedia(dialogId: Long, messageIds: List<Int>): Result<Unit>
    fun cancelPendingRequests(): Result<Unit>
}
