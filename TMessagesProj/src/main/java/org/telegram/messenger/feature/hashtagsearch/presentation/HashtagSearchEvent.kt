package org.telegram.messenger.feature.hashtagsearch.presentation

import org.telegram.messenger.feature.hashtagsearch.domain.model.HashtagSearchType

sealed class HashtagSearchEvent {
    data class Search(
        val query: String,
        val searchType: HashtagSearchType,
        val guid: Int = 0
    ) : HashtagSearchEvent()

    data class SetSearchType(
        val searchType: HashtagSearchType
    ) : HashtagSearchEvent()

    data class JumpToMessage(
        val index: Int,
        val guid: Int = 0
    ) : HashtagSearchEvent()

    data class AddToHistory(
        val hashtag: String
    ) : HashtagSearchEvent()

    data class RemoveFromHistory(
        val hashtag: String
    ) : HashtagSearchEvent()

    object ClearHistory : HashtagSearchEvent()
    object ClearResults : HashtagSearchEvent()
    object ClearError : HashtagSearchEvent()
}
