package org.telegram.messenger.feature.authtokens.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.authtokens.domain.model.SavedLoginTokenModel
import org.telegram.messenger.feature.authtokens.domain.model.SavedLogoutTokenModel
import org.telegram.messenger.feature.authtokens.domain.repository.AuthTokensRepository
import org.telegram.messenger.feature.authtokens.domain.usecase.AddLogoutTokenUseCase
import org.telegram.messenger.feature.authtokens.domain.usecase.ClearAllTokensUseCase
import org.telegram.messenger.feature.authtokens.domain.usecase.ObserveAuthTokensStateUseCase
import org.telegram.messenger.feature.authtokens.domain.usecase.RefreshAuthTokensUseCase
import org.telegram.messenger.feature.authtokens.domain.usecase.RemoveTokenUseCase
import org.telegram.messenger.feature.authtokens.domain.usecase.SaveLoginTokenUseCase

class AuthTokensViewModel(
    private val observeAuthTokensState: ObserveAuthTokensStateUseCase,
    private val saveLoginToken: SaveLoginTokenUseCase,
    private val addLogoutToken: AddLogoutTokenUseCase,
    private val removeToken: RemoveTokenUseCase,
    private val clearAllTokens: ClearAllTokensUseCase,
    private val refreshAuthTokens: RefreshAuthTokensUseCase,
    private val repository: AuthTokensRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthTokensUiState())
    val uiState: StateFlow<AuthTokensUiState> = _uiState.asStateFlow()

    init {
        observeAuthTokensState()
            .onEach { tokensState ->
                _uiState.update { it.copy(state = tokensState) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: AuthTokensEvent) {
        when (event) {
            is AuthTokensEvent.SaveLoginToken -> handleSaveLoginToken(event.token)
            is AuthTokensEvent.AddLogoutToken -> handleAddLogoutToken(event.token)
            is AuthTokensEvent.RemoveToken -> handleRemoveToken(event.hexToken)
            is AuthTokensEvent.SelectToken -> _uiState.update { it.copy(selectedToken = event.token) }
            AuthTokensEvent.ClearAllTokens -> handleClearAll()
            AuthTokensEvent.ClearLoginTokens -> {
                repository.clearLoginTokens()
                _uiState.update { it.copy(statusMessage = "Login tokens cleared") }
            }
            AuthTokensEvent.ClearLogoutTokens -> {
                repository.clearLogoutTokens()
                _uiState.update { it.copy(statusMessage = "Logout tokens cleared") }
            }
            AuthTokensEvent.Refresh -> {
                refreshAuthTokens()
                _uiState.update { it.copy(statusMessage = "Refreshed") }
            }
            AuthTokensEvent.DismissMessage -> {
                _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
            }
        }
    }

    private fun handleSaveLoginToken(token: SavedLoginTokenModel) {
        val success = saveLoginToken(token)
        if (success) {
            _uiState.update { it.copy(statusMessage = "Login token saved", errorMessage = null) }
        } else {
            _uiState.update { it.copy(errorMessage = "Invalid login token format") }
        }
    }

    private fun handleAddLogoutToken(token: SavedLogoutTokenModel) {
        val success = addLogoutToken(token)
        if (success) {
            _uiState.update { it.copy(statusMessage = "Logout token added", errorMessage = null) }
        } else {
            _uiState.update { it.copy(errorMessage = "Invalid logout token format") }
        }
    }

    private fun handleRemoveToken(hexToken: String) {
        removeToken(hexToken)
        _uiState.update {
            it.copy(
                statusMessage = "Token removed",
                errorMessage = null,
                selectedToken = if (it.selectedToken?.hexToken == hexToken) null else it.selectedToken
            )
        }
    }

    private fun handleClearAll() {
        clearAllTokens()
        _uiState.update {
            it.copy(
                selectedToken = null,
                statusMessage = "All tokens cleared",
                errorMessage = null
            )
        }
    }
}
