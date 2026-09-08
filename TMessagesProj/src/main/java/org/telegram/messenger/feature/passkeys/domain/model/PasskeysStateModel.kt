package org.telegram.messenger.feature.passkeys.domain.model

data class PasskeysStateModel(
    val passkeys: List<PasskeyModel> = emptyList(),
    val maxPasskeys: Int = 10,
    val isSupported: Boolean = true,
) {
    val canAddPasskey: Boolean
        get() = isSupported && passkeys.size < maxPasskeys
}
