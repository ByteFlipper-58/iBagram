package org.telegram.messenger.feature.contentpreview.domain.usecase

import org.telegram.messenger.feature.contentpreview.domain.repository.ContentPreviewRepository

/**
 * Юзкейс для обновления прогресса вертикального сдвига и статуса видимости меню.
 */
class UpdatePreviewDragUseCase(
    private val repository: ContentPreviewRepository,
    private val calculatePreviewDragUseCase: CalculatePreviewDragUseCase
) {
    operator fun invoke(startY: Float, currentY: Float, maxDragDistance: Float = 200f) {
        val gesture = calculatePreviewDragUseCase(startY, currentY, maxDragDistance)
        repository.updateDragProgress(
            dragProgress = gesture.progress,
            isMenuVisible = gesture.shouldShowMenu
        )
    }
}
