package org.telegram.messenger.feature.emojieffects.domain.usecase

import kotlin.math.min

/**
 * Рассчитывает базовый размер текстуры/фильтра оверлея для анимаций эмодзи.
 */
class CalculateEmojiBoundsUseCase {

    operator fun invoke(
        isTablet: Boolean,
        screenWidth: Int,
        screenHeight: Int,
        density: Float,
        minTabletSide: Float = 0f
    ): Int {
        val safeDensity = if (density <= 0f) 1f else density
        val w: Float = if (isTablet) {
            val side = if (minTabletSide > 0f) minTabletSide else min(screenWidth, screenHeight).toFloat()
            side * 0.4f
        } else {
            min(screenWidth, screenHeight).toFloat() * 0.5f
        }
        return (2f * w / safeDensity).toInt()
    }
}
