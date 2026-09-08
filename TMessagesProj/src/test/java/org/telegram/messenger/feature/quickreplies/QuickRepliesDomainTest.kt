package org.telegram.messenger.feature.quickreplies

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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.quickreplies.data.mapper.QuickReplyMapper
import org.telegram.messenger.feature.quickreplies.domain.model.QuickRepliesLimitModel
import org.telegram.messenger.feature.quickreplies.domain.model.QuickReplyModel
import org.telegram.messenger.feature.quickreplies.domain.repository.QuickRepliesRepository
import org.telegram.messenger.feature.quickreplies.domain.usecase.CanAddNewQuickReplyUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.CheckQuickReplyNameBusyUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.DeleteQuickRepliesUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.FindQuickReplyUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.GetQuickRepliesUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.LoadQuickRepliesUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.ObserveQuickRepliesUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.RenameQuickReplyUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.ReorderQuickRepliesUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.SendQuickReplyUseCase
import org.telegram.messenger.feature.quickreplies.presentation.QuickRepliesEvent
import org.telegram.messenger.feature.quickreplies.presentation.QuickRepliesViewModel
import org.telegram.ui.Business.QuickRepliesController

@OptIn(ExperimentalCoroutinesApi::class)
class QuickRepliesDomainTest {

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
    fun testDomainModels() {
        val model = QuickReplyModel(
            id = 1,
            name = "hello",
            order = 0,
            topMessageId = 42,
            messagesCount = 3,
            isSpecial = true
        )

        assertEquals(1, model.id)
        assertEquals("hello", model.name)
        assertEquals(0, model.order)
        assertEquals(42, model.topMessageId)
        assertEquals(3, model.messagesCount)
        assertTrue(model.isSpecial)

        val customModel = QuickReplyModel(
            id = 2,
            name = "prices",
            order = 1,
            topMessageId = 100,
            messagesCount = 5,
            isSpecial = false
        )
        assertFalse(customModel.isSpecial)

        val limitModel = QuickRepliesLimitModel(
            currentCount = 5,
            maxLimit = 100,
            canAddNew = true
        )
        assertEquals(5, limitModel.currentCount)
        assertEquals(100, limitModel.maxLimit)
        assertTrue(limitModel.canAddNew)
    }

    @Test
    fun testQuickReplyMapper() {
        val controller = QuickRepliesController.getInstance(0)
        val legacy = controller.QuickReply().apply {
            id = 10
            name = "hello"
            order = 2
            topMessageId = 55
            messagesCount = 4
        }

        val mapped = QuickReplyMapper.mapToQuickReply(legacy)
        assertEquals(10, mapped.id)
        assertEquals("hello", mapped.name)
        assertEquals(2, mapped.order)
        assertEquals(55, mapped.topMessageId)
        assertEquals(4, mapped.messagesCount)
        assertTrue(mapped.isSpecial)

        val list = QuickReplyMapper.mapToQuickReplyList(listOf(legacy))
        assertEquals(1, list.size)
        assertEquals(10, list[0].id)
    }

    @Test
    fun testQuickRepliesUseCases() = runTest(testDispatcher) {
        val fakeRepo = FakeQuickRepliesRepository()

        val observeUseCase = ObserveQuickRepliesUseCase(fakeRepo)
        val getUseCase = GetQuickRepliesUseCase(fakeRepo)
        val loadUseCase = LoadQuickRepliesUseCase(fakeRepo)
        val findUseCase = FindQuickReplyUseCase(fakeRepo)
        val checkNameUseCase = CheckQuickReplyNameBusyUseCase(fakeRepo)
        val canAddUseCase = CanAddNewQuickReplyUseCase(fakeRepo)
        val renameUseCase = RenameQuickReplyUseCase(fakeRepo)
        val reorderUseCase = ReorderQuickRepliesUseCase(fakeRepo)
        val deleteUseCase = DeleteQuickRepliesUseCase(fakeRepo)
        val sendUseCase = SendQuickReplyUseCase(fakeRepo)

        val initial = getUseCase()
        assertTrue(initial is Result.Success)
        assertEquals(2, (initial as Result.Success).data.size)

        loadUseCase(true)
        assertTrue(fakeRepo.loadCalled)

        val foundById = findUseCase.byId(1)
        assertTrue(foundById is Result.Success)
        assertEquals("hello", (foundById as Result.Success).data?.name)

        val foundByName = findUseCase.byName("pricing")
        assertTrue(foundByName is Result.Success)
        assertEquals(2, (foundByName as Result.Success).data?.id)

        val isBusy = checkNameUseCase("pricing", 1)
        assertTrue(isBusy is Result.Success)
        assertTrue((isBusy as Result.Success).data)

        val canAdd = canAddUseCase()
        assertTrue(canAdd is Result.Success)
        assertTrue((canAdd as Result.Success).data)

        renameUseCase(1, "greeting")
        assertEquals("greeting", fakeRepo.replies[0].name)

        reorderUseCase(listOf(2, 1))
        assertEquals(0, fakeRepo.replies.find { it.id == 2 }?.order)

        deleteUseCase(listOf(2))
        assertEquals(1, fakeRepo.replies.size)

        val sendResult = sendUseCase(-100L, 1)
        assertTrue(sendResult is Result.Success)
        assertEquals(-100L to 1, fakeRepo.lastSent)
    }

    @Test
    fun testQuickRepliesViewModel() = runTest(testDispatcher) {
        val fakeRepo = FakeQuickRepliesRepository()
        val viewModel = QuickRepliesViewModel(
            observeQuickRepliesUseCase = ObserveQuickRepliesUseCase(fakeRepo),
            loadQuickRepliesUseCase = LoadQuickRepliesUseCase(fakeRepo),
            canAddNewQuickReplyUseCase = CanAddNewQuickReplyUseCase(fakeRepo),
            renameQuickReplyUseCase = RenameQuickReplyUseCase(fakeRepo),
            reorderQuickRepliesUseCase = ReorderQuickRepliesUseCase(fakeRepo),
            deleteQuickRepliesUseCase = DeleteQuickRepliesUseCase(fakeRepo),
            sendQuickReplyUseCase = SendQuickReplyUseCase(fakeRepo)
        )

        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.replies.size)
        assertTrue(viewModel.uiState.value.canAddNew)
        assertFalse(viewModel.uiState.value.isLoading)

        // 1. Load
        viewModel.onEvent(QuickRepliesEvent.Load(true))
        advanceUntilIdle()
        assertTrue(fakeRepo.loadCalled)

        // 2. Rename
        viewModel.onEvent(QuickRepliesEvent.Rename(1, "welcome"))
        advanceUntilIdle()
        assertEquals("Quick reply renamed", viewModel.uiState.value.actionSuccessMessage)

        // 3. Reorder
        viewModel.onEvent(QuickRepliesEvent.Reorder(listOf(2, 1)))
        advanceUntilIdle()

        // 4. Send
        viewModel.onEvent(QuickRepliesEvent.Send(-100L, 1))
        advanceUntilIdle()
        assertEquals("Quick reply sent", viewModel.uiState.value.actionSuccessMessage)

        // 5. Delete
        viewModel.onEvent(QuickRepliesEvent.Delete(listOf(2)))
        advanceUntilIdle()
        assertEquals("Quick replies deleted", viewModel.uiState.value.actionSuccessMessage)

        // 6. ClearMessages
        viewModel.onEvent(QuickRepliesEvent.ClearMessages)
        assertNull(viewModel.uiState.value.actionSuccessMessage)

        // 7. Error handling
        fakeRepo.shouldFail = true
        viewModel.onEvent(QuickRepliesEvent.Rename(1, "error"))
        advanceUntilIdle()
        assertEquals("Repository error", viewModel.uiState.value.errorMessage)
    }

    private class FakeQuickRepliesRepository : QuickRepliesRepository {
        var shouldFail = false
        var loadCalled = false
        var lastSent: Pair<Long, Int>? = null

        val replies = mutableListOf(
            QuickReplyModel(1, "hello", 0, 10, 2, true),
            QuickReplyModel(2, "pricing", 1, 11, 4, false)
        )

        private val flow = MutableStateFlow<List<QuickReplyModel>>(replies)

        override fun observeQuickReplies(): Flow<List<QuickReplyModel>> = flow.asStateFlow()

        override suspend fun getQuickReplies(): Result<List<QuickReplyModel>> {
            if (shouldFail) return Result.Failure(AppError.Generic("Repository error"))
            return Result.Success(replies.toList())
        }

        override suspend fun loadQuickReplies(force: Boolean): Result<Unit> {
            if (shouldFail) return Result.Failure(AppError.Generic("Repository error"))
            loadCalled = true
            return Result.Success(Unit)
        }

        override suspend fun findReplyById(id: Int): Result<QuickReplyModel?> {
            if (shouldFail) return Result.Failure(AppError.Generic("Repository error"))
            return Result.Success(replies.find { it.id == id })
        }

        override suspend fun findReplyByName(name: String): Result<QuickReplyModel?> {
            if (shouldFail) return Result.Failure(AppError.Generic("Repository error"))
            return Result.Success(replies.find { it.name.equals(name, ignoreCase = true) })
        }

        override suspend fun isNameBusy(name: String, exceptId: Int): Result<Boolean> {
            if (shouldFail) return Result.Failure(AppError.Generic("Repository error"))
            val exists = replies.any { it.name.equals(name, ignoreCase = true) && it.id != exceptId }
            return Result.Success(exists)
        }

        override suspend fun canAddNew(): Result<Boolean> {
            if (shouldFail) return Result.Failure(AppError.Generic("Repository error"))
            return Result.Success(true)
        }

        override suspend fun renameReply(id: Int, newName: String): Result<Unit> {
            if (shouldFail) return Result.Failure(AppError.Generic("Repository error"))
            val index = replies.indexOfFirst { it.id == id }
            if (index != -1) {
                val old = replies[index]
                replies[index] = old.copy(name = newName)
                flow.value = replies.toList()
            }
            return Result.Success(Unit)
        }

        override suspend fun reorderReplies(ids: List<Int>): Result<Unit> {
            if (shouldFail) return Result.Failure(AppError.Generic("Repository error"))
            ids.forEachIndexed { newOrder, id ->
                val index = replies.indexOfFirst { it.id == id }
                if (index != -1) {
                    replies[index] = replies[index].copy(order = newOrder)
                }
            }
            flow.value = replies.toList()
            return Result.Success(Unit)
        }

        override suspend fun deleteReplies(ids: List<Int>): Result<Unit> {
            if (shouldFail) return Result.Failure(AppError.Generic("Repository error"))
            replies.removeAll { ids.contains(it.id) }
            flow.value = replies.toList()
            return Result.Success(Unit)
        }

        override suspend fun sendQuickReply(dialogId: Long, shortcutId: Int): Result<Unit> {
            if (shouldFail) return Result.Failure(AppError.Generic("Repository error"))
            lastSent = dialogId to shortcutId
            return Result.Success(Unit)
        }
    }
}
