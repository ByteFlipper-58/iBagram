package org.telegram.messenger.feature.security.captcha

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
import org.telegram.messenger.feature.security.captcha.data.mapper.CaptchaMapper
import org.telegram.messenger.feature.security.captcha.domain.model.CaptchaAction
import org.telegram.messenger.feature.security.captcha.domain.model.CaptchaRequestModel
import org.telegram.messenger.feature.security.captcha.domain.model.CaptchaResult
import org.telegram.messenger.feature.security.captcha.domain.repository.CaptchaRepository
import org.telegram.messenger.feature.security.captcha.domain.usecase.CancelCaptchaUseCase
import org.telegram.messenger.feature.security.captcha.domain.usecase.GetActiveCaptchaRequestsUseCase
import org.telegram.messenger.feature.security.captcha.domain.usecase.ObserveActiveCaptchaRequestsUseCase
import org.telegram.messenger.feature.security.captcha.domain.usecase.SubmitCaptchaResultUseCase
import org.telegram.messenger.feature.security.captcha.domain.usecase.VerifyCaptchaUseCase
import org.telegram.messenger.feature.security.captcha.presentation.CaptchaEvent
import org.telegram.messenger.feature.security.captcha.presentation.CaptchaUiState
import org.telegram.messenger.feature.security.captcha.presentation.CaptchaViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class CaptchaDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeCaptchaRepository : CaptchaRepository {
        val requests = mutableListOf<CaptchaRequestModel>()
        val submittedResults = mutableListOf<Pair<List<Int>, String>>()
        var shouldFail = false
        var failureErrorCode = "RECAPTCHA_FAILED_TEST_ERROR"
        var returnToken = "valid_test_token_12345"

        private val stateFlow = MutableStateFlow<List<CaptchaRequestModel>>(emptyList())

        fun emit() {
            stateFlow.value = requests.toList()
        }

        override fun observeActiveRequests(): Flow<List<CaptchaRequestModel>> = stateFlow.asStateFlow()

        override fun getActiveRequests(): List<CaptchaRequestModel> = requests.toList()

        override suspend fun verifyCaptcha(
            currentAccount: Int,
            requestToken: Int,
            action: String,
            keyId: String
        ): Result<CaptchaResult> {
            val domainAction = CaptchaMapper.mapAction(action)
            val existing = requests.indexOfFirst {
                it.currentAccount == currentAccount && it.action.rawAction == domainAction.rawAction && it.keyId == keyId
            }
            if (existing >= 0) {
                requests[existing] = requests[existing].withAdditionalToken(requestToken)
            } else {
                requests.add(
                    CaptchaRequestModel(
                        currentAccount = currentAccount,
                        action = domainAction,
                        keyId = keyId,
                        requestTokens = setOf(requestToken)
                    )
                )
            }
            emit()

            return if (shouldFail) {
                Result.Success(CaptchaResult.Failure(failureErrorCode, "Simulated error"))
            } else {
                Result.Success(CaptchaResult.Success(returnToken))
            }
        }

        override suspend fun submitCaptchaResult(
            currentAccount: Int,
            requestTokens: List<Int>,
            token: String
        ): Result<Unit> {
            submittedResults.add(requestTokens to token)
            requests.removeAll { it.currentAccount == currentAccount && it.requestTokens.any { reqToken -> reqToken in requestTokens } }
            emit()
            return Result.Success(Unit)
        }

        override fun cancelCaptcha(
            currentAccount: Int,
            action: String,
            keyId: String
        ): Result<Unit> {
            requests.removeAll {
                it.currentAccount == currentAccount &&
                    it.action.rawAction.equals(action, ignoreCase = true) &&
                    it.keyId == keyId
            }
            emit()
            return Result.Success(Unit)
        }
    }

    @Test
    fun testDomainModelsAndActions() {
        val loginAction = CaptchaAction.fromString("login")
        assertTrue(loginAction is CaptchaAction.Login)
        assertEquals("login", loginAction.rawAction)

        val signupAction = CaptchaAction.fromString("SIGNUP")
        assertTrue(signupAction is CaptchaAction.SignUp)
        assertEquals("signup", signupAction.rawAction)

        val customAction = CaptchaAction.fromString("custom_verification")
        assertTrue(customAction is CaptchaAction.Custom)
        assertEquals("custom_verification", customAction.rawAction)

        val request = CaptchaRequestModel(
            currentAccount = 0,
            action = loginAction,
            keyId = "recaptcha_key_abc",
            requestTokens = setOf(101)
        )
        assertEquals(0, request.currentAccount)
        assertEquals(loginAction, request.action)
        assertEquals("recaptcha_key_abc", request.keyId)
        assertTrue(request.requestTokens.contains(101))

        val updatedRequest = request.withAdditionalToken(102)
        assertEquals(2, updatedRequest.requestTokens.size)
        assertTrue(updatedRequest.requestTokens.contains(102))

        val successResult = CaptchaResult.Success("test_token")
        assertTrue(successResult.isSuccess)
        assertEquals("test_token", successResult.tokenOrNull)

        val failureResult = CaptchaResult.Failure("RECAPTCHA_FAILED_NO_ACTIVITY", "No activity")
        assertFalse(failureResult.isSuccess)
        assertNull(failureResult.tokenOrNull)
        assertEquals("RECAPTCHA_FAILED_NO_ACTIVITY", failureResult.errorCode)
    }

    @Test
    fun testMapperAndFormatting() {
        assertEquals(CaptchaAction.Login, CaptchaMapper.mapAction("login"))
        assertEquals("login", CaptchaMapper.mapActionToString(CaptchaAction.Login))

        val ex = IllegalStateException("client connection refused")
        val formatted = CaptchaMapper.formatException(ex)
        assertEquals("CLIENT_CONNECTION_REFUSED", formatted)
        assertEquals("NULL", CaptchaMapper.formatException(null))

        val mappedNull = CaptchaMapper.mapResult(null)
        assertTrue(mappedNull is CaptchaResult.Failure)
        assertEquals("RECAPTCHA_FAILED_TOKEN_NULL", (mappedNull as CaptchaResult.Failure).errorCode)

        val mappedError = CaptchaMapper.mapResult("RECAPTCHA_FAILED_TIMEOUT")
        assertTrue(mappedError is CaptchaResult.Failure)
        assertEquals("RECAPTCHA_FAILED_TIMEOUT", (mappedError as CaptchaResult.Failure).errorCode)

        val mappedSuccess = CaptchaMapper.mapResult("valid_token_xyz")
        assertTrue(mappedSuccess is CaptchaResult.Success)
        assertEquals("valid_token_xyz", (mappedSuccess as CaptchaResult.Success).token)

        val createdReq = CaptchaMapper.createRequest(1, "signup", "key_999", 55)
        assertEquals(1, createdReq.currentAccount)
        assertEquals(CaptchaAction.SignUp, createdReq.action)
        assertEquals("key_999", createdReq.keyId)
        assertTrue(createdReq.requestTokens.contains(55))
    }

    @Test
    fun testUseCasesVerificationAndSubmission() = runTest {
        val repo = FakeCaptchaRepository()
        val observeUseCase = ObserveActiveCaptchaRequestsUseCase(repo)
        val getActiveUseCase = GetActiveCaptchaRequestsUseCase(repo)
        val verifyUseCase = VerifyCaptchaUseCase(repo)
        val submitUseCase = SubmitCaptchaResultUseCase(repo)
        val cancelUseCase = CancelCaptchaUseCase(repo)

        assertEquals(0, getActiveUseCase().size)

        val verifyResult = verifyUseCase(
            currentAccount = 0,
            requestToken = 1001,
            action = "login",
            keyId = "key_1"
        )
        assertTrue(verifyResult is Result.Success)
        val captchaResult = (verifyResult as Result.Success).data
        assertTrue(captchaResult is CaptchaResult.Success)
        assertEquals("valid_test_token_12345", (captchaResult as CaptchaResult.Success).token)

        val active = getActiveUseCase()
        assertEquals(1, active.size)
        assertEquals(1001, active[0].requestTokens.first())

        // Verify another token deduplication
        verifyUseCase(
            currentAccount = 0,
            requestToken = 1002,
            action = "login",
            keyId = "key_1"
        )
        assertEquals(1, getActiveUseCase().size)
        assertEquals(2, getActiveUseCase()[0].requestTokens.size)

        // Submit result
        val submitResult = submitUseCase(
            currentAccount = 0,
            requestTokens = listOf(1001, 1002),
            token = "valid_test_token_12345"
        )
        assertTrue(submitResult is Result.Success)
        assertEquals(1, repo.submittedResults.size)
        assertEquals(listOf(1001, 1002), repo.submittedResults[0].first)
        assertEquals("valid_test_token_12345", repo.submittedResults[0].second)
        assertEquals(0, getActiveUseCase().size)

        // Test cancellation
        verifyUseCase(
            currentAccount = 0,
            requestToken = 2001,
            action = "signup",
            keyId = "key_2"
        )
        assertEquals(1, getActiveUseCase().size)
        cancelUseCase(0, "signup", "key_2")
        assertEquals(0, getActiveUseCase().size)

        val observed = observeUseCase().first()
        assertEquals(0, observed.size)
    }

    @Test
    fun testViewModelMviLifecycle() = runTest {
        val repo = FakeCaptchaRepository()
        val viewModel = CaptchaViewModel(
            observeActiveCaptchaRequestsUseCase = ObserveActiveCaptchaRequestsUseCase(repo),
            getActiveCaptchaRequestsUseCase = GetActiveCaptchaRequestsUseCase(repo),
            verifyCaptchaUseCase = VerifyCaptchaUseCase(repo),
            submitCaptchaResultUseCase = SubmitCaptchaResultUseCase(repo),
            cancelCaptchaUseCase = CancelCaptchaUseCase(repo)
        )

        assertTrue(viewModel.uiState.value is CaptchaUiState.Idle)

        // Trigger verification
        viewModel.onEvent(
            CaptchaEvent.Verify(
                currentAccount = 0,
                requestToken = 3001,
                action = "login",
                keyId = "test_key"
            )
        )

        advanceUntilIdle()

        val successState = viewModel.uiState.value
        assertTrue(successState is CaptchaUiState.Success)
        assertEquals("valid_test_token_12345", (successState as CaptchaUiState.Success).token)
        assertEquals(1, repo.submittedResults.size)
        assertEquals("valid_test_token_12345", repo.submittedResults[0].second)

        // Test error flow
        repo.shouldFail = true
        repo.failureErrorCode = "RECAPTCHA_FAILED_NETWORK_TIMEOUT"

        viewModel.onEvent(
            CaptchaEvent.Verify(
                currentAccount = 0,
                requestToken = 3002,
                action = "login",
                keyId = "test_key"
            )
        )

        advanceUntilIdle()

        val errorState = viewModel.uiState.value
        assertTrue(errorState is CaptchaUiState.Error)
        assertEquals("RECAPTCHA_FAILED_NETWORK_TIMEOUT", (errorState as CaptchaUiState.Error).errorCode)

        // Test reset
        viewModel.onEvent(CaptchaEvent.Reset)
        assertTrue(viewModel.uiState.value is CaptchaUiState.Idle)

        // Test cancel
        viewModel.onEvent(
            CaptchaEvent.Cancel(
                currentAccount = 0,
                action = "login",
                keyId = "test_key"
            )
        )
        assertTrue(viewModel.uiState.value is CaptchaUiState.Idle)
    }
}
