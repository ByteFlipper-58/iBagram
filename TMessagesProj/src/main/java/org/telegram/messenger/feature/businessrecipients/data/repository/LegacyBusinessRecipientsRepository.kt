package org.telegram.messenger.feature.businessrecipients.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.businessrecipients.data.mapper.BusinessRecipientsMapper
import org.telegram.messenger.feature.businessrecipients.domain.model.BusinessRecipientsModel
import org.telegram.messenger.feature.businessrecipients.domain.model.RecipientFilterType
import org.telegram.messenger.feature.businessrecipients.domain.model.RecipientValidationResult
import org.telegram.messenger.feature.businessrecipients.domain.repository.BusinessRecipientsRepository

/**
 * Thread-safe adapter implementation for Telegram Business recipients configuration.
 */
class LegacyBusinessRecipientsRepository(
    initialModel: BusinessRecipientsModel? = null
) : BusinessRecipientsRepository {

    private val _state = MutableStateFlow(initialModel ?: BusinessRecipientsModel())
    private val state: StateFlow<BusinessRecipientsModel> = _state.asStateFlow()

    override fun observeRecipients(): StateFlow<BusinessRecipientsModel> = state

    override fun getRecipients(): BusinessRecipientsModel = _state.value

    override fun setRecipients(model: BusinessRecipientsModel) {
        _state.value = model
    }

    override fun toggleExcludeSelected(exclude: Boolean) {
        _state.update { it.copy(excludeSelected = exclude) }
    }

    override fun toggleFilter(filter: RecipientFilterType, enabled: Boolean) {
        _state.update { current ->
            when (filter) {
                RecipientFilterType.EXISTING_CHATS -> current.copy(existingChats = enabled)
                RecipientFilterType.NEW_CHATS -> current.copy(newChats = enabled)
                RecipientFilterType.CONTACTS -> current.copy(contacts = enabled)
                RecipientFilterType.NON_CONTACTS -> current.copy(nonContacts = enabled)
            }
        }
    }

    override fun addSelectedUsers(userIds: Collection<Long>) {
        if (userIds.isEmpty()) return
        _state.update { current ->
            val newSelected = current.selectedUserIds + userIds
            val newExcluded = current.excludedUserIds - userIds.toSet()
            current.copy(
                selectedUserIds = newSelected,
                excludedUserIds = newExcluded
            )
        }
    }

    override fun removeSelectedUser(userId: Long) {
        _state.update { current ->
            current.copy(selectedUserIds = current.selectedUserIds - userId)
        }
    }

    override fun addExcludedUsers(userIds: Collection<Long>) {
        if (userIds.isEmpty()) return
        _state.update { current ->
            val newExcluded = current.excludedUserIds + userIds
            val newSelected = current.selectedUserIds - userIds.toSet()
            current.copy(
                selectedUserIds = newSelected,
                excludedUserIds = newExcluded
            )
        }
    }

    override fun removeExcludedUser(userId: Long) {
        _state.update { current ->
            current.copy(excludedUserIds = current.excludedUserIds - userId)
        }
    }

    override fun hasChanges(initial: BusinessRecipientsModel, current: BusinessRecipientsModel): Boolean {
        return BusinessRecipientsMapper.hasChanges(initial, current)
    }

    override fun validate(model: BusinessRecipientsModel): RecipientValidationResult {
        return BusinessRecipientsMapper.validate(model)
    }

    override fun reset() {
        _state.value = BusinessRecipientsModel()
    }
}
