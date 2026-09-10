package org.telegram.messenger.feature.messaging.topics.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messaging.topics.domain.model.ForumUnreadCountModel
import org.telegram.messenger.feature.messaging.topics.domain.repository.TopicsRepository

class ObserveForumUnreadCountUseCase(
    private val repository: TopicsRepository
) {
    operator fun invoke(chatId: Long): Flow<ForumUnreadCountModel> {
        return repository.observeForumUnreadCount(chatId)
    }
}
