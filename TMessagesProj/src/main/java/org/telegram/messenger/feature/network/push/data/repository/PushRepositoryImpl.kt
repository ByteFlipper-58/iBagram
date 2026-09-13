package org.telegram.messenger.feature.network.push.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.telegram.messenger.feature.network.push.data.datasource.PushLocalDataSource
import org.telegram.messenger.feature.network.push.data.datasource.PushRemoteDataSource
import org.telegram.messenger.feature.network.push.domain.model.PushRegistrationResult
import org.telegram.messenger.feature.network.push.domain.model.PushServiceType
import org.telegram.messenger.feature.network.push.domain.model.PushStatusModel
import org.telegram.messenger.feature.network.push.domain.repository.PushRepository

/**
 * Modern repository implementation managing push token state and remote provider interactions.
 */
class PushRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: PushLocalDataSource,
    private val remoteDataSource: PushRemoteDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PushRepository {

    private val statusFlow = MutableStateFlow(localDataSource.computeStatus())

    private fun updateStatus() {
        statusFlow.value = localDataSource.computeStatus()
    }

    override fun observePushStatus(): Flow<PushStatusModel> {
        updateStatus()
        return statusFlow.asStateFlow()
    }

    override fun getPushStatus(): PushStatusModel {
        val status = localDataSource.computeStatus()
        statusFlow.value = status
        return status
    }

    override fun isPushServiceAvailable(): Boolean {
        return localDataSource.hasServices()
    }

    override suspend fun requestPushToken(): Result<PushRegistrationResult> = withContext(ioDispatcher) {
        val provider = localDataSource.getPushProvider()
        val currentToken = localDataSource.getPushToken()
        val result = remoteDataSource.requestPushToken(provider, currentToken)
        updateStatus()
        result
    }

    override suspend fun registerPushToken(
        serviceType: PushServiceType,
        token: String?
    ): Result<Unit> = withContext(ioDispatcher) {
        val result = remoteDataSource.sendRegistrationToServer(serviceType, token)
        updateStatus()
        result
    }

    override suspend fun resetPushToken(): Result<Unit> = withContext(ioDispatcher) {
        val legacyType = localDataSource.getPushType()
        val provider = localDataSource.getPushProvider()
        val result = remoteDataSource.resetRegistration(legacyType, provider)
        updateStatus()
        result
    }
}
