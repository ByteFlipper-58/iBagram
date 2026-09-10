package org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.model.HashtagSearchType
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.repository.HashtagSearchRepository

class ClearHashtagSearchResultsUseCase(
    private val repository: HashtagSearchRepository
) {
    operator fun invoke(searchType: HashtagSearchType? = null): Result<Unit> {
        return repository.clearSearchResults(searchType)
    }
}
