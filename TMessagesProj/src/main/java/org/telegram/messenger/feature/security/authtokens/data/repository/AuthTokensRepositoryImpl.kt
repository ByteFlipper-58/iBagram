package org.telegram.messenger.feature.security.authtokens.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.security.authtokens.data.datasource.AuthTokensLocalDataSource
import org.telegram.messenger.feature.security.authtokens.data.datasource.AuthTokensRemoteDataSource
import org.telegram.messenger.feature.security.authtokens.domain.model.AuthTokensState
import org.telegram.messenger.feature.security.authtokens.domain.model.SavedLoginTokenModel
import org.telegram.messenger.feature.security.authtokens.domain.model.SavedLogoutTokenModel
import org.telegram.messenger.feature.security.authtokens.domain.repository.AuthTokensRepository

/**
 * Clean repository implementation managing authentication tokens lifecycle and state observation.
 */
class AuthTokensRepositoryImpl(
    private val currentAccount: Int = 0,
    private val remoteDataSource: AuthTokensRemoteDataSource = AuthTokensRemoteDataSource(currentAccount),
    private val localDataSource: AuthTokensLocalDataSource = AuthTokensLocalDataSource(currentAccount),
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) : AuthTokensRepository {

    private val _state = MutableStateFlow(AuthTokensState())

    init {
        refresh()
    }

    override fun observeState(): Flow<AuthTokensState> = _state.asStateFlow()

    override fun getState(): AuthTokensState = _state.value

    override fun getSavedLoginTokens(): List<SavedLoginTokenModel> {
        return localDataSource.getSavedLoginTokens()
    }

    override fun saveLoginToken(token: SavedLoginTokenModel) {
        localDataSource.saveLoginToken(token)
        emitState()
    }

    override fun removeLoginToken(hexToken: String) {
        localDataSource.removeLoginToken(hexToken)
        emitState()
    }

    override fun getSavedLogoutTokens(): List<SavedLogoutTokenModel> {
        return localDataSource.getSavedLogoutTokens()
    }

    override fun saveLogoutTokens(tokens: List<SavedLogoutTokenModel>) {
        localDataSource.saveLogoutTokens(tokens)
        emitState()
    }

    override fun addLogoutToken(token: SavedLogoutTokenModel) {
        localDataSource.addLogoutToken(token)
        emitState()
    }

    override fun removeLogoutToken(hexToken: String) {
        localDataSource.removeLogoutToken(hexToken)
        emitState()
    }

    override fun clearAllTokens() {
        localDataSource.clearAllTokens()
        emitState()
    }

    override fun clearLoginTokens() {
        localDataSource.clearLoginTokens()
        emitState()
    }

    override fun clearLogoutTokens() {
        localDataSource.clearLogoutTokens()
        emitState()
    }

    override fun refresh() {
        val (login, logout) = localDataSource.loadTokens()
        _state.update {
            it.copy(
                loginTokens = login,
                logoutTokens = logout
            )
        }
    }

    private fun emitState() {
        _state.update {
            it.copy(
                loginTokens = localDataSource.getSavedLoginTokens(),
                logoutTokens = localDataSource.getSavedLogoutTokens()
            )
        }
    }
}
