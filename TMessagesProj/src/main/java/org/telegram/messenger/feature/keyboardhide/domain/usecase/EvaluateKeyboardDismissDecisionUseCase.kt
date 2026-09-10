package org.telegram.messenger.feature.keyboardhide.domain.usecase

import org.telegram.messenger.feature.keyboardhide.domain.model.KeyboardDismissDecision
import org.telegram.messenger.feature.keyboardhide.domain.repository.KeyboardHideRepository

class EvaluateKeyboardDismissDecisionUseCase(
    private val repository: KeyboardHideRepository
) {
    operator fun invoke(
        currentProgress: Float,
        lastDifferentProgress: Float,
        velocityY: Float = 0f
    ): KeyboardDismissDecision {
        return repository.evaluateDismissDecision(currentProgress, lastDifferentProgress, velocityY)
    }
}
