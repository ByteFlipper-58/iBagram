package org.telegram.messenger.feature.messaging.chatmeta.domain.model

/**
 * Pure domain model tracking stats and active requests for chat messages metadata.
 */
data class ChatMetadataStatsModel(
    val activeDialogId: Long = 0L,
    val activeReactionsRequestsCount: Int = 0,
    val activeExtendedMediaRequestsCount: Int = 0,
    val totalReactionsCheckedCount: Long = 0L,
    val totalExtendedMediaCheckedCount: Long = 0L,
    val totalStoriesCheckedCount: Long = 0L,
    val isChecking: Boolean = false
) {
    val totalCheckedCount: Long
        get() = totalReactionsCheckedCount + totalExtendedMediaCheckedCount + totalStoriesCheckedCount

    val hasPendingRequests: Boolean
        get() = activeReactionsRequestsCount > 0 || activeExtendedMediaRequestsCount > 0
}
