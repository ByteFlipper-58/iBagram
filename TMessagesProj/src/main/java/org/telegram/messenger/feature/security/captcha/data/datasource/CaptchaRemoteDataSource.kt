package org.telegram.messenger.feature.security.captcha.data.datasource

import android.app.Activity
import com.google.android.recaptcha.Recaptcha
import com.google.android.recaptcha.RecaptchaAction
import kotlinx.coroutines.suspendCancellableCoroutine
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.FileLog
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.captcha.data.mapper.CaptchaMapper
import org.telegram.messenger.feature.security.captcha.domain.model.CaptchaAction
import org.telegram.tgnet.ConnectionsManager
import kotlin.coroutines.resume

/**
 * Remote data source for Google Play Services reCAPTCHA Enterprise and MTProto captcha result delivery.
 */
open class CaptchaRemoteDataSource {

    open suspend fun executeRecaptchaTask(
        action: CaptchaAction,
        keyId: String
    ): Result<String> {
        val activity: Activity? = AndroidUtilities.getActivity()
        if (activity == null) {
            FileLog.e("CaptchaRemoteDataSource: no activity found")
            return Result.Failure(AppError.InvalidInput("RECAPTCHA_FAILED_NO_ACTIVITY"))
        }

        return suspendCancellableCoroutine { continuation ->
            Recaptcha.getTasksClient(activity.application, keyId)
                .addOnSuccessListener { client ->
                    val recaptchaAction = toRecaptchaAction(action)
                    client.executeTask(recaptchaAction)
                        .addOnSuccessListener { token ->
                            FileLog.d("CaptchaRemoteDataSource: got token: $token")
                            if (continuation.isActive) {
                                if (token != null) {
                                    continuation.resume(Result.Success(token))
                                } else {
                                    continuation.resume(Result.Failure(AppError.InvalidInput("RECAPTCHA_FAILED_TOKEN_NULL")))
                                }
                            }
                        }
                        .addOnFailureListener { exception ->
                            FileLog.e("CaptchaRemoteDataSource: executeTask failure", exception)
                            if (continuation.isActive) {
                                continuation.resume(Result.Failure(AppError.Generic("RECAPTCHA_FAILED_TASK_EXCEPTION_${CaptchaMapper.formatException(exception)}", exception)))
                            }
                        }
                }
                .addOnFailureListener { exception ->
                    FileLog.e("CaptchaRemoteDataSource: getTasksClient failure", exception)
                    if (continuation.isActive) {
                        continuation.resume(Result.Failure(AppError.Generic("RECAPTCHA_FAILED_GETCLIENT_EXCEPTION_${CaptchaMapper.formatException(exception)}", exception)))
                    }
                }
        }
    }

    open fun submitResultToNative(
        currentAccount: Int,
        requestTokens: IntArray,
        token: String
    ): Result<Unit> {
        return try {
            ConnectionsManager.native_receivedCaptchaResult(currentAccount, requestTokens, token)
            Result.Success(Unit)
        } catch (e: Throwable) {
            FileLog.e("CaptchaRemoteDataSource: submitResultToNative failed", e)
            Result.Failure(AppError.Generic(e.message ?: "Failed to submit native captcha result", e))
        }
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
