package org.telegram.messenger.feature.search.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.search.domain.model.SearchResultModel
import org.telegram.messenger.feature.search.domain.repository.SearchRepository

class GetRecentSearchesUseCase(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(): Result<List<SearchResultModel>> {
        return repository.getRecentSearches()
    }
}
