package org.telegram.messenger.feature.media.pip.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.feature.media.pip.domain.model.PipSessionInfo
import org.telegram.messenger.feature.media.pip.domain.model.PipSourceModel
import org.telegram.messenger.feature.media.pip.domain.model.PipState
import org.telegram.messenger.feature.media.pip.domain.repository.PipRepository
import org.telegram.messenger.pip.PipActivityController

/**
 * Legacy implementation of [PipRepository] that manages Picture-in-Picture source priority arbitration,
 * media session eligibility, and state lifecycle synchronization.
 */
class LegacyPipRepository(
    private var pipActivityController: PipActivityController? = null
) : PipRepository {

    private val lock = Any()
    private val sourcesMap = LinkedHashMap<String, PipSourceModel>()

    private val _sessionFlow = MutableStateFlow(PipSessionInfo())
    override fun observeSessionInfo(): Flow<PipSessionInfo> = _sessionFlow.asStateFlow()
    override fun getSessionInfo(): PipSessionInfo = _sessionFlow.value

    fun attachPipController(controller: PipActivityController?) {
        synchronized(lock) {
            this.pipActivityController = controller
        }
    }

    override fun registerSource(source: PipSourceModel) {
        synchronized(lock) {
            sourcesMap[source.tag] = source
            recomputeSessionLocked()
        }
    }

    override fun unregisterSource(tag: String) {
        synchronized(lock) {
            if (sourcesMap.remove(tag) != null) {
                recomputeSessionLocked()
            }
        }
    }

    override fun updateSourceAvailability(tag: String, isAvailable: Boolean) {
        synchronized(lock) {
            val existing = sourcesMap[tag] ?: return
            sourcesMap[tag] = existing.copy(isAvailable = isAvailable)
            recomputeSessionLocked()
        }
    }

    override fun updateSourceRatio(tag: String, width: Int, height: Int) {
        synchronized(lock) {
            val existing = sourcesMap[tag] ?: return
            sourcesMap[tag] = existing.copy(aspectRatioWidth = width, aspectRatioHeight = height)
            recomputeSessionLocked()
        }
    }

    override fun updateSourceAttached(tag: String, isAttached: Boolean) {
        synchronized(lock) {
            val existing = sourcesMap[tag] ?: return
            sourcesMap[tag] = existing.copy(isAttachedToPip = isAttached)
            recomputeSessionLocked()
        }
    }

    override fun updatePipState(state: PipState, byActivityStop: Boolean) {
        synchronized(lock) {
            val current = _sessionFlow.value
            _sessionFlow.value = current.copy(pipState = state)
        }
    }

    override fun triggerPipAction(tag: String, actionId: Int) {
        synchronized(lock) {
            val current = _sessionFlow.value
            _sessionFlow.value = current.copy(lastAction = tag to actionId)
        }
    }

    override fun canEnterPip(): Boolean {
        synchronized(lock) {
            val legacyHasContent = pipActivityController?.hasContentForPictureInPictureMode()
            return legacyHasContent ?: (_sessionFlow.value.activeSource != null)
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

        val currentState = _sessionFlow.value.pipState
        val mediaSessionActive = maxSource?.needsMediaSession == true

        _sessionFlow.value = _sessionFlow.value.copy(
            activeSource = maxSource,
            registeredSources = allSources,
            isMediaSessionActive = mediaSessionActive
        )
    }
}
