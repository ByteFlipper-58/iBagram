package org.telegram.messenger.feature.security.passkeys

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
import org.telegram.messenger.feature.security.passkeys.domain.model.PasskeyModel
import org.telegram.messenger.feature.security.passkeys.domain.model.PasskeysStateModel
import org.telegram.messenger.feature.security.passkeys.domain.repository.PasskeysRepository
import org.telegram.messenger.feature.security.passkeys.domain.usecase.CheckCanAddPasskeyUseCase
import org.telegram.messenger.feature.security.passkeys.domain.usecase.DeletePasskeyUseCase
import org.telegram.messenger.feature.security.passkeys.domain.usecase.GetPasskeysUseCase
import org.telegram.messenger.feature.security.passkeys.domain.usecase.IsPasskeysSupportedUseCase
import org.telegram.messenger.feature.security.passkeys.domain.usecase.ObservePasskeysUseCase
import org.telegram.messenger.feature.security.passkeys.presentation.PasskeysEvent
import org.telegram.messenger.feature.security.passkeys.presentation.PasskeysUiState
import org.telegram.messenger.feature.security.passkeys.presentation.PasskeysViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class PasskeysDomainTest {

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
    fun testPasskeyModelProperties() {
        val passkey = PasskeyModel(
            id = "pk_12345",
            name = "Pixel 9 Pro",
            createdDate = 1700000000L,
            lastUsageDate = 1700050000L,
            softwareEmojiId = 999L
        )

        assertEquals("pk_12345", passkey.id)
        assertEquals("Pixel 9 Pro", passkey.name)
        assertEquals(1700000000L, passkey.createdDate)
        assertEquals(1700050000L, passkey.lastUsageDate)
        assertEquals(999L, passkey.softwareEmojiId)
    }

    @Test
    fun testPasskeysStateModelCanAddLogic() {
        val passkeys = listOf(
            PasskeyModel("1", "Key 1", 1000L),
            PasskeyModel("2", "Key 2", 2000L)
        )

        val stateUnderLimit = PasskeysStateModel(
            passkeys = passkeys,
            maxPasskeys = 5,
            isSupported = true
        )
        assertTrue(stateUnderLimit.canAddPasskey)

        val stateAtLimit = PasskeysStateModel(
            passkeys = passkeys,
            maxPasskeys = 2,
            isSupported = true
        )
        assertFalse(stateAtLimit.canAddPasskey)

        val stateUnsupported = PasskeysStateModel(
            passkeys = passkeys,
            maxPasskeys = 5,
            isSupported = false
        )
        assertFalse(stateUnsupported.canAddPasskey)
    }

    @Test
    fun testPasskeysUseCasesWithFakeRepository() = runTest {
        val fakeRepo = FakePasskeysRepository()

        val observeUseCase = ObservePasskeysUseCase(fakeRepo)
        val getUseCase = GetPasskeysUseCase(fakeRepo)
        val deleteUseCase = DeletePasskeyUseCase(fakeRepo)
        val canAddUseCase = CheckCanAddPasskeyUseCase(fakeRepo)
        val isSupportedUseCase = IsPasskeysSupportedUseCase(fakeRepo)

        assertTrue(isSupportedUseCase())

        val listResult = getUseCase()
        assertTrue(listResult is Result.Success)
        assertEquals(2, (listResult as Result.Success).data.size)
        assertTrue(canAddUseCase())

        // Delete passkey
        val deleteResult = deleteUseCase("pk_1")
        assertTrue(deleteResult is Result.Success)

        val remainingResult = getUseCase()
        assertEquals(1, (remainingResult as Result.Success).data.size)
        assertEquals("pk_2", remainingResult.data[0].id)
    }

    @Test
    fun testPasskeysViewModelWorkflow() = runTest {
        val fakeRepo = FakePasskeysRepository()

        val viewModel = PasskeysViewModel(
            observePasskeysUseCase = ObservePasskeysUseCase(fakeRepo),
            getPasskeysUseCase = GetPasskeysUseCase(fakeRepo),
            deletePasskeyUseCase = DeletePasskeyUseCase(fakeRepo),
            checkCanAddPasskeyUseCase = CheckCanAddPasskeyUseCase(fakeRepo),
            isPasskeysSupportedUseCase = IsPasskeysSupportedUseCase(fakeRepo)
        )

        assertEquals(PasskeysUiState.Initial, viewModel.uiState.value)

        viewModel.onEvent(PasskeysEvent.LoadPasskeys())
        advanceUntilIdle()

        val loadedState = viewModel.uiState.value
        assertTrue(loadedState is PasskeysUiState.Success)
        val successState = loadedState as PasskeysUiState.Success
        assertEquals(2, successState.passkeys.size)
        assertTrue(successState.canAddPasskey)
        assertNull(successState.error)

        // Delete passkey
        viewModel.onEvent(PasskeysEvent.DeletePasskey("pk_1"))
        advanceUntilIdle()

        val afterDeleteState = viewModel.uiState.value as PasskeysUiState.Success
        assertEquals(1, afterDeleteState.passkeys.size)
        assertEquals("pk_2", afterDeleteState.passkeys[0].id)
        assertNull(afterDeleteState.deletingPasskeyId)

        // Clear error
        viewModel.onEvent(PasskeysEvent.ClearError)
        val clearedState = viewModel.uiState.value as PasskeysUiState.Success
        assertNull(clearedState.error)
    }

    private class FakePasskeysRepository : PasskeysRepository {
        private val list = mutableListOf(
            PasskeyModel("pk_1", "Pixel Key", 1000L),
            PasskeyModel("pk_2", "YubiKey", 2000L)
        )

        private val stateFlow = MutableStateFlow(
            PasskeysStateModel(
                passkeys = list.toList(),
                maxPasskeys = 5,
                isSupported = true
            )
        )

        override fun observePasskeys(): Flow<PasskeysStateModel> = stateFlow.asStateFlow()

        override suspend fun getPasskeys(force: Boolean): Result<List<PasskeyModel>> {
            return Result.Success(list.toList())
        }

        override suspend fun deletePasskey(id: String): Result<Unit> {
            list.removeAll { it.id == id }
            stateFlow.value = stateFlow.value.copy(passkeys = list.toList())
            return Result.Success(Unit)
        }

        override suspend fun isSupported(): Boolean = true

        override suspend fun getMaxPasskeys(): Int = 5
    }
}
