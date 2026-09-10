package org.telegram.messenger.feature.messaging.sendmessages.presentation

import org.telegram.messenger.feature.messaging.sendmessages.domain.model.PendingSendModel
import org.telegram.messenger.feature.messaging.sendmessages.domain.model.SendStatus

data class SendMessagesUiState(
    val pendingSends: List<PendingSendModel> = emptyList(),
    val activeUploadsCount: Int = 0,
    val failedCount: Int = 0,
    val isSending: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {
        fun fromList(list: List<PendingSendModel>, errorMessage: String? = null): SendMessagesUiState {
            val active = list.count { it.status == SendStatus.UPLOADING || it.status == SendStatus.PREPARING }
            val failed = list.count { it.status == SendStatus.FAILED }
            val sending = list.any { it.status == SendStatus.SENDING || it.status == SendStatus.UPLOADING }

            return SendMessagesUiState(
                pendingSends = list,
                activeUploadsCount = active,
                failedCount = failed,
                isSending = sending,
                errorMessage = errorMessage
            )
        }
    }
}
