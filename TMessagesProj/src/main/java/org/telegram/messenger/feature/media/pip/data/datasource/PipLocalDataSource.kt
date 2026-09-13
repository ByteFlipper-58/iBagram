package org.telegram.messenger.feature.media.pip.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.media.pip.domain.model.PipSessionInfo
import org.telegram.messenger.feature.media.pip.domain.model.PipSourceModel
import org.telegram.messenger.feature.media.pip.domain.model.PipState

/**
 * Thread-safe local data source managing active PiP sources,
 * priority arbitration, aspect ratio tracking, and session state.
 */
open class PipLocalDataSource(
    protected val currentAccount: Int
) {
    private val lock = Any()
    private val sourcesMap = LinkedHashMap<String, PipSourceModel>()
    private val _sessionFlow = MutableStateFlow(PipSessionInfo())

    open fun observeSessionInfo(): Flow<PipSessionInfo> = _sessionFlow.asStateFlow()

    open fun getSessionInfo(): PipSessionInfo = synchronized(lock) { _sessionFlow.value }

    open fun registerSource(source: PipSourceModel) {
        synchronized(lock) {
            sourcesMap[source.tag] = source
            recomputeSessionLocked()
        }
    }

    open fun unregisterSource(tag: String) {
        synchronized(lock) {
            if (sourcesMap.remove(tag) != null) {
                recomputeSessionLocked()
            }
        }
    }

    open fun updateSourceAvailability(tag: String, isAvailable: Boolean) {
        synchronized(lock) {
            val existing = sourcesMap[tag] ?: return
            sourcesMap[tag] = existing.copy(isAvailable = isAvailable)
            recomputeSessionLocked()
        }
    }

    open fun updateSourceRatio(tag: String, width: Int, height: Int) {
        synchronized(lock) {
            val existing = sourcesMap[tag] ?: return
            sourcesMap[tag] = existing.copy(aspectRatioWidth = width, aspectRatioHeight = height)
            recomputeSessionLocked()
        }
    }

    open fun updateSourceAttached(tag: String, isAttached: Boolean) {
        synchronized(lock) {
            val existing = sourcesMap[tag] ?: return
            sourcesMap[tag] = existing.copy(isAttachedToPip = isAttached)
            recomputeSessionLocked()
        }
    }

    open fun updatePipState(state: PipState, byActivityStop: Boolean = false) {
        synchronized(lock) {
            val current = _sessionFlow.value
            _sessionFlow.value = current.copy(pipState = state)
        }
    }

    open fun recordPipAction(tag: String, actionId: Int) {
        synchronized(lock) {
            val current = _sessionFlow.value
            _sessionFlow.value = current.copy(lastAction = tag to actionId)
        }
    }

    open fun canEnterPip(): Boolean {
        synchronized(lock) {
            return _sessionFlow.value.activeSource != null
        }
    }

    private fun recomputeSessionLocked() {
        val allSources = sourcesMap.values.toList()
        var maxSource: PipSourceModel? = null

        for (source in allSources) {
            if (!source.isEligible) {
                continue
            }
            if (maxSource == null || source.priority > maxSource.priority) {
                maxSource = source
            }
        }

        val mediaSessionActive = maxSource?.needsMediaSession == true

        _sessionFlow.value = _sessionFlow.value.copy(
            activeSource = maxSource,
            registeredSources = allSources,
            isMediaSessionActive = mediaSessionActive
        )
    }
}
