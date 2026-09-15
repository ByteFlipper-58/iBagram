package org.telegram.messenger.feature.messaging.emojieffects

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.messaging.emojieffects.data.datasource.EmojiEffectsLocalDataSource
import org.telegram.messenger.feature.messaging.emojieffects.data.datasource.EmojiEffectsRemoteDataSource
import org.telegram.messenger.feature.messaging.emojieffects.data.repository.EmojiEffectsRepositoryImpl
import org.telegram.messenger.feature.messaging.emojieffects.domain.model.EmojiEffectItem

class EmojiEffectsRepositoryImplTest {

    private lateinit var localDataSource: EmojiEffectsLocalDataSource
    private lateinit var remoteDataSource: EmojiEffectsRemoteDataSource
    private lateinit var repository: EmojiEffectsRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = EmojiEffectsLocalDataSource(0)
        remoteDataSource = EmojiEffectsRemoteDataSource(0)
        repository = EmojiEffectsRepositoryImpl(
            currentAccount = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource
        )
    }

    @Test
    fun initialState_isIdle() {
        val state = repository.getState()
        assertTrue(state.isIdle)
        assertTrue(state.activeEffects.isEmpty())
        assertNull(state.currentSession)
    }

    @Test
    fun recordTap_createsSession() {
        val session = repository.recordTap(
            messageId = 123,
            emoticon = "\uD83D\uDD25",
            animationIndex = 0,
            timestampMs = 1000L
        )
        assertEquals(123, session.messageId)
        assertEquals("\uD83D\uDD25", session.emoticon)
        assertEquals(1, session.actionsCount)

        val state = repository.getState()
        assertNotNull(state.currentSession)
        assertEquals(123, state.currentSession?.messageId)
    }

    @Test
    fun recordMultipleTaps_appendsActions() {
        repository.recordTap(10, "❤️", 0, 1000L)
        val session = repository.recordTap(10, "❤️", 1, 1500L)
        assertEquals(2, session.actionsCount)
        assertEquals(listOf(0L, 500L), session.relativeTimeOffsetsMs)
    }

    @Test
    fun drainCurrentSession_returnsAndClears() {
        repository.recordTap(10, "❤️", 0, 1000L)
        val drained = repository.drainCurrentSession()
        assertNotNull(drained)
        assertNull(repository.getState().currentSession)
    }

    @Test
    fun effectLifecycle_startUpdateRemove() = runBlocking {
        val effect = EmojiEffectItem(id = "e1", messageId = 5, emoticon = "🎉")
        repository.startEffect(effect)

        var state = repository.observeState().first()
        assertFalse(state.isIdle)
        assertEquals(1, state.activeEffects.size)

        repository.updateEffectProgress("e1", 0.5f)
        assertEquals(0.5f, repository.getState().activeEffects[0].progress)

        repository.removeEffect("e1")
        state = repository.getState()
        assertTrue(state.isIdle)
        assertTrue(state.activeEffects.isEmpty())
    }

    @Test
    fun animationIndex_trackingPerDocument() {
        assertEquals(0, repository.getLastAnimationIndex(999L))
        repository.updateLastAnimationIndex(999L, 2)
        assertEquals(2, repository.getLastAnimationIndex(999L))
    }

    @Test
    fun clear_resetsAll() {
        repository.startEffect(EmojiEffectItem(id = "e2", messageId = 1))
        repository.recordTap(1, "👍", 0, 100L)
        repository.clear()

        val state = repository.getState()
        assertTrue(state.isIdle)
        assertTrue(state.activeEffects.isEmpty())
        assertNull(state.currentSession)
    }
}
