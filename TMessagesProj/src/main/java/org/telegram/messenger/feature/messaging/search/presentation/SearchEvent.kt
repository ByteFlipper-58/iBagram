package org.telegram.messenger.feature.messaging.search.presentation

import org.telegram.messenger.feature.messaging.search.domain.model.SearchFilter
import org.telegram.messenger.feature.messaging.search.domain.model.SearchResultModel

sealed class SearchEvent {
    data class QueryChanged(val query: String) : SearchEvent()
    data class FilterChanged(val filter: SearchFilter) : SearchEvent()
    data class SearchSubmitted(val query: String) : SearchEvent()
    data class RecentSearchClicked(val item: SearchResultModel) : SearchEvent()
    object ClearRecentSearches : SearchEvent()
    data class RemoveRecentSearch(val id: Long) : SearchEvent()
    data class HashtagClicked(val hashtag: String) : SearchEvent()
    object ClearRecentHashtags : SearchEvent()
    object DismissError : SearchEvent()
}
