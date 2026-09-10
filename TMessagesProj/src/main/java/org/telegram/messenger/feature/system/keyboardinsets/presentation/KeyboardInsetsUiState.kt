package org.telegram.messenger.feature.system.keyboardinsets.presentation

import org.telegram.messenger.feature.system.keyboardinsets.domain.model.KeyboardInsetsModel

data class KeyboardInsetsUiState(
    val insets: KeyboardInsetsModel = KeyboardInsetsModel.DEFAULT
)
