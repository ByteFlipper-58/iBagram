package org.telegram.messenger.feature.search.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.search.domain.model.SearchFilter
import org.telegram.messenger.feature.search.domain.model.SearchResultModel
import org.telegram.messenger.feature.search.domain.repository.SearchRepository

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
