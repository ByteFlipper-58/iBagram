package org.telegram.messenger.feature.sessions.domain.model

data class WebSessionModel(
    val hash: Long,
    val botId: Long,
    val domain: String,
    val browser: String,
    val platform: String,
    val dateCreated: Int,
    val dateActive: Int,
    val ip: String,
    val region: String
)
