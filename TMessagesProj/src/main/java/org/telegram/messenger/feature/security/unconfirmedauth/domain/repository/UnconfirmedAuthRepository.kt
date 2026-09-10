package org.telegram.messenger.feature.security.unconfirmedauth.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.unconfirmedauth.domain.model.UnconfirmedAuthStateModel

interface UnconfirmedAuthRepository {
    fun observeUnconfirmedAuths(): Flow<UnconfirmedAuthStateModel>
    suspend fun getUnconfirmedAuths(): UnconfirmedAuthStateModel
    suspend fun confirmAuth(hash: Long): Result<Boolean>
    suspend fun denyAuth(hash: Long): Result<Boolean>
    suspend fun confirmAll(): Result<Int>
    suspend fun denyAll(): Result<Int>
    suspend fun clear(): Result<Unit>
}
