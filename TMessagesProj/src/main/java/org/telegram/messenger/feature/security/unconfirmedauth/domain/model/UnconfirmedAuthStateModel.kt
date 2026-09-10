package org.telegram.messenger.feature.security.unconfirmedauth.domain.model

data class UnconfirmedAuthStateModel(
    val auths: List<UnconfirmedAuthModel> = emptyList(),
    val isLoading: Boolean = false,
) {
    val hasPendingAuths: Boolean get() = auths.isNotEmpty()
}
