package org.telegram.messenger.feature.captcha.domain.model

data class CaptchaRequestModel(
    val currentAccount: Int,
    val action: CaptchaAction,
    val keyId: String,
    val requestTokens: Set<Int> = emptySet()
) {
    val key: Int
        get() = 31 * (31 * currentAccount + action.rawAction.hashCode()) + keyId.hashCode()

    fun withAdditionalToken(token: Int): CaptchaRequestModel {
        return copy(requestTokens = requestTokens + token)
    }
}
