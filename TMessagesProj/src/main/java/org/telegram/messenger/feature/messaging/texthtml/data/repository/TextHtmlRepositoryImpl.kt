package org.telegram.messenger.feature.messaging.texthtml.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.messaging.texthtml.data.datasource.TextHtmlLocalDataSource
import org.telegram.messenger.feature.messaging.texthtml.data.datasource.TextHtmlRemoteDataSource
import org.telegram.messenger.feature.messaging.texthtml.domain.model.RichFormattedText
import org.telegram.messenger.feature.messaging.texthtml.domain.model.TextHtmlState
import org.telegram.messenger.feature.messaging.texthtml.domain.repository.TextHtmlRepository

/**
 * Чистая реализация [TextHtmlRepository], управляющая преобразованием форматированного текста в HTML и обратно.
 */
class TextHtmlRepositoryImpl(
    private val currentAccount: Int,
    private val localDataSource: TextHtmlLocalDataSource,
    private val remoteDataSource: TextHtmlRemoteDataSource
) : TextHtmlRepository {

    override fun convertToHtml(text: RichFormattedText): String =
        localDataSource.convertToHtml(text)

    override fun parseFromHtml(html: String): RichFormattedText =
        localDataSource.parseFromHtml(html)

    override fun escapeHtml(rawText: String): String =
        localDataSource.escapeHtml(rawText)

    override fun unescapeHtml(escapedHtml: String): String =
        localDataSource.unescapeHtml(escapedHtml)

    override fun stripFormatting(html: String): String =
        localDataSource.stripFormatting(html)

    override fun observeState(): StateFlow<TextHtmlState> =
        localDataSource.stateFlow

    override fun clearState() {
        localDataSource.clearState()
    }
}
