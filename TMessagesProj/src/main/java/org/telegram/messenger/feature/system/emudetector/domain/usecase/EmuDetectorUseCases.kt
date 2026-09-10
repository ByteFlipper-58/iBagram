package org.telegram.messenger.feature.system.emudetector.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.system.emudetector.domain.model.DetectionIndicator
import org.telegram.messenger.feature.system.emudetector.domain.model.EmulatorDetectorConfig
import org.telegram.messenger.feature.system.emudetector.domain.model.EmulatorDiagnostics
import org.telegram.messenger.feature.system.emudetector.domain.model.EmulatorHeuristics
import org.telegram.messenger.feature.system.emudetector.domain.model.EnvironmentVerdict
import org.telegram.messenger.feature.system.emudetector.domain.repository.EmuDetectorRepository

class DetectEnvironmentUseCase(
    private val repository: EmuDetectorRepository
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): EmulatorDiagnostics {
        return repository.detectEnvironment(forceRefresh)
    }
}

class IsEmulatorUseCase(
    private val repository: EmuDetectorRepository
) {
    operator fun invoke(): Boolean {
        return repository.isEmulator()
    }
}

class GetCachedDiagnosticsUseCase(
    private val repository: EmuDetectorRepository
) {
    operator fun invoke(): EmulatorDiagnostics? {
        return repository.getCachedDiagnostics()
    }
}

class ObserveDiagnosticsUseCase(
    private val repository: EmuDetectorRepository
) {
    operator fun invoke(): StateFlow<EmulatorDiagnostics?> {
        return repository.observeDiagnostics()
    }
}

class ObserveIsEmulatorUseCase(
    private val repository: EmuDetectorRepository
) {
    operator fun invoke(): StateFlow<Boolean> {
        return repository.observeIsEmulator()
    }
}

class GetDetectorConfigUseCase(
    private val repository: EmuDetectorRepository
) {
    operator fun invoke(): EmulatorDetectorConfig {
        return repository.getConfig()
    }
}

class UpdateDetectorConfigUseCase(
    private val repository: EmuDetectorRepository
) {
    operator fun invoke(config: EmulatorDetectorConfig) {
        repository.updateConfig(config)
    }
}

class AddCustomPackageNameUseCase(
    private val repository: EmuDetectorRepository
) {
    operator fun invoke(packageName: String) {
        repository.addCustomPackage(packageName)
    }
}

class ClearDetectorCacheUseCase(
    private val repository: EmuDetectorRepository
) {
    operator fun invoke() {
        repository.clearCache()
    }
}

class CalculateConfidenceScoreUseCase {
    operator fun invoke(indicators: List<DetectionIndicator>): Int {
        return EmulatorHeuristics.calculateConfidenceScore(indicators)
    }
}

class EvaluateEnvironmentVerdictUseCase {
    operator fun invoke(
        confidenceScore: Int,
        threshold: Int,
        hasCriticalIndicator: Boolean
    ): EnvironmentVerdict {
        return EmulatorHeuristics.evaluateVerdict(confidenceScore, threshold, hasCriticalIndicator)
    }
}
