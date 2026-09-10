package org.telegram.messenger.feature.messaging.chatmeta.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.chatmeta.domain.repository.ChatMessagesMetadataRepository

class CancelPendingMetadataRequestsUseCase(
    private val repository: ChatMessagesMetadataRepository
) {
    operator fun invoke(): Result<Unit> {
        return repository.cancelPendingRequests()
    }
}
