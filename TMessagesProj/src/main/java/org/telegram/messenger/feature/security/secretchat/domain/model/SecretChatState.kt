package org.telegram.messenger.feature.security.secretchat.domain.model

/**
 * Pure Kotlin typed enum representing the lifecycle states of an end-to-end encrypted secret chat.
 * Decouples presentation and domain layers from legacy TLRPC EncryptedChat class subtypes.
 */
enum class SecretChatState {
    WAITING,
    REQUESTED,
    ACTIVE,
    DISCARDED,
    UNKNOWN
}
