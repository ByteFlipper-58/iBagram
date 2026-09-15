package org.telegram.messenger.feature.messaging.search.data.repository

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.search.data.datasource.SearchLocalDataSource
import org.telegram.messenger.feature.messaging.search.data.datasource.SearchRemoteDataSource
import org.telegram.messenger.feature.messaging.search.domain.model.SearchFilter
import org.telegram.messenger.feature.messaging.search.domain.model.SearchResultModel
import org.telegram.messenger.feature.messaging.search.domain.repository.SearchRepository

/**
 * Чистая реализация [SearchRepository], объединяющая локальный кэш и удаленный поиск по Telegram.
 */
class SearchRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: SearchLocalDataSource,
    private val remoteDataSource: SearchRemoteDataSource
) : SearchRepository {

    override suspend fun searchGlobal(query: String, filter: SearchFilter): Result<List<SearchResultModel>> {
        val remote = remoteDataSource.searchGlobal(query, filter)
        if (remote is Result.Success && remote.data.isNotEmpty()) {
            return remote
        }
        return localDataSource.searchLocal(query)
    }

    override suspend fun searchLocal(query: String): Result<List<SearchResultModel>> =
        localDataSource.searchLocal(query)

    override suspend fun getRecentSearches(): Result<List<SearchResultModel>> =
        localDataSource.getRecentSearches()

    override suspend fun clearRecentSearches(): Result<Unit> =
        localDataSource.clearRecentSearches()

    override suspend fun removeRecentSearch(id: Long): Result<Unit> =
        localDataSource.removeRecentSearch(id)

    override suspend fun getRecentHashtags(): Result<List<String>> =
        localDataSource.getRecentHashtags()

    override suspend fun putRecentHashtag(hashtag: String): Result<Unit> =
        localDataSource.putRecentHashtag(hashtag)

    override suspend fun clearRecentHashtags(): Result<Unit> =
        localDataSource.clearRecentHashtags()
}
