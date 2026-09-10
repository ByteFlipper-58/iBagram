package org.telegram.messenger.feature.media.camera.domain.usecase

import org.telegram.messenger.feature.media.camera.domain.model.CameraResolutionModel
import org.telegram.messenger.feature.media.camera.domain.repository.CameraRepository

class ChooseOptimalResolutionUseCase(
    private val repository: CameraRepository
) {
    operator fun invoke(
        resolutions: List<CameraResolutionModel>,
        targetWidth: Int,
        targetHeight: Int,
        targetAspectWidth: Int,
        targetAspectHeight: Int,
        notBigger: Boolean = false
    ): CameraResolutionModel? {
        return repository.chooseOptimalResolution(
            resolutions = resolutions,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            targetAspectWidth = targetAspectWidth,
            targetAspectHeight = targetAspectHeight,
            notBigger = notBigger
        )
    }
}
