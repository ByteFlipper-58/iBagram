package org.telegram.messenger.feature.authtokens

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.telegram.messenger.feature.authtokens.data.mapper.AuthTokensMapper
import org.telegram.messenger.feature.authtokens.data.repository.LegacyAuthTokensRepository
import org.telegram.messenger.feature.authtokens.domain.model.AuthTokenUserInfoModel
import org.telegram.messenger.feature.authtokens.domain.model.SavedLoginTokenModel
import org.telegram.messenger.feature.authtokens.domain.model.SavedLogoutTokenModel
import org.telegram.messenger.feature.authtokens.domain.usecase.AddLogoutTokenUseCase
import org.telegram.messenger.feature.authtokens.domain.usecase.ClearAllTokensUseCase
import org.telegram.messenger.feature.authtokens.domain.usecase.GetSavedLoginTokensUseCase
import org.telegram.messenger.feature.authtokens.domain.usecase.GetSavedLogoutTokensUseCase
import org.telegram.messenger.feature.authtokens.domain.usecase.ObserveAuthTokensStateUseCase
import org.telegram.messenger.feature.authtokens.domain.usecase.PruneTokensListUseCase
import org.telegram.messenger.feature.authtokens.domain.usecase.RefreshAuthTokensUseCase
import org.telegram.messenger.feature.authtokens.domain.usecase.RemoveTokenUseCase
import org.telegram.messenger.feature.authtokens.domain.usecase.SaveLoginTokenUseCase
import org.telegram.messenger.feature.authtokens.domain.usecase.SaveLogoutTokensUseCase
import org.telegram.messenger.feature.authtokens.domain.usecase.ValidateAuthTokenFormatUseCase
import org.telegram.messenger.feature.authtokens.presentation.AuthTokensEvent
import org.telegram.messenger.feature.authtokens.presentation.AuthTokensViewModel
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class AuthTokensDomainTest {

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
    fun testPruneTokensListUseCase() {
        val pruner = PruneTokensListUseCase()

        val empty = emptyList<String>()
        assertEquals(emptyList<String>(), pruner(empty))

        val items = (1..30).map { "token_$it" }
        val pruned = pruner(items, maxCount = 20)

        assertEquals(20, pruned.size)
        assertEquals("token_1", pruned.first())
        assertEquals("token_20", pruned.last())
    }

    @Test
    fun testValidateAuthTokenFormatUseCase() {
        val validator = ValidateAuthTokenFormatUseCase()

        // Null / blank
        assertFalse(validator(null))
        assertFalse(validator(""))
        assertFalse(validator("   "))

        // Odd length
        assertFalse(validator("123"))
        assertFalse(validator("abcde"))

        // Invalid hex chars
        assertFalse(validator("12zz"))
        assertFalse(validator("ghij"))

        // Valid hex
        assertTrue(validator("0123456789abcdef"))
        assertTrue(validator("AABBCCDDEEFF0011"))
        assertTrue(validator("deadbeef"))
    }

    @Test
    fun testMapperLogic() {
        val auth = TLRPC.TL_auth_authorization().apply {
            otherwise_relogin_days = 14
            setup_password_required = true
            user = TLRPC.TL_user().apply {
                id = 12345678L
                first_name = "Pavel"
                last_name = "Durov"
                username = "durov"
                phone = "+1234567890"
            }
        }

        val loginModel = AuthTokensMapper.toSavedLoginToken(auth, hex = "deadbeef")
        assertEquals("deadbeef", loginModel.hexToken)
        assertEquals(12345678L, loginModel.userId)
        assertEquals(14, loginModel.otherwiseReloginDays)
        assertTrue(loginModel.isPasswordSetupRequired)
        assertNotNull(loginModel.userInfo)
        assertEquals("Pavel", loginModel.userInfo?.firstName)
        assertEquals("Durov", loginModel.userInfo?.lastName)
        assertEquals("durov", loginModel.userInfo?.username)

        val logout = TLRPC.TL_auth_loggedOut().apply {
            flags = 3
        }

        val logoutModel = AuthTokensMapper.toSavedLogoutToken(logout, hex = "cafebabe")
        assertEquals("cafebabe", logoutModel.hexToken)
        assertEquals(3, logoutModel.flags)
    }

    @Test
    fun testRepositoryOperations() {
        val repository = LegacyAuthTokensRepository(currentAccount = 0, ioDispatcher = testDispatcher)

        repository.clearAllTokens()
        assertEquals(0, repository.getState().loginTokens.size)
        assertEquals(0, repository.getState().logoutTokens.size)

        val token1 = SavedLoginTokenModel(
            hexToken = "aabbccdd",
            userId = 1001L,
            userInfo = AuthTokenUserInfoModel(userId = 1001L, firstName = "User1")
        )
        val token2 = SavedLoginTokenModel(
            hexToken = "eeff0011",
            userId = 1002L,
            userInfo = AuthTokenUserInfoModel(userId = 1002L, firstName = "User2")
        )

        repository.saveLoginToken(token1)
        repository.saveLoginToken(token2)

        val loginTokens = repository.getSavedLoginTokens()
        assertEquals(2, loginTokens.size)
        assertEquals("eeff0011", loginTokens[0].hexToken) // Newest first

        val logoutToken = SavedLogoutTokenModel(hexToken = "deadbeef", flags = 1)
        repository.addLogoutToken(logoutToken)

        val logoutTokens = repository.getSavedLogoutTokens()
        assertEquals(1, logoutTokens.size)
        assertEquals("deadbeef", logoutTokens[0].hexToken)

        // Remove token
        repository.removeLoginToken("aabbccdd")
        assertEquals(1, repository.getSavedLoginTokens().size)
        assertEquals("eeff0011", repository.getSavedLoginTokens()[0].hexToken)

        // Clear all
        repository.clearAllTokens()
        assertEquals(0, repository.getSavedLoginTokens().size)
        assertEquals(0, repository.getSavedLogoutTokens().size)
    }

    @Test
    fun testAuthTokensViewModelMviFlow() = runTest(testDispatcher) {
        val repository = LegacyAuthTokensRepository(currentAccount = 0, ioDispatcher = testDispatcher)
        repository.clearAllTokens()

        val observeState = ObserveAuthTokensStateUseCase(repository)
        val saveLogin = SaveLoginTokenUseCase(repository)
        val addLogout = AddLogoutTokenUseCase(repository)
        val removeToken = RemoveTokenUseCase(repository)
        val clearAll = ClearAllTokensUseCase(repository)
        val refresh = RefreshAuthTokensUseCase(repository)

        val viewModel = AuthTokensViewModel(
            observeAuthTokensState = observeState,
            saveLoginToken = saveLogin,
            addLogoutToken = addLogout,
            removeToken = removeToken,
            clearAllTokens = clearAll,
            refreshAuthTokens = refresh,
            repository = repository
        )

        advanceUntilIdle()

        val initialState = viewModel.uiState.value
        assertEquals(0, initialState.state.loginTokens.size)
        assertEquals(0, initialState.state.logoutTokens.size)

        // Event: SaveLoginToken with invalid hex
        val invalidToken = SavedLoginTokenModel(hexToken = "invalid_hex", userId = 1L)
        viewModel.onEvent(AuthTokensEvent.SaveLoginToken(invalidToken))
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals(0, viewModel.uiState.value.state.loginTokens.size)

        // Event: SaveLoginToken with valid hex
        val validToken = SavedLoginTokenModel(hexToken = "aabbccddeeff", userId = 2L)
        viewModel.onEvent(AuthTokensEvent.SaveLoginToken(validToken))
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(1, viewModel.uiState.value.state.loginTokens.size)
        assertEquals("aabbccddeeff", viewModel.uiState.value.state.loginTokens[0].hexToken)

        // Event: AddLogoutToken
        val validLogout = SavedLogoutTokenModel(hexToken = "112233445566", flags = 0)
        viewModel.onEvent(AuthTokensEvent.AddLogoutToken(validLogout))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.state.logoutTokens.size)

        // Event: SelectToken
        viewModel.onEvent(AuthTokensEvent.SelectToken(validToken))
        assertEquals(validToken, viewModel.uiState.value.selectedToken)

        // Event: RemoveToken
        viewModel.onEvent(AuthTokensEvent.RemoveToken("aabbccddeeff"))
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.state.loginTokens.size)
        assertNull(viewModel.uiState.value.selectedToken)

        // Event: ClearAllTokens
        viewModel.onEvent(AuthTokensEvent.ClearAllTokens)
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.state.logoutTokens.size)
        assertEquals("All tokens cleared", viewModel.uiState.value.statusMessage)
    }
}
