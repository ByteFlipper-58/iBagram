package org.telegram.messenger.feature.system.floatingdebug.presentation

import org.telegram.messenger.feature.system.floatingdebug.domain.model.FloatingDebugState

data class FloatingDebugUiState(
    val state: FloatingDebugState = FloatingDebugState.DEFAULT,
    val isMenuOpen: Boolean = false
)
