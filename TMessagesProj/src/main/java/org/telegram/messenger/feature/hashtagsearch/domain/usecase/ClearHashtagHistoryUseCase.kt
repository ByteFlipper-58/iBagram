package org.telegram.messenger.feature.hashtagsearch.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.hashtagsearch.domain.repository.HashtagSearchRepository

class ClearHashtagHistoryUseCase(
    private val repository: HashtagSearchRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.clearHistory()
    }
}
