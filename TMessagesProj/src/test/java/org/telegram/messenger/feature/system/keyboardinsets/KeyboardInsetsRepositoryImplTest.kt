package org.telegram.messenger.feature.system.keyboardinsets

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.keyboardinsets.data.datasource.KeyboardInsetsLocalDataSource
import org.telegram.messenger.feature.system.keyboardinsets.data.datasource.KeyboardInsetsRemoteDataSource
import org.telegram.messenger.feature.system.keyboardinsets.data.repository.KeyboardInsetsRepositoryImpl
import org.telegram.messenger.feature.system.keyboardinsets.domain.model.InAppImeMode
import org.telegram.messenger.feature.system.keyboardinsets.domain.model.KeyboardVisibilityState
import org.telegram.ui.Components.inset.WindowInsetsInAppController

class KeyboardInsetsRepositoryImplTest {

    private lateinit var localDataSource: KeyboardInsetsLocalDataSource
    private lateinit var remoteDataSource: KeyboardInsetsRemoteDataSource
    private lateinit var repository: KeyboardInsetsRepositoryImpl

    private var requestedHeight: Int? = null
    private var resetWait: Boolean? = null

    private val fakeController = object : WindowInsetsInAppController {
        override fun requestInAppKeyboardHeight(inAppKeyboardHeight: Int) {
            requestedHeight = inAppKeyboardHeight
        }

        override fun resetInAppKeyboardHeight(waitKeyboardOpen: Boolean) {
            resetWait = waitKeyboardOpen
        }
    }

    @Before
    fun setUp() {
        requestedHeight = null
        resetWait = null
        localDataSource = KeyboardInsetsLocalDataSource(fakeController)
        remoteDataSource = KeyboardInsetsRemoteDataSource()
        repository = KeyboardInsetsRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun testInitialInsets() {
        val insets = repository.getKeyboardInsets()
        assertNotNull(insets)
        assertEquals(0, insets.inAppKeyboardHeight)
        assertEquals(KeyboardVisibilityState.FULLY_HIDDEN, insets.keyboardState)
    }

    @Test
    fun testRequestAndResetInAppKeyboardHeight() {
        repository.requestInAppKeyboardHeight(300)
        assertEquals(300, requestedHeight)
        assertEquals(300, repository.getKeyboardInsets().inAppKeyboardHeight)
        assertEquals(InAppImeMode.VISIBLE, repository.getKeyboardInsets().inAppImeMode)

        repository.resetInAppKeyboardHeight(true)
        assertEquals(true, resetWait)
        assertEquals(0, repository.getKeyboardInsets().inAppKeyboardHeight)
        assertEquals(InAppImeMode.HIDE_AFTER_KEYBOARD_OPEN, repository.getKeyboardInsets().inAppImeMode)
    }

    @Test
    fun testUpdateSystemInsets() {
        repository.updateSystemInsets(top = 80, bottom = 120, imeBottom = 500, animated = true)
        val insets = repository.getKeyboardInsets()
        assertEquals(80, insets.systemBarsTop)
        assertEquals(120, insets.systemBarsBottom)
        assertEquals(500, insets.imeBottom)
        assertEquals(KeyboardVisibilityState.ANIMATING_TO_FULLY_VISIBLE, insets.keyboardState)
        assertEquals(1.0f, insets.keyboardVisibility, 0.001f)
    }

    @Test
    fun testRemoteDataSource() = runBlocking {
        val result = remoteDataSource.syncKeyboardInsetsConfig()
        assertTrue(result.isSuccess)
    }
}
