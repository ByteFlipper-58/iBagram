package org.telegram.messenger.feature.messaging.hashtagsearch.domain.model

data class HashtagSearchResultModel(
    val query: String,
    val searchType: HashtagSearchType,
    val messages: List<HashtagMessageModel> = emptyList(),
    val totalCount: Int = 0,
    val selectedIndex: Int = 0,
    val endReached: Boolean = false,
    val isLoading: Boolean = false
) {
    val canNavigateNext: Boolean
        get() = selectedIndex < messages.size - 1

    val canNavigatePrev: Boolean
        get() = selectedIndex > 0

    val selectedMessage: HashtagMessageModel?
        get() = messages.getOrNull(selectedIndex)
}
