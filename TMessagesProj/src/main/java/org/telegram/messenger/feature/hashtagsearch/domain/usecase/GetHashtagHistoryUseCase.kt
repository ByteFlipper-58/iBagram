package org.telegram.messenger.feature.hashtagsearch.domain.usecase

import org.telegram.messenger.feature.hashtagsearch.domain.repository.HashtagSearchRepository

class GetHashtagHistoryUseCase(
    private val repository: HashtagSearchRepository
) {
    operator fun invoke(): List<String> {
        return repository.getHistory()
    }
}
