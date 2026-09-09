package org.telegram.messenger.feature.autodelete

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.autodelete.domain.model.AutoDeleteTtlModel
import org.telegram.messenger.feature.autodelete.domain.model.GlobalAutoDeleteStateModel
import org.telegram.messenger.feature.autodelete.domain.repository.AutoDeleteRepository
import org.telegram.messenger.feature.autodelete.domain.usecase.GetChatAutoDeleteUseCase
import org.telegram.messenger.feature.autodelete.domain.usecase.GetGlobalAutoDeleteUseCase
import org.telegram.messenger.feature.autodelete.domain.usecase.ObserveGlobalAutoDeleteUseCase
import org.telegram.messenger.feature.autodelete.domain.usecase.SetChatAutoDeleteUseCase
import org.telegram.messenger.feature.autodelete.domain.usecase.SetChatsAutoDeleteBatchUseCase
import org.telegram.messenger.feature.autodelete.domain.usecase.SetGlobalAutoDeleteUseCase
import org.telegram.messenger.feature.autodelete.presentation.AutoDeleteEvent
import org.telegram.messenger.feature.autodelete.presentation.AutoDeleteUiState
import org.telegram.messenger.feature.autodelete.presentation.AutoDeleteViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class AutoDeleteDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testAutoDeleteTtlModelCalculations() {
        val off = AutoDeleteTtlModel.OFF
        assertEquals(0, off.periodSeconds)
        assertEquals(0, off.periodMinutes)
        assertEquals(0, off.periodDays)
        assertFalse(off.isEnabled)

        val oneDay = AutoDeleteTtlModel.ONE_DAY
        assertEquals(86400, oneDay.periodSeconds)
        assertEquals(1440, oneDay.periodMinutes)
        assertEquals(1, oneDay.periodDays)
        assertTrue(oneDay.isEnabled)

        val oneWeek = AutoDeleteTtlModel.ONE_WEEK
        assertEquals(604800, oneWeek.periodSeconds)
        assertEquals(10080, oneWeek.periodMinutes)
        assertEquals(7, oneWeek.periodDays)
        assertTrue(oneWeek.isEnabled)

        val customFromMinutes = AutoDeleteTtlModel.fromMinutes(120)
        assertEquals(7200, customFromMinutes.periodSeconds)
        assertEquals(120, customFromMinutes.periodMinutes)

        val customFromDays = AutoDeleteTtlModel.fromDays(3)
        assertEquals(3 * 86400, customFromDays.periodSeconds)
        assertEquals(3, customFromDays.periodDays)
    }

    @Test
    fun testAutoDeleteUseCasesWithFakeRepository() = runTest {
        val fakeRepo = FakeAutoDeleteRepository()

        val observeUseCase = ObserveGlobalAutoDeleteUseCase(fakeRepo)
        val getGlobalUseCase = GetGlobalAutoDeleteUseCase(fakeRepo)
        val setGlobalUseCase = SetGlobalAutoDeleteUseCase(fakeRepo)
        val getChatUseCase = GetChatAutoDeleteUseCase(fakeRepo)
        val setChatUseCase = SetChatAutoDeleteUseCase(fakeRepo)
        val setBatchUseCase = SetChatsAutoDeleteBatchUseCase(fakeRepo)

        // Initial global TTL
        val initialGlobal = getGlobalUseCase()
        assertTrue(initialGlobal is Result.Success)
        assertEquals(AutoDeleteTtlModel.OFF, (initialGlobal as Result.Success).data)

        // Set global TTL
        val setGlobalResult = setGlobalUseCase(AutoDeleteTtlModel.ONE_DAY)
        assertTrue(setGlobalResult is Result.Success)

        val updatedGlobal = getGlobalUseCase()
        assertEquals(AutoDeleteTtlModel.ONE_DAY, (updatedGlobal as Result.Success).data)

        // Chat TTL
        val initialChatTtl = getChatUseCase(12345L)
        assertTrue(initialChatTtl is Result.Success)
        assertEquals(AutoDeleteTtlModel.OFF, (initialChatTtl as Result.Success).data)

        val setChatResult = setChatUseCase(12345L, AutoDeleteTtlModel.ONE_WEEK)
        assertTrue(setChatResult is Result.Success)

        val updatedChatTtl = getChatUseCase(12345L)
        assertEquals(AutoDeleteTtlModel.ONE_WEEK, (updatedChatTtl as Result.Success).data)

        // Batch Chat TTL
        val setBatchResult = setBatchUseCase(listOf(100L, 200L, 300L), AutoDeleteTtlModel.ONE_MONTH)
        assertTrue(setBatchResult is Result.Success)

        assertEquals(AutoDeleteTtlModel.ONE_MONTH, (getChatUseCase(100L) as Result.Success).data)
        assertEquals(AutoDeleteTtlModel.ONE_MONTH, (getChatUseCase(200L) as Result.Success).data)
        assertEquals(AutoDeleteTtlModel.ONE_MONTH, (getChatUseCase(300L) as Result.Success).data)
    }

    @Test
    fun testAutoDeleteViewModelWorkflow() = runTest {
        val fakeRepo = FakeAutoDeleteRepository()

        val viewModel = AutoDeleteViewModel(
            observeGlobalAutoDeleteUseCase = ObserveGlobalAutoDeleteUseCase(fakeRepo),
            getGlobalAutoDeleteUseCase = GetGlobalAutoDeleteUseCase(fakeRepo),
            setGlobalAutoDeleteUseCase = SetGlobalAutoDeleteUseCase(fakeRepo),
            getChatAutoDeleteUseCase = GetChatAutoDeleteUseCase(fakeRepo),
            setChatAutoDeleteUseCase = SetChatAutoDeleteUseCase(fakeRepo),
            setChatsAutoDeleteBatchUseCase = SetChatsAutoDeleteBatchUseCase(fakeRepo)
        )

        advanceUntilIdle()

        val loadedState = viewModel.uiState.value
        assertTrue(loadedState is AutoDeleteUiState.Success)
        val successState = loadedState as AutoDeleteUiState.Success
        assertEquals(AutoDeleteTtlModel.OFF, successState.globalTtl)
        assertFalse(successState.isSaving)
        assertNull(successState.error)

        // Set global TTL via ViewModel
        viewModel.onEvent(AutoDeleteEvent.SetGlobalTtl(AutoDeleteTtlModel.ONE_DAY))
        advanceUntilIdle()

        val afterGlobalSetState = viewModel.uiState.value as AutoDeleteUiState.Success
        assertEquals(AutoDeleteTtlModel.ONE_DAY, afterGlobalSetState.globalTtl)
        assertFalse(afterGlobalSetState.isSaving)

        // Set Chat TTL via ViewModel
        viewModel.onEvent(AutoDeleteEvent.SetChatTtl(123L, AutoDeleteTtlModel.ONE_WEEK))
        advanceUntilIdle()

        val chatTtlResult = fakeRepo.getChatAutoDelete(123L)
        assertEquals(AutoDeleteTtlModel.ONE_WEEK, (chatTtlResult as Result.Success).data)

        // Set Batch Chats TTL via ViewModel
        viewModel.onEvent(AutoDeleteEvent.SetChatsTtlBatch(listOf(1L, 2L), AutoDeleteTtlModel.ONE_MONTH))
        advanceUntilIdle()

        assertEquals(AutoDeleteTtlModel.ONE_MONTH, (fakeRepo.getChatAutoDelete(1L) as Result.Success).data)
        assertEquals(AutoDeleteTtlModel.ONE_MONTH, (fakeRepo.getChatAutoDelete(2L) as Result.Success).data)

        // Clear Error
        viewModel.onEvent(AutoDeleteEvent.ClearError)
        val finalState = viewModel.uiState.value as AutoDeleteUiState.Success
        assertNull(finalState.error)
    }

    private class FakeAutoDeleteRepository : AutoDeleteRepository {
        private var globalTtl = AutoDeleteTtlModel.OFF
        private val chatTtls = mutableMapOf<Long, AutoDeleteTtlModel>()
        private val stateFlow = MutableStateFlow(GlobalAutoDeleteStateModel(ttl = globalTtl))

        override fun observeGlobalAutoDelete(): Flow<GlobalAutoDeleteStateModel> = stateFlow.asStateFlow()

        override suspend fun getGlobalAutoDelete(forceRefresh: Boolean): Result<AutoDeleteTtlModel> {
            return Result.Success(globalTtl)
        }

        override suspend fun setGlobalAutoDelete(ttl: AutoDeleteTtlModel): Result<Unit> {
            globalTtl = ttl
            stateFlow.value = GlobalAutoDeleteStateModel(ttl = globalTtl)
            return Result.Success(Unit)
        }

        override suspend fun getChatAutoDelete(chatId: Long): Result<AutoDeleteTtlModel> {
            return Result.Success(chatTtls[chatId] ?: AutoDeleteTtlModel.OFF)
        }

        override suspend fun setChatAutoDelete(
            chatId: Long,
            ttl: AutoDeleteTtlModel
        ): Result<Unit> {
            chatTtls[chatId] = ttl
            return Result.Success(Unit)
        }

        override suspend fun setChatsAutoDeleteBatch(
            chatIds: List<Long>,
            ttl: AutoDeleteTtlModel
        ): Result<Unit> {
            for (id in chatIds) {
                chatTtls[id] = ttl
            }
            return Result.Success(Unit)
        }
    }
}
