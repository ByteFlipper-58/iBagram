package org.telegram.messenger.feature.keyboardhide.presentation

import org.telegram.messenger.feature.keyboardhide.domain.model.KeyboardHideState

data class KeyboardHideUiState(
    val hideState: KeyboardHideState = KeyboardHideState(),
    val isDragging: Boolean = false
)
