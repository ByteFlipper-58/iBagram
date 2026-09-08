package org.telegram.messenger.feature.search.presentation

import org.telegram.messenger.feature.search.domain.model.SearchFilter
import org.telegram.messenger.feature.search.domain.model.SearchResultModel

data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.ALL,
    val isLoading: Boolean = false,
    val localResults: List<SearchResultModel> = emptyList(),
    val globalResults: List<SearchResultModel> = emptyList(),
    val recentSearches: List<SearchResultModel> = emptyList(),
    val recentHashtags: List<String> = emptyList(),
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && query.isNotBlank() && localResults.isEmpty() && globalResults.isEmpty()

    val showRecents: Boolean
        get() = query.isBlank() && (recentSearches.isNotEmpty() || recentHashtags.isNotEmpty())
}
