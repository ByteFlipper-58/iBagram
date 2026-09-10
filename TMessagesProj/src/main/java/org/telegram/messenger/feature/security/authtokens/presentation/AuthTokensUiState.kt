package org.telegram.messenger.feature.security.authtokens.presentation

import org.telegram.messenger.feature.security.authtokens.domain.model.AuthTokensState
import org.telegram.messenger.feature.security.authtokens.domain.model.SavedLoginTokenModel

data class AuthTokensUiState(
    val state: AuthTokensState = AuthTokensState(),
    val isLoading: Boolean = false,
    val selectedToken: SavedLoginTokenModel? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)
