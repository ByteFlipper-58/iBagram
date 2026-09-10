package org.telegram.messenger.feature.social.profile.domain.repository

import kotlinx.coroutines.flow.Flow
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.social.profile.domain.model.ProfileModel

/**
 * Domain boundary contract for managing user/chat profiles.
 */
interface ProfileRepository {
    /**
     * Observe the reactive stream of profile updates for a specific peer ID.
     */
    fun observeProfile(id: Long): Flow<ProfileModel?>

    /**
     * Get cached profile data for a specific peer ID.
     */
    suspend fun getProfile(id: Long): Result<ProfileModel>

    /**
     * Request fresh/full profile data from storage or network.
     */
    suspend fun loadFullProfile(id: Long): Result<ProfileModel>

    /**
     * Block a user or peer.
     */
    suspend fun blockPeer(id: Long): Result<Unit>

    /**
     * Unblock a user or peer.
     */
    suspend fun unblockPeer(id: Long): Result<Unit>
}
