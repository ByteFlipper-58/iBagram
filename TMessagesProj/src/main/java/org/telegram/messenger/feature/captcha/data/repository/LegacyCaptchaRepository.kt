package org.telegram.messenger.feature.captcha.data.repository

import android.app.Activity
import com.google.android.recaptcha.Recaptcha
import com.google.android.recaptcha.RecaptchaAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.FileLog
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.captcha.data.mapper.CaptchaMapper
import org.telegram.messenger.feature.captcha.domain.model.CaptchaAction
import org.telegram.messenger.feature.captcha.domain.model.CaptchaRequestModel
import org.telegram.messenger.feature.captcha.domain.model.CaptchaResult
import org.telegram.messenger.feature.captcha.domain.repository.CaptchaRepository
import org.telegram.tgnet.ConnectionsManager
import kotlin.coroutines.resume

class LegacyCaptchaRepository(
    private val defaultAccount: Int
) : CaptchaRepository {

    private val _activeRequests = MutableStateFlow<List<CaptchaRequestModel>>(emptyList())

    override fun observeActiveRequests(): Flow<List<CaptchaRequestModel>> = _activeRequests.asStateFlow()

    override fun getActiveRequests(): List<CaptchaRequestModel> = _activeRequests.value

    override suspend fun verifyCaptcha(
        currentAccount: Int,
        requestToken: Int,
        action: String,
        keyId: String
    ): Result<CaptchaResult> = withContext(Dispatchers.Main) {
        val domainAction = CaptchaMapper.mapAction(action)
        val existingIndex = _activeRequests.value.indexOfFirst {
            it.currentAccount == currentAccount && it.action.rawAction.equals(action, ignoreCase = true) && it.keyId == keyId
        }

        if (existingIndex >= 0) {
            _activeRequests.update { list ->
                list.mapIndexed { idx, req ->
                    if (idx == existingIndex) req.withAdditionalToken(requestToken) else req
                }
            }
        } else {
            val newRequest = CaptchaRequestModel(
                currentAccount = currentAccount,
                action = domainAction,
                keyId = keyId,
                requestTokens = setOf(requestToken)
            )
            _activeRequests.update { it + newRequest }
        }

        val activity: Activity? = AndroidUtilities.getActivity()
        if (activity == null) {
            FileLog.e("LegacyCaptchaRepository: no activity found")
            val failure = CaptchaResult.Failure("RECAPTCHA_FAILED_NO_ACTIVITY", "No active activity")
            return@withContext Result.Success(failure)
        }

        val resultToken = suspendCancellableCoroutine<String> { continuation ->
            Recaptcha.getTasksClient(activity.application, keyId)
                .addOnSuccessListener { client ->
                    val recaptchaAction = toRecaptchaAction(domainAction)
                    client.executeTask(recaptchaAction)
                        .addOnSuccessListener { token ->
                            FileLog.d("LegacyCaptchaRepository: got token for {action=$action, key_id=$keyId}: $token")
                            if (continuation.isActive) {
                                continuation.resume(token ?: "RECAPTCHA_FAILED_TOKEN_NULL")
                            }
                        }
                        .addOnFailureListener { exception ->
                            FileLog.e("LegacyCaptchaRepository: executeTask failure", exception)
                            if (continuation.isActive) {
                                continuation.resume("RECAPTCHA_FAILED_TASK_EXCEPTION_${CaptchaMapper.formatException(exception)}")
                            }
                        }
                }
                .addOnFailureListener { exception ->
                    FileLog.e("LegacyCaptchaRepository: getTasksClient failure", exception)
                    if (continuation.isActive) {
                        continuation.resume("RECAPTCHA_FAILED_GETCLIENT_EXCEPTION_${CaptchaMapper.formatException(exception)}")
                    }
                }
        }

        val captchaResult = CaptchaMapper.mapResult(resultToken)
        Result.Success(captchaResult)
    }

    override suspend fun submitCaptchaResult(
        currentAccount: Int,
        requestTokens: List<Int>,
        token: String
    ): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val tokensArray = requestTokens.toIntArray()
            ConnectionsManager.native_receivedCaptchaResult(currentAccount, tokensArray, token)

            _activeRequests.update { list ->
                list.filterNot { it.currentAccount == currentAccount && it.requestTokens.any { reqToken -> reqToken in requestTokens } }
            }

            Result.Success(Unit)
        } catch (e: Throwable) {
            FileLog.e("LegacyCaptchaRepository: submitCaptchaResult failed", e)
            Result.Failure(AppError.Generic(e.message ?: "Failed to submit captcha result"))
        }
    }

    override fun cancelCaptcha(
        currentAccount: Int,
        action: String,
        keyId: String
    ): Result<Unit> {
        _activeRequests.update { list ->
            list.filterNot {
                it.currentAccount == currentAccount &&
                    it.action.rawAction.equals(action, ignoreCase = true) &&
                    it.keyId == keyId
            }
        }
        return Result.Success(Unit)
    }

    private fun toRecaptchaAction(action: CaptchaAction): RecaptchaAction {
        return when (action) {
            is CaptchaAction.Login -> RecaptchaAction.LOGIN
            is CaptchaAction.SignUp -> RecaptchaAction.SIGNUP
            is CaptchaAction.Custom -> {
                when (action.actionName.lowercase()) {
                    "login" -> RecaptchaAction.LOGIN
                    "signup" -> RecaptchaAction.SIGNUP
                    else -> RecaptchaAction.custom(action.actionName)
                }
            }
        }
    }
}
