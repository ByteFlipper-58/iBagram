package org.telegram.messenger.feature.messaging.emojieffects.domain.usecase

import org.telegram.messenger.feature.messaging.emojieffects.domain.model.EmojiOverlayGeometry

/**
 * Рассчитывает координаты оверлея для анимации эмодзи или стикера,
 * а также определяет, не вышел ли элемент за границы видимости списка сообщений.
 */
class CalculateEmojiOverlayPositionUseCase {

    operator fun invoke(
        cellX: Float,
        cellY: Float,
        imageX: Float,
        imageY: Float,
        imageWidth: Float,
        imageHeight: Float,
        isOut: Boolean,
        isPremiumSticker: Boolean,
        marginDp24: Float = 24f,
        listTopPadding: Float = 0f,
        listBottomBound: Float = Float.MAX_VALUE
    ): EmojiOverlayGeometry {
        val posX: Float
        val posY: Float

        if (isPremiumSticker) {
            posX = cellX + imageX
            posY = cellY + imageY
        } else {
            val baseOffsetX = if (isOut) {
                -imageWidth * 2f + marginDp24
            } else {
                -marginDp24
            }
            posX = cellX + imageX + baseOffsetX
            posY = cellY + imageY - imageWidth
        }

        // Проверка видимости относительно вертикальных границ списка
        val outsideTop: Boolean
        val outsideBottom: Boolean

        if (isPremiumSticker) {
            val halfHeight = imageHeight / 2f
            outsideBottom = (listBottomBound - cellY) <= halfHeight
            outsideTop = (cellY - listTopPadding + halfHeight) <= 0f
        } else {
            outsideTop = (cellY + imageHeight) < listTopPadding
            outsideBottom = cellY > listBottomBound
        }

        return EmojiOverlayGeometry(
            x = posX,
            y = posY,
            width = imageWidth,
            height = imageHeight,
            isOutside = outsideTop || outsideBottom
        )
    }
}
