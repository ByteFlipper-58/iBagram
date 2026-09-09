package org.telegram.messenger.feature.hashtagsearch.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.hashtagsearch.domain.model.HashtagSearchType
import org.telegram.messenger.feature.hashtagsearch.domain.repository.HashtagSearchRepository

class ClearHashtagSearchResultsUseCase(
    private val repository: HashtagSearchRepository
) {
    operator fun invoke(searchType: HashtagSearchType? = null): Result<Unit> {
        return repository.clearSearchResults(searchType)
    }
}
