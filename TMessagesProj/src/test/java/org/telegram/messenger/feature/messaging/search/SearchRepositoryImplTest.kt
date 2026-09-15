package org.telegram.messenger.feature.messaging.search

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.search.data.datasource.SearchLocalDataSource
import org.telegram.messenger.feature.messaging.search.data.datasource.SearchRemoteDataSource
import org.telegram.messenger.feature.messaging.search.data.repository.SearchRepositoryImpl
import org.telegram.messenger.feature.messaging.search.domain.model.SearchResultModel
import org.telegram.messenger.feature.messaging.search.domain.model.SearchResultType

class SearchRepositoryImplTest {

    private lateinit var localDataSource: SearchLocalDataSource
    private lateinit var remoteDataSource: SearchRemoteDataSource
    private lateinit var repository: SearchRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = SearchLocalDataSource(0)
        remoteDataSource = SearchRemoteDataSource(0)
        repository = SearchRepositoryImpl(0, localDataSource, remoteDataSource)
    }

    @Test
    fun testEmptyRecentSearchesInitially() = runBlocking {
        val res = repository.getRecentSearches()
        assertTrue(res is Result.Success)
        val data = (res as Result.Success).data
        assertNotNull(data)
        assertTrue(data.isEmpty())
    }

    @Test
    fun testAddAndGetRecentSearches() = runBlocking {
        val item = SearchResultModel(
            id = 100L,
            type = SearchResultType.USER,
            title = "John Doe",
            username = "johndoe"
        )
        localDataSource.addRecentSearch(item)

        val res = repository.getRecentSearches()
        assertTrue(res is Result.Success)
        val list = (res as Result.Success).data
        assertEquals(1, list.size)
        assertEquals(100L, list[0].id)
        assertEquals("John Doe", list[0].title)
    }

    @Test
    fun testRemoveRecentSearch() = runBlocking {
        val item1 = SearchResultModel(id = 1L, type = SearchResultType.USER, title = "Alice")
        val item2 = SearchResultModel(id = 2L, type = SearchResultType.USER, title = "Bob")
        localDataSource.setRecentSearches(listOf(item1, item2))

        val removeRes = repository.removeRecentSearch(1L)
        assertTrue(removeRes is Result.Success)

        val listRes = repository.getRecentSearches()
        val list = (listRes as Result.Success).data
        assertEquals(1, list.size)
        assertEquals(2L, list[0].id)
    }

    @Test
    fun testClearRecentSearches() = runBlocking {
        val item = SearchResultModel(id = 1L, type = SearchResultType.USER, title = "Alice")
        localDataSource.addRecentSearch(item)

        val clearRes = repository.clearRecentSearches()
        assertTrue(clearRes is Result.Success)

        val listRes = repository.getRecentSearches()
        val list = (listRes as Result.Success).data
        assertTrue(list.isEmpty())
    }

    @Test
    fun testRecentHashtagsLifecycle() = runBlocking {
        val putRes = repository.putRecentHashtag("android")
        assertTrue(putRes is Result.Success)

        val getRes = repository.getRecentHashtags()
        assertTrue(getRes is Result.Success)
        val list = (getRes as Result.Success).data
        assertEquals(1, list.size)
        assertEquals("#android", list[0])

        repository.clearRecentHashtags()
        val emptyRes = repository.getRecentHashtags()
        assertTrue((emptyRes as Result.Success).data.isEmpty())
    }

    @Test
    fun testSearchLocalThroughDataSource() = runBlocking {
        val item = SearchResultModel(id = 10L, type = SearchResultType.USER, title = "Durov", username = "durov")
        localDataSource.addRecentSearch(item)

        val res = repository.searchLocal("durov")
        assertTrue(res is Result.Success)
        val results = (res as Result.Success).data
        assertEquals(1, results.size)
        assertEquals("Durov", results[0].title)
    }
}
