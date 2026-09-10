package org.telegram.messenger.feature.windowvisibility.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.windowvisibility.domain.model.WindowVisibilityController
import org.telegram.messenger.feature.windowvisibility.domain.model.WindowVisibilityState
import org.telegram.messenger.feature.windowvisibility.domain.repository.WindowVisibilityRepository

class RequestHideWindowUseCase(private val repository: WindowVisibilityRepository) {
    operator fun invoke(reasonTag: String, description: String = ""): WindowVisibilityState {
        return repository.requestHide(reasonTag, description)
    }
}

class ReleaseHideWindowUseCase(private val repository: WindowVisibilityRepository) {
    operator fun invoke(reasonTag: String): WindowVisibilityState {
        return repository.releaseHide(reasonTag)
    }
}

class ToggleWindowHideUseCase(private val repository: WindowVisibilityRepository) {
    operator fun invoke(reasonTag: String, hide: Boolean, description: String = ""): WindowVisibilityState {
        return repository.toggleHide(reasonTag, hide, description)
    }
}

class CheckIsWindowVisibleUseCase(private val repository: WindowVisibilityRepository) {
    operator fun invoke(): Boolean {
        return repository.isVisible()
    }
}

class GetWindowVisibilityStateUseCase(private val repository: WindowVisibilityRepository) {
    operator fun invoke(): WindowVisibilityState {
        return repository.getCurrentState()
    }
}

class GetActiveHideReasonsUseCase(private val repository: WindowVisibilityRepository) {
    operator fun invoke(): Set<String> {
        return repository.getActiveReasons()
    }
}

class ResetWindowVisibilityUseCase(private val repository: WindowVisibilityRepository) {
    operator fun invoke(): WindowVisibilityState {
        return repository.resetAllReasons()
    }
}

class ObserveWindowVisibilityStateUseCase(private val repository: WindowVisibilityRepository) {
    operator fun invoke(): StateFlow<WindowVisibilityState> {
        return repository.observeState()
    }
}

class ObserveWindowVisibilityChangesUseCase(private val repository: WindowVisibilityRepository) {
    operator fun invoke(): Flow<Boolean> {
        return repository.observeVisibilityChanges()
    }
}

class CreateVisibilityControllerUseCase(private val repository: WindowVisibilityRepository) {
    operator fun invoke(reasonTag: String): WindowVisibilityController {
        return repository.obtainController(reasonTag)
    }
}
