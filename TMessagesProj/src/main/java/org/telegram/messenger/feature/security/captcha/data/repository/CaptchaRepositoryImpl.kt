package org.telegram.messenger.feature.security.captcha.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.captcha.data.datasource.CaptchaLocalDataSource
import org.telegram.messenger.feature.security.captcha.data.datasource.CaptchaRemoteDataSource
import org.telegram.messenger.feature.security.captcha.data.mapper.CaptchaMapper
import org.telegram.messenger.feature.security.captcha.domain.model.CaptchaRequestModel
import org.telegram.messenger.feature.security.captcha.domain.model.CaptchaResult
import org.telegram.messenger.feature.security.captcha.domain.repository.CaptchaRepository

/**
 * Modern repository implementation for reCAPTCHA Enterprise verification flows.
 */
class CaptchaRepositoryImpl(
    private val remoteDataSource: CaptchaRemoteDataSource,
    private val localDataSource: CaptchaLocalDataSource,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : CaptchaRepository {

    override fun observeActiveRequests(): Flow<List<CaptchaRequestModel>> =
        localDataSource.observeActiveRequests()

    override fun getActiveRequests(): List<CaptchaRequestModel> =
        localDataSource.getActiveRequests()

    override suspend fun verifyCaptcha(
        currentAccount: Int,
        requestToken: Int,
        action: String,
        keyId: String
    ): Result<CaptchaResult> = withContext(mainDispatcher) {
        val domainAction = CaptchaMapper.mapAction(action)
        localDataSource.addOrUpdateRequest(currentAccount, requestToken, domainAction, keyId)

        val taskResult = remoteDataSource.executeRecaptchaTask(domainAction, keyId)
        val captchaResult = when (taskResult) {
            is Result.Success -> CaptchaMapper.mapResult(taskResult.data)
            is Result.Failure -> {
                val errorMsg = taskResult.error.message ?: "reCAPTCHA task execution failed"
                CaptchaResult.Failure("RECAPTCHA_FAILED", errorMsg)
            }
        }
        Result.Success(captchaResult)
    }

    override suspend fun submitCaptchaResult(
        currentAccount: Int,
        requestTokens: List<Int>,
        token: String
    ): Result<Unit> = withContext(mainDispatcher) {
        val nativeRes = remoteDataSource.submitResultToNative(currentAccount, requestTokens.toIntArray(), token)
        if (nativeRes is Result.Success) {
            localDataSource.removeRequests(currentAccount, requestTokens)
        }
        nativeRes
    }

    override fun cancelCaptcha(
        currentAccount: Int,
        action: String,
        keyId: String
    ): Result<Unit> {
        localDataSource.cancelRequest(currentAccount, action, keyId)
        return Result.Success(Unit)
    }
}
