package org.telegram.messenger.feature.authtokens.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.AuthTokensHelper
import org.telegram.messenger.feature.authtokens.data.mapper.AuthTokensMapper
import org.telegram.messenger.feature.authtokens.domain.model.AuthTokensState
import org.telegram.messenger.feature.authtokens.domain.model.SavedLoginTokenModel
import org.telegram.messenger.feature.authtokens.domain.model.SavedLogoutTokenModel
import org.telegram.messenger.feature.authtokens.domain.repository.AuthTokensRepository
import java.util.Collections

class LegacyAuthTokensRepository(
    private val currentAccount: Int = 0,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AuthTokensRepository {

    private val localLoginTokens = Collections.synchronizedList(mutableListOf<SavedLoginTokenModel>())
    private val localLogoutTokens = Collections.synchronizedList(mutableListOf<SavedLogoutTokenModel>())

    private val _state = MutableStateFlow(AuthTokensState())

    init {
        loadTokens()
    }

    private fun loadTokens() {
        val loadedLogin = mutableListOf<SavedLoginTokenModel>()
        runCatching {
            val list = AuthTokensHelper.getSavedLogInTokens()
            if (list != null) {
                for (auth in list) {
                    loadedLogin.add(AuthTokensMapper.toSavedLoginToken(auth))
                }
            }
        }

        val loadedLogout = mutableListOf<SavedLogoutTokenModel>()
        runCatching {
            val list = AuthTokensHelper.getSavedLogOutTokens()
            if (list != null) {
                for (token in list) {
                    loadedLogout.add(AuthTokensMapper.toSavedLogoutToken(token))
                }
            }
        }

        synchronized(localLoginTokens) {
            if (loadedLogin.isNotEmpty() || localLoginTokens.isEmpty()) {
                localLoginTokens.clear()
                localLoginTokens.addAll(loadedLogin)
            }
        }

        synchronized(localLogoutTokens) {
            if (loadedLogout.isNotEmpty() || localLogoutTokens.isEmpty()) {
                localLogoutTokens.clear()
                localLogoutTokens.addAll(loadedLogout)
            }
        }

        emitState()
    }

    private fun emitState() {
        val loginCopy = synchronized(localLoginTokens) { localLoginTokens.toList() }
        val logoutCopy = synchronized(localLogoutTokens) { localLogoutTokens.toList() }
        _state.update {
            it.copy(
                loginTokens = loginCopy,
                logoutTokens = logoutCopy
            )
        }
    }

    override fun observeState(): Flow<AuthTokensState> = _state.asStateFlow()

    override fun getState(): AuthTokensState = _state.value

    override fun getSavedLoginTokens(): List<SavedLoginTokenModel> {
        return synchronized(localLoginTokens) { localLoginTokens.toList() }
    }

    override fun saveLoginToken(token: SavedLoginTokenModel) {
        synchronized(localLoginTokens) {
            localLoginTokens.removeAll { it.hexToken == token.hexToken || (it.userId != 0L && it.userId == token.userId) }
            localLoginTokens.add(0, token)
            while (localLoginTokens.size > 20) {
                localLoginTokens.removeAt(localLoginTokens.lastIndex)
            }
        }

        val tl = AuthTokensMapper.toTLAuthAuthorization(token.hexToken)
        if (tl != null) {
            runCatching { AuthTokensHelper.saveLogInToken(tl) }
        }

        emitState()
    }

    override fun removeLoginToken(hexToken: String) {
        synchronized(localLoginTokens) {
            localLoginTokens.removeAll { it.hexToken == hexToken }
        }
        emitState()
    }

    override fun getSavedLogoutTokens(): List<SavedLogoutTokenModel> {
        return synchronized(localLogoutTokens) { localLogoutTokens.toList() }
    }

    override fun saveLogoutTokens(tokens: List<SavedLogoutTokenModel>) {
        val capped = tokens.take(20)
        synchronized(localLogoutTokens) {
            localLogoutTokens.clear()
            localLogoutTokens.addAll(capped)
        }

        val tlList = java.util.ArrayList<org.telegram.tgnet.TLRPC.TL_auth_loggedOut>()
        for (item in capped) {
            val tl = AuthTokensMapper.toTLLoggedOut(item.hexToken)
            if (tl != null) {
                tlList.add(tl)
            }
        }
        if (tlList.isNotEmpty()) {
            runCatching { AuthTokensHelper.saveLogOutTokens(tlList) }
        }

        emitState()
    }

    override fun addLogoutToken(token: SavedLogoutTokenModel) {
        synchronized(localLogoutTokens) {
            localLogoutTokens.removeAll { it.hexToken == token.hexToken }
            localLogoutTokens.add(token)
            while (localLogoutTokens.size > 20) {
                localLogoutTokens.removeAt(0)
            }
        }

        val tl = AuthTokensMapper.toTLLoggedOut(token.hexToken)
        if (tl != null) {
            runCatching { AuthTokensHelper.addLogOutToken(tl) }
        }

        emitState()
    }

    override fun removeLogoutToken(hexToken: String) {
        synchronized(localLogoutTokens) {
            localLogoutTokens.removeAll { it.hexToken == hexToken }
        }
        emitState()
    }

    override fun clearAllTokens() {
        synchronized(localLoginTokens) { localLoginTokens.clear() }
        synchronized(localLogoutTokens) { localLogoutTokens.clear() }
        runCatching { AuthTokensHelper.clearLogInTokens() }
        emitState()
    }

    override fun clearLoginTokens() {
        synchronized(localLoginTokens) { localLoginTokens.clear() }
        runCatching { AuthTokensHelper.clearLogInTokens() }
        emitState()
    }

    override fun clearLogoutTokens() {
        synchronized(localLogoutTokens) { localLogoutTokens.clear() }
        emitState()
    }

    override fun refresh() {
        loadTokens()
    }
}
