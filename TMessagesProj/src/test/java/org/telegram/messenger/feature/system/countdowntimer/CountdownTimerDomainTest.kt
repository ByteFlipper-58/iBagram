package org.telegram.messenger.feature.system.countdowntimer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.countdowntimer.data.mapper.CountdownTimerMapper
import org.telegram.messenger.feature.system.countdowntimer.data.repository.LegacyCountdownTimerRepository
import org.telegram.messenger.feature.system.countdowntimer.domain.model.CountdownTimerStatus
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.ClearAllCountdownTimersUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.DecomposeCountdownTimeUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.FormatCountdownTimeUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.GetCountdownTimerUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.IsCountdownTimerRunningUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.ObserveCountdownStateUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.ObserveCountdownTimerUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.PauseCountdownTimerUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.ResumeCountdownTimerUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.StartCountdownTimerUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.StopCountdownTimerUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.TickCountdownTimerUseCase
import org.telegram.messenger.feature.system.countdowntimer.presentation.CountdownTimerEvent
import org.telegram.messenger.feature.system.countdowntimer.presentation.CountdownTimerViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class CountdownTimerDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: LegacyCountdownTimerRepository
    private lateinit var startCountdownTimerUseCase: StartCountdownTimerUseCase
    private lateinit var stopCountdownTimerUseCase: StopCountdownTimerUseCase
    private lateinit var pauseCountdownTimerUseCase: PauseCountdownTimerUseCase
    private lateinit var resumeCountdownTimerUseCase: ResumeCountdownTimerUseCase
    private lateinit var getCountdownTimerUseCase: GetCountdownTimerUseCase
    private lateinit var isCountdownTimerRunningUseCase: IsCountdownTimerRunningUseCase
    private lateinit var tickCountdownTimerUseCase: TickCountdownTimerUseCase
    private lateinit var clearAllCountdownTimersUseCase: ClearAllCountdownTimersUseCase
    private lateinit var observeCountdownTimerUseCase: ObserveCountdownTimerUseCase
    private lateinit var observeCountdownStateUseCase: ObserveCountdownStateUseCase
    private lateinit var decomposeCountdownTimeUseCase: DecomposeCountdownTimeUseCase
    private lateinit var formatCountdownTimeUseCase: FormatCountdownTimeUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = LegacyCountdownTimerRepository(dispatcher = testDispatcher)
        startCountdownTimerUseCase = StartCountdownTimerUseCase(repository)
        stopCountdownTimerUseCase = StopCountdownTimerUseCase(repository)
        pauseCountdownTimerUseCase = PauseCountdownTimerUseCase(repository)
        resumeCountdownTimerUseCase = ResumeCountdownTimerUseCase(repository)
        getCountdownTimerUseCase = GetCountdownTimerUseCase(repository)
        isCountdownTimerRunningUseCase = IsCountdownTimerRunningUseCase(repository)
        tickCountdownTimerUseCase = TickCountdownTimerUseCase(repository)
        clearAllCountdownTimersUseCase = ClearAllCountdownTimersUseCase(repository)
        observeCountdownTimerUseCase = ObserveCountdownTimerUseCase(repository)
        observeCountdownStateUseCase = ObserveCountdownStateUseCase(repository)
        decomposeCountdownTimeUseCase = DecomposeCountdownTimeUseCase()
        formatCountdownTimeUseCase = FormatCountdownTimeUseCase(decomposeCountdownTimeUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testFormatAndDecomposeCountdownTime() {
        // 90061s = 1d (86400) + 1h (3600) + 1m (60) + 1s
        val components = decomposeCountdownTimeUseCase(90061)
        assertEquals(1L, components.days)
        assertEquals(1L, components.hours)
        assertEquals(1L, components.minutes)
        assertEquals(1L, components.seconds)
        assertEquals(90061L, components.totalSeconds)

        val formattedDays = formatCountdownTimeUseCase(90061)
        assertEquals("1d 01:01:01", formattedDays)

        val formattedHours = formatCountdownTimeUseCase(3665)
        assertEquals("01:01:05", formattedHours)

        val formattedMinutes = formatCountdownTimeUseCase(125)
        assertEquals("02:05", formattedMinutes)

        val formattedZero = formatCountdownTimeUseCase(0)
        assertEquals("00:00", formattedZero)
    }

    @Test
    fun testStartAndTickTimer() = runTest {
        val initialTick = startCountdownTimerUseCase("gift_timer", 10)
        assertEquals("gift_timer", initialTick.timerId)
        assertEquals(10L, initialTick.remainingSeconds)
        assertEquals(10L, initialTick.initialSeconds)
        assertTrue(initialTick.isRunning)
        assertFalse(initialTick.isFinished)
        assertEquals(0f, initialTick.progress, 0.001f)

        // Manual tick by 3 seconds
        val tick1 = tickCountdownTimerUseCase("gift_timer", 3)
        assertNotNull(tick1)
        assertEquals(7L, tick1!!.remainingSeconds)
        assertEquals(0.3f, tick1.progress, 0.001f)
        assertTrue(tick1.isRunning)

        // Manual tick to 0
        val tick2 = tickCountdownTimerUseCase("gift_timer", 7)
        assertNotNull(tick2)
        assertEquals(0L, tick2!!.remainingSeconds)
        assertEquals(1.0f, tick2.progress, 0.001f)
        assertTrue(tick2.isFinished)
        assertFalse(tick2.isRunning)
        assertEquals(CountdownTimerStatus.FINISHED, tick2.status)
    }

    @Test
    fun testPauseResumeStopTimer() = runTest {
        startCountdownTimerUseCase("auction", 60)
        assertTrue(isCountdownTimerRunningUseCase("auction"))

        val paused = pauseCountdownTimerUseCase("auction")
        assertNotNull(paused)
        assertEquals(CountdownTimerStatus.PAUSED, paused!!.status)
        assertFalse(isCountdownTimerRunningUseCase("auction"))

        val resumed = resumeCountdownTimerUseCase("auction")
        assertNotNull(resumed)
        assertEquals(CountdownTimerStatus.RUNNING, resumed!!.status)
        assertTrue(isCountdownTimerRunningUseCase("auction"))

        val stopped = stopCountdownTimerUseCase("auction")
        assertNotNull(stopped)
        assertEquals(CountdownTimerStatus.IDLE, stopped!!.status)
        assertFalse(isCountdownTimerRunningUseCase("auction"))
    }

    @Test
    fun testMultipleTimersState() = runTest {
        startCountdownTimerUseCase("timer_a", 30)
        startCountdownTimerUseCase("timer_b", 45)

        val state = repository.getCurrentStateIfAvailable()
        assertEquals(2, state.activeTimers.size)

        val timerA = getCountdownTimerUseCase("timer_a")
        val timerB = getCountdownTimerUseCase("timer_b")
        assertNotNull(timerA)
        assertNotNull(timerB)
        assertEquals(30L, timerA!!.remainingSeconds)
        assertEquals(45L, timerB!!.remainingSeconds)
    }

    @Test
    fun testClearAllTimers() = runTest {
        startCountdownTimerUseCase("timer_1", 20)
        startCountdownTimerUseCase("timer_2", 40)
        assertEquals(2, repository.observeState().value.activeTimers.size)

        val clearedState = clearAllCountdownTimersUseCase()
        assertTrue(clearedState.activeTimers.isEmpty())
        assertFalse(isCountdownTimerRunningUseCase("timer_1"))
        assertFalse(isCountdownTimerRunningUseCase("timer_2"))
    }

    @Test
    fun testCountdownTimerViewModelFlow() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = CountdownTimerViewModel(
            startCountdownTimerUseCase = startCountdownTimerUseCase,
            stopCountdownTimerUseCase = stopCountdownTimerUseCase,
            pauseCountdownTimerUseCase = pauseCountdownTimerUseCase,
            resumeCountdownTimerUseCase = resumeCountdownTimerUseCase,
            getCountdownTimerUseCase = getCountdownTimerUseCase,
            isCountdownTimerRunningUseCase = isCountdownTimerRunningUseCase,
            tickCountdownTimerUseCase = tickCountdownTimerUseCase,
            clearAllCountdownTimersUseCase = clearAllCountdownTimersUseCase,
            observeCountdownStateUseCase = observeCountdownStateUseCase,
            formatCountdownTimeUseCase = formatCountdownTimeUseCase
        )

        assertEquals("00:00", viewModel.uiState.value.formattedTime)
        assertFalse(viewModel.uiState.value.isRunning)

        // Start timer
        viewModel.onEvent(CountdownTimerEvent.Start("poll_close", 15))
        testScheduler.runCurrent()

        assertEquals("poll_close", viewModel.uiState.value.currentTimerId)
        assertEquals(15L, viewModel.uiState.value.remainingSeconds)
        assertEquals("00:15", viewModel.uiState.value.formattedTime)
        assertTrue(viewModel.uiState.value.isRunning)

        // Manual tick
        viewModel.onEvent(CountdownTimerEvent.TickManual("poll_close", 5))
        testScheduler.runCurrent()

        assertEquals(10L, viewModel.uiState.value.remainingSeconds)
        assertEquals("00:10", viewModel.uiState.value.formattedTime)
        assertEquals(0.333f, viewModel.uiState.value.progress, 0.01f)

        // Stop timer
        viewModel.onEvent(CountdownTimerEvent.Stop("poll_close"))
        testScheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isRunning)
    }

    private fun LegacyCountdownTimerRepository.getCurrentStateIfAvailable(): org.telegram.messenger.feature.system.countdowntimer.domain.model.CountdownTimerState {
        return observeState().value
    }
}
