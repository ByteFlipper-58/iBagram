package org.telegram.messenger.feature.proxy.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.proxy.domain.model.ProxyModel
import org.telegram.messenger.feature.proxy.domain.usecase.AddProxyUseCase
import org.telegram.messenger.feature.proxy.domain.usecase.CheckProxyPingUseCase
import org.telegram.messenger.feature.proxy.domain.usecase.DeleteProxyUseCase
import org.telegram.messenger.feature.proxy.domain.usecase.DisableProxyUseCase
import org.telegram.messenger.feature.proxy.domain.usecase.EnableProxyUseCase
import org.telegram.messenger.feature.proxy.domain.usecase.GetProxySettingsUseCase
import org.telegram.messenger.feature.proxy.domain.usecase.ObserveProxySettingsUseCase
import org.telegram.messenger.feature.proxy.domain.usecase.ToggleProxyRotationUseCase

class ProxyViewModel(
    private val observeProxySettingsUseCase: ObserveProxySettingsUseCase,
    private val getProxySettingsUseCase: GetProxySettingsUseCase,
    private val addProxyUseCase: AddProxyUseCase,
    private val deleteProxyUseCase: DeleteProxyUseCase,
    private val enableProxyUseCase: EnableProxyUseCase,
    private val disableProxyUseCase: DisableProxyUseCase,
    private val toggleProxyRotationUseCase: ToggleProxyRotationUseCase,
    private val checkProxyPingUseCase: CheckProxyPingUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProxyUiState>(ProxyUiState.Initial)
    val uiState: StateFlow<ProxyUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        loadSettings()
    }

    fun onEvent(event: ProxyEvent) {
        when (event) {
            is ProxyEvent.LoadSettings -> loadSettings()
            is ProxyEvent.AddProxy -> addProxy(
                address = event.address,
                port = event.port,
                username = event.username,
                password = event.password,
                secret = event.secret
            )
            is ProxyEvent.DeleteProxy -> deleteProxy(event.proxy)
            is ProxyEvent.EnableProxy -> enableProxy(event.proxy)
            is ProxyEvent.DisableProxy -> disableProxy()
            is ProxyEvent.ToggleRotation -> toggleRotation(event.enabled, event.timeoutMinutes)
            is ProxyEvent.CheckPing -> checkPing(event.proxy)
            is ProxyEvent.ClearError -> clearError()
        }
    }

    fun loadSettings() {
        observeJob?.cancel()
        if (_uiState.value is ProxyUiState.Initial) {
            _uiState.value = ProxyUiState.Loading
        }

        viewModelScope.launch {
            observeJob = launch {
                observeProxySettingsUseCase()
                    .catch { e ->
                        _uiState.value = ProxyUiState.Error(e.message ?: "Failed to observe proxy settings")
                    }
                    .collect { settings ->
                        val current = _uiState.value
                        val checkingProxy = if (current is ProxyUiState.Success) current.checkingProxy else null
                        val isSaving = if (current is ProxyUiState.Success) current.isSaving else false
                        val error = if (current is ProxyUiState.Success) current.error else null

                        _uiState.value = ProxyUiState.Success(
                            settings = settings,
                            checkingProxy = checkingProxy,
                            isSaving = isSaving,
                            error = error
                        )
                    }
            }
        }
    }

    fun addProxy(
        address: String,
        port: Int,
        username: String = "",
        password: String = "",
        secret: String = ""
    ) {
        viewModelScope.launch {
            setSaving(true)
            when (val result = addProxyUseCase(address, port, username, password, secret)) {
                is Result.Success -> {
                    setSaving(false)
                }
                is Result.Failure -> {
                    updateError(result.error.message)
                }
            }
        }
    }

    fun deleteProxy(proxy: ProxyModel) {
        viewModelScope.launch {
            setSaving(true)
            when (val result = deleteProxyUseCase(proxy)) {
                is Result.Success -> {
                    setSaving(false)
                }
                is Result.Failure -> {
                    updateError(result.error.message)
                }
            }
        }
    }

    fun enableProxy(proxy: ProxyModel) {
        viewModelScope.launch {
            setSaving(true)
            when (val result = enableProxyUseCase(proxy)) {
                is Result.Success -> {
                    setSaving(false)
                }
                is Result.Failure -> {
                    updateError(result.error.message)
                }
            }
        }
    }

    fun disableProxy() {
        viewModelScope.launch {
            setSaving(true)
            when (val result = disableProxyUseCase()) {
                is Result.Success -> {
                    setSaving(false)
                }
                is Result.Failure -> {
                    updateError(result.error.message)
                }
            }
        }
    }

    fun toggleRotation(enabled: Boolean, timeoutMinutes: Int = 10) {
        viewModelScope.launch {
            setSaving(true)
            when (val result = toggleProxyRotationUseCase(enabled, timeoutMinutes)) {
                is Result.Success -> {
                    setSaving(false)
                }
                is Result.Failure -> {
                    updateError(result.error.message)
                }
            }
        }
    }

    fun checkPing(proxy: ProxyModel) {
        viewModelScope.launch {
            val current = _uiState.value
            if (current is ProxyUiState.Success) {
                _uiState.value = current.copy(checkingProxy = proxy)
            }
            when (val result = checkProxyPingUseCase(proxy)) {
                is Result.Success -> {
                    val state = _uiState.value
                    if (state is ProxyUiState.Success) {
                        _uiState.value = state.copy(checkingProxy = null)
                    }
                }
                is Result.Failure -> {
                    val state = _uiState.value
                    if (state is ProxyUiState.Success) {
                        _uiState.value = state.copy(checkingProxy = null)
                    }
                }
            }
        }
    }

    fun clearError() {
        val current = _uiState.value
        if (current is ProxyUiState.Success && current.error != null) {
            _uiState.value = current.copy(error = null)
        }
    }

    private fun setSaving(isSaving: Boolean) {
        val current = _uiState.value
        if (current is ProxyUiState.Success) {
            _uiState.value = current.copy(isSaving = isSaving)
        }
    }

    private fun updateError(error: String) {
        val current = _uiState.value
        if (current is ProxyUiState.Success) {
            _uiState.value = current.copy(error = error, isSaving = false)
        } else {
            _uiState.value = ProxyUiState.Error(error)
        }
    }

    override fun onCleared() {
        super.onCleared()
        observeJob?.cancel()
    }
}
