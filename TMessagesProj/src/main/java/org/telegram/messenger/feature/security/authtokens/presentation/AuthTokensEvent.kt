package org.telegram.messenger.feature.security.authtokens.presentation

import org.telegram.messenger.feature.security.authtokens.domain.model.SavedLoginTokenModel
import org.telegram.messenger.feature.security.authtokens.domain.model.SavedLogoutTokenModel

sealed class AuthTokensEvent {
    data class SaveLoginToken(val token: SavedLoginTokenModel) : AuthTokensEvent()
    data class AddLogoutToken(val token: SavedLogoutTokenModel) : AuthTokensEvent()
    data class RemoveToken(val hexToken: String) : AuthTokensEvent()
    data class SelectToken(val token: SavedLoginTokenModel?) : AuthTokensEvent()
    object ClearAllTokens : AuthTokensEvent()
    object ClearLoginTokens : AuthTokensEvent()
    object ClearLogoutTokens : AuthTokensEvent()
    object Refresh : AuthTokensEvent()
    object DismissMessage : AuthTokensEvent()
}
