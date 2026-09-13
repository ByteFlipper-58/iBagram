package org.telegram.messenger.feature.messaging.hashtagsearch.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.hashtagsearch.data.datasource.HashtagSearchLocalDataSource
import org.telegram.messenger.feature.messaging.hashtagsearch.data.datasource.HashtagSearchRemoteDataSource
import org.telegram.messenger.feature.messaging.hashtagsearch.data.mapper.HashtagMapper
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.model.HashtagSearchResultModel
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.model.HashtagSearchType
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.repository.HashtagSearchRepository

open class HashtagSearchRepositoryImpl(
    private val currentAccount: Int,
    private val remoteDataSource: HashtagSearchRemoteDataSource,
    private val localDataSource: HashtagSearchLocalDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : HashtagSearchRepository {

    override fun observeHistory(): Flow<List<String>> = localDataSource.historyFlow

    override fun getHistory(): List<String> = localDataSource.history

    override suspend fun addHashtagToHistory(hashtag: String): Result<Unit> = withContext(mainDispatcher) {
        localDataSource.putToHistory(hashtag)
        Result.Success(Unit)
    }

    override suspend fun removeHashtagFromHistory(hashtag: String): Result<Unit> = withContext(mainDispatcher) {
        localDataSource.removeFromHistory(hashtag)
        Result.Success(Unit)
    }

    override suspend fun clearHistory(): Result<Unit> = withContext(mainDispatcher) {
        localDataSource.clearHistory()
        Result.Success(Unit)
    }

    override fun observeSearchResult(searchType: HashtagSearchType): Flow<HashtagSearchResultModel> {
        return when (searchType) {
            HashtagSearchType.MY_MESSAGES -> localDataSource.myMessagesResult
            HashtagSearchType.PUBLIC_POSTS -> localDataSource.publicPostsResult
            HashtagSearchType.CHANNEL_POSTS -> localDataSource.channelPostsResult
        }
    }

    override fun getSearchResult(searchType: HashtagSearchType): HashtagSearchResultModel {
        return localDataSource.getSearchResult(searchType)
    }

    override suspend fun searchHashtag(
        query: String,
        searchType: HashtagSearchType,
        guid: Int,
        loadIndex: Int
    ): Result<HashtagSearchResultModel> = withContext(mainDispatcher) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.Success(localDataSource.getSearchResult(searchType))
        }

        localDataSource.putToHistory(trimmed)

        val previous = localDataSource.getSearchResult(searchType)
        localDataSource.updateSearchResult(
            searchType,
            previous.copy(
                query = trimmed,
                isLoading = true
            )
        )

        val remoteRes = when (searchType) {
            HashtagSearchType.MY_MESSAGES -> remoteDataSource.searchGlobal(query = trimmed)
            HashtagSearchType.PUBLIC_POSTS -> remoteDataSource.searchPosts(query = trimmed)
            HashtagSearchType.CHANNEL_POSTS -> remoteDataSource.searchPosts(query = trimmed)
        }

        when (remoteRes) {
            is Result.Success -> {
                val messagesRes = remoteRes.data
                val mappedMessages = messagesRes.messages.mapNotNull { msg ->
                    org.telegram.messenger.feature.messaging.hashtagsearch.domain.model.HashtagMessageModel(
                        id = msg.id,
                        realId = msg.realId,
                        dialogId = org.telegram.messenger.DialogObject.getPeerDialogId(msg.peer_id),
                        text = msg.message ?: "",
                        date = msg.date.toLong(),
                        chatTitle = null,
                        isGroupPrimary = false
                    )
                }

                val updatedModel = HashtagSearchResultModel(
                    query = trimmed,
                    searchType = searchType,
                    messages = mappedMessages,
                    totalCount = messagesRes.count,
                    selectedIndex = 0,
                    endReached = messagesRes.messages.isEmpty() || mappedMessages.size >= messagesRes.count,
                    isLoading = false
                )
                localDataSource.updateSearchResult(searchType, updatedModel)
                localDataSource.notifyHashtagSearchUpdated(guid, updatedModel.totalCount, updatedModel.endReached)
                Result.Success(updatedModel)
            }
            is Result.Failure -> {
                val fallbackModel = previous.copy(isLoading = false)
                localDataSource.updateSearchResult(searchType, fallbackModel)
                Result.Failure(remoteRes.error)
            }
        }
    }

    override suspend fun jumpToMessage(
        guid: Int,
        index: Int,
        searchType: HashtagSearchType
    ): Result<Unit> = withContext(mainDispatcher) {
        val current = localDataSource.getSearchResult(searchType)
        if (index in current.messages.indices) {
            val updated = current.copy(selectedIndex = index)
            localDataSource.updateSearchResult(searchType, updated)
            localDataSource.notifyHashtagSearchUpdated(guid, updated.totalCount, updated.endReached)
        }
        Result.Success(Unit)
    }

    override fun clearSearchResults(searchType: HashtagSearchType?): Result<Unit> {
        localDataSource.clearSearchResults(searchType)
        return Result.Success(Unit)
    }
}
