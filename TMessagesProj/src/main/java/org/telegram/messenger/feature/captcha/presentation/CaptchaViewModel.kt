package org.telegram.messenger.feature.captcha.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.captcha.data.mapper.CaptchaMapper
import org.telegram.messenger.feature.captcha.domain.model.CaptchaResult
import org.telegram.messenger.feature.captcha.domain.usecase.CancelCaptchaUseCase
import org.telegram.messenger.feature.captcha.domain.usecase.GetActiveCaptchaRequestsUseCase
import org.telegram.messenger.feature.captcha.domain.usecase.ObserveActiveCaptchaRequestsUseCase
import org.telegram.messenger.feature.captcha.domain.usecase.SubmitCaptchaResultUseCase
import org.telegram.messenger.feature.captcha.domain.usecase.VerifyCaptchaUseCase

class CaptchaViewModel(
    val observeActiveCaptchaRequestsUseCase: ObserveActiveCaptchaRequestsUseCase,
    val getActiveCaptchaRequestsUseCase: GetActiveCaptchaRequestsUseCase,
    val verifyCaptchaUseCase: VerifyCaptchaUseCase,
    val submitCaptchaResultUseCase: SubmitCaptchaResultUseCase,
    val cancelCaptchaUseCase: CancelCaptchaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CaptchaUiState>(CaptchaUiState.Idle)
    val uiState: StateFlow<CaptchaUiState> = _uiState.asStateFlow()

    fun onEvent(event: CaptchaEvent) {
        when (event) {
            is CaptchaEvent.Verify -> verify(
                currentAccount = event.currentAccount,
                requestToken = event.requestToken,
                action = event.action,
                keyId = event.keyId
            )
            is CaptchaEvent.SubmitResult -> submitResult(
                currentAccount = event.currentAccount,
                requestTokens = event.requestTokens,
                token = event.token
            )
            is CaptchaEvent.Cancel -> cancel(
                currentAccount = event.currentAccount,
                action = event.action,
                keyId = event.keyId
            )
            is CaptchaEvent.Reset -> {
                _uiState.value = CaptchaUiState.Idle
            }
        }
    }

    private fun verify(
        currentAccount: Int,
        requestToken: Int,
        action: String,
        keyId: String
    ) {
        val request = CaptchaMapper.createRequest(
            currentAccount = currentAccount,
            action = action,
            keyId = keyId,
            initialToken = requestToken
        )
        _uiState.value = CaptchaUiState.Verifying(request)

        viewModelScope.launch {
            when (val result = verifyCaptchaUseCase(currentAccount, requestToken, action, keyId)) {
                is Result.Success -> {
                    when (val captchaResult = result.data) {
                        is CaptchaResult.Success -> {
                            submitCaptchaResultUseCase(
                                currentAccount = currentAccount,
                                requestTokens = listOf(requestToken),
                                token = captchaResult.token
                            )
                            _uiState.value = CaptchaUiState.Success(
                                token = captchaResult.token,
                                request = request
                            )
                        }
                        is CaptchaResult.Failure -> {
                            submitCaptchaResultUseCase(
                                currentAccount = currentAccount,
                                requestTokens = listOf(requestToken),
                                token = captchaResult.errorCode
                            )
                            _uiState.value = CaptchaUiState.Error(
                                errorCode = captchaResult.errorCode,
                                message = captchaResult.message,
                                request = request
                            )
                        }
                    }
                }
                is Result.Failure -> {
                    val errorCode = "RECAPTCHA_FAILED_GENERIC_ERROR"
                    submitCaptchaResultUseCase(
                        currentAccount = currentAccount,
                        requestTokens = listOf(requestToken),
                        token = errorCode
                    )
                    _uiState.value = CaptchaUiState.Error(
                        errorCode = errorCode,
                        message = result.error.message,
                        request = request
                    )
                }
            }
        }
    }

    private fun submitResult(
        currentAccount: Int,
        requestTokens: List<Int>,
        token: String
    ) {
        viewModelScope.launch {
            submitCaptchaResultUseCase(
                currentAccount = currentAccount,
                requestTokens = requestTokens,
                token = token
            )
        }
    }

    private fun cancel(
        currentAccount: Int,
        action: String,
        keyId: String
    ) {
        cancelCaptchaUseCase(currentAccount, action, keyId)
        _uiState.value = CaptchaUiState.Idle
    }
}
