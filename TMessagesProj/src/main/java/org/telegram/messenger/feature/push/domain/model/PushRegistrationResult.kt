package org.telegram.messenger.feature.push.domain.model

sealed class PushRegistrationResult {
    data class Success(
        val token: String?,
        val serviceType: PushServiceType
    ) : PushRegistrationResult()

    data class Failure(
        val error: String
    ) : PushRegistrationResult()
}
