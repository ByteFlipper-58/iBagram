package org.telegram.messenger.feature.bottomviews.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.bottomviews.domain.model.BottomViewsVisibilityState

/**
 * Contract for managing and arbitrating bottom view visibility animations and priority flags.
 */
interface BottomViewsVisibilityRepository {
    fun getVisibility(containerId: Int): Float
    fun setViewVisible(containerId: Int, isVisible: Boolean, animated: Boolean = true)
    fun getCurrentPriorityContainerId(): Int
    fun getState(): BottomViewsVisibilityState
    fun observeState(): Flow<BottomViewsVisibilityState>
}
