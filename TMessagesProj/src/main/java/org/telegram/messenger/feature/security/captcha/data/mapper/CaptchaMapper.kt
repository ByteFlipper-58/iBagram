package org.telegram.messenger.feature.security.captcha.data.mapper

import org.telegram.messenger.feature.security.captcha.domain.model.CaptchaAction
import org.telegram.messenger.feature.security.captcha.domain.model.CaptchaRequestModel
import org.telegram.messenger.feature.security.captcha.domain.model.CaptchaResult

object CaptchaMapper {

    fun mapAction(action: String): CaptchaAction {
        return CaptchaAction.fromString(action)
    }

    fun mapActionToString(action: CaptchaAction): String {
        return action.rawAction
    }

    fun formatException(e: Throwable?): String {
        if (e == null) return "NULL"
        val message = e.message ?: return "MSG_NULL"
        return message.replace(" ", "_").uppercase()
    }

    fun mapResult(token: String?): CaptchaResult {
        return when {
            token == null -> CaptchaResult.Failure("RECAPTCHA_FAILED_TOKEN_NULL")
            token.startsWith("RECAPTCHA_FAILED_") -> CaptchaResult.Failure(token)
            else -> CaptchaResult.Success(token)
        }
    }

    fun createRequest(
        currentAccount: Int,
        action: String,
        keyId: String,
        initialToken: Int? = null
    ): CaptchaRequestModel {
        val tokens = if (initialToken != null) setOf(initialToken) else emptySet()
        return CaptchaRequestModel(
            currentAccount = currentAccount,
            action = mapAction(action),
            keyId = keyId,
            requestTokens = tokens
        )
    }
}
