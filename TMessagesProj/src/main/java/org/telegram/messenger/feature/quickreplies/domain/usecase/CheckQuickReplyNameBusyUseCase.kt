package org.telegram.messenger.feature.quickreplies.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.quickreplies.domain.repository.QuickRepliesRepository

class CheckQuickReplyNameBusyUseCase(
    private val repository: QuickRepliesRepository
) {
    suspend operator fun invoke(name: String, exceptId: Int = -1): Result<Boolean> {
        return repository.isNameBusy(name, exceptId)
    }
}
