package org.telegram.messenger.feature.countdowntimer.presentation

/**
 * MVI Events for countdown timer manipulation.
 */
sealed class CountdownTimerEvent {
    data class Start(val timerId: String, val seconds: Long) : CountdownTimerEvent()
    data class Stop(val timerId: String) : CountdownTimerEvent()
    data class Pause(val timerId: String) : CountdownTimerEvent()
    data class Resume(val timerId: String) : CountdownTimerEvent()
    data class TickManual(val timerId: String, val stepSeconds: Long = 1) : CountdownTimerEvent()
    data class SelectTimer(val timerId: String) : CountdownTimerEvent()
    object ClearAll : CountdownTimerEvent()
    object DismissError : CountdownTimerEvent()
}
