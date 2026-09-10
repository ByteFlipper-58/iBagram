package org.telegram.messenger.feature.messaging.topics.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.topics.domain.model.ForumUnreadCountModel
import org.telegram.messenger.feature.messaging.topics.domain.repository.TopicsRepository

class GetForumUnreadCountUseCase(
    private val repository: TopicsRepository
) {
    suspend operator fun invoke(chatId: Long): Result<ForumUnreadCountModel> {
        return repository.getForumUnreadCount(chatId)
    }
}
