package org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase

import org.telegram.messenger.feature.messaging.hashtagsearch.domain.repository.HashtagSearchRepository

class GetHashtagHistoryUseCase(
    private val repository: HashtagSearchRepository
) {
    operator fun invoke(): List<String> {
        return repository.getHistory()
    }
}
