package org.telegram.messenger.feature.push.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.push.domain.model.PushRegistrationResult
import org.telegram.messenger.feature.push.domain.usecase.GetPushStatusUseCase
import org.telegram.messenger.feature.push.domain.usecase.IsPushAvailableUseCase
import org.telegram.messenger.feature.push.domain.usecase.ObservePushStatusUseCase
import org.telegram.messenger.feature.push.domain.usecase.RegisterPushTokenUseCase
import org.telegram.messenger.feature.push.domain.usecase.RequestPushTokenUseCase
import org.telegram.messenger.feature.push.domain.usecase.ResetPushTokenUseCase

class PushViewModel(
    private val observePushStatusUseCase: ObservePushStatusUseCase,
    private val getPushStatusUseCase: GetPushStatusUseCase,
    private val isPushAvailableUseCase: IsPushAvailableUseCase,
    private val requestPushTokenUseCase: RequestPushTokenUseCase,
    private val registerPushTokenUseCase: RegisterPushTokenUseCase,
    private val resetPushTokenUseCase: ResetPushTokenUseCase,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val _uiState = MutableStateFlow(
        PushUiState(
            status = getPushStatusUseCase(),
            isAvailable = isPushAvailableUseCase()
        )
    )
    val uiState: StateFlow<PushUiState> = _uiState.asStateFlow()

    init {
        observePushStatusUseCase()
            .onEach { status ->
                _uiState.update { it.copy(status = status, isAvailable = isPushAvailableUseCase()) }
            }
            .launchIn(scope)
    }

    fun onEvent(event: PushEvent) {
        when (event) {
            PushEvent.RefreshStatus -> refreshStatus()
            PushEvent.RequestPushToken -> requestPushToken()
            is PushEvent.RegisterPushToken -> registerPushToken(event)
            PushEvent.ResetPushToken -> resetPushToken()
            PushEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
            PushEvent.DismissInfo -> _uiState.update { it.copy(infoMessage = null) }
        }
    }

    private fun refreshStatus() {
        val status = getPushStatusUseCase()
        val isAvailable = isPushAvailableUseCase()
        _uiState.update { it.copy(status = status, isAvailable = isAvailable) }
    }

    private fun requestPushToken() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            requestPushTokenUseCase()
                .onSuccess { result ->
                    when (result) {
                        is PushRegistrationResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    infoMessage = "Push token requested successfully"
                                )
                            }
                        }
                        is PushRegistrationResult.Failure -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = result.error
                                )
                            }
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to request push token"
                        )
                    }
                }
        }
    }

    private fun registerPushToken(event: PushEvent.RegisterPushToken) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            registerPushTokenUseCase(event.serviceType, event.token)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            infoMessage = "Token registered on server"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Registration failed"
                        )
                    }
                }
        }
    }

    private fun resetPushToken() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            resetPushTokenUseCase()
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            infoMessage = "Push token reset requested"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Reset failed"
                        )
                    }
                }
        }
    }
}
