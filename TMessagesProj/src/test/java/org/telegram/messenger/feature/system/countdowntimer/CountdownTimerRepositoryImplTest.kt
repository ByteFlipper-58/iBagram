package org.telegram.messenger.feature.system.countdowntimer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.countdowntimer.data.datasource.CountdownTimerLocalDataSource
import org.telegram.messenger.feature.system.countdowntimer.data.datasource.CountdownTimerRemoteDataSource
import org.telegram.messenger.feature.system.countdowntimer.data.repository.CountdownTimerRepositoryImpl
import org.telegram.messenger.feature.system.countdowntimer.domain.model.CountdownTimerStatus

@OptIn(ExperimentalCoroutinesApi::class)
class CountdownTimerRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var localDataSource: CountdownTimerLocalDataSource
    private lateinit var remoteDataSource: CountdownTimerRemoteDataSource
    private lateinit var repository: CountdownTimerRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = CountdownTimerLocalDataSource(
            dispatcher = testDispatcher,
            scope = testScope
        )
        remoteDataSource = CountdownTimerRemoteDataSource(currentAccount = 0)
        repository = CountdownTimerRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun testStartTimer() {
        val tick = repository.start("timer_1", 60)
        assertNotNull(tick)
        assertEquals("timer_1", tick.timerId)
        assertEquals(60L, tick.remainingSeconds)
        assertEquals(60L, tick.initialSeconds)
        assertEquals(CountdownTimerStatus.RUNNING, tick.status)
        assertTrue(repository.isRunning("timer_1"))
    }

    @Test
    fun testStopTimer() {
        repository.start("timer_2", 30)
        assertTrue(repository.isRunning("timer_2"))

        val stopped = repository.stop("timer_2")
        assertNotNull(stopped)
        assertEquals(CountdownTimerStatus.IDLE, stopped!!.status)
        assertFalse(repository.isRunning("timer_2"))
    }

    @Test
    fun testPauseAndResumeTimer() {
        repository.start("timer_3", 100)
        val paused = repository.pause("timer_3")
        assertNotNull(paused)
        assertEquals(CountdownTimerStatus.PAUSED, paused!!.status)
        assertFalse(repository.isRunning("timer_3"))

        val resumed = repository.resume("timer_3")
        assertNotNull(resumed)
        assertEquals(CountdownTimerStatus.RUNNING, resumed!!.status)
        assertTrue(repository.isRunning("timer_3"))
    }

    @Test
    fun testManualTick() {
        repository.start("timer_4", 10)
        val ticked = repository.tick("timer_4", 3)
        assertNotNull(ticked)
        assertEquals(7L, ticked!!.remainingSeconds)
        assertEquals(CountdownTimerStatus.RUNNING, ticked.status)

        // Tick past zero
        val finished = repository.tick("timer_4", 10)
        assertNotNull(finished)
        assertEquals(0L, finished!!.remainingSeconds)
        assertEquals(CountdownTimerStatus.FINISHED, finished.status)
        assertTrue(finished.isFinished)
    }

    @Test
    fun testClearAll() {
        repository.start("t1", 10)
        repository.start("t2", 20)
        assertEquals(2, repository.observeState().value.activeTimers.size)

        val cleared = repository.clearAll()
        assertTrue(cleared.activeTimers.isEmpty())
        assertNull(repository.getTimer("t1"))
        assertNull(repository.getTimer("t2"))
    }

    @Test
    fun testObserveStateEmitsUpdates() = runTest(testDispatcher) {
        val initial = repository.observeState().first()
        assertTrue(initial.activeTimers.isEmpty())

        repository.start("obs_timer", 50)
        val state = repository.observeState().value
        assertEquals(1, state.activeTimers.size)
        assertEquals(50L, state.activeTimers["obs_timer"]?.remainingSeconds)
    }

    @Test
    fun testRemoteDataSourceReturnsResult() = runTest(testDispatcher) {
        val result = remoteDataSource.fetchServerTimeOffset()
        assertTrue(result.isSuccess)
        assertEquals(0L, result.getOrNull())
    }
}
