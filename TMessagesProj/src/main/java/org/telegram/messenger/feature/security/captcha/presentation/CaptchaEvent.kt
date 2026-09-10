package org.telegram.messenger.feature.security.captcha.presentation

sealed class CaptchaEvent {
    data class Verify(
        val currentAccount: Int,
        val requestToken: Int,
        val action: String,
        val keyId: String
    ) : CaptchaEvent()

    data class SubmitResult(
        val currentAccount: Int,
        val requestTokens: List<Int>,
        val token: String
    ) : CaptchaEvent()

    data class Cancel(
        val currentAccount: Int,
        val action: String,
        val keyId: String
    ) : CaptchaEvent()

    object Reset : CaptchaEvent()
}
