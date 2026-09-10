package org.telegram.messenger.feature.leakdetector.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.leakdetector.domain.model.LeakDetectorConfig
import org.telegram.messenger.feature.leakdetector.domain.model.LeakDetectorState
import org.telegram.messenger.feature.leakdetector.domain.model.LeakReport
import org.telegram.messenger.feature.leakdetector.domain.model.TrackedClassStats
import org.telegram.messenger.feature.leakdetector.domain.repository.LeakDetectorRepository

class StartLeakDetectionUseCase(private val repository: LeakDetectorRepository) {
    operator fun invoke(config: LeakDetectorConfig = LeakDetectorConfig()) {
        repository.start(config)
    }
}

class StopLeakDetectionUseCase(private val repository: LeakDetectorRepository) {
    operator fun invoke() {
        repository.stop()
    }
}

class TrackInstanceUseCase(private val repository: LeakDetectorRepository) {
    operator fun invoke(tagOrClassName: String, instance: Any) {
        repository.track(tagOrClassName, instance)
    }
}

class TriggerLeakCheckUseCase(private val repository: LeakDetectorRepository) {
    operator fun invoke(): List<LeakReport> {
        return repository.triggerCheck()
    }
}

class ConfirmLeakUseCase(private val repository: LeakDetectorRepository) {
    operator fun invoke(className: String): LeakReport? {
        return repository.confirmLeak(className)
    }
}

class GetTrackedClassesStatsUseCase(private val repository: LeakDetectorRepository) {
    operator fun invoke(): List<TrackedClassStats> {
        return repository.getTrackedStats()
    }
}

class GetConfirmedLeaksUseCase(private val repository: LeakDetectorRepository) {
    operator fun invoke(): List<LeakReport> {
        return repository.getReportedLeaks()
    }
}

class ResetLeakDetectorUseCase(private val repository: LeakDetectorRepository) {
    operator fun invoke() {
        repository.reset()
    }
}

class ObserveLeakDetectorStateUseCase(private val repository: LeakDetectorRepository) {
    operator fun invoke(): StateFlow<LeakDetectorState> {
        return repository.observeState()
    }
}

class ObserveConfirmedLeaksUseCase(private val repository: LeakDetectorRepository) {
    operator fun invoke(): Flow<LeakReport> {
        return repository.observeLeaks()
    }
}
