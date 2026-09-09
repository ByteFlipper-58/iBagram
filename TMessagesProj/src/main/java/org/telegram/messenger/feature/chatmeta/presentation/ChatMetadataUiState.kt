package org.telegram.messenger.feature.chatmeta.presentation

import org.telegram.messenger.feature.chatmeta.domain.model.ChatMetadataBatchResult
import org.telegram.messenger.feature.chatmeta.domain.model.ChatMetadataStatsModel

/**
 * UI State for chat messages metadata sync and inspection.
 */
data class ChatMetadataUiState(
    val stats: ChatMetadataStatsModel = ChatMetadataStatsModel(),
    val lastBatchResult: ChatMetadataBatchResult = ChatMetadataBatchResult(),
    val isChecking: Boolean = false,
    val infoMessage: String? = null
) {
    val hasPendingRequests: Boolean get() = stats.hasPendingRequests
    val totalChecked: Long get() = stats.totalCheckedCount
}
