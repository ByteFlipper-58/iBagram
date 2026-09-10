package org.telegram.messenger.feature.system.themes.domain.model

/**
 * Immutable domain model representing an accent color variant of a theme.
 */
data class ThemeAccentModel(
    val id: Int,
    val accentColor: Int,
    val accentColor2: Int = 0,
    val myMessagesAccentColor: Int = 0,
    val isDefault: Boolean = false
)
