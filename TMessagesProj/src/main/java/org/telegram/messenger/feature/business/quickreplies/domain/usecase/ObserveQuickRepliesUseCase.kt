package org.telegram.messenger.feature.business.quickreplies.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.business.quickreplies.domain.model.QuickReplyModel
import org.telegram.messenger.feature.business.quickreplies.domain.repository.QuickRepliesRepository

class ObserveQuickRepliesUseCase(
    private val repository: QuickRepliesRepository
) {
    operator fun invoke(): Flow<List<QuickReplyModel>> {
        return repository.observeQuickReplies()
    }
}
