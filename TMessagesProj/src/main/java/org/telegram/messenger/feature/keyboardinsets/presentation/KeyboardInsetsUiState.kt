package org.telegram.messenger.feature.keyboardinsets.presentation

import org.telegram.messenger.feature.keyboardinsets.domain.model.KeyboardInsetsModel

data class KeyboardInsetsUiState(
    val insets: KeyboardInsetsModel = KeyboardInsetsModel.DEFAULT
)
