package org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.repository.HashtagSearchRepository

class ObserveHashtagHistoryUseCase(
    private val repository: HashtagSearchRepository
) {
    operator fun invoke(): Flow<List<String>> {
        return repository.observeHistory()
    }
}
