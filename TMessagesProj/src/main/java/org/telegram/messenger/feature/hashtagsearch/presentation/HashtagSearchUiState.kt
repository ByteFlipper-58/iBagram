package org.telegram.messenger.feature.hashtagsearch.presentation

import org.telegram.messenger.feature.hashtagsearch.domain.model.HashtagSearchResultModel
import org.telegram.messenger.feature.hashtagsearch.domain.model.HashtagSearchType

data class HashtagSearchUiState(
    val history: List<String> = emptyList(),
    val query: String = "",
    val searchType: HashtagSearchType = HashtagSearchType.MY_MESSAGES,
    val searchResult: HashtagSearchResultModel? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val hasResults: Boolean
        get() = (searchResult?.messages?.isNotEmpty() == true)

    val canNavigateNext: Boolean
        get() = searchResult?.canNavigateNext == true

    val canNavigatePrev: Boolean
        get() = searchResult?.canNavigatePrev == true

    val selectedIndex: Int
        get() = searchResult?.selectedIndex ?: 0

    val totalCount: Int
        get() = searchResult?.totalCount ?: 0
}
