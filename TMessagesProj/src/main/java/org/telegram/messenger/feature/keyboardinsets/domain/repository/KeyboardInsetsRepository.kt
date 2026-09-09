package org.telegram.messenger.feature.keyboardinsets.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.keyboardinsets.domain.model.KeyboardInsetsModel

/**
 * Contract for managing in-app keyboard heights, IME window insets and navigation bar offsets.
 */
interface KeyboardInsetsRepository {
    fun requestInAppKeyboardHeight(height: Int)
    fun resetInAppKeyboardHeight(waitKeyboardOpen: Boolean)
    fun requestInAppKeyboardHeightIncludeNavbar(height: Int, navigationBarHeight: Int)
    fun updateSystemInsets(top: Int, bottom: Int, imeBottom: Int, animated: Boolean)
    fun getKeyboardInsets(): KeyboardInsetsModel
    fun observeKeyboardInsets(): Flow<KeyboardInsetsModel>
}
