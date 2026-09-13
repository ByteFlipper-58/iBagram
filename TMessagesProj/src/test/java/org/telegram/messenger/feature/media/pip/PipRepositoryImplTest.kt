package org.telegram.messenger.feature.media.pip

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.media.pip.data.datasource.PipLocalDataSource
import org.telegram.messenger.feature.media.pip.data.datasource.PipRemoteDataSource
import org.telegram.messenger.feature.media.pip.data.repository.PipRepositoryImpl
import org.telegram.messenger.feature.media.pip.domain.model.PipSourceModel
import org.telegram.messenger.feature.media.pip.domain.model.PipState

@OptIn(ExperimentalCoroutinesApi::class)
class PipRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var localDataSource: PipLocalDataSource
    private lateinit var remoteDataSource: PipRemoteDataSource
    private lateinit var repository: PipRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        localDataSource = PipLocalDataSource(0)
        remoteDataSource = PipRemoteDataSource(0)
        repository = PipRepositoryImpl(
            currentAccount = 0,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            mainDispatcher = Dispatchers.Unconfined
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialSessionInfo() {
        val session = repository.getSessionInfo()
        assertNull(session.activeSource)
        assertEquals(PipState.IDLE, session.pipState)
        assertFalse(repository.canEnterPip())
    }

    @Test
    fun testRegisterAndUnregisterSource() {
        val source = PipSourceModel(
            tag = "video_player",
            priority = 10,
            isAvailable = true,
            isAttachedToPip = true,
            needsMediaSession = true,
            aspectRatioWidth = 16,
            aspectRatioHeight = 9
        )

        repository.registerSource(source)
        val sessionWithSource = repository.getSessionInfo()
        assertNotNull(sessionWithSource.activeSource)
        assertEquals("video_player", sessionWithSource.activeSource?.tag)
        assertTrue(repository.canEnterPip())

        repository.unregisterSource("video_player")
        val sessionEmpty = repository.getSessionInfo()
        assertNull(sessionEmpty.activeSource)
        assertFalse(repository.canEnterPip())
    }

    @Test
    fun testUpdateSourceAvailability() {
        val source = PipSourceModel(
            tag = "call_pip",
            priority = 20,
            isAvailable = true,
            isAttachedToPip = false
        )

        repository.registerSource(source)
        assertTrue(repository.canEnterPip())

        repository.updateSourceAvailability("call_pip", false)
        val session = repository.getSessionInfo()
        assertNull(session.activeSource)
        assertFalse(repository.canEnterPip())
    }

    @Test
    fun testUpdatePipState() {
        repository.updatePipState(PipState.IN_PIP)
        assertEquals(PipState.IN_PIP, repository.getSessionInfo().pipState)

        repository.updatePipState(PipState.STASHED)
        assertEquals(PipState.STASHED, repository.getSessionInfo().pipState)
    }

    @Test
    fun testTriggerPipAction() {
        repository.triggerPipAction("custom_tag", 101)
        val lastAction = repository.getSessionInfo().lastAction
        assertNotNull(lastAction)
        assertEquals("custom_tag", lastAction?.first)
        assertEquals(101, lastAction?.second)
    }

    @Test
    fun testObserveSessionInfo() = runTest {
        val initial = repository.observeSessionInfo().first()
        assertNotNull(initial)
        assertEquals(PipState.IDLE, initial.pipState)
    }
}
