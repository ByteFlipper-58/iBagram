package org.telegram.messenger.feature.messaging.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
import org.telegram.messenger.feature.messaging.search.domain.model.SearchFilter
import org.telegram.messenger.feature.messaging.search.domain.model.SearchResultModel
import org.telegram.messenger.feature.messaging.search.domain.model.SearchResultType
import org.telegram.messenger.feature.messaging.search.domain.repository.SearchRepository
import org.telegram.messenger.feature.messaging.search.domain.usecase.ClearRecentHashtagsUseCase
import org.telegram.messenger.feature.messaging.search.domain.usecase.ClearRecentSearchesUseCase
import org.telegram.messenger.feature.messaging.search.domain.usecase.GetRecentHashtagsUseCase
import org.telegram.messenger.feature.messaging.search.domain.usecase.GetRecentSearchesUseCase
import org.telegram.messenger.feature.messaging.search.domain.usecase.PutRecentHashtagUseCase
import org.telegram.messenger.feature.messaging.search.domain.usecase.RemoveRecentSearchUseCase
import org.telegram.messenger.feature.messaging.search.domain.usecase.SearchGlobalUseCase
import org.telegram.messenger.feature.messaging.search.domain.usecase.SearchLocalUseCase
import org.telegram.messenger.feature.messaging.search.presentation.SearchEvent
import org.telegram.messenger.feature.messaging.search.presentation.SearchViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class SearchDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeSearchRepository : SearchRepository {
        var shouldSucceed = true
        var errorMessage = "Network failure"

        val globalItems = mutableListOf<SearchResultModel>()
        val localItems = mutableListOf<SearchResultModel>()
        val recentSearches = mutableListOf<SearchResultModel>()
        val recentHashtags = mutableListOf<String>()

        override suspend fun searchGlobal(query: String, filter: SearchFilter): Result<List<SearchResultModel>> {
            if (!shouldSucceed) return Result.Failure(AppError.Generic(errorMessage))
            val filtered = globalItems.filter { item ->
                when (filter) {
                    SearchFilter.ALL -> true
                    SearchFilter.USERS -> item.type == SearchResultType.USER
                    SearchFilter.CHANNELS -> item.type == SearchResultType.CHANNEL
                    SearchFilter.GROUPS -> item.type == SearchResultType.GROUP
                    SearchFilter.BOTS -> item.type == SearchResultType.BOT
                    SearchFilter.MESSAGES -> item.type == SearchResultType.MESSAGE
                } && (item.title.contains(query, ignoreCase = true) || (item.username?.contains(query, ignoreCase = true) == true))
            }
            return Result.Success(filtered)
        }

        override suspend fun searchLocal(query: String): Result<List<SearchResultModel>> {
            if (!shouldSucceed) return Result.Failure(AppError.Generic(errorMessage))
            if (query.isBlank()) return Result.Success(emptyList())
            val filtered = localItems.filter { item ->
                item.title.contains(query, ignoreCase = true) || (item.username?.contains(query, ignoreCase = true) == true)
            }
            return Result.Success(filtered)
        }

        override suspend fun getRecentSearches(): Result<List<SearchResultModel>> {
            if (!shouldSucceed) return Result.Failure(AppError.Generic(errorMessage))
            return Result.Success(recentSearches.toList())
        }

        override suspend fun clearRecentSearches(): Result<Unit> {
            if (!shouldSucceed) return Result.Failure(AppError.Generic(errorMessage))
            recentSearches.clear()
            return Result.Success(Unit)
        }

        override suspend fun removeRecentSearch(id: Long): Result<Unit> {
            if (!shouldSucceed) return Result.Failure(AppError.Generic(errorMessage))
            recentSearches.removeAll { it.id == id }
            return Result.Success(Unit)
        }

        override suspend fun getRecentHashtags(): Result<List<String>> {
            if (!shouldSucceed) return Result.Failure(AppError.Generic(errorMessage))
            return Result.Success(recentHashtags.toList())
        }

        override suspend fun putRecentHashtag(hashtag: String): Result<Unit> {
            if (!shouldSucceed) return Result.Failure(AppError.Generic(errorMessage))
            recentHashtags.remove(hashtag)
            recentHashtags.add(0, hashtag)
            return Result.Success(Unit)
        }

        override suspend fun clearRecentHashtags(): Result<Unit> {
            if (!shouldSucceed) return Result.Failure(AppError.Generic(errorMessage))
            recentHashtags.clear()
            return Result.Success(Unit)
        }
    }

    private fun createRepository(): FakeSearchRepository {
        return FakeSearchRepository().apply {
            globalItems.addAll(
                listOf(
                    SearchResultModel(id = 1L, title = "Alice Developer", username = "alice_dev", type = SearchResultType.USER),
                    SearchResultModel(id = 2L, title = "BotFather", username = "botfather", type = SearchResultType.BOT),
                    SearchResultModel(id = 3L, title = "Telegram News", username = "tgnews", type = SearchResultType.CHANNEL),
                    SearchResultModel(id = 4L, title = "Kotlin Developers", username = "kotlindevs", type = SearchResultType.GROUP)
                )
            )
            localItems.addAll(
                listOf(
                    SearchResultModel(id = 10L, title = "Bob Friend", username = "bob_friend", type = SearchResultType.USER),
                    SearchResultModel(id = 11L, title = "Family Group", type = SearchResultType.GROUP)
                )
            )
            recentSearches.addAll(
                listOf(
                    SearchResultModel(id = 100L, title = "Saved Search 1", type = SearchResultType.USER),
                    SearchResultModel(id = 101L, title = "Saved Search 2", type = SearchResultType.CHANNEL)
                )
            )
            recentHashtags.addAll(listOf("#kotlin", "#android", "#telegram"))
        }
    }

    @Test
    fun testSearchGlobalAllFilterReturnsMixedResults() = runTest(testDispatcher) {
        val repo = createRepository()
        val useCase = SearchGlobalUseCase(repo)

        val result = useCase("dev", SearchFilter.ALL)
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(2, data.size) // Alice Developer and Kotlin Developers
        assertTrue(data.any { it.type == SearchResultType.USER })
        assertTrue(data.any { it.type == SearchResultType.GROUP })
    }

    @Test
    fun testSearchGlobalUsersFilterReturnsOnlyUsers() = runTest(testDispatcher) {
        val repo = createRepository()
        val useCase = SearchGlobalUseCase(repo)

        val result = useCase("dev", SearchFilter.USERS)
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(1, data.size)
        assertEquals(SearchResultType.USER, data[0].type)
        assertEquals("Alice Developer", data[0].title)
    }

    @Test
    fun testSearchGlobalChannelsFilterReturnsOnlyChannels() = runTest(testDispatcher) {
        val repo = createRepository()
        val useCase = SearchGlobalUseCase(repo)

        val result = useCase("news", SearchFilter.CHANNELS)
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(1, data.size)
        assertEquals(SearchResultType.CHANNEL, data[0].type)
        assertEquals("Telegram News", data[0].title)
    }

    @Test
    fun testSearchGlobalBotsFilterReturnsOnlyBots() = runTest(testDispatcher) {
        val repo = createRepository()
        val useCase = SearchGlobalUseCase(repo)

        val result = useCase("bot", SearchFilter.BOTS)
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(1, data.size)
        assertEquals(SearchResultType.BOT, data[0].type)
        assertEquals("BotFather", data[0].title)
    }

    @Test
    fun testSearchLocalReturnsMatchingItems() = runTest(testDispatcher) {
        val repo = createRepository()
        val useCase = SearchLocalUseCase(repo)

        val result = useCase("friend")
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(1, data.size)
        assertEquals("Bob Friend", data[0].title)
    }

    @Test
    fun testSearchLocalEmptyQueryReturnsEmptyList() = runTest(testDispatcher) {
        val repo = createRepository()
        val useCase = SearchLocalUseCase(repo)

        val result = useCase("   ")
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertTrue(data.isEmpty())
    }

    @Test
    fun testRecentSearchesLoadRemoveAndClear() = runTest(testDispatcher) {
        val repo = createRepository()
        val getRecentUseCase = GetRecentSearchesUseCase(repo)
        val removeRecentUseCase = RemoveRecentSearchUseCase(repo)
        val clearRecentUseCase = ClearRecentSearchesUseCase(repo)

        // 1. Initial recent searches
        val initial = getRecentUseCase()
        assertTrue(initial is Result.Success)
        assertEquals(2, (initial as Result.Success).data.size)

        // 2. Remove single item
        val removeResult = removeRecentUseCase(100L)
        assertTrue(removeResult is Result.Success)
        val afterRemove = getRecentUseCase()
        assertEquals(1, (afterRemove as Result.Success).data.size)
        assertEquals(101L, afterRemove.data[0].id)

        // 3. Clear all
        val clearResult = clearRecentUseCase()
        assertTrue(clearResult is Result.Success)
        val afterClear = getRecentUseCase()
        assertTrue((afterClear as Result.Success).data.isEmpty())
    }

    @Test
    fun testRecentHashtagsLoadPutAndClear() = runTest(testDispatcher) {
        val repo = createRepository()
        val getHashtagsUseCase = GetRecentHashtagsUseCase(repo)
        val putHashtagUseCase = PutRecentHashtagUseCase(repo)
        val clearHashtagsUseCase = ClearRecentHashtagsUseCase(repo)

        // 1. Initial hashtags
        val initial = getHashtagsUseCase()
        assertTrue(initial is Result.Success)
        assertEquals(3, (initial as Result.Success).data.size)

        // 2. Put new hashtag
        val putResult = putHashtagUseCase("#jetpack")
        assertTrue(putResult is Result.Success)
        val afterPut = getHashtagsUseCase()
        assertEquals(4, (afterPut as Result.Success).data.size)
        assertEquals("#jetpack", afterPut.data[0])

        // 3. Clear hashtags
        val clearResult = clearHashtagsUseCase()
        assertTrue(clearResult is Result.Success)
        val afterClear = getHashtagsUseCase()
        assertTrue((afterClear as Result.Success).data.isEmpty())
    }

    @Test
    fun testSearchViewModelQueryDebounceAndExecution() = runTest(testDispatcher) {
        val repo = createRepository()
        val viewModel = SearchViewModel(
            searchGlobalUseCase = SearchGlobalUseCase(repo),
            searchLocalUseCase = SearchLocalUseCase(repo),
            getRecentSearchesUseCase = GetRecentSearchesUseCase(repo),
            clearRecentSearchesUseCase = ClearRecentSearchesUseCase(repo),
            removeRecentSearchUseCase = RemoveRecentSearchUseCase(repo),
            getRecentHashtagsUseCase = GetRecentHashtagsUseCase(repo),
            putRecentHashtagUseCase = PutRecentHashtagUseCase(repo),
            clearRecentHashtagsUseCase = ClearRecentHashtagsUseCase(repo)
        )

        advanceUntilIdle()

        // Verify initial state has loaded recents
        assertTrue(viewModel.uiState.value.showRecents)
        assertEquals(2, viewModel.uiState.value.recentSearches.size)
        assertEquals(3, viewModel.uiState.value.recentHashtags.size)

        // Trigger query change
        viewModel.onEvent(SearchEvent.QueryChanged("dev"))

        // Local search runs immediately
        advanceUntilIdle()
        // Wait, before debounce 300ms, global search has not completed yet
        advanceTimeBy(350)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("dev", state.query)
        assertFalse(state.isLoading)
        assertEquals(2, state.globalResults.size)
        assertFalse(state.showRecents)
    }

    @Test
    fun testSearchViewModelFilterChanged() = runTest(testDispatcher) {
        val repo = createRepository()
        val viewModel = SearchViewModel(
            searchGlobalUseCase = SearchGlobalUseCase(repo),
            searchLocalUseCase = SearchLocalUseCase(repo),
            getRecentSearchesUseCase = GetRecentSearchesUseCase(repo),
            clearRecentSearchesUseCase = ClearRecentSearchesUseCase(repo),
            removeRecentSearchUseCase = RemoveRecentSearchUseCase(repo),
            getRecentHashtagsUseCase = GetRecentHashtagsUseCase(repo),
            putRecentHashtagUseCase = PutRecentHashtagUseCase(repo),
            clearRecentHashtagsUseCase = ClearRecentHashtagsUseCase(repo)
        )

        advanceUntilIdle()

        // Submit query directly
        viewModel.onEvent(SearchEvent.SearchSubmitted("dev"))
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.globalResults.size)

        // Change filter to USERS
        viewModel.onEvent(SearchEvent.FilterChanged(SearchFilter.USERS))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SearchFilter.USERS, state.filter)
        assertEquals(1, state.globalResults.size)
        assertEquals("Alice Developer", state.globalResults[0].title)
    }

    @Test
    fun testSearchViewModelClearRecentsAndHashtags() = runTest(testDispatcher) {
        val repo = createRepository()
        val viewModel = SearchViewModel(
            searchGlobalUseCase = SearchGlobalUseCase(repo),
            searchLocalUseCase = SearchLocalUseCase(repo),
            getRecentSearchesUseCase = GetRecentSearchesUseCase(repo),
            clearRecentSearchesUseCase = ClearRecentSearchesUseCase(repo),
            removeRecentSearchUseCase = RemoveRecentSearchUseCase(repo),
            getRecentHashtagsUseCase = GetRecentHashtagsUseCase(repo),
            putRecentHashtagUseCase = PutRecentHashtagUseCase(repo),
            clearRecentHashtagsUseCase = ClearRecentHashtagsUseCase(repo)
        )

        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.recentSearches.size)

        viewModel.onEvent(SearchEvent.ClearRecentSearches)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.recentSearches.isEmpty())

        viewModel.onEvent(SearchEvent.ClearRecentHashtags)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.recentHashtags.isEmpty())
    }

    @Test
    fun testSearchViewModelErrorHandling() = runTest(testDispatcher) {
        val repo = createRepository().apply {
            shouldSucceed = false
            errorMessage = "Server unreachable"
        }
        val viewModel = SearchViewModel(
            searchGlobalUseCase = SearchGlobalUseCase(repo),
            searchLocalUseCase = SearchLocalUseCase(repo),
            getRecentSearchesUseCase = GetRecentSearchesUseCase(repo),
            clearRecentSearchesUseCase = ClearRecentSearchesUseCase(repo),
            removeRecentSearchUseCase = RemoveRecentSearchUseCase(repo),
            getRecentHashtagsUseCase = GetRecentHashtagsUseCase(repo),
            putRecentHashtagUseCase = PutRecentHashtagUseCase(repo),
            clearRecentHashtagsUseCase = ClearRecentHashtagsUseCase(repo)
        )

        advanceUntilIdle()

        viewModel.onEvent(SearchEvent.SearchSubmitted("test"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Server unreachable", state.errorMessage)

        viewModel.onEvent(SearchEvent.DismissError)
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
