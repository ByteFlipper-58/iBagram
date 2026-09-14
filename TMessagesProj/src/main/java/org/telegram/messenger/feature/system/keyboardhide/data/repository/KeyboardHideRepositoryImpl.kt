package org.telegram.messenger.feature.system.keyboardhide.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.system.keyboardhide.data.datasource.KeyboardHideLocalDataSource
import org.telegram.messenger.feature.system.keyboardhide.data.datasource.KeyboardHideRemoteDataSource
import org.telegram.messenger.feature.system.keyboardhide.domain.model.KeyboardDismissDecision
import org.telegram.messenger.feature.system.keyboardhide.domain.model.KeyboardDragSpec
import org.telegram.messenger.feature.system.keyboardhide.domain.model.KeyboardHideProgressResult
import org.telegram.messenger.feature.system.keyboardhide.domain.model.KeyboardHideState
import org.telegram.messenger.feature.system.keyboardhide.domain.repository.KeyboardHideRepository

class KeyboardHideRepositoryImpl(
    private val localDataSource: KeyboardHideLocalDataSource,
    private val remoteDataSource: KeyboardHideRemoteDataSource
) : KeyboardHideRepository {

    override fun calculateProgress(spec: KeyboardDragSpec): KeyboardHideProgressResult {
        return localDataSource.calculateProgress(spec)
    }

    override fun evaluateDismissDecision(
        currentProgress: Float,
        lastDifferentProgress: Float,
        velocityY: Float
    ): KeyboardDismissDecision {
        return localDataSource.evaluateDismissDecision(currentProgress, lastDifferentProgress, velocityY)
    }

    override fun observeState(): StateFlow<KeyboardHideState> = localDataSource.observeState()

    override fun getState(): KeyboardHideState = localDataSource.getState()

    override fun setEnabled(enabled: Boolean) {
        localDataSource.setEnabled(enabled)
    }

    override fun startMoving(keyboardSize: Int, bottomNavBarSize: Int, isKeyboard: Boolean) {
        localDataSource.startMoving(keyboardSize, bottomNavBarSize, isKeyboard)
    }

    override fun updateMoving(rawProgress: Float, progress: Float, translationY: Float) {
        localDataSource.updateMoving(rawProgress, progress, translationY)
    }

    override fun endMoving(shouldDismiss: Boolean) {
        localDataSource.endMoving(shouldDismiss)
    }

    override fun finishDismiss(dismissed: Boolean) {
        localDataSource.finishDismiss(dismissed)
    }

    override fun reset() {
        localDataSource.reset()
    }
}
