package org.telegram.messenger.feature.media.chromecast.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.chromecast.ChromecastController
import org.telegram.messenger.feature.media.chromecast.data.mapper.ChromecastMapper
import org.telegram.messenger.feature.media.chromecast.domain.model.ChromecastMediaModel
import org.telegram.messenger.feature.media.chromecast.domain.model.ChromecastStateModel
import org.telegram.messenger.feature.media.chromecast.domain.repository.ChromecastRepository
import java.io.File

class LegacyChromecastRepository(
    private val controllerProvider: () -> ChromecastController? = {
        try {
            if (ApplicationLoader.applicationContext != null) {
                ChromecastController.getInstance()
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }
) : ChromecastRepository {

    private var activeMedia: ChromecastMediaModel? = null
    private val stateFlow = MutableStateFlow(computeState())

    private fun computeState(): ChromecastStateModel {
        val controller = controllerProvider()
        val isCasting = controller?.isCasting ?: false
        return ChromecastMapper.toDomainState(
            isCasting = isCasting,
            deviceName = null,
            currentMedia = activeMedia,
            isConnected = isCasting
        )
    }

    private fun updateState() {
        stateFlow.value = computeState()
    }

    override fun observeChromecastState(): Flow<ChromecastStateModel> = stateFlow.asStateFlow()

    override fun getChromecastState(): ChromecastStateModel {
        val state = computeState()
        stateFlow.value = state
        return state
    }

    override fun isCasting(): Boolean {
        return try {
            controllerProvider()?.isCasting ?: false
        } catch (_: Throwable) {
            false
        }
    }

    override fun isPlaying(media: ChromecastMediaModel): Boolean {
        return isCasting() && activeMedia == media
    }

    override suspend fun castMedia(media: ChromecastMediaModel): Result<Unit> = withContext(Dispatchers.Main) {
        runCatching {
            activeMedia = media
            updateState()
        }
    }

    override suspend fun stopCasting(): Result<Unit> = withContext(Dispatchers.Main) {
        runCatching {
            activeMedia = null
            updateState()
        }
    }

    override suspend fun setCoverFile(file: File?): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            if (file == null) {
                null
            } else {
                val controller = controllerProvider()
                controller?.setCover(file) ?: "/cover_${file.name}"
            }
        }
    }
}
