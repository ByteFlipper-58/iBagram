package org.telegram.messenger.feature.security.authtokens.domain.model

/**
 * Pure domain models for authentication tokens used in fast re-login and session logout tracking.
 */

data class AuthTokenUserInfoModel(
    val userId: Long,
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val phone: String = ""
)

data class SavedLoginTokenModel(
    val hexToken: String,
    val futureAuthTokenBase64: String = "",
    val userId: Long = 0L,
    val userInfo: AuthTokenUserInfoModel? = null,
    val otherwiseReloginDays: Int = 0,
    val isPasswordSetupRequired: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)

data class SavedLogoutTokenModel(
    val hexToken: String,
    val futureAuthTokenBase64: String = "",
    val flags: Int = 0,
    val timestampMs: Long = System.currentTimeMillis()
)

data class AuthTokensState(
    val loginTokens: List<SavedLoginTokenModel> = emptyList(),
    val logoutTokens: List<SavedLogoutTokenModel> = emptyList(),
    val maxSavedTokens: Int = 20
)
