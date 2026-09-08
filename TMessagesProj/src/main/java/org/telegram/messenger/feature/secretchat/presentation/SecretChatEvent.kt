package org.telegram.messenger.feature.secretchat.presentation

/**
 * Single-shot events emitted by [SecretChatViewModel].
 */
sealed interface SecretChatEvent {
    object ChatAccepted : SecretChatEvent
    object ChatDeclined : SecretChatEvent
    data class TtlUpdated(val ttlSeconds: Int) : SecretChatEvent
    object ScreenshotSent : SecretChatEvent
    data class ShowError(val message: String) : SecretChatEvent
}
