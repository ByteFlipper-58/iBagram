package org.telegram.messenger.feature.countdowntimer.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.countdowntimer.data.mapper.CountdownTimerMapper
import org.telegram.messenger.feature.countdowntimer.domain.model.CountdownTimerState
import org.telegram.messenger.feature.countdowntimer.domain.model.CountdownTimerStatus
import org.telegram.messenger.feature.countdowntimer.domain.model.CountdownTimerTick
import org.telegram.messenger.feature.countdowntimer.domain.repository.CountdownTimerRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe repository implementing [CountdownTimerRepository].
 * Manages concurrent countdown timers with reactive StateFlow/SharedFlow streams.
 */
class LegacyCountdownTimerRepository(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
) : CountdownTimerRepository {

    private val lock = Any()
    private val timers = ConcurrentHashMap<String, CountdownTimerTick>()
    private val jobs = ConcurrentHashMap<String, Job>()

    private val _stateFlow = MutableStateFlow(CountdownTimerState())
    val stateFlow: StateFlow<CountdownTimerState> = _stateFlow.asStateFlow()

    private val _tickFlow = MutableSharedFlow<CountdownTimerTick>(replay = 1, extraBufferCapacity = 64)

    override fun start(timerId: String, seconds: Long): CountdownTimerTick {
        val safeSeconds = seconds.coerceAtLeast(0)
        val tick = CountdownTimerMapper.buildTick(
            timerId = timerId,
            remainingSeconds = safeSeconds,
            initialSeconds = safeSeconds,
            status = if (safeSeconds > 0) CountdownTimerStatus.RUNNING else CountdownTimerStatus.FINISHED
        )

        synchronized(lock) {
            jobs.remove(timerId)?.cancel()
            timers[timerId] = tick
            updateStateLocked()
        }

        _tickFlow.tryEmit(tick)

        if (safeSeconds > 0) {
            val job = scope.launch {
                while (isActive) {
                    delay(1000)
                    val nextTick = tick(timerId, 1)
                    if (nextTick == null || nextTick.isFinished) {
                        break
                    }
                }
            }
            jobs[timerId] = job
        }

        return tick
    }

    override fun stop(timerId: String): CountdownTimerTick? {
        val tick = synchronized(lock) {
            jobs.remove(timerId)?.cancel()
            val existing = timers[timerId] ?: return null
            val stopped = existing.copy(status = CountdownTimerStatus.IDLE)
            timers[timerId] = stopped
            updateStateLocked()
            stopped
        }
        _tickFlow.tryEmit(tick)
        return tick
    }

    override fun pause(timerId: String): CountdownTimerTick? {
        val tick = synchronized(lock) {
            jobs.remove(timerId)?.cancel()
            val existing = timers[timerId] ?: return null
            if (existing.status != CountdownTimerStatus.RUNNING) return existing
            val paused = existing.copy(status = CountdownTimerStatus.PAUSED)
            timers[timerId] = paused
            updateStateLocked()
            paused
        }
        _tickFlow.tryEmit(tick)
        return tick
    }

    override fun resume(timerId: String): CountdownTimerTick? {
        val existing = timers[timerId] ?: return null
        if (existing.status != CountdownTimerStatus.PAUSED || existing.remainingSeconds <= 0) {
            return existing
        }

        val resumed = existing.copy(status = CountdownTimerStatus.RUNNING)
        synchronized(lock) {
            jobs.remove(timerId)?.cancel()
            timers[timerId] = resumed
            updateStateLocked()
        }

        _tickFlow.tryEmit(resumed)

        val job = scope.launch {
            while (isActive) {
                delay(1000)
                val nextTick = tick(timerId, 1)
                if (nextTick == null || nextTick.isFinished) {
                    break
                }
            }
        }
        jobs[timerId] = job

        return resumed
    }

    override fun getTimer(timerId: String): CountdownTimerTick? {
        return timers[timerId]
    }

    override fun isRunning(timerId: String): Boolean {
        return timers[timerId]?.isRunning == true
    }

    override fun tick(timerId: String, stepSeconds: Long): CountdownTimerTick? {
        val nextTick = synchronized(lock) {
            val existing = timers[timerId] ?: return null
            if (existing.status != CountdownTimerStatus.RUNNING) return existing

            val remaining = (existing.remainingSeconds - stepSeconds).coerceAtLeast(0)
            val updated = CountdownTimerMapper.buildTick(
                timerId = timerId,
                remainingSeconds = remaining,
                initialSeconds = existing.initialSeconds,
                status = if (remaining <= 0) CountdownTimerStatus.FINISHED else CountdownTimerStatus.RUNNING
            )
            timers[timerId] = updated
            if (updated.isFinished) {
                jobs.remove(timerId)?.cancel()
            }
            updateStateLocked()
            updated
        }

        _tickFlow.tryEmit(nextTick)
        return nextTick
    }

    override fun clearAll(): CountdownTimerState {
        val newState = synchronized(lock) {
            jobs.values.forEach { it.cancel() }
            jobs.clear()
            timers.clear()
            val state = CountdownTimerState(emptyMap())
            _stateFlow.value = state
            state
        }
        return newState
    }

    override fun observeTimer(timerId: String): Flow<CountdownTimerTick> {
        return _tickFlow.asSharedFlow().filter { it.timerId == timerId }
    }

    override fun observeState(): StateFlow<CountdownTimerState> {
        return stateFlow
    }

    private fun updateStateLocked() {
        _stateFlow.value = CountdownTimerState(HashMap(timers))
    }
}
