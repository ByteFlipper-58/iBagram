package org.telegram.messenger.feature.camera.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.camera.domain.model.CameraStateModel
import org.telegram.messenger.feature.camera.domain.repository.CameraRepository

class ObserveCameraStateUseCase(
    private val repository: CameraRepository
) {
    operator fun invoke(): Flow<CameraStateModel> = repository.observeCameraState()
}
