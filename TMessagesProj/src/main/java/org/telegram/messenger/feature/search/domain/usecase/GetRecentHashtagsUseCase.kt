package org.telegram.messenger.feature.search.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.search.domain.repository.SearchRepository

class GetRecentHashtagsUseCase(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(): Result<List<String>> {
        return repository.getRecentHashtags()
    }
}
