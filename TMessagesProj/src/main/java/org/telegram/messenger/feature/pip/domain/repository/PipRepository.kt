package org.telegram.messenger.feature.pip.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.feature.pip.domain.model.PipSessionInfo
import org.telegram.messenger.feature.pip.domain.model.PipSourceModel
import org.telegram.messenger.feature.pip.domain.model.PipState

/**
 * Repository interface defining Picture-in-Picture session management,
 * source priority arbitration, and state machine controls.
 */
interface PipRepository {
    fun observeSessionInfo(): Flow<PipSessionInfo>
    fun getSessionInfo(): PipSessionInfo

    fun registerSource(source: PipSourceModel)
    fun unregisterSource(tag: String)

    fun updateSourceAvailability(tag: String, isAvailable: Boolean)
    fun updateSourceRatio(tag: String, width: Int, height: Int)
    fun updateSourceAttached(tag: String, isAttached: Boolean)

    fun updatePipState(state: PipState, byActivityStop: Boolean = false)
    fun triggerPipAction(tag: String, actionId: Int)

    fun canEnterPip(): Boolean
}
