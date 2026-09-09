package org.telegram.messenger.feature.hashtagsearch.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.hashtagsearch.domain.repository.HashtagSearchRepository

class AddHashtagToHistoryUseCase(
    private val repository: HashtagSearchRepository
) {
    suspend operator fun invoke(hashtag: String): Result<Unit> {
        return repository.addHashtagToHistory(hashtag)
    }
}
