package org.telegram.messenger.feature.hashtagsearch.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.hashtagsearch.domain.repository.HashtagSearchRepository

class ObserveHashtagHistoryUseCase(
    private val repository: HashtagSearchRepository
) {
    operator fun invoke(): Flow<List<String>> {
        return repository.observeHistory()
    }
}
