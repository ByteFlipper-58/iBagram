package org.telegram.messenger.feature.system.countdowntimer.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.system.countdowntimer.domain.model.CountdownTimeComponents
import org.telegram.messenger.feature.system.countdowntimer.domain.model.CountdownTimerState
import org.telegram.messenger.feature.system.countdowntimer.domain.model.CountdownTimerTick
import org.telegram.messenger.feature.system.countdowntimer.domain.repository.CountdownTimerRepository

class StartCountdownTimerUseCase(private val repository: CountdownTimerRepository) {
    operator fun invoke(timerId: String, seconds: Long): CountdownTimerTick {
        return repository.start(timerId, seconds)
    }
}

class StopCountdownTimerUseCase(private val repository: CountdownTimerRepository) {
    operator fun invoke(timerId: String): CountdownTimerTick? {
        return repository.stop(timerId)
    }
}

class PauseCountdownTimerUseCase(private val repository: CountdownTimerRepository) {
    operator fun invoke(timerId: String): CountdownTimerTick? {
        return repository.pause(timerId)
    }
}

class ResumeCountdownTimerUseCase(private val repository: CountdownTimerRepository) {
    operator fun invoke(timerId: String): CountdownTimerTick? {
        return repository.resume(timerId)
    }
}

class GetCountdownTimerUseCase(private val repository: CountdownTimerRepository) {
    operator fun invoke(timerId: String): CountdownTimerTick? {
        return repository.getTimer(timerId)
    }
}

class IsCountdownTimerRunningUseCase(private val repository: CountdownTimerRepository) {
    operator fun invoke(timerId: String): Boolean {
        return repository.isRunning(timerId)
    }
}

class TickCountdownTimerUseCase(private val repository: CountdownTimerRepository) {
    operator fun invoke(timerId: String, stepSeconds: Long = 1): CountdownTimerTick? {
        return repository.tick(timerId, stepSeconds)
    }
}

class ClearAllCountdownTimersUseCase(private val repository: CountdownTimerRepository) {
    operator fun invoke(): CountdownTimerState {
        return repository.clearAll()
    }
}

class ObserveCountdownTimerUseCase(private val repository: CountdownTimerRepository) {
    operator fun invoke(timerId: String): Flow<CountdownTimerTick> {
        return repository.observeTimer(timerId)
    }
}

class ObserveCountdownStateUseCase(private val repository: CountdownTimerRepository) {
    operator fun invoke(): StateFlow<CountdownTimerState> {
        return repository.observeState()
    }
}

class DecomposeCountdownTimeUseCase {
    operator fun invoke(totalSeconds: Long): CountdownTimeComponents {
        val safeSeconds = totalSeconds.coerceAtLeast(0)
        val days = safeSeconds / 86400
        val remainingAfterDays = safeSeconds % 86400
        val hours = remainingAfterDays / 3600
        val remainingAfterHours = remainingAfterDays % 3600
        val minutes = remainingAfterHours / 60
        val seconds = remainingAfterHours % 60

        return CountdownTimeComponents(
            days = days,
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            totalSeconds = safeSeconds
        )
    }
}

class FormatCountdownTimeUseCase(
    private val decomposeUseCase: DecomposeCountdownTimeUseCase = DecomposeCountdownTimeUseCase()
) {
    operator fun invoke(totalSeconds: Long, includeDaysIfPresent: Boolean = true): String {
        val components = decomposeUseCase(totalSeconds)
        return when {
            components.days > 0 && includeDaysIfPresent -> {
                String.format("%dd %02d:%02d:%02d", components.days, components.hours, components.minutes, components.seconds)
            }
            components.hours > 0 -> {
                String.format("%02d:%02d:%02d", components.hours, components.minutes, components.seconds)
            }
            else -> {
                String.format("%02d:%02d", components.minutes, components.seconds)
            }
        }
    }
}
