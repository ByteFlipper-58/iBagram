package org.telegram.messenger.feature.security.captcha.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.captcha.domain.model.CaptchaRequestModel
import org.telegram.messenger.feature.security.captcha.domain.model.CaptchaResult

interface CaptchaRepository {
    fun observeActiveRequests(): Flow<List<CaptchaRequestModel>>
    fun getActiveRequests(): List<CaptchaRequestModel>
    suspend fun verifyCaptcha(
        currentAccount: Int,
        requestToken: Int,
        action: String,
        keyId: String
    ): Result<CaptchaResult>
    suspend fun submitCaptchaResult(
        currentAccount: Int,
        requestTokens: List<Int>,
        token: String
    ): Result<Unit>
    fun cancelCaptcha(
        currentAccount: Int,
        action: String,
        keyId: String
    ): Result<Unit>
}
