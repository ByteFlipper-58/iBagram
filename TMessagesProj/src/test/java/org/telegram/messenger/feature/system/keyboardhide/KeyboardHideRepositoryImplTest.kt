package org.telegram.messenger.feature.system.keyboardhide

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.keyboardhide.data.datasource.KeyboardHideLocalDataSource
import org.telegram.messenger.feature.system.keyboardhide.data.datasource.KeyboardHideRemoteDataSource
import org.telegram.messenger.feature.system.keyboardhide.data.repository.KeyboardHideRepositoryImpl
import org.telegram.messenger.feature.system.keyboardhide.domain.model.KeyboardDragSpec

class KeyboardHideRepositoryImplTest {

    private lateinit var localDataSource: KeyboardHideLocalDataSource
    private lateinit var remoteDataSource: KeyboardHideRemoteDataSource
    private lateinit var repository: KeyboardHideRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = KeyboardHideLocalDataSource(initialEnabled = true, testMode = true)
        remoteDataSource = KeyboardHideRemoteDataSource()
        repository = KeyboardHideRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun testInitialState() {
        val state = repository.getState()
        assertTrue(state.isEnabled)
        assertFalse(state.isMovingKeyboard)
        assertEquals(0f, state.currentProgress, 0.001f)
    }

    @Test
    fun testDragLifecycle() {
        repository.startMoving(keyboardSize = 500, bottomNavBarSize = 50, isKeyboard = true)
        val movingState = repository.getState()
        assertTrue(movingState.isMovingKeyboard)
        assertEquals(500, movingState.keyboardSize)

        repository.updateMoving(rawProgress = 0.4f, progress = 0.4f, translationY = 200f)
        assertEquals(0.4f, repository.getState().currentProgress, 0.001f)
        assertEquals(200f, repository.getState().translationY, 0.001f)

        repository.endMoving(shouldDismiss = true)
        assertFalse(repository.getState().isMovingKeyboard)
        assertTrue(repository.getState().isEndingMovingKeyboard)

        repository.finishDismiss(dismissed = true)
        assertFalse(repository.getState().isEndingMovingKeyboard)
        assertEquals(1.0f, repository.getState().currentProgress, 0.001f)
    }

    @Test
    fun testCalculateProgressAndDecision() {
        val spec = KeyboardDragSpec(
            fromY = 100f,
            currentRawY = 300f,
            keyboardSize = 400,
            bottomNavBarSize = 50
        )
        val progressResult = repository.calculateProgress(spec)
        assertNotNull(progressResult)

        val decision = repository.evaluateDismissDecision(
            currentProgress = 0.6f,
            lastDifferentProgress = 0.5f,
            velocityY = 1000f
        )
        assertNotNull(decision)
    }

    @Test
    fun testRemoteDataSource() = runBlocking {
        val result = remoteDataSource.syncKeyboardHideConfig()
        assertTrue(result.isSuccess)
    }
}
