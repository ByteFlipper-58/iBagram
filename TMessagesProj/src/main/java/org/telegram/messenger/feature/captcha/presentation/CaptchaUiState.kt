package org.telegram.messenger.feature.captcha.presentation

import org.telegram.messenger.feature.captcha.domain.model.CaptchaRequestModel

sealed class CaptchaUiState {
    object Idle : CaptchaUiState()

    data class Verifying(
        val request: CaptchaRequestModel
    ) : CaptchaUiState()

    data class Success(
        val token: String,
        val request: CaptchaRequestModel
    ) : CaptchaUiState()

    data class Error(
        val errorCode: String,
        val message: String? = null,
        val request: CaptchaRequestModel? = null
    ) : CaptchaUiState()
}
