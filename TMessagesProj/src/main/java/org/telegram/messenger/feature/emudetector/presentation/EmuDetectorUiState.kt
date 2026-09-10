package org.telegram.messenger.feature.emudetector.presentation

import org.telegram.messenger.feature.emudetector.domain.model.DetectionIndicator
import org.telegram.messenger.feature.emudetector.domain.model.EmulatorDetectorConfig
import org.telegram.messenger.feature.emudetector.domain.model.EmulatorDiagnostics
import org.telegram.messenger.feature.emudetector.domain.model.EmulatorType
import org.telegram.messenger.feature.emudetector.domain.model.EnvironmentVerdict

data class EmuDetectorUiState(
    val diagnostics: EmulatorDiagnostics? = null,
    val config: EmulatorDetectorConfig = EmulatorDetectorConfig(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val isEmulator: Boolean
        get() = diagnostics?.isEmulator ?: false

    val verdict: EnvironmentVerdict
        get() = diagnostics?.verdict ?: EnvironmentVerdict.PHYSICAL_DEVICE

    val confidenceScore: Int
        get() = diagnostics?.confidenceScore ?: 0

    val triggeredIndicators: List<DetectionIndicator>
        get() = diagnostics?.triggeredIndicators ?: emptyList()

    val detectedTypes: List<EmulatorType>
        get() = diagnostics?.detectedTypes ?: emptyList()

    val isEvaluated: Boolean
        get() = diagnostics != null

    val isPhysicalDevice: Boolean
        get() = verdict == EnvironmentVerdict.PHYSICAL_DEVICE
}
