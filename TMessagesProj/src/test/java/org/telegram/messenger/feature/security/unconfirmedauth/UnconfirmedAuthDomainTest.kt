package org.telegram.messenger.feature.security.unconfirmedauth

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
import org.telegram.messenger.feature.security.unconfirmedauth.data.mapper.UnconfirmedAuthMapper
import org.telegram.messenger.feature.security.unconfirmedauth.domain.model.UnconfirmedAuthModel
import org.telegram.messenger.feature.security.unconfirmedauth.domain.model.UnconfirmedAuthStateModel
import org.telegram.messenger.feature.security.unconfirmedauth.domain.repository.UnconfirmedAuthRepository
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.ClearUnconfirmedAuthsUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.ConfirmAllAuthsUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.ConfirmAuthUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.DenyAllAuthsUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.DenyAuthUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.GetUnconfirmedAuthsUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.ObserveUnconfirmedAuthsUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.presentation.UnconfirmedAuthEvent
import org.telegram.messenger.feature.security.unconfirmedauth.presentation.UnconfirmedAuthUiState
import org.telegram.messenger.feature.security.unconfirmedauth.presentation.UnconfirmedAuthViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class UnconfirmedAuthDomainTest {

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
    fun testUnconfirmedAuthModelsAndMappers() {
        val model1 = UnconfirmedAuthModel(
            hash = 1001L,
            date = 1700000000,
            device = "Chrome Windows",
            location = "London, UK",
            isBot = false,
            botId = 0L,
            expiresAfterSeconds = 3600L,
            isExpired = false
        )
        val model2 = UnconfirmedAuthModel(
            hash = 2002L,
            date = 1700000100,
            device = "Telegram Bot",
            location = "Berlin, DE",
            isBot = true,
            botId = 5005L,
            expiresAfterSeconds = 0L,
            isExpired = true
        )

        assertFalse(model1.isExpired)
        assertTrue(model2.isExpired)

        val emptyState = UnconfirmedAuthStateModel(emptyList())
        assertFalse(emptyState.hasPendingAuths)

        val populatedState = UnconfirmedAuthStateModel(listOf(model1, model2))
        assertTrue(populatedState.hasPendingAuths)
        assertEquals(2, populatedState.auths.size)

        // Null checks on mapper
        assertNull(UnconfirmedAuthMapper.mapAuth(null))
        assertEquals(emptyList<UnconfirmedAuthModel>(), UnconfirmedAuthMapper.mapAuthList(null))
    }

    @Test
    fun testUnconfirmedAuthUseCasesWithFakeRepository() = runTest {
        val fakeRepo = FakeUnconfirmedAuthRepository()

        val sample1 = UnconfirmedAuthModel(
            hash = 1L,
            date = 1000,
            device = "iPhone 15",
            location = "Paris"
        )
        val sample2 = UnconfirmedAuthModel(
            hash = 2L,
            date = 2000,
            device = "Pixel 8",
            location = "Tokyo"
        )
        fakeRepo.setAuths(listOf(sample1, sample2))

        val getUseCase = GetUnconfirmedAuthsUseCase(fakeRepo)
        val confirmUseCase = ConfirmAuthUseCase(fakeRepo)
        val denyUseCase = DenyAuthUseCase(fakeRepo)
        val confirmAllUseCase = ConfirmAllAuthsUseCase(fakeRepo)
        val denyAllUseCase = DenyAllAuthsUseCase(fakeRepo)
        val clearUseCase = ClearUnconfirmedAuthsUseCase(fakeRepo)

        // Get
        val current = getUseCase()
        assertEquals(2, current.auths.size)
        assertTrue(current.hasPendingAuths)

        // Confirm single
        val confirmRes = confirmUseCase(1L)
        assertTrue(confirmRes is Result.Success && confirmRes.data)
        assertEquals(1, getUseCase().auths.size)

        // Deny single
        val denyRes = denyUseCase(2L)
        assertTrue(denyRes is Result.Success && denyRes.data)
        assertEquals(0, getUseCase().auths.size)

        // Confirm All
        fakeRepo.setAuths(listOf(sample1, sample2))
        val confirmAllRes = confirmAllUseCase()
        assertTrue(confirmAllRes is Result.Success && confirmAllRes.data == 2)
        assertEquals(0, getUseCase().auths.size)

        // Deny All
        fakeRepo.setAuths(listOf(sample1, sample2))
        val denyAllRes = denyAllUseCase()
        assertTrue(denyAllRes is Result.Success && denyAllRes.data == 2)
        assertEquals(0, getUseCase().auths.size)

        // Clear
        fakeRepo.setAuths(listOf(sample1))
        val clearRes = clearUseCase()
        assertTrue(clearRes is Result.Success)
        assertEquals(0, getUseCase().auths.size)
    }

    @Test
    fun testUnconfirmedAuthViewModelWorkflow() = runTest {
        val fakeRepo = FakeUnconfirmedAuthRepository()
        val sample = UnconfirmedAuthModel(
            hash = 101L,
            date = 5000,
            device = "MacBook Pro",
            location = "San Francisco"
        )
        fakeRepo.setAuths(listOf(sample))

        val viewModel = UnconfirmedAuthViewModel(
            observeUnconfirmedAuthsUseCase = ObserveUnconfirmedAuthsUseCase(fakeRepo),
            getUnconfirmedAuthsUseCase = GetUnconfirmedAuthsUseCase(fakeRepo),
            confirmAuthUseCase = ConfirmAuthUseCase(fakeRepo),
            denyAuthUseCase = DenyAuthUseCase(fakeRepo),
            confirmAllAuthsUseCase = ConfirmAllAuthsUseCase(fakeRepo),
            denyAllAuthsUseCase = DenyAllAuthsUseCase(fakeRepo),
            clearUnconfirmedAuthsUseCase = ClearUnconfirmedAuthsUseCase(fakeRepo)
        )

        advanceUntilIdle()

        // Verify loaded state
        val loadedState = viewModel.uiState.value
        assertTrue(loadedState is UnconfirmedAuthUiState.Success)
        val successState = loadedState as UnconfirmedAuthUiState.Success
        assertEquals(1, successState.auths.size)
        assertEquals(101L, successState.auths[0].hash)
        assertFalse(successState.isProcessing)

        // Confirm auth via event
        viewModel.onEvent(UnconfirmedAuthEvent.ConfirmAuth(101L))
        advanceUntilIdle()

        val afterConfirm = viewModel.uiState.value as UnconfirmedAuthUiState.Success
        assertEquals(0, afterConfirm.auths.size)
        assertNull(afterConfirm.errorMessage)

        // Add 2 auths and Deny All
        fakeRepo.setAuths(listOf(
            sample.copy(hash = 201L),
            sample.copy(hash = 202L)
        ))
        advanceUntilIdle()

        val twoAuthsState = viewModel.uiState.value as UnconfirmedAuthUiState.Success
        assertEquals(2, twoAuthsState.auths.size)

        viewModel.onEvent(UnconfirmedAuthEvent.DenyAll)
        advanceUntilIdle()

        val afterDenyAll = viewModel.uiState.value as UnconfirmedAuthUiState.Success
        assertEquals(0, afterDenyAll.auths.size)

        // Clear error event
        viewModel.onEvent(UnconfirmedAuthEvent.ClearError)
        val finalState = viewModel.uiState.value as UnconfirmedAuthUiState.Success
        assertNull(finalState.errorMessage)
    }

    private class FakeUnconfirmedAuthRepository : UnconfirmedAuthRepository {
        private val list = mutableListOf<UnconfirmedAuthModel>()
        private val stateFlow = MutableStateFlow(UnconfirmedAuthStateModel(emptyList()))

        fun setAuths(auths: List<UnconfirmedAuthModel>) {
            list.clear()
            list.addAll(auths)
            stateFlow.value = UnconfirmedAuthStateModel(list.toList())
        }

        override fun observeUnconfirmedAuths(): Flow<UnconfirmedAuthStateModel> = stateFlow.asStateFlow()

        override suspend fun getUnconfirmedAuths(): UnconfirmedAuthStateModel {
            return UnconfirmedAuthStateModel(list.toList())
        }

        override suspend fun confirmAuth(hash: Long): Result<Boolean> {
            val removed = list.removeAll { it.hash == hash }
            stateFlow.value = UnconfirmedAuthStateModel(list.toList())
            return Result.Success(removed)
        }

        override suspend fun denyAuth(hash: Long): Result<Boolean> {
            val removed = list.removeAll { it.hash == hash }
            stateFlow.value = UnconfirmedAuthStateModel(list.toList())
            return Result.Success(removed)
        }

        override suspend fun confirmAll(): Result<Int> {
            val count = list.size
            list.clear()
            stateFlow.value = UnconfirmedAuthStateModel(list.toList())
            return Result.Success(count)
        }

        override suspend fun denyAll(): Result<Int> {
            val count = list.size
            list.clear()
            stateFlow.value = UnconfirmedAuthStateModel(list.toList())
            return Result.Success(count)
        }

        override suspend fun clear(): Result<Unit> {
            list.clear()
            stateFlow.value = UnconfirmedAuthStateModel(list.toList())
            return Result.Success(Unit)
        }
    }
}
