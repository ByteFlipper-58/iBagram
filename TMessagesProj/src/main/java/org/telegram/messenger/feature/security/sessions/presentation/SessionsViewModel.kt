package org.telegram.messenger.feature.security.sessions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.sessions.domain.usecase.AcceptQrLoginUseCase
import org.telegram.messenger.feature.security.sessions.domain.usecase.LoadSessionsUseCase
import org.telegram.messenger.feature.security.sessions.domain.usecase.LoadWebSessionsUseCase
import org.telegram.messenger.feature.security.sessions.domain.usecase.ObserveSessionsUseCase
import org.telegram.messenger.feature.security.sessions.domain.usecase.ObserveWebSessionsUseCase
import org.telegram.messenger.feature.security.sessions.domain.usecase.SetSessionsTtlUseCase
import org.telegram.messenger.feature.security.sessions.domain.usecase.TerminateAllOtherSessionsUseCase
import org.telegram.messenger.feature.security.sessions.domain.usecase.TerminateAllWebSessionsUseCase
import org.telegram.messenger.feature.security.sessions.domain.usecase.TerminateSessionUseCase
import org.telegram.messenger.feature.security.sessions.domain.usecase.TerminateWebSessionUseCase
import org.telegram.messenger.feature.security.sessions.domain.usecase.UpdateSessionSettingsUseCase

class SessionsViewModel(
    private val observeSessionsUseCase: ObserveSessionsUseCase,
    private val observeWebSessionsUseCase: ObserveWebSessionsUseCase,
    private val loadSessionsUseCase: LoadSessionsUseCase,
    private val loadWebSessionsUseCase: LoadWebSessionsUseCase,
    private val terminateSessionUseCase: TerminateSessionUseCase,
    private val terminateAllOtherSessionsUseCase: TerminateAllOtherSessionsUseCase,
    private val terminateWebSessionUseCase: TerminateWebSessionUseCase,
    private val terminateAllWebSessionsUseCase: TerminateAllWebSessionsUseCase,
    private val updateSessionSettingsUseCase: UpdateSessionSettingsUseCase,
    private val setSessionsTtlUseCase: SetSessionsTtlUseCase,
    private val acceptQrLoginUseCase: AcceptQrLoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionsUiState())
    val uiState: StateFlow<SessionsUiState> = _uiState.asStateFlow()

    init {
        observeSessionsUseCase()
            .onEach { sessionsList ->
                _uiState.update { current ->
                    current.copy(
                        currentSession = sessionsList.currentSession,
                        otherSessions = sessionsList.otherSessions,
                        passwordPendingSessions = sessionsList.passwordPendingSessions,
                        ttlDays = sessionsList.ttlDays
                    )
                }
            }
            .launchIn(viewModelScope)

        observeWebSessionsUseCase()
            .onEach { webSessions ->
                _uiState.update { current ->
                    current.copy(webSessions = webSessions)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: SessionsEvent) {
        when (event) {
            is SessionsEvent.LoadSessions -> loadSessions(event.silent)
            is SessionsEvent.LoadWebSessions -> loadWebSessions(event.silent)
            is SessionsEvent.TerminateSession -> terminateSession(event.hash)
            is SessionsEvent.TerminateAllOtherSessions -> terminateAllOtherSessions()
            is SessionsEvent.TerminateWebSession -> terminateWebSession(event.hash)
            is SessionsEvent.TerminateAllWebSessions -> terminateAllWebSessions()
            is SessionsEvent.UpdateSessionSettings -> updateSessionSettings(
                event.hash,
                event.acceptSecretChats,
                event.acceptCalls
            )
            is SessionsEvent.SetSessionsTtl -> setSessionsTtl(event.ttlDays)
            is SessionsEvent.AcceptQrLogin -> acceptQrLogin(event.token)
            is SessionsEvent.AcceptQrLoginByLink -> acceptQrLoginByLink(event.link)
            is SessionsEvent.ClearMessages -> clearMessages()
        }
    }

    fun loadSessions(silent: Boolean = false) {
        if (!silent) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        }
        viewModelScope.launch {
            when (val result = loadSessionsUseCase()) {
                is Result.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            currentSession = result.data.currentSession,
                            otherSessions = result.data.otherSessions,
                            passwordPendingSessions = result.data.passwordPendingSessions,
                            ttlDays = result.data.ttlDays,
                            isLoading = false
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.message) }
                }
            }
        }
    }

    fun loadWebSessions(silent: Boolean = false) {
        if (!silent) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        }
        viewModelScope.launch {
            when (val result = loadWebSessionsUseCase()) {
                is Result.Success -> {
                    _uiState.update { it.copy(webSessions = result.data, isLoading = false) }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.message) }
                }
            }
        }
    }

    fun terminateSession(hash: Long) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = terminateSessionUseCase(hash)) {
                is Result.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            otherSessions = current.otherSessions.filter { it.hash != hash },
                            passwordPendingSessions = current.passwordPendingSessions.filter { it.hash != hash },
                            isLoading = false,
                            actionSuccessMessage = "Session terminated"
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.message) }
                }
            }
        }
    }

    fun terminateAllOtherSessions() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = terminateAllOtherSessionsUseCase()) {
                is Result.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            otherSessions = emptyList(),
                            passwordPendingSessions = emptyList(),
                            isLoading = false,
                            actionSuccessMessage = "All other sessions terminated"
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.message) }
                }
            }
        }
    }

    fun terminateWebSession(hash: Long) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = terminateWebSessionUseCase(hash)) {
                is Result.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            webSessions = current.webSessions.filter { it.hash != hash },
                            isLoading = false,
                            actionSuccessMessage = "Web session terminated"
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.message) }
                }
            }
        }
    }

    fun terminateAllWebSessions() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = terminateAllWebSessionsUseCase()) {
                is Result.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            webSessions = emptyList(),
                            isLoading = false,
                            actionSuccessMessage = "All web sessions terminated"
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.message) }
                }
            }
        }
    }

    fun updateSessionSettings(hash: Long, acceptSecretChats: Boolean, acceptCalls: Boolean) {
        viewModelScope.launch {
            when (val result = updateSessionSettingsUseCase(hash, acceptSecretChats, acceptCalls)) {
                is Result.Success -> {
                    val updateItem: (org.telegram.messenger.feature.security.sessions.domain.model.SessionModel) -> org.telegram.messenger.feature.security.sessions.domain.model.SessionModel = { s ->
                        if (s.hash == hash) {
                            s.copy(acceptSecretChats = acceptSecretChats, acceptCalls = acceptCalls)
                        } else s
                    }
                    _uiState.update { current ->
                        current.copy(
                            currentSession = current.currentSession?.let(updateItem),
                            otherSessions = current.otherSessions.map(updateItem),
                            passwordPendingSessions = current.passwordPendingSessions.map(updateItem),
                            actionSuccessMessage = "Settings updated"
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    fun setSessionsTtl(ttlDays: Int) {
        viewModelScope.launch {
            when (val result = setSessionsTtlUseCase(ttlDays)) {
                is Result.Success -> {
                    _uiState.update { it.copy(ttlDays = ttlDays, actionSuccessMessage = "TTL updated") }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    fun acceptQrLogin(token: ByteArray) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = acceptQrLoginUseCase.byToken(token)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, actionSuccessMessage = "QR login accepted") }
                    loadSessions(true)
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.message) }
                }
            }
        }
    }

    fun acceptQrLoginByLink(link: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = acceptQrLoginUseCase.byLink(link)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, actionSuccessMessage = "QR login accepted") }
                    loadSessions(true)
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.message) }
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccessMessage = null) }
    }
}
