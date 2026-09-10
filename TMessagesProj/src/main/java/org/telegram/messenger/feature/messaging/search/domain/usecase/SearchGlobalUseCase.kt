package org.telegram.messenger.feature.messaging.search.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.search.domain.model.SearchFilter
import org.telegram.messenger.feature.messaging.search.domain.model.SearchResultModel
import org.telegram.messenger.feature.messaging.search.domain.repository.SearchRepository

class SearchGlobalUseCase(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(
        query: String,
        filter: SearchFilter = SearchFilter.ALL
    ): Result<List<SearchResultModel>> {
        return repository.searchGlobal(query.trim(), filter)
    }
}
