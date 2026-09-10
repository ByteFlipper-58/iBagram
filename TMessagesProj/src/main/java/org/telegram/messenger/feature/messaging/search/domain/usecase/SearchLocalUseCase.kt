package org.telegram.messenger.feature.messaging.search.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.search.domain.model.SearchResultModel
import org.telegram.messenger.feature.messaging.search.domain.repository.SearchRepository

class SearchLocalUseCase(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(query: String): Result<List<SearchResultModel>> {
        return repository.searchLocal(query.trim())
    }
}
