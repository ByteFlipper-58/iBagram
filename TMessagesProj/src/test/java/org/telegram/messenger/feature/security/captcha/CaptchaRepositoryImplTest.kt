package org.telegram.messenger.feature.security.captcha

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.captcha.data.datasource.CaptchaLocalDataSource
import org.telegram.messenger.feature.security.captcha.data.datasource.CaptchaRemoteDataSource
import org.telegram.messenger.feature.security.captcha.data.repository.CaptchaRepositoryImpl
import org.telegram.messenger.feature.security.captcha.domain.model.CaptchaAction
import org.telegram.messenger.feature.security.captcha.domain.model.CaptchaResult

@OptIn(ExperimentalCoroutinesApi::class)
class CaptchaRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var localDataSource: CaptchaLocalDataSource
    private lateinit var remoteDataSource: FakeCaptchaRemoteDataSource
    private lateinit var repository: CaptchaRepositoryImpl

    private class FakeCaptchaRemoteDataSource : CaptchaRemoteDataSource() {
        var returnedToken: String = "test_captcha_token_123"
        var submitSuccess: Boolean = true

        override suspend fun executeRecaptchaTask(
            action: CaptchaAction,
            keyId: String
        ): Result<String> {
            return Result.Success(returnedToken)
        }

        override fun submitResultToNative(
            currentAccount: Int,
            requestTokens: IntArray,
            token: String
        ): Result<Unit> {
            return if (submitSuccess) Result.Success(Unit) else Result.Failure(org.telegram.messenger.core.result.AppError.Generic("Submit error"))
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        localDataSource = CaptchaLocalDataSource()
        remoteDataSource = FakeCaptchaRemoteDataSource()
        repository = CaptchaRepositoryImpl(
            remoteDataSource = remoteDataSource,
            localDataSource = localDataSource,
            mainDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testVerifyCaptchaSuccess() = runTest {
        val result = repository.verifyCaptcha(
            currentAccount = 0,
            requestToken = 42,
            action = "login",
            keyId = "key_recaptcha_enterprise"
        )
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertTrue(data is CaptchaResult.Success)
        assertEquals("test_captcha_token_123", (data as CaptchaResult.Success).token)

        val active = repository.getActiveRequests()
        assertEquals(1, active.size)
        assertEquals(0, active[0].currentAccount)
        assertEquals("login", active[0].action.rawAction)
        assertTrue(active[0].requestTokens.contains(42))
    }

    @Test
    fun testSubmitCaptchaResultRemovesActiveRequest() = runTest {
        repository.verifyCaptcha(
            currentAccount = 0,
            requestToken = 101,
            action = "signup",
            keyId = "key_recaptcha"
        )
        assertEquals(1, repository.getActiveRequests().size)

        val submitRes = repository.submitCaptchaResult(
            currentAccount = 0,
            requestTokens = listOf(101),
            token = "token_xyz"
        )
        assertTrue(submitRes is Result.Success)
        assertTrue(repository.getActiveRequests().isEmpty())
    }

    @Test
    fun testCancelCaptcha() = runTest {
        repository.verifyCaptcha(
            currentAccount = 0,
            requestToken = 55,
            action = "custom_action",
            keyId = "key_abc"
        )
        assertEquals(1, repository.getActiveRequests().size)

        val cancelRes = repository.cancelCaptcha(
            currentAccount = 0,
            action = "custom_action",
            keyId = "key_abc"
        )
        assertTrue(cancelRes is Result.Success)
        assertTrue(repository.getActiveRequests().isEmpty())
    }
}
