package org.telegram.messenger.feature.system.keyboardhide.presentation

import org.telegram.messenger.feature.system.keyboardhide.domain.model.KeyboardHideState

data class KeyboardHideUiState(
    val hideState: KeyboardHideState = KeyboardHideState(),
    val isDragging: Boolean = false
)
