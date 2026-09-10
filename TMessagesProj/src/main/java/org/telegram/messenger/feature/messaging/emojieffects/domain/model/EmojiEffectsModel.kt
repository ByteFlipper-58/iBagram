package org.telegram.messenger.feature.messaging.emojieffects.domain.model

/**
 * Одиночное действие интерактивной анимации эмодзи (тапа).
 *
 * @property index Номер/индекс анимации (0-based)
 * @property timeOffsetSeconds Временное смещение от начала сессии в секундах
 */
data class EmojiInteractionAction(
    val index: Int,
    val timeOffsetSeconds: Double
) {
    val delayMillis: Long
        get() = (timeOffsetSeconds * 1000.0).toLong()
}

/**
 * Сессия интерактивных тапов пользователя по эмодзи в чате.
 *
 * @property messageId ID целевого сообщения
 * @property emoticon Исходный эмодзи сообщения
 * @property initialTimestampMs Временная метка первого тапа
 * @property relativeTimeOffsetsMs Список смещений по времени относительно первого тапа (в мс)
 * @property animationIndexes Список индексов воспроизведенных анимаций
 */
data class EmojiInteractionSession(
    val messageId: Int,
    val emoticon: String,
    val initialTimestampMs: Long = 0L,
    val relativeTimeOffsetsMs: List<Long> = emptyList(),
    val animationIndexes: List<Int> = emptyList()
) {
    val actionsCount: Int
        get() = animationIndexes.size

    fun toActions(): List<EmojiInteractionAction> {
        return animationIndexes.indices.map { i ->
            val offsetMs = relativeTimeOffsetsMs.getOrElse(i) { 0L }
            EmojiInteractionAction(
                index = animationIndexes[i],
                timeOffsetSeconds = offsetMs / 1000.0
            )
        }
    }
}

/**
 * Результат проверки квоты на запуск новой анимации оверлея.
 */
enum class EmojiAnimationQuotaStatus {
    ALLOWED,
    EXCEEDED_GLOBAL_LIMIT,
    EXCEEDED_MESSAGE_LIMIT,
    CACHE_GENERATING
}

/**
 * Результат оценки возможности запуска анимации.
 */
data class EmojiAnimationQuotaResult(
    val status: EmojiAnimationQuotaStatus,
    val isAllowed: Boolean = (status == EmojiAnimationQuotaStatus.ALLOWED)
)

/**
 * Координаты и габариты оверлея для отрисовки анимации.
 */
data class EmojiOverlayGeometry(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val isOutside: Boolean = false
)

/**
 * Описание элемента активного эффекта анимации эмодзи/стикера.
 */
data class EmojiEffectItem(
    val id: String,
    val messageId: Int,
    val documentId: Long = 0L,
    val emoticon: String = "",
    val isOut: Boolean = false,
    val isPremiumSticker: Boolean = false,
    val isMessageEffect: Boolean = false,
    val isReaction: Boolean = false,
    val progress: Float = 0f,
    val isRemoving: Boolean = false,
    val removeProgress: Float = 0f,
    val geometry: EmojiOverlayGeometry = EmojiOverlayGeometry(0f, 0f, 0f, 0f)
) {
    val isPlaying: Boolean
        get() = progress in 0f..0.999f && !isRemoving

    val alpha: Float
        get() = (1f - removeProgress).coerceIn(0f, 1f)
}

/**
 * Состояние слоя оверлея интерактивных анимаций.
 */
data class EmojiEffectsState(
    val activeEffects: List<EmojiEffectItem> = emptyList(),
    val currentSession: EmojiInteractionSession? = null,
    val lastAnimationIndexPerDocument: Map<Long, Int> = emptyMap(),
    val isIdle: Boolean = activeEffects.isEmpty()
)
