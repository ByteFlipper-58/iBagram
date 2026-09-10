package org.telegram.messenger.feature.system.windowvisibility.presentation

/**
 * UI State for window visibility arbitration.
 */
data class WindowVisibilityUiState(
    val isVisible: Boolean = true,
    val isHidden: Boolean = false,
    val reasonsCount: Int = 0,
    val activeReasons: Set<String> = emptySet(),
    val lastChangedReason: String? = null,
    val errorMessage: String? = null
)
