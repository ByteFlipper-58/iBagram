package org.telegram.messenger.feature.messaging.richcaption.data.datasource

import org.telegram.messenger.MessagesController
import org.telegram.messenger.UserConfig

/**
 * Удаленный источник данных для запроса серверных лимитов и конфигураций подписей к медиа.
 */
class RichCaptionRemoteDataSource(
    private val currentAccount: Int
) {

    fun getMaxCaptionLength(): Int {
        return try {
            val isPremium = UserConfig.getInstance(currentAccount)?.isPremium ?: false
            val controller = MessagesController.getInstance(currentAccount)
            if (isPremium) {
                controller?.captionLengthLimitPremium ?: 2048
            } else {
                controller?.captionLengthLimitDefault ?: 1024
            }
        } catch (_: Throwable) {
            1024
        }
    }
}
