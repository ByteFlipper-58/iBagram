package org.telegram.messenger.feature.refreshrate.domain.usecase

import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.refreshrate.domain.repository.RefreshRateRepository

class RecordFrameMetricUseCase(
    private val repository: RefreshRateRepository
) {
    operator fun invoke(durationNs: Long): Result<Unit> {
        return repository.recordFrameDuration(durationNs)
    }
}
