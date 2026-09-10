package org.telegram.messenger.feature.richcaption.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.richcaption.domain.model.CaptionEntitySpan
import org.telegram.messenger.feature.richcaption.domain.model.CaptionMeasureSpec
import org.telegram.messenger.feature.richcaption.domain.model.RichCaptionModel

interface RichCaptionRepository {
    fun observeCaption(): Flow<RichCaptionModel>
    fun getCaption(): RichCaptionModel
    fun setCaptionText(text: String, spans: List<CaptionEntitySpan> = emptyList())
    fun setCaptionCredit(credit: String?)
    fun setLocked(locked: Boolean)
    fun calculateAvailableWidth(spec: CaptionMeasureSpec): Int
    fun isPressWithinBounds(localX: Int, localY: Int, textLeft: Int, textTop: Int, textWidth: Int, textHeight: Int): Boolean
    fun clear()
}
