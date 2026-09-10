package org.telegram.messenger.feature.messaging.hashtagsearch.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.model.HashtagSearchResultModel
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.model.HashtagSearchType

interface HashtagSearchRepository {
    fun observeHistory(): Flow<List<String>>
    fun getHistory(): List<String>
    suspend fun addHashtagToHistory(hashtag: String): Result<Unit>
    suspend fun removeHashtagFromHistory(hashtag: String): Result<Unit>
    suspend fun clearHistory(): Result<Unit>

    fun observeSearchResult(searchType: HashtagSearchType): Flow<HashtagSearchResultModel>
    fun getSearchResult(searchType: HashtagSearchType): HashtagSearchResultModel
    suspend fun searchHashtag(
        query: String,
        searchType: HashtagSearchType,
        guid: Int = 0,
        loadIndex: Int = 0
    ): Result<HashtagSearchResultModel>

    suspend fun jumpToMessage(
        guid: Int,
        index: Int,
        searchType: HashtagSearchType
    ): Result<Unit>

    fun clearSearchResults(searchType: HashtagSearchType? = null): Result<Unit>
}
