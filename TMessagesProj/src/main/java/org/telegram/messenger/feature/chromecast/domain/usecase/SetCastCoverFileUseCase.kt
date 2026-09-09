package org.telegram.messenger.feature.chromecast.domain.usecase

import org.telegram.messenger.feature.chromecast.domain.repository.ChromecastRepository
import java.io.File

class SetCastCoverFileUseCase(
    private val repository: ChromecastRepository
) {
    suspend operator fun invoke(file: File?): Result<String?> = repository.setCoverFile(file)
}
