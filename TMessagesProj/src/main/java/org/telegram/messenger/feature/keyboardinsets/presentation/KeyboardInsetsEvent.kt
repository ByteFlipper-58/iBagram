package org.telegram.messenger.feature.keyboardinsets.presentation

sealed class KeyboardInsetsEvent {
    data class RequestHeight(val height: Int) : KeyboardInsetsEvent()
    data class ResetHeight(val waitKeyboardOpen: Boolean = true) : KeyboardInsetsEvent()
    data class RequestHeightWithNavbar(val height: Int, val navigationBarHeight: Int) : KeyboardInsetsEvent()
    data class UpdateInsets(val top: Int, val bottom: Int, val imeBottom: Int, val animated: Boolean = true) : KeyboardInsetsEvent()
}
