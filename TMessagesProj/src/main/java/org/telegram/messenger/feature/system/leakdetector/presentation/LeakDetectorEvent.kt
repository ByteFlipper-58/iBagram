package org.telegram.messenger.feature.system.leakdetector.presentation

import org.telegram.messenger.feature.system.leakdetector.domain.model.LeakDetectorConfig

/**
 * MVI Events for memory leak detector.
 */
sealed class LeakDetectorEvent {
    data class Start(val config: LeakDetectorConfig = LeakDetectorConfig()) : LeakDetectorEvent()
    object Stop : LeakDetectorEvent()
    data class Track(val tagOrClassName: String, val instance: Any) : LeakDetectorEvent()
    object TriggerCheck : LeakDetectorEvent()
    data class Confirm(val className: String) : LeakDetectorEvent()
    object Reset : LeakDetectorEvent()
    object DismissError : LeakDetectorEvent()
}
