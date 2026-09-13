package org.telegram.messenger.feature.messaging.hashtagsearch

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.hashtagsearch.data.datasource.HashtagSearchLocalDataSource
import org.telegram.messenger.feature.messaging.hashtagsearch.data.datasource.HashtagSearchRemoteDataSource
import org.telegram.messenger.feature.messaging.hashtagsearch.data.repository.HashtagSearchRepositoryImpl
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.model.HashtagSearchResultModel
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.model.HashtagSearchType
import org.telegram.tgnet.TLRPC

class HashtagSearchRepositoryImplTest {

    private lateinit var fakeRemoteDataSource: FakeHashtagSearchRemoteDataSource
    private lateinit var fakeLocalDataSource: FakeHashtagSearchLocalDataSource
    private lateinit var repository: HashtagSearchRepositoryImpl

    private class FakeHashtagSearchRemoteDataSource : HashtagSearchRemoteDataSource(0) {
        var shouldSucceed = true
        var returnedMessages = ArrayList<TLRPC.Message>()

        override suspend fun searchGlobal(
            query: String,
            limit: Int,
            offsetRate: Int,
            offsetPeer: TLRPC.InputPeer?,
            offsetId: Int
        ): Result<TLRPC.messages_Messages> {
            if (!shouldSucceed) {
                return Result.Failure(org.telegram.messenger.core.result.AppError.Network("Search failed"))
            }
            val res = TLRPC.TL_messages_messages().apply {
                this.messages = returnedMessages
                this.count = returnedMessages.size
            }
            return Result.Success(res)
        }

        override suspend fun searchPosts(
            query: String,
            limit: Int,
            offsetRate: Int,
            offsetPeer: TLRPC.InputPeer?,
            offsetId: Int
        ): Result<TLRPC.messages_Messages> {
            if (!shouldSucceed) {
                return Result.Failure(org.telegram.messenger.core.result.AppError.Network("Search posts failed"))
            }
            val res = TLRPC.TL_messages_messages().apply {
                this.messages = returnedMessages
                this.count = returnedMessages.size
            }
            return Result.Success(res)
        }
    }

    private class FakeHashtagSearchLocalDataSource : HashtagSearchLocalDataSource(0)

    @Before
    fun setup() {
        fakeRemoteDataSource = FakeHashtagSearchRemoteDataSource()
        fakeLocalDataSource = FakeHashtagSearchLocalDataSource()
        repository = HashtagSearchRepositoryImpl(
            currentAccount = 0,
            remoteDataSource = fakeRemoteDataSource,
            localDataSource = fakeLocalDataSource,
            mainDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
        )
    }

    @Test
    fun testHistoryCrud() = runBlocking {
        repository.clearHistory()
        assertTrue(repository.getHistory().isEmpty())

        repository.addHashtagToHistory("telegram")
        val history1 = repository.getHistory()
        assertEquals(1, history1.size)
        assertEquals("#telegram", history1[0])

        repository.addHashtagToHistory("#android")
        val history2 = repository.getHistory()
        assertEquals(2, history2.size)
        assertEquals("#android", history2[0])
        assertEquals("#telegram", history2[1])

        repository.removeHashtagFromHistory("#telegram")
        val history3 = repository.getHistory()
        assertEquals(1, history3.size)
        assertEquals("#android", history3[0])

        repository.clearHistory()
        assertTrue(repository.getHistory().isEmpty())
    }

    @Test
    fun testObserveHistoryFlow() = runBlocking {
        repository.clearHistory()
        repository.addHashtagToHistory("crypto")
        val flowValue = repository.observeHistory().first()
        assertEquals(listOf("#crypto"), flowValue)
    }

    @Test
    fun testSearchHashtagSuccess() = runBlocking {
        fakeRemoteDataSource.shouldSucceed = true
        fakeRemoteDataSource.returnedMessages.clear()

        val dummyMsg = TLRPC.TL_message().apply {
            this.id = 101
            this.realId = 101
            this.message = "Hello #crypto"
            this.date = 1700000000
            this.peer_id = TLRPC.TL_peerUser().apply { user_id = 42 }
        }
        fakeRemoteDataSource.returnedMessages.add(dummyMsg)

        val result = repository.searchHashtag("#crypto", HashtagSearchType.MY_MESSAGES)
        assertTrue(result is Result.Success)

        val model = (result as Result.Success).data
        assertEquals("#crypto", model.query)
        assertEquals(1, model.messages.size)
        assertEquals(101, model.messages[0].id)
        assertEquals("Hello #crypto", model.messages[0].text)
        assertFalse(model.isLoading)
        assertTrue(model.endReached)
    }

    @Test
    fun testSearchHashtagFailure() = runBlocking {
        fakeRemoteDataSource.shouldSucceed = false

        val result = repository.searchHashtag("#unknown", HashtagSearchType.MY_MESSAGES)
        assertTrue(result is Result.Failure)

        val current = repository.getSearchResult(HashtagSearchType.MY_MESSAGES)
        assertFalse(current.isLoading)
    }

    @Test
    fun testJumpToMessageAndClear() = runBlocking {
        val dummy1 = TLRPC.TL_message().apply { id = 1; message = "msg 1" }
        val dummy2 = TLRPC.TL_message().apply { id = 2; message = "msg 2" }
        fakeRemoteDataSource.returnedMessages.addAll(listOf(dummy1, dummy2))

        repository.searchHashtag("#test", HashtagSearchType.PUBLIC_POSTS)
        val initial = repository.getSearchResult(HashtagSearchType.PUBLIC_POSTS)
        assertEquals(0, initial.selectedIndex)

        repository.jumpToMessage(guid = 1, index = 1, searchType = HashtagSearchType.PUBLIC_POSTS)
        val updated = repository.getSearchResult(HashtagSearchType.PUBLIC_POSTS)
        assertEquals(1, updated.selectedIndex)

        repository.clearSearchResults(HashtagSearchType.PUBLIC_POSTS)
        val cleared = repository.getSearchResult(HashtagSearchType.PUBLIC_POSTS)
        assertTrue(cleared.messages.isEmpty())
        assertEquals("", cleared.query)
    }
}
