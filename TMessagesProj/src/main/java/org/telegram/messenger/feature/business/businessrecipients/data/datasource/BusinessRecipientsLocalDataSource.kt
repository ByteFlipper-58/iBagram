package org.telegram.messenger.feature.business.businessrecipients.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.business.businessrecipients.data.mapper.BusinessRecipientsMapper
import org.telegram.messenger.feature.business.businessrecipients.domain.model.BusinessRecipientsModel
import org.telegram.messenger.feature.business.businessrecipients.domain.model.RecipientFilterType
import org.telegram.messenger.feature.business.businessrecipients.domain.model.RecipientValidationResult

class BusinessRecipientsLocalDataSource(
    initialModel: BusinessRecipientsModel? = null
) {
    private val _state = MutableStateFlow(initialModel ?: BusinessRecipientsModel())
    val state: StateFlow<BusinessRecipientsModel> = _state.asStateFlow()

    fun getRecipients(): BusinessRecipientsModel = _state.value

    fun setRecipients(model: BusinessRecipientsModel) {
        _state.value = model
    }

    fun toggleExcludeSelected(exclude: Boolean) {
        _state.update { it.copy(excludeSelected = exclude) }
    }

    fun toggleFilter(filter: RecipientFilterType, enabled: Boolean) {
        _state.update { current ->
            when (filter) {
                RecipientFilterType.EXISTING_CHATS -> current.copy(existingChats = enabled)
                RecipientFilterType.NEW_CHATS -> current.copy(newChats = enabled)
                RecipientFilterType.CONTACTS -> current.copy(contacts = enabled)
                RecipientFilterType.NON_CONTACTS -> current.copy(nonContacts = enabled)
            }
        }
    }

    fun addSelectedUsers(userIds: Collection<Long>) {
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

    fun removeSelectedUser(userId: Long) {
        _state.update { current ->
            current.copy(selectedUserIds = current.selectedUserIds - userId)
        }
    }

    fun addExcludedUsers(userIds: Collection<Long>) {
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

    fun removeExcludedUser(userId: Long) {
        _state.update { current ->
            current.copy(excludedUserIds = current.excludedUserIds - userId)
        }
    }

    fun hasChanges(initial: BusinessRecipientsModel, current: BusinessRecipientsModel): Boolean {
        return BusinessRecipientsMapper.hasChanges(initial, current)
    }

    fun validate(model: BusinessRecipientsModel): RecipientValidationResult {
        return BusinessRecipientsMapper.validate(model)
    }

    fun reset() {
        _state.value = BusinessRecipientsModel()
    }
}
