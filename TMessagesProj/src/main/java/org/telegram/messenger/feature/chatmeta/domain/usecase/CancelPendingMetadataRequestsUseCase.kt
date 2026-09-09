package org.telegram.messenger.feature.chatmeta.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.chatmeta.domain.repository.ChatMessagesMetadataRepository

class CancelPendingMetadataRequestsUseCase(
    private val repository: ChatMessagesMetadataRepository
) {
    operator fun invoke(): Result<Unit> {
        return repository.cancelPendingRequests()
    }
}
