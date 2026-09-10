package org.telegram.messenger.feature.security.captcha.domain.model

sealed class CaptchaResult {
    data class Success(val token: String) : CaptchaResult()
    data class Failure(val errorCode: String, val message: String? = null) : CaptchaResult()

    val isSuccess: Boolean
        get() = this is Success

    val tokenOrNull: String?
        get() = (this as? Success)?.token
}
