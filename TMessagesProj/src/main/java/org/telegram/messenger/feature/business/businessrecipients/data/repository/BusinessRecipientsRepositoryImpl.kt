package org.telegram.messenger.feature.business.businessrecipients.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.telegram.messenger.feature.business.businessrecipients.data.datasource.BusinessRecipientsLocalDataSource
import org.telegram.messenger.feature.business.businessrecipients.domain.model.BusinessRecipientsModel
import org.telegram.messenger.feature.business.businessrecipients.domain.model.RecipientFilterType
import org.telegram.messenger.feature.business.businessrecipients.domain.model.RecipientValidationResult
import org.telegram.messenger.feature.business.businessrecipients.domain.repository.BusinessRecipientsRepository

class BusinessRecipientsRepositoryImpl(
    private val localDataSource: BusinessRecipientsLocalDataSource
) : BusinessRecipientsRepository {

    override fun observeRecipients(): StateFlow<BusinessRecipientsModel> {
        return localDataSource.state
    }

    override fun getRecipients(): BusinessRecipientsModel {
        return localDataSource.getRecipients()
    }

    override fun setRecipients(model: BusinessRecipientsModel) {
        localDataSource.setRecipients(model)
    }

    override fun toggleExcludeSelected(exclude: Boolean) {
        localDataSource.toggleExcludeSelected(exclude)
    }

    override fun toggleFilter(filter: RecipientFilterType, enabled: Boolean) {
        localDataSource.toggleFilter(filter, enabled)
    }

    override fun addSelectedUsers(userIds: Collection<Long>) {
        localDataSource.addSelectedUsers(userIds)
    }

    override fun removeSelectedUser(userId: Long) {
        localDataSource.removeSelectedUser(userId)
    }

    override fun addExcludedUsers(userIds: Collection<Long>) {
        localDataSource.addExcludedUsers(userIds)
    }

    override fun removeExcludedUser(userId: Long) {
        localDataSource.removeExcludedUser(userId)
    }

    override fun hasChanges(initial: BusinessRecipientsModel, current: BusinessRecipientsModel): Boolean {
        return localDataSource.hasChanges(initial, current)
    }

    override fun validate(model: BusinessRecipientsModel): RecipientValidationResult {
        return localDataSource.validate(model)
    }

    override fun reset() {
        localDataSource.reset()
    }
}
