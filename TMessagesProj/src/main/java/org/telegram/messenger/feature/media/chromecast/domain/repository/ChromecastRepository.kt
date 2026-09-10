package org.telegram.messenger.feature.media.chromecast.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.media.chromecast.domain.model.ChromecastMediaModel
import org.telegram.messenger.feature.media.chromecast.domain.model.ChromecastStateModel
import java.io.File

interface ChromecastRepository {
    fun observeChromecastState(): Flow<ChromecastStateModel>
    fun getChromecastState(): ChromecastStateModel
    fun isCasting(): Boolean
    fun isPlaying(media: ChromecastMediaModel): Boolean
    suspend fun castMedia(media: ChromecastMediaModel): Result<Unit>
    suspend fun stopCasting(): Result<Unit>
    suspend fun setCoverFile(file: File?): Result<String?>
}
