package org.telegram.messenger.feature.network.push.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.PushListenerController
import org.telegram.messenger.SharedConfig
import org.telegram.messenger.UserConfig
import org.telegram.messenger.feature.network.push.data.mapper.PushMapper
import org.telegram.messenger.feature.network.push.domain.model.PushRegistrationResult
import org.telegram.messenger.feature.network.push.domain.model.PushServiceType
import org.telegram.messenger.feature.network.push.domain.model.PushStatusModel
import org.telegram.messenger.feature.network.push.domain.repository.PushRepository

class LegacyPushRepository(
    private val currentAccount: Int,
    private val providerProvider: () -> PushListenerController.IPushListenerServiceProvider? = {
        try {
            if (ApplicationLoader.applicationContext != null) {
                ApplicationLoader.getPushProvider()
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    },
    private val sharedConfigTokenProvider: () -> String? = {
        try {
            SharedConfig.pushString
        } catch (_: Throwable) {
            null
        }
    },
    private val sharedConfigTypeProvider: () -> Int = {
        try {
            SharedConfig.pushType
        } catch (_: Throwable) {
            PushListenerController.PUSH_TYPE_FIREBASE
        }
    },
    private val sharedConfigStatusProvider: () -> String? = {
        try {
            SharedConfig.pushStringStatus
        } catch (_: Throwable) {
            null
        }
    },
    private val userConfigRegisteredProvider: (Int) -> Boolean = { acc ->
        try {
            UserConfig.getInstance(acc).registeredForPush
        } catch (_: Throwable) {
            false
        }
    },
    private val sendRegistrationAction: (Int, String?) -> Unit = { type, token ->
        try {
            PushListenerController.sendRegistrationToServer(type, token)
        } catch (_: Throwable) {
        }
    }
) : PushRepository {

    private val statusFlow = MutableStateFlow(computeStatus())

    private fun computeStatus(): PushStatusModel {
        val provider = try { providerProvider() } catch (_: Throwable) { null }
        val hasServices = try { provider?.hasServices() ?: false } catch (_: Throwable) { false }
        val providerTitle = try { provider?.logTitle ?: "Google Play Services" } catch (_: Throwable) { "Google Play Services" }
        val legacyType = try { sharedConfigTypeProvider() } catch (_: Throwable) { PushListenerController.PUSH_TYPE_FIREBASE }
        val token = try { sharedConfigTokenProvider() } catch (_: Throwable) { null }
        val status = try { sharedConfigStatusProvider() } catch (_: Throwable) { null }
        val isRegistered = try { userConfigRegisteredProvider(currentAccount) } catch (_: Throwable) { false }

        var regCount = 0
        for (i in 0 until 4) {
            if (try { userConfigRegisteredProvider(i) } catch (_: Throwable) { false }) {
                regCount++
            }
        }

        return PushMapper.mapToPushStatus(
            token = token,
            legacyPushType = legacyType,
            status = status,
            hasServices = hasServices,
            isRegisteredForAccount = isRegistered,
            registeredAccountsCount = regCount,
            providerTitle = providerTitle
        )
    }

    private fun updateStatus() {
        statusFlow.value = computeStatus()
    }

    override fun observePushStatus(): Flow<PushStatusModel> = statusFlow.asStateFlow()

    override fun getPushStatus(): PushStatusModel {
        val status = computeStatus()
        statusFlow.value = status
        return status
    }

    override fun isPushServiceAvailable(): Boolean {
        return try {
            providerProvider()?.hasServices() ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override suspend fun requestPushToken(): Result<PushRegistrationResult> = withContext(Dispatchers.IO) {
        runCatching {
            val provider = providerProvider()
            if (provider != null) {
                provider.onRequestPushToken()
                val currentToken = sharedConfigTokenProvider()
                val serviceType = PushMapper.toPushServiceType(provider.pushType)
                updateStatus()
                PushRegistrationResult.Success(currentToken, serviceType)
            } else {
                PushRegistrationResult.Failure("Push provider not available")
            }
        }
    }

    override suspend fun registerPushToken(
        serviceType: PushServiceType,
        token: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val legacyType = PushMapper.toLegacyPushType(serviceType)
            sendRegistrationAction(legacyType, token)
            updateStatus()
        }
    }

    override suspend fun resetPushToken(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val legacyType = sharedConfigTypeProvider()
            sendRegistrationAction(legacyType, null)
            providerProvider()?.onRequestPushToken()
            updateStatus()
        }
    }
}
