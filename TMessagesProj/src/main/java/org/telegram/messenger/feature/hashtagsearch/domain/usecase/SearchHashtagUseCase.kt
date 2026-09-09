package org.telegram.messenger.feature.hashtagsearch.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.hashtagsearch.domain.model.HashtagSearchResultModel
import org.telegram.messenger.feature.hashtagsearch.domain.model.HashtagSearchType
import org.telegram.messenger.feature.hashtagsearch.domain.repository.HashtagSearchRepository

class SearchHashtagUseCase(
    private val repository: HashtagSearchRepository
) {
    suspend operator fun invoke(
        query: String,
        searchType: HashtagSearchType,
        guid: Int = 0,
        loadIndex: Int = 0
    ): Result<HashtagSearchResultModel> {
        return repository.searchHashtag(
            query = query,
            searchType = searchType,
            guid = guid,
            loadIndex = loadIndex
        )
    }
}
