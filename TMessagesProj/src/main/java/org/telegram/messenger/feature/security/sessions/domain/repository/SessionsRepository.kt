package org.telegram.messenger.feature.security.sessions.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.sessions.domain.model.SessionsListModel
import org.telegram.messenger.feature.security.sessions.domain.model.WebSessionModel

interface SessionsRepository {
    fun observeSessions(): Flow<SessionsListModel>
    fun observeWebSessions(): Flow<List<WebSessionModel>>

    suspend fun getSessions(): Result<SessionsListModel>
    suspend fun loadSessions(): Result<SessionsListModel>

    suspend fun getWebSessions(): Result<List<WebSessionModel>>
    suspend fun loadWebSessions(): Result<List<WebSessionModel>>

    suspend fun terminateSession(hash: Long): Result<Unit>
    suspend fun terminateAllOtherSessions(): Result<Unit>

    suspend fun terminateWebSession(hash: Long): Result<Unit>
    suspend fun terminateAllWebSessions(): Result<Unit>

    suspend fun updateSessionSettings(
        hash: Long,
        acceptSecretChats: Boolean,
        acceptCalls: Boolean
    ): Result<Unit>

    suspend fun setSessionsTtl(ttlDays: Int): Result<Unit>

    suspend fun acceptQrLogin(token: ByteArray): Result<Unit>
    suspend fun acceptQrLoginByLink(link: String): Result<Unit>
}
