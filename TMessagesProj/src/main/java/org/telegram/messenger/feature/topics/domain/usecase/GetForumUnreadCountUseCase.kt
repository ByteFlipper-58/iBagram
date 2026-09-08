package org.telegram.messenger.feature.topics.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.topics.domain.model.ForumUnreadCountModel
import org.telegram.messenger.feature.topics.domain.repository.TopicsRepository

class GetForumUnreadCountUseCase(
    private val repository: TopicsRepository
) {
    suspend operator fun invoke(chatId: Long): Result<ForumUnreadCountModel> {
        return repository.getForumUnreadCount(chatId)
    }
}
