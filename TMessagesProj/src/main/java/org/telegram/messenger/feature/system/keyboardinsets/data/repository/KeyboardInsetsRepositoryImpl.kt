package org.telegram.messenger.feature.system.keyboardinsets.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.system.keyboardinsets.data.datasource.KeyboardInsetsLocalDataSource
import org.telegram.messenger.feature.system.keyboardinsets.data.datasource.KeyboardInsetsRemoteDataSource
import org.telegram.messenger.feature.system.keyboardinsets.domain.model.KeyboardInsetsModel
import org.telegram.messenger.feature.system.keyboardinsets.domain.repository.KeyboardInsetsRepository

class KeyboardInsetsRepositoryImpl(
    private val localDataSource: KeyboardInsetsLocalDataSource,
    private val remoteDataSource: KeyboardInsetsRemoteDataSource
) : KeyboardInsetsRepository {

    override fun requestInAppKeyboardHeight(height: Int) {
        localDataSource.requestInAppKeyboardHeight(height)
    }

    override fun resetInAppKeyboardHeight(waitKeyboardOpen: Boolean) {
        localDataSource.resetInAppKeyboardHeight(waitKeyboardOpen)
    }

    override fun requestInAppKeyboardHeightIncludeNavbar(height: Int, navigationBarHeight: Int) {
        localDataSource.requestInAppKeyboardHeightIncludeNavbar(height, navigationBarHeight)
    }

    override fun updateSystemInsets(top: Int, bottom: Int, imeBottom: Int, animated: Boolean) {
        localDataSource.updateSystemInsets(top, bottom, imeBottom, animated)
    }

    override fun getKeyboardInsets(): KeyboardInsetsModel {
        return localDataSource.getKeyboardInsets()
    }

    override fun observeKeyboardInsets(): Flow<KeyboardInsetsModel> {
        return localDataSource.observeKeyboardInsets()
    }
}
