package org.telegram.messenger.feature.media.pip.domain.usecase

import org.telegram.messenger.feature.media.pip.domain.repository.PipRepository

class UpdatePipSourceStateUseCase(
    private val repository: PipRepository
) {
    fun setAvailability(tag: String, isAvailable: Boolean) {
        repository.updateSourceAvailability(tag, isAvailable)
    }

    fun setAspectRatio(tag: String, width: Int, height: Int) {
        repository.updateSourceRatio(tag, width, height)
    }

    fun setAttached(tag: String, isAttached: Boolean) {
        repository.updateSourceAttached(tag, isAttached)
    }
}
