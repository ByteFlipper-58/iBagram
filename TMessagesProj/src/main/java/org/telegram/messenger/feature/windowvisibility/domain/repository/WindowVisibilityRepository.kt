package org.telegram.messenger.feature.windowvisibility.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.windowvisibility.domain.model.WindowVisibilityController
import org.telegram.messenger.feature.windowvisibility.domain.model.WindowVisibilityState

/**
 * Repository contract for window visibility arbitration and reference counting.
 */
interface WindowVisibilityRepository {
    /**
     * Registers a reason to hide the window.
     */
    fun requestHide(reasonTag: String, description: String = ""): WindowVisibilityState

    /**
     * Releases a reason to hide the window.
     */
    fun releaseHide(reasonTag: String): WindowVisibilityState

    /**
     * Toggles hiding state for a specific reason tag.
     */
    fun toggleHide(reasonTag: String, hide: Boolean, description: String = ""): WindowVisibilityState

    /**
     * Checks if the window is currently visible.
     */
    fun isVisible(): Boolean

    /**
     * Checks if the window is currently hidden.
     */
    fun isHidden(): Boolean

    /**
     * Gets the total count of active hide reasons.
     */
    fun getReasonsCount(): Int

    /**
     * Gets the set of active hide reasons.
     */
    fun getActiveReasons(): Set<String>

    /**
     * Gets the current visibility state snapshot.
     */
    fun getCurrentState(): WindowVisibilityState

    /**
     * Resets all hide reasons, making the window visible immediately.
     */
    fun resetAllReasons(): WindowVisibilityState

    /**
     * Observes the aggregated visibility state reactively.
     */
    fun observeState(): StateFlow<WindowVisibilityState>

    /**
     * Observes visibility boolean changes reactively.
     */
    fun observeVisibilityChanges(): Flow<Boolean>

    /**
     * Obtains an isolated controller for a specific subsystem tag.
     */
    fun obtainController(reasonTag: String): WindowVisibilityController
}
