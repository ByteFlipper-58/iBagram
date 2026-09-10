package org.telegram.messenger.feature.messaging.richcaption.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.messaging.richcaption.data.mapper.RichCaptionMapper
import org.telegram.messenger.feature.messaging.richcaption.domain.model.CaptionEntitySpan
import org.telegram.messenger.feature.messaging.richcaption.domain.model.CaptionMeasureSpec
import org.telegram.messenger.feature.messaging.richcaption.domain.model.RichCaptionModel
import org.telegram.messenger.feature.messaging.richcaption.domain.repository.RichCaptionRepository

class LegacyRichCaptionRepository(
    initialModel: RichCaptionModel = RichCaptionModel()
) : RichCaptionRepository {

    private val lock = Any()
    private val _captionState = MutableStateFlow(initialModel)

    override fun observeCaption(): Flow<RichCaptionModel> = _captionState.asStateFlow()

    override fun getCaption(): RichCaptionModel = _captionState.value

    override fun setCaptionText(text: String, spans: List<CaptionEntitySpan>) {
        synchronized(lock) {
            val current = _captionState.value
            _captionState.value = RichCaptionMapper.createCaption(
                plainText = text,
                credit = current.credit,
                spans = spans,
                isLocked = current.isLocked
            )
        }
    }

    override fun setCaptionCredit(credit: String?) {
        synchronized(lock) {
            val current = _captionState.value
            _captionState.value = current.copy(
                credit = credit?.trim()?.ifEmpty { null }
            )
        }
    }

    override fun setLocked(locked: Boolean) {
        synchronized(lock) {
            _captionState.value = _captionState.value.copy(isLocked = locked)
        }
    }

    override fun calculateAvailableWidth(spec: CaptionMeasureSpec): Int {
        return spec.availableWidth
    }

    override fun isPressWithinBounds(
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

    override fun clear() {
        synchronized(lock) {
            _captionState.value = RichCaptionModel()
        }
    }
}
