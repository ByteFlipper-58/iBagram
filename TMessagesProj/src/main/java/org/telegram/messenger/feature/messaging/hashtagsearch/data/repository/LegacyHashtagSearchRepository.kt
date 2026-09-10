package org.telegram.messenger.feature.messaging.hashtagsearch.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.telegram.messenger.HashtagSearchController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.core.events.NotificationCenterFlowBridge
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.hashtagsearch.data.mapper.HashtagMapper
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.model.HashtagSearchResultModel
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.model.HashtagSearchType
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.repository.HashtagSearchRepository

class LegacyHashtagSearchRepository(
    private val account: Int
) : HashtagSearchRepository {

    private val controller: HashtagSearchController
        get() = HashtagSearchController.getInstance(account)

    private val _historyFlow = MutableStateFlow<List<String>>(emptyList())
    private val _myMessagesResult = MutableStateFlow(HashtagSearchResultModel(query = "", searchType = HashtagSearchType.MY_MESSAGES))
    private val _publicPostsResult = MutableStateFlow(HashtagSearchResultModel(query = "", searchType = HashtagSearchType.PUBLIC_POSTS))
    private val _channelPostsResult = MutableStateFlow(HashtagSearchResultModel(query = "", searchType = HashtagSearchType.CHANNEL_POSTS))

    init {
        syncHistoryInternal()
    }

    private fun syncHistoryInternal() {
        try {
            _historyFlow.value = controller.history.toList()
        } catch (_: Throwable) {
            // Ignore during early initialization
        }
    }

    override fun observeHistory(): Flow<List<String>> = _historyFlow.asStateFlow()

    override fun getHistory(): List<String> {
        syncHistoryInternal()
        return _historyFlow.value
    }

    override suspend fun addHashtagToHistory(hashtag: String): Result<Unit> = withContext(Dispatchers.Main) {
        val normalized = HashtagMapper.normalizeHashtag(hashtag)
        if (normalized.isNotEmpty()) {
            controller.putToHistory(normalized)
            syncHistoryInternal()
        }
        Result.Success(Unit)
    }

    override suspend fun removeHashtagFromHistory(hashtag: String): Result<Unit> = withContext(Dispatchers.Main) {
        controller.removeHashtagFromHistory(hashtag)
        syncHistoryInternal()
        Result.Success(Unit)
    }

    override suspend fun clearHistory(): Result<Unit> = withContext(Dispatchers.Main) {
        controller.clearHistory()
        syncHistoryInternal()
        Result.Success(Unit)
    }

    override fun observeSearchResult(searchType: HashtagSearchType): Flow<HashtagSearchResultModel> {
        return when (searchType) {
            HashtagSearchType.MY_MESSAGES -> _myMessagesResult.asStateFlow()
            HashtagSearchType.PUBLIC_POSTS -> _publicPostsResult.asStateFlow()
            HashtagSearchType.CHANNEL_POSTS -> _channelPostsResult.asStateFlow()
        }
    }

    override fun getSearchResult(searchType: HashtagSearchType): HashtagSearchResultModel {
        val typeInt = HashtagMapper.mapSearchTypeToInt(searchType)
        val sr = controller.getSearchResult(typeInt)
        return HashtagSearchResultModel(
            query = sr.lastHashtag ?: "",
            searchType = searchType,
            messages = HashtagMapper.mapMessages(sr.messages),
            totalCount = sr.count,
            selectedIndex = sr.selectedIndex,
            endReached = sr.endReached,
            isLoading = sr.loading
        )
    }

    override suspend fun searchHashtag(
        query: String,
        searchType: HashtagSearchType,
        guid: Int,
        loadIndex: Int
    ): Result<HashtagSearchResultModel> = withContext(Dispatchers.Main) {
        val typeInt = HashtagMapper.mapSearchTypeToInt(searchType)
        controller.searchHashtag(query, guid, typeInt, loadIndex)
        val current = getSearchResult(searchType)
        updateFlow(searchType, current)
        Result.Success(current)
    }

    override suspend fun jumpToMessage(
        guid: Int,
        index: Int,
        searchType: HashtagSearchType
    ): Result<Unit> = withContext(Dispatchers.Main) {
        val typeInt = HashtagMapper.mapSearchTypeToInt(searchType)
        controller.jumpToMessage(guid, index, typeInt)
        val current = getSearchResult(searchType)
        updateFlow(searchType, current)
        Result.Success(Unit)
    }

    override fun clearSearchResults(searchType: HashtagSearchType?): Result<Unit> {
        if (searchType != null) {
            val typeInt = HashtagMapper.mapSearchTypeToInt(searchType)
            controller.clearSearchResults(typeInt)
            updateFlow(searchType, HashtagSearchResultModel(query = "", searchType = searchType))
        } else {
            controller.clearSearchResults()
            updateFlow(HashtagSearchType.MY_MESSAGES, HashtagSearchResultModel(query = "", searchType = HashtagSearchType.MY_MESSAGES))
            updateFlow(HashtagSearchType.PUBLIC_POSTS, HashtagSearchResultModel(query = "", searchType = HashtagSearchType.PUBLIC_POSTS))
            updateFlow(HashtagSearchType.CHANNEL_POSTS, HashtagSearchResultModel(query = "", searchType = HashtagSearchType.CHANNEL_POSTS))
        }
        return Result.Success(Unit)
    }

    private fun updateFlow(searchType: HashtagSearchType, model: HashtagSearchResultModel) {
        when (searchType) {
            HashtagSearchType.MY_MESSAGES -> _myMessagesResult.value = model
            HashtagSearchType.PUBLIC_POSTS -> _publicPostsResult.value = model
            HashtagSearchType.CHANNEL_POSTS -> _channelPostsResult.value = model
        }
    }
}
