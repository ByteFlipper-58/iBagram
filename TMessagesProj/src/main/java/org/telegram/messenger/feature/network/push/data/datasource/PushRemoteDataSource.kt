package org.telegram.messenger.feature.network.push.data.datasource

import org.telegram.messenger.PushListenerController
import org.telegram.messenger.core.data.BaseRemoteDataSource
import org.telegram.messenger.feature.network.push.data.mapper.PushMapper
import org.telegram.messenger.feature.network.push.domain.model.PushRegistrationResult
import org.telegram.messenger.feature.network.push.domain.model.PushServiceType

/**
 * Remote data source managing MTProto push token registration and push service provider requests.
 */
open class PushRemoteDataSource(
    currentAccount: Int
) : BaseRemoteDataSource(currentAccount) {

    open suspend fun requestPushToken(
        provider: PushListenerController.IPushListenerServiceProvider?,
        currentToken: String?
    ): Result<PushRegistrationResult> {
        return runCatching {
            if (provider != null) {
                provider.onRequestPushToken()
                val serviceType = PushMapper.toPushServiceType(provider.pushType)
                PushRegistrationResult.Success(currentToken, serviceType)
            } else {
                PushRegistrationResult.Failure("Push provider not available")
            }
        }
    }

    open suspend fun sendRegistrationToServer(
        serviceType: PushServiceType,
        token: String?
    ): Result<Unit> {
        return runCatching {
            val legacyType = PushMapper.toLegacyPushType(serviceType)
            PushListenerController.sendRegistrationToServer(legacyType, token)
        }
    }

    open suspend fun resetRegistration(
        legacyType: Int,
        provider: PushListenerController.IPushListenerServiceProvider?
    ): Result<Unit> {
        return runCatching {
            PushListenerController.sendRegistrationToServer(legacyType, null)
            provider?.onRequestPushToken()
            Unit
        }
    }
}
