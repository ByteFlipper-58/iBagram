package org.telegram.messenger.feature.system.settings.domain.model

/**
 * Pure Kotlin immutable domain model representing user and application settings.
 * Shields presentation and domain layers from legacy mutable singletons [SharedConfig] and [UserConfig].
 */
data class SettingsModel(
    val fontSize: Int = 16,
    val bubbleRadius: Int = 17,
    val saveToGallery: Boolean = false,
    val streamMedia: Boolean = true,
    val suggestStickers: Boolean = true,
    val inappCamera: Boolean = true,
    val distanceSystemType: Int = 0, // 0 = metric, 1 = imperial
    val syncContacts: Boolean = true,
    val suggestContacts: Boolean = true,
    val showCallsTab: Boolean = false
)
