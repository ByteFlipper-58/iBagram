package org.telegram.messenger.feature.media.voip

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.media.voip.data.datasource.VoIPLocalDataSource
import org.telegram.messenger.feature.media.voip.data.datasource.VoIPRemoteDataSource
import org.telegram.messenger.feature.media.voip.data.repository.VoIPRepositoryImpl
import org.telegram.messenger.feature.media.voip.domain.model.CallState

class VoIPRepositoryImplTest {

    private lateinit var localDataSource: VoIPLocalDataSource
    private lateinit var remoteDataSource: VoIPRemoteDataSource
    private lateinit var repository: VoIPRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = VoIPLocalDataSource(0)
        remoteDataSource = VoIPRemoteDataSource(0)
        repository = VoIPRepositoryImpl(
            currentAccount = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource
        )
    }

    @Test
    fun startCall_initializesOutgoingCall() = runBlocking {
        val result = repository.startCall(userId = 555L, isVideo = true)
        assertTrue(result is Result.Success)

        val call = repository.getCurrentCall()
        assertNotNull(call)
        assertEquals(555L, call?.userId)
        assertTrue(call?.isVideo == true)
        assertTrue(call?.isOutgoing == true)
        assertEquals(CallState.REQUESTING, call?.state)
    }

    @Test
    fun acceptCall_transitionsStateToActive() = runBlocking {
        repository.startCall(userId = 123L, isVideo = false)
        val result = repository.acceptCall()
        assertTrue(result is Result.Success)

        val call = repository.observeCurrentCall().first()
        assertEquals(CallState.ACTIVE, call?.state)
    }

    @Test
    fun declineCall_transitionsStateToEnded() = runBlocking {
        repository.startCall(userId = 123L, isVideo = false)
        val result = repository.declineCall()
        assertTrue(result is Result.Success)

        val call = repository.getCurrentCall()
        assertEquals(CallState.ENDED, call?.state)
    }

    @Test
    fun hangUp_transitionsStateToEnded() = runBlocking {
        repository.startCall(userId = 123L, isVideo = false)
        repository.acceptCall()
        val result = repository.hangUp()
        assertTrue(result is Result.Success)

        val call = repository.getCurrentCall()
        assertEquals(CallState.ENDED, call?.state)
    }

    @Test
    fun toggleMute_and_toggleSpeakerphone_togglesFlags() = runBlocking {
        repository.startCall(userId = 123L, isVideo = false)

        val muteResult = repository.toggleMute()
        assertTrue(muteResult is Result.Success)
        assertTrue((muteResult as Result.Success).data)
        assertTrue(repository.getCurrentCall()?.isMuted == true)

        val speakerResult = repository.toggleSpeakerphone()
        assertTrue(speakerResult is Result.Success)
        assertTrue((speakerResult as Result.Success).data)
        assertTrue(repository.getCurrentCall()?.isSpeakerphoneOn == true)
    }
}
