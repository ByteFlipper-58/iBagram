package org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.repository.HashtagSearchRepository

class RemoveHashtagFromHistoryUseCase(
    private val repository: HashtagSearchRepository
) {
    suspend operator fun invoke(hashtag: String): Result<Unit> {
        return repository.removeHashtagFromHistory(hashtag)
    }
}
