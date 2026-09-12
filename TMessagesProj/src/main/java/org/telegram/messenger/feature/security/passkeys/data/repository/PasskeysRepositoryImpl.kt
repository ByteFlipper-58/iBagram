package org.telegram.messenger.feature.security.passkeys.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.security.passkeys.data.datasource.PasskeysLocalDataSource
import org.telegram.messenger.feature.security.passkeys.data.datasource.PasskeysRemoteDataSource
import org.telegram.messenger.feature.security.passkeys.data.mapper.PasskeyMapper
import org.telegram.messenger.feature.security.passkeys.domain.model.PasskeyModel
import org.telegram.messenger.feature.security.passkeys.domain.model.PasskeysStateModel
import org.telegram.messenger.feature.security.passkeys.domain.repository.PasskeysRepository
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account

/**
 * Production implementation of [PasskeysRepository] coordinating local hardware/config
 * checks with remote MTProto passkey operations.
 */
class PasskeysRepositoryImpl(
    private val localDataSource: PasskeysLocalDataSource,
    private val remoteDataSource: PasskeysRemoteDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PasskeysRepository {

    private val _passkeysState = MutableStateFlow(PasskeysStateModel())

    override fun observePasskeys(): Flow<PasskeysStateModel> = _passkeysState.asStateFlow()

    override suspend fun isSupported(): Boolean = withContext(ioDispatcher) {
        localDataSource.isSupported()
    }

    override suspend fun getMaxPasskeys(): Int = withContext(ioDispatcher) {
        localDataSource.getMaxPasskeys()
    }

    override suspend fun getPasskeys(force: Boolean): Result<List<PasskeyModel>> =
        withContext(ioDispatcher) {
            val supported = isSupported()
            val max = getMaxPasskeys()

            if (!supported) {
                val emptyState = PasskeyMapper.toState(emptyList(), max, false)
                _passkeysState.value = emptyState
                return@withContext Result.Success(emptyList())
            }

            if (!force && _passkeysState.value.passkeys.isNotEmpty()) {
                return@withContext Result.Success(_passkeysState.value.passkeys)
            }

            when (val remoteResult = remoteDataSource.getPasskeys()) {
                is Result.Success -> {
                    val domainList = PasskeyMapper.toDomainList(remoteResult.data.passkeys)
                    val newState = PasskeyMapper.toState(domainList, max, true)
                    _passkeysState.value = newState
                    Result.Success(domainList)
                }
                is Result.Failure -> {
                    Result.failure(remoteResult.error)
                }
            }
        }

    override suspend fun deletePasskey(id: String): Result<Unit> = withContext(ioDispatcher) {
        when (val remoteResult = remoteDataSource.deletePasskey(id)) {
            is Result.Success -> {
                if (remoteResult.data is TLRPC.TL_boolFalse) {
                    Result.failure("Could not delete passkey")
                } else {
                    val updatedList = _passkeysState.value.passkeys.filter { it.id != id }
                    _passkeysState.value = _passkeysState.value.copy(passkeys = updatedList)
                    Result.Success(Unit)
                }
            }
            is Result.Failure -> {
                Result.failure(remoteResult.error)
            }
        }
    }
}
