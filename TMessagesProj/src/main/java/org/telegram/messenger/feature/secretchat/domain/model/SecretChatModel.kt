package org.telegram.messenger.feature.secretchat.domain.model

/**
 * Pure Kotlin immutable domain model representing an end-to-end encrypted secret chat.
 * Decouples presentation from legacy [org.telegram.tgnet.TLRPC.EncryptedChat].
 */
data class SecretChatModel(
    val chatId: Int,
    val dialogId: Long,
    val userId: Long,
    val userName: String = "",
    val state: SecretChatState = SecretChatState.UNKNOWN,
    val ttlSeconds: Int = 0,
    val keyFingerprint: Long = 0L,
    val isCreator: Boolean = false,
    val createdAt: Int = 0
)
