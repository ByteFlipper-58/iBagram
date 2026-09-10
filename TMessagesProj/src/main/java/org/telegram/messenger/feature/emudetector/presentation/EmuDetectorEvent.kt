package org.telegram.messenger.feature.emudetector.presentation

import org.telegram.messenger.feature.emudetector.domain.model.EmulatorDetectorConfig

sealed interface EmuDetectorEvent {
    data class RunDetection(val forceRefresh: Boolean = false) : EmuDetectorEvent
    data class UpdateConfig(val config: EmulatorDetectorConfig) : EmuDetectorEvent
    data class AddCustomPackage(val packageName: String) : EmuDetectorEvent
    data object ClearCache : EmuDetectorEvent
    data object DismissError : EmuDetectorEvent
}
