package org.telegram.messenger.feature.search.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.search.domain.repository.SearchRepository

class PutRecentHashtagUseCase(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(hashtag: String): Result<Unit> {
        val cleanTag = hashtag.trim()
        if (cleanTag.isEmpty()) {
            return Result.Success(Unit)
        }
        return repository.putRecentHashtag(cleanTag)
    }
}
