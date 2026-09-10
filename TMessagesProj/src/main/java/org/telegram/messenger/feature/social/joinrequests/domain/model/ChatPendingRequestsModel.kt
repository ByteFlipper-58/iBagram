package org.telegram.messenger.feature.social.joinrequests.domain.model

data class ChatPendingRequestsModel(
    val chatId: Long,
    val pendingCount: Int,
    val recentRequestersUserIds: List<Long> = emptyList()
)
