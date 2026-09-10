package org.telegram.messenger.feature.flagsecure.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.flagsecure.domain.model.SecurityEvaluationResult
import org.telegram.messenger.feature.flagsecure.domain.model.SecurityReasonType
import org.telegram.messenger.feature.flagsecure.domain.model.SecurityRuleSpec
import org.telegram.messenger.feature.flagsecure.domain.model.SecurityRulesEvaluator
import org.telegram.messenger.feature.flagsecure.domain.model.WindowSecurityState
import org.telegram.messenger.feature.flagsecure.domain.repository.FlagSecureRepository

class AttachSecurityReasonUseCase(
    private val repository: FlagSecureRepository
) {
    operator fun invoke(
        windowId: String,
        reason: SecurityReasonType,
        condition: (() -> Boolean)? = null
    ): WindowSecurityState {
        return repository.attachReason(windowId, reason, condition)
    }
}

class DetachSecurityReasonUseCase(
    private val repository: FlagSecureRepository
) {
    operator fun invoke(
        windowId: String,
        reason: SecurityReasonType
    ): WindowSecurityState {
        return repository.detachReason(windowId, reason)
    }
}

class InvalidateWindowSecurityUseCase(
    private val repository: FlagSecureRepository
) {
    operator fun invoke(windowId: String): WindowSecurityState {
        return repository.invalidateWindow(windowId)
    }
}

class IsWindowSecuredUseCase(
    private val repository: FlagSecureRepository
) {
    operator fun invoke(windowId: String): Boolean {
        return repository.isWindowSecured(windowId)
    }
}

class GetWindowSecurityStateUseCase(
    private val repository: FlagSecureRepository
) {
    operator fun invoke(windowId: String): WindowSecurityState {
        return repository.getWindowState(windowId)
    }
}

class GetAllWindowStatesUseCase(
    private val repository: FlagSecureRepository
) {
    operator fun invoke(): Map<String, WindowSecurityState> {
        return repository.getAllWindowStates()
    }
}

class ResetWindowSecurityUseCase(
    private val repository: FlagSecureRepository
) {
    operator fun invoke(windowId: String) {
        repository.resetWindow(windowId)
    }
}

class ObserveWindowStateUseCase(
    private val repository: FlagSecureRepository
) {
    operator fun invoke(windowId: String): StateFlow<WindowSecurityState> {
        return repository.observeWindowState(windowId)
    }
}

class ObserveAllWindowStatesUseCase(
    private val repository: FlagSecureRepository
) {
    operator fun invoke(): StateFlow<Map<String, WindowSecurityState>> {
        return repository.observeAllWindowStates()
    }
}

class EvaluateSecurityRuleUseCase {
    operator fun invoke(spec: SecurityRuleSpec): SecurityEvaluationResult {
        return SecurityRulesEvaluator.evaluate(spec)
    }
}
