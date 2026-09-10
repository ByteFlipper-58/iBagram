package org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.model.HashtagSearchType
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.repository.HashtagSearchRepository

class JumpToHashtagMessageUseCase(
    private val repository: HashtagSearchRepository
) {
    suspend operator fun invoke(
        guid: Int,
        index: Int,
        searchType: HashtagSearchType
    ): Result<Unit> {
        return repository.jumpToMessage(
            guid = guid,
            index = index,
            searchType = searchType
        )
    }
}
