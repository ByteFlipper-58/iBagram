package org.telegram.messenger.feature.messaging.richcaption.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.messaging.richcaption.data.mapper.RichCaptionMapper
import org.telegram.messenger.feature.messaging.richcaption.domain.model.CaptionEntitySpan
import org.telegram.messenger.feature.messaging.richcaption.domain.model.CaptionMeasureSpec
import org.telegram.messenger.feature.messaging.richcaption.domain.model.RichCaptionModel

/**
 * Потокобезопасный локальный источник данных для хранения текста подписи к медиа, форматирования и геометрии нажатий.
 */
class RichCaptionLocalDataSource(
    initialModel: RichCaptionModel = RichCaptionModel()
) {

    private val lock = Any()
    private val _captionState = MutableStateFlow(initialModel)

    fun observeCaption(): StateFlow<RichCaptionModel> = _captionState.asStateFlow()

    fun getCaption(): RichCaptionModel = _captionState.value

    fun setCaptionText(text: String, spans: List<CaptionEntitySpan>) = synchronized(lock) {
        val current = _captionState.value
        _captionState.value = RichCaptionMapper.createCaption(
            plainText = text,
            credit = current.credit,
            spans = spans,
            isLocked = current.isLocked
        )
    }

    fun setCaptionCredit(credit: String?) = synchronized(lock) {
        val current = _captionState.value
        _captionState.value = current.copy(
            credit = credit?.trim()?.ifEmpty { null }
        )
    }

    fun setLocked(locked: Boolean) = synchronized(lock) {
        _captionState.value = _captionState.value.copy(isLocked = locked)
    }

    fun calculateAvailableWidth(spec: CaptionMeasureSpec): Int {
        return spec.availableWidth
    }

    fun isPressWithinBounds(
        localX: Int,
        localY: Int,
        textLeft: Int,
        textTop: Int,
        textWidth: Int,
        textHeight: Int
    ): Boolean {
        if (textWidth <= 0 || textHeight <= 0) return false
        val inX = localX in textLeft until (textLeft + textWidth)
        val inY = localY in textTop until (textTop + textHeight)
        return inX && inY
    }

    fun clear() = synchronized(lock) {
        _captionState.value = RichCaptionModel()
    }
}
