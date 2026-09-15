package org.telegram.messenger.feature.messaging.richcaption.data.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.messaging.richcaption.data.datasource.RichCaptionLocalDataSource
import org.telegram.messenger.feature.messaging.richcaption.data.datasource.RichCaptionRemoteDataSource
import org.telegram.messenger.feature.messaging.richcaption.domain.model.CaptionEntitySpan
import org.telegram.messenger.feature.messaging.richcaption.domain.model.CaptionMeasureSpec
import org.telegram.messenger.feature.messaging.richcaption.domain.model.RichCaptionModel
import org.telegram.messenger.feature.messaging.richcaption.domain.repository.RichCaptionRepository

/**
 * Чистая реализация [RichCaptionRepository], координирующая хранение текста подписи, разметку и расчеты геометрии.
 */
class RichCaptionRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: RichCaptionLocalDataSource,
    private val remoteDataSource: RichCaptionRemoteDataSource
) : RichCaptionRepository {

    override fun observeCaption(): Flow<RichCaptionModel> = localDataSource.observeCaption()

    override fun getCaption(): RichCaptionModel = localDataSource.getCaption()

    override fun setCaptionText(text: String, spans: List<CaptionEntitySpan>) {
        localDataSource.setCaptionText(text, spans)
    }

    override fun setCaptionCredit(credit: String?) {
        localDataSource.setCaptionCredit(credit)
    }

    override fun setLocked(locked: Boolean) {
        localDataSource.setLocked(locked)
    }

    override fun calculateAvailableWidth(spec: CaptionMeasureSpec): Int {
        return localDataSource.calculateAvailableWidth(spec)
    }

    override fun isPressWithinBounds(
        localX: Int,
        localY: Int,
        textLeft: Int,
        textTop: Int,
        textWidth: Int,
        textHeight: Int
    ): Boolean {
        return localDataSource.isPressWithinBounds(
            localX = localX,
            localY = localY,
            textLeft = textLeft,
            textTop = textTop,
            textWidth = textWidth,
            textHeight = textHeight
        )
    }

    override fun clear() {
        localDataSource.clear()
    }
}
