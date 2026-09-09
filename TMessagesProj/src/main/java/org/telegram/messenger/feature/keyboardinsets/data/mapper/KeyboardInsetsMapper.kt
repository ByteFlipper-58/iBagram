package org.telegram.messenger.feature.keyboardinsets.data.mapper

import org.telegram.messenger.feature.keyboardinsets.domain.model.InAppImeMode
import org.telegram.messenger.feature.keyboardinsets.domain.model.KeyboardInsetsModel
import org.telegram.messenger.feature.keyboardinsets.domain.model.KeyboardVisibilityState

object KeyboardInsetsMapper {

    fun toModel(
        inAppKeyboardHeight: Int,
        navigationBarHeight: Int,
        systemBarsTop: Int,
        systemBarsBottom: Int,
        imeBottom: Int,
        keyboardVisibility: Float,
        keyboardState: KeyboardVisibilityState,
        inAppImeMode: InAppImeMode
    ): KeyboardInsetsModel {
        return KeyboardInsetsModel(
            inAppKeyboardHeight = inAppKeyboardHeight,
            navigationBarHeight = navigationBarHeight,
            systemBarsTop = systemBarsTop,
            systemBarsBottom = systemBarsBottom,
            imeBottom = imeBottom,
            keyboardVisibility = keyboardVisibility,
            keyboardState = keyboardState,
            inAppImeMode = inAppImeMode
        )
    }
}
