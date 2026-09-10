package org.telegram.messenger.feature.network.push.domain.model

data class PushStatusModel(
    val token: String?,
    val serviceType: PushServiceType,
    val status: String?,
    val hasServices: Boolean,
    val isRegisteredForCurrentAccount: Boolean,
    val registeredAccountsCount: Int,
    val providerTitle: String
) {
    val isTokenValid: Boolean
        get() = !token.isNullOrBlank() && status != "__FIREBASE_FAILED__" && status != "__HUAWEI_FAILED__"
}
