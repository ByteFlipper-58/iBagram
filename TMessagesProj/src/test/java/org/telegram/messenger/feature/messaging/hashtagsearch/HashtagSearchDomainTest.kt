package org.telegram.messenger.feature.messaging.hashtagsearch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.hashtagsearch.data.mapper.HashtagMapper
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.model.HashtagMessageModel
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.model.HashtagSearchResultModel
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.model.HashtagSearchType
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.repository.HashtagSearchRepository
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.AddHashtagToHistoryUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.ClearHashtagHistoryUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.ClearHashtagSearchResultsUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.GetHashtagHistoryUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.JumpToHashtagMessageUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.ObserveHashtagHistoryUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.ObserveHashtagSearchResultUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.RemoveHashtagFromHistoryUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.SearchHashtagUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.presentation.HashtagSearchEvent
import org.telegram.messenger.feature.messaging.hashtagsearch.presentation.HashtagSearchUiState
import org.telegram.messenger.feature.messaging.hashtagsearch.presentation.HashtagSearchViewModel
import org.telegram.ui.ChatActivity

@OptIn(ExperimentalCoroutinesApi::class)
class HashtagSearchDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeHashtagSearchRepository : HashtagSearchRepository {
        val historyList = mutableListOf<String>()
        var shouldFail = false

        private val historyFlow = MutableStateFlow<List<String>>(emptyList())
        private val myMessagesFlow = MutableStateFlow(HashtagSearchResultModel(query = "", searchType = HashtagSearchType.MY_MESSAGES))
        private val publicPostsFlow = MutableStateFlow(HashtagSearchResultModel(query = "", searchType = HashtagSearchType.PUBLIC_POSTS))
        private val channelPostsFlow = MutableStateFlow(HashtagSearchResultModel(query = "", searchType = HashtagSearchType.CHANNEL_POSTS))

        fun setHistoryList(list: List<String>) {
            historyList.clear()
            historyList.addAll(list)
            historyFlow.value = historyList.toList()
        }

        override fun observeHistory(): Flow<List<String>> = historyFlow.asStateFlow()

        override fun getHistory(): List<String> = historyList.toList()

        override suspend fun addHashtagToHistory(hashtag: String): Result<Unit> {
            val normalized = HashtagMapper.normalizeHashtag(hashtag)
            if (normalized.isNotEmpty()) {
                val index = historyList.indexOf(normalized)
                if (index != -1) {
                    if (index != 0) {
                        historyList.removeAt(index)
                        historyList.add(0, normalized)
                    }
                } else {
                    historyList.add(0, normalized)
                }
                if (historyList.size > 100) {
                    historyList.removeAt(historyList.size - 1)
                }
                historyFlow.value = historyList.toList()
            }
            return Result.Success(Unit)
        }

        override suspend fun removeHashtagFromHistory(hashtag: String): Result<Unit> {
            historyList.remove(hashtag)
            historyFlow.value = historyList.toList()
            return Result.Success(Unit)
        }

        override suspend fun clearHistory(): Result<Unit> {
            historyList.clear()
            historyFlow.value = emptyList()
            return Result.Success(Unit)
        }

        override fun observeSearchResult(searchType: HashtagSearchType): Flow<HashtagSearchResultModel> {
            return when (searchType) {
                HashtagSearchType.MY_MESSAGES -> myMessagesFlow.asStateFlow()
                HashtagSearchType.PUBLIC_POSTS -> publicPostsFlow.asStateFlow()
                HashtagSearchType.CHANNEL_POSTS -> channelPostsFlow.asStateFlow()
            }
        }

        override fun getSearchResult(searchType: HashtagSearchType): HashtagSearchResultModel {
            return when (searchType) {
                HashtagSearchType.MY_MESSAGES -> myMessagesFlow.value
                HashtagSearchType.PUBLIC_POSTS -> publicPostsFlow.value
                HashtagSearchType.CHANNEL_POSTS -> channelPostsFlow.value
            }
        }

        override suspend fun searchHashtag(
            query: String,
            searchType: HashtagSearchType,
            guid: Int,
            loadIndex: Int
        ): Result<HashtagSearchResultModel> {
            if (shouldFail) {
                return Result.Failure(AppError.Network("Network search failed", 500))
            }
            val result = HashtagSearchResultModel(
                query = query,
                searchType = searchType,
                messages = listOf(
                    HashtagMessageModel(
                        id = 101,
                        realId = 101,
                        dialogId = -100123L,
                        text = "First post with $query",
                        date = 1700000000L,
                        chatTitle = "News Channel"
                    ),
                    HashtagMessageModel(
                        id = 102,
                        realId = 102,
                        dialogId = -100124L,
                        text = "Second post with $query",
                        date = 1700000500L,
                        chatTitle = "Tech Channel"
                    )
                ),
                totalCount = 2,
                selectedIndex = 0,
                endReached = true,
                isLoading = false
            )
            when (searchType) {
                HashtagSearchType.MY_MESSAGES -> myMessagesFlow.value = result
                HashtagSearchType.PUBLIC_POSTS -> publicPostsFlow.value = result
                HashtagSearchType.CHANNEL_POSTS -> channelPostsFlow.value = result
            }
            return Result.Success(result)
        }

        override suspend fun jumpToMessage(
            guid: Int,
            index: Int,
            searchType: HashtagSearchType
        ): Result<Unit> {
            val current = getSearchResult(searchType)
            if (index in current.messages.indices) {
                val updated = current.copy(selectedIndex = index)
                when (searchType) {
                    HashtagSearchType.MY_MESSAGES -> myMessagesFlow.value = updated
                    HashtagSearchType.PUBLIC_POSTS -> publicPostsFlow.value = updated
                    HashtagSearchType.CHANNEL_POSTS -> channelPostsFlow.value = updated
                }
            }
            return Result.Success(Unit)
        }

        override fun clearSearchResults(searchType: HashtagSearchType?): Result<Unit> {
            val empty = HashtagSearchResultModel(query = "", searchType = searchType ?: HashtagSearchType.MY_MESSAGES)
            if (searchType != null) {
                when (searchType) {
                    HashtagSearchType.MY_MESSAGES -> myMessagesFlow.value = empty
                    HashtagSearchType.PUBLIC_POSTS -> publicPostsFlow.value = empty
                    HashtagSearchType.CHANNEL_POSTS -> channelPostsFlow.value = empty
                }
            } else {
                myMessagesFlow.value = HashtagSearchResultModel(query = "", searchType = HashtagSearchType.MY_MESSAGES)
                publicPostsFlow.value = HashtagSearchResultModel(query = "", searchType = HashtagSearchType.PUBLIC_POSTS)
                channelPostsFlow.value = HashtagSearchResultModel(query = "", searchType = HashtagSearchType.CHANNEL_POSTS)
            }
            return Result.Success(Unit)
        }
    }

    @Test
    fun testDomainModelsAndSearchTypes() {
        assertEquals(ChatActivity.SEARCH_MY_MESSAGES, HashtagSearchType.MY_MESSAGES.id)
        assertEquals(ChatActivity.SEARCH_PUBLIC_POSTS, HashtagSearchType.PUBLIC_POSTS.id)
        assertEquals(ChatActivity.SEARCH_CHANNEL_POSTS, HashtagSearchType.CHANNEL_POSTS.id)

        assertEquals(HashtagSearchType.MY_MESSAGES, HashtagSearchType.fromId(1))
        assertEquals(HashtagSearchType.PUBLIC_POSTS, HashtagSearchType.fromId(2))
        assertEquals(HashtagSearchType.CHANNEL_POSTS, HashtagSearchType.fromId(3))
        assertEquals(HashtagSearchType.MY_MESSAGES, HashtagSearchType.fromId(999))

        val msg1 = HashtagMessageModel(1, 1, -100L, "Hello #telegram", 1000L, "Channel", false)
        val msg2 = HashtagMessageModel(2, 2, -100L, "World #telegram", 2000L, "Channel", false)

        val result = HashtagSearchResultModel(
            query = "#telegram",
            searchType = HashtagSearchType.PUBLIC_POSTS,
            messages = listOf(msg1, msg2),
            totalCount = 2,
            selectedIndex = 0,
            endReached = true,
            isLoading = false
        )

        assertEquals("#telegram", result.query)
        assertEquals(HashtagSearchType.PUBLIC_POSTS, result.searchType)
        assertEquals(2, result.messages.size)
        assertEquals(0, result.selectedIndex)
        assertEquals(msg1, result.selectedMessage)
        assertTrue(result.canNavigateNext)
        assertFalse(result.canNavigatePrev)

        val navigated = result.copy(selectedIndex = 1)
        assertEquals(msg2, navigated.selectedMessage)
        assertFalse(navigated.canNavigateNext)
        assertTrue(navigated.canNavigatePrev)
    }

    @Test
    fun testMapperOperations() {
        assertEquals("#telegram", HashtagMapper.normalizeHashtag("telegram"))
        assertEquals("#telegram", HashtagMapper.normalizeHashtag("#telegram"))
        assertEquals("\$TON", HashtagMapper.normalizeHashtag("\$TON"))
        assertEquals("", HashtagMapper.normalizeHashtag("   "))

        assertEquals(ChatActivity.SEARCH_MY_MESSAGES, HashtagMapper.mapSearchTypeToInt(HashtagSearchType.MY_MESSAGES))
        assertEquals(ChatActivity.SEARCH_PUBLIC_POSTS, HashtagMapper.mapSearchTypeToInt(HashtagSearchType.PUBLIC_POSTS))
        assertEquals(ChatActivity.SEARCH_CHANNEL_POSTS, HashtagMapper.mapSearchTypeToInt(HashtagSearchType.CHANNEL_POSTS))

        assertEquals(HashtagSearchType.MY_MESSAGES, HashtagMapper.mapIntToSearchType(ChatActivity.SEARCH_MY_MESSAGES))
        assertEquals(HashtagSearchType.PUBLIC_POSTS, HashtagMapper.mapIntToSearchType(ChatActivity.SEARCH_PUBLIC_POSTS))
        assertEquals(HashtagSearchType.CHANNEL_POSTS, HashtagMapper.mapIntToSearchType(ChatActivity.SEARCH_CHANNEL_POSTS))
    }

    @Test
    fun testUseCasesAndHistoryOperations() = runTest {
        val repo = FakeHashtagSearchRepository()
        val observeHistory = ObserveHashtagHistoryUseCase(repo)
        val getHistory = GetHashtagHistoryUseCase(repo)
        val addHistory = AddHashtagToHistoryUseCase(repo)
        val removeHistory = RemoveHashtagFromHistoryUseCase(repo)
        val clearHistory = ClearHashtagHistoryUseCase(repo)
        val searchUseCase = SearchHashtagUseCase(repo)
        val jumpUseCase = JumpToHashtagMessageUseCase(repo)
        val clearResults = ClearHashtagSearchResultsUseCase(repo)

        assertEquals(0, getHistory().size)

        addHistory("android")
        addHistory("kotlin")
        addHistory("#android") // duplicate should move to top

        val history = getHistory()
        assertEquals(2, history.size)
        assertEquals("#android", history[0])
        assertEquals("#kotlin", history[1])

        removeHistory("#kotlin")
        assertEquals(1, getHistory().size)

        val searchResult = searchUseCase("#android", HashtagSearchType.PUBLIC_POSTS)
        assertTrue(searchResult is Result.Success)
        val data = (searchResult as Result.Success).data
        assertEquals(2, data.messages.size)
        assertEquals(0, data.selectedIndex)

        jumpUseCase(0, 1, HashtagSearchType.PUBLIC_POSTS)
        val updatedResult = repo.getSearchResult(HashtagSearchType.PUBLIC_POSTS)
        assertEquals(1, updatedResult.selectedIndex)

        clearResults(HashtagSearchType.PUBLIC_POSTS)
        assertEquals(0, repo.getSearchResult(HashtagSearchType.PUBLIC_POSTS).messages.size)

        clearHistory()
        assertEquals(0, getHistory().size)
    }

    @Test
    fun testViewModelMviLifecycle() = runTest {
        val repo = FakeHashtagSearchRepository()
        repo.setHistoryList(listOf("#initial"))

        val viewModel = HashtagSearchViewModel(
            observeHashtagHistoryUseCase = ObserveHashtagHistoryUseCase(repo),
            getHashtagHistoryUseCase = GetHashtagHistoryUseCase(repo),
            addHashtagToHistoryUseCase = AddHashtagToHistoryUseCase(repo),
            removeHashtagFromHistoryUseCase = RemoveHashtagFromHistoryUseCase(repo),
            clearHashtagHistoryUseCase = ClearHashtagHistoryUseCase(repo),
            observeHashtagSearchResultUseCase = ObserveHashtagSearchResultUseCase(repo),
            searchHashtagUseCase = SearchHashtagUseCase(repo),
            jumpToHashtagMessageUseCase = JumpToHashtagMessageUseCase(repo),
            clearHashtagSearchResultsUseCase = ClearHashtagSearchResultsUseCase(repo)
        )

        advanceUntilIdle()
        assertEquals(listOf("#initial"), viewModel.uiState.value.history)

        viewModel.onEvent(HashtagSearchEvent.Search("#news", HashtagSearchType.PUBLIC_POSTS))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("#news", state.query)
        assertEquals(HashtagSearchType.PUBLIC_POSTS, state.searchType)
        assertTrue(state.hasResults)
        assertEquals(2, state.totalCount)
        assertEquals(0, state.selectedIndex)
        assertTrue(state.canNavigateNext)
        assertFalse(state.canNavigatePrev)
        assertTrue(state.history.contains("#news"))

        viewModel.onEvent(HashtagSearchEvent.JumpToMessage(1))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.selectedIndex)

        viewModel.onEvent(HashtagSearchEvent.ClearResults)
        assertNull(viewModel.uiState.value.searchResult)
        assertFalse(viewModel.uiState.value.hasResults)

        viewModel.onEvent(HashtagSearchEvent.ClearHistory)
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.history.size)
    }
}
