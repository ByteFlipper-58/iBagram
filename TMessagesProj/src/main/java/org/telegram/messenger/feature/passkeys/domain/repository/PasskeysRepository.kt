package org.telegram.messenger.feature.passkeys.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.passkeys.domain.model.PasskeyModel
import org.telegram.messenger.feature.passkeys.domain.model.PasskeysStateModel

interface PasskeysRepository {
    fun observePasskeys(): Flow<PasskeysStateModel>
    suspend fun getPasskeys(force: Boolean = false): Result<List<PasskeyModel>>
    suspend fun deletePasskey(id: String): Result<Unit>
    suspend fun isSupported(): Boolean
    suspend fun getMaxPasskeys(): Int
}
