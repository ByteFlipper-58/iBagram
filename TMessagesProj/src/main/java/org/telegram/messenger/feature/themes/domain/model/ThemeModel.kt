package org.telegram.messenger.feature.themes.domain.model

/**
 * Immutable domain model representing an available Telegram theme.
 */
data class ThemeModel(
    val key: String,
    val name: String,
    val pathToFile: String? = null,
    val assetName: String? = null,
    val slug: String? = null,
    val isDark: Boolean = false,
    val isDefault: Boolean = false,
    val accents: List<ThemeAccentModel> = emptyList(),
    val currentAccentId: Int = -1,
    val previewColor: Int = 0,
    val isLoaded: Boolean = true
)
