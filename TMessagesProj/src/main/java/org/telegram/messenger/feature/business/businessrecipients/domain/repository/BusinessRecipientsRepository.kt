package org.telegram.messenger.feature.business.businessrecipients.domain.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.business.businessrecipients.domain.model.BusinessRecipientsModel
import org.telegram.messenger.feature.business.businessrecipients.domain.model.RecipientFilterType
import org.telegram.messenger.feature.business.businessrecipients.domain.model.RecipientValidationResult

/**
 * Repository interface for managing Telegram Business recipients configurations.
 */
interface BusinessRecipientsRepository {

    /**
     * Observes real-time business recipients state.
     */
    fun observeRecipients(): StateFlow<BusinessRecipientsModel>

    /**
     * Returns snapshot of current recipients configuration.
     */
    fun getRecipients(): BusinessRecipientsModel

    /**
     * Sets full recipients model.
     */
    fun setRecipients(model: BusinessRecipientsModel)

    /**
     * Toggles between include (false) and exclude (true) modes.
     */
    fun toggleExcludeSelected(exclude: Boolean)

    /**
     * Toggles a category filter on or off.
     */
    fun toggleFilter(filter: RecipientFilterType, enabled: Boolean)

    /**
     * Adds users to the included set (removing any overlap from excluded).
     */
    fun addSelectedUsers(userIds: Collection<Long>)

    /**
     * Removes a single user from included set.
     */
    fun removeSelectedUser(userId: Long)

    /**
     * Adds users to the excluded set (removing any overlap from included).
     */
    fun addExcludedUsers(userIds: Collection<Long>)

    /**
     * Removes a single user from excluded set.
     */
    fun removeExcludedUser(userId: Long)

    /**
     * Checks if current configuration differs from initial state.
     */
    fun hasChanges(initial: BusinessRecipientsModel, current: BusinessRecipientsModel): Boolean

    /**
     * Validates configuration according to Telegram Business constraints.
     */
    fun validate(model: BusinessRecipientsModel): RecipientValidationResult

    /**
     * Resets configuration to default empty exclude mode.
     */
    fun reset()
}
