package org.telegram.messenger.feature.security.authtokens.data.datasource

import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.AuthTokensHelper
import org.telegram.messenger.feature.security.authtokens.data.mapper.AuthTokensMapper
import org.telegram.messenger.feature.security.authtokens.domain.model.SavedLoginTokenModel
import org.telegram.messenger.feature.security.authtokens.domain.model.SavedLogoutTokenModel
import org.telegram.tgnet.TLRPC
import java.util.Collections

/**
 * Local data source managing saved login/logout tokens in SharedPreferences and memory caches.
 */
open class AuthTokensLocalDataSource(
    private val account: Int = 0
) {

    private val localLoginTokens = Collections.synchronizedList(mutableListOf<SavedLoginTokenModel>())
    private val localLogoutTokens = Collections.synchronizedList(mutableListOf<SavedLogoutTokenModel>())

    protected open fun isLegacyAvailable(): Boolean {
        return try {
            ApplicationLoader.applicationContext != null
        } catch (_: Throwable) {
            false
        }
    }

    open fun loadTokens(): Pair<List<SavedLoginTokenModel>, List<SavedLogoutTokenModel>> {
        val loadedLogin = mutableListOf<SavedLoginTokenModel>()
        if (isLegacyAvailable()) {
            runCatching {
                val list = AuthTokensHelper.getSavedLogInTokens()
                if (list != null) {
                    for (auth in list) {
                        loadedLogin.add(AuthTokensMapper.toSavedLoginToken(auth))
                    }
                }
            }
        }

        val loadedLogout = mutableListOf<SavedLogoutTokenModel>()
        if (isLegacyAvailable()) {
            runCatching {
                val list = AuthTokensHelper.getSavedLogOutTokens()
                if (list != null) {
                    for (token in list) {
                        loadedLogout.add(AuthTokensMapper.toSavedLogoutToken(token))
                    }
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

        return Pair(getSavedLoginTokens(), getSavedLogoutTokens())
    }

    open fun getSavedLoginTokens(): List<SavedLoginTokenModel> {
        return synchronized(localLoginTokens) { localLoginTokens.toList() }
    }

    open fun saveLoginToken(token: SavedLoginTokenModel) {
        synchronized(localLoginTokens) {
            localLoginTokens.removeAll { it.hexToken == token.hexToken || (it.userId != 0L && it.userId == token.userId) }
            localLoginTokens.add(0, token)
            while (localLoginTokens.size > 20) {
                localLoginTokens.removeAt(localLoginTokens.lastIndex)
            }
        }

        if (isLegacyAvailable()) {
            val tl = AuthTokensMapper.toTLAuthAuthorization(token.hexToken)
            if (tl != null) {
                runCatching { AuthTokensHelper.saveLogInToken(tl) }
            }
        }
    }

    open fun removeLoginToken(hexToken: String) {
        synchronized(localLoginTokens) {
            localLoginTokens.removeAll { it.hexToken == hexToken }
        }

        if (isLegacyAvailable()) {
            runCatching {
                val current = AuthTokensHelper.getSavedLogInTokens() ?: return@runCatching
                var removed = false
                val iterator = current.iterator()
                while (iterator.hasNext()) {
                    val auth = iterator.next()
                    val authHex = AuthTokensMapper.toSavedLoginToken(auth).hexToken
                    if (authHex == hexToken) {
                        iterator.remove()
                        removed = true
                    }
                }
                if (removed) {
                    AuthTokensHelper.clearLogInTokens()
                    for (auth in current.reversed()) {
                        AuthTokensHelper.saveLogInToken(auth)
                    }
                }
            }
        }
    }

    open fun getSavedLogoutTokens(): List<SavedLogoutTokenModel> {
        return synchronized(localLogoutTokens) { localLogoutTokens.toList() }
    }

    open fun saveLogoutTokens(tokens: List<SavedLogoutTokenModel>) {
        synchronized(localLogoutTokens) {
            localLogoutTokens.clear()
            localLogoutTokens.addAll(tokens.take(20))
        }

        if (isLegacyAvailable()) {
            runCatching {
                val list = ArrayList<TLRPC.TL_auth_loggedOut>()
                for (token in tokens) {
                    val tl = AuthTokensMapper.toTLLoggedOut(token.hexToken)
                    if (tl != null) {
                        list.add(tl)
                    }
                }
                AuthTokensHelper.saveLogOutTokens(list)
            }
        }
    }

    open fun addLogoutToken(token: SavedLogoutTokenModel) {
        val current = getSavedLogoutTokens().toMutableList()
        current.removeAll { it.hexToken == token.hexToken }
        current.add(0, token)
        saveLogoutTokens(current)
    }

    open fun removeLogoutToken(hexToken: String) {
        val current = getSavedLogoutTokens().toMutableList()
        current.removeAll { it.hexToken == hexToken }
        saveLogoutTokens(current)
    }

    open fun clearLoginTokens() {
        synchronized(localLoginTokens) {
            localLoginTokens.clear()
        }
        if (isLegacyAvailable()) {
            try {
                AuthTokensHelper.clearLogInTokens()
            } catch (_: Throwable) {}
        }
    }

    open fun clearLogoutTokens() {
        synchronized(localLogoutTokens) {
            localLogoutTokens.clear()
        }
        if (isLegacyAvailable()) {
            runCatching { AuthTokensHelper.saveLogOutTokens(ArrayList()) }
        }
    }

    open fun clearAllTokens() {
        clearLoginTokens()
        clearLogoutTokens()
    }
}
