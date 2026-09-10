package org.telegram.messenger.feature.network.push.domain.model

sealed class PushRegistrationResult {
    data class Success(
        val token: String?,
        val serviceType: PushServiceType
    ) : PushRegistrationResult()

    data class Failure(
        val error: String
    ) : PushRegistrationResult()
}
