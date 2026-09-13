package org.telegram.messenger.feature.media.chromecast.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.media.chromecast.data.mapper.ChromecastMapper
import org.telegram.messenger.feature.media.chromecast.domain.model.ChromecastMediaModel
import org.telegram.messenger.feature.media.chromecast.domain.model.ChromecastStateModel

/**
 * Local data source managing Cast playback session state, current media, and cover cache.
 */
open class ChromecastLocalDataSource(
    protected val currentAccount: Int
) {
    private val lock = Any()
    private var activeMedia: ChromecastMediaModel? = null
    private var coverPath: String? = null
    private val _stateFlow = MutableStateFlow(
        ChromecastMapper.toDomainState(
            isCasting = false,
            deviceName = null,
            currentMedia = null,
            isConnected = false
        )
    )

    open fun observeState(): Flow<ChromecastStateModel> = _stateFlow.asStateFlow()

    open fun getState(): ChromecastStateModel = synchronized(lock) { _stateFlow.value }

    open fun getActiveMedia(): ChromecastMediaModel? = synchronized(lock) { activeMedia }

    open fun setActiveMedia(media: ChromecastMediaModel?, isCasting: Boolean) {
        synchronized(lock) {
            activeMedia = media
            _stateFlow.value = ChromecastMapper.toDomainState(
                isCasting = isCasting,
                deviceName = if (isCasting) "Chromecast" else null,
                currentMedia = media,
                isConnected = isCasting
            )
        }
    }

    open fun updateCastingState(isCasting: Boolean) {
        synchronized(lock) {
            _stateFlow.value = _stateFlow.value.copy(
                isCasting = isCasting,
                isConnected = isCasting,
                deviceName = if (isCasting) "Chromecast" else null
            )
        }
    }

    open fun setCoverPath(path: String?) {
        synchronized(lock) {
            coverPath = path
        }
    }

    open fun getCoverPath(): String? = synchronized(lock) { coverPath }
}
