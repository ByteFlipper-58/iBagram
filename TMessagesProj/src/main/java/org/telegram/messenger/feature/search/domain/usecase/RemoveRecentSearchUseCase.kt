package org.telegram.messenger.feature.search.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.search.domain.repository.SearchRepository

class RemoveRecentSearchUseCase(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        return repository.removeRecentSearch(id)
    }
}
