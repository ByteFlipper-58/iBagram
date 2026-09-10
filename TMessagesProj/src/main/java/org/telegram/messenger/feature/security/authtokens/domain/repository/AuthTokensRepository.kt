package org.telegram.messenger.feature.security.authtokens.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.security.authtokens.domain.model.AuthTokensState
import org.telegram.messenger.feature.security.authtokens.domain.model.SavedLoginTokenModel
import org.telegram.messenger.feature.security.authtokens.domain.model.SavedLogoutTokenModel

interface AuthTokensRepository {
    fun observeState(): Flow<AuthTokensState>
    fun getState(): AuthTokensState

    fun getSavedLoginTokens(): List<SavedLoginTokenModel>
    fun saveLoginToken(token: SavedLoginTokenModel)
    fun removeLoginToken(hexToken: String)

    fun getSavedLogoutTokens(): List<SavedLogoutTokenModel>
    fun saveLogoutTokens(tokens: List<SavedLogoutTokenModel>)
    fun addLogoutToken(token: SavedLogoutTokenModel)
    fun removeLogoutToken(hexToken: String)

    fun clearAllTokens()
    fun clearLoginTokens()
    fun clearLogoutTokens()
    fun refresh()
}
