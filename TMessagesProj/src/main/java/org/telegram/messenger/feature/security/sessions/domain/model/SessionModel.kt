package org.telegram.messenger.feature.security.sessions.domain.model

data class SessionModel(
    val hash: Long,
    val deviceModel: String,
    val platform: String,
    val systemVersion: String,
    val appName: String,
    val appVersion: String,
    val dateCreated: Int,
    val dateActive: Int,
    val ip: String,
    val country: String,
    val region: String,
    val isCurrent: Boolean,
    val isOfficialApp: Boolean,
    val isPasswordPending: Boolean,
    val acceptSecretChats: Boolean,
    val acceptCalls: Boolean,
    val canAcceptSecretChats: Boolean,
    val canAcceptCalls: Boolean,
    val isUnconfirmed: Boolean
)
