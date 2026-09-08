package org.telegram.messenger.feature.search.domain.repository

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.search.domain.model.SearchFilter
import org.telegram.messenger.feature.search.domain.model.SearchResultModel

interface SearchRepository {
    suspend fun searchGlobal(query: String, filter: SearchFilter = SearchFilter.ALL): Result<List<SearchResultModel>>
    suspend fun searchLocal(query: String): Result<List<SearchResultModel>>
    suspend fun getRecentSearches(): Result<List<SearchResultModel>>
    suspend fun clearRecentSearches(): Result<Unit>
    suspend fun removeRecentSearch(id: Long): Result<Unit>
    suspend fun getRecentHashtags(): Result<List<String>>
    suspend fun putRecentHashtag(hashtag: String): Result<Unit>
    suspend fun clearRecentHashtags(): Result<Unit>
}
