package org.telegram.messenger.feature.media.contentpreview.domain.usecase

import org.telegram.messenger.feature.media.contentpreview.domain.model.ContentPreviewGesture

/**
 * Юзкейс для вычисления прогресса сдвига пальца и решения об активации контекстного меню.
 */
class CalculatePreviewDragUseCase {

    operator fun invoke(
        startY: Float,
        currentY: Float,
        maxDragDistance: Float = 200f
    ): ContentPreviewGesture {
        return ContentPreviewGesture(
            startY = startY,
            currentY = currentY,
            maxDragDistance = maxDragDistance
        )
    }
}
