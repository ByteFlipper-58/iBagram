package org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.model.HashtagSearchResultModel
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.model.HashtagSearchType
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.repository.HashtagSearchRepository

class ObserveHashtagSearchResultUseCase(
    private val repository: HashtagSearchRepository
) {
    operator fun invoke(searchType: HashtagSearchType): Flow<HashtagSearchResultModel> {
        return repository.observeSearchResult(searchType)
    }
}
