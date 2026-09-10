package org.telegram.messenger.feature.business.businessrecipients.presentation

import org.telegram.messenger.feature.business.businessrecipients.domain.model.BusinessRecipientsModel
import org.telegram.messenger.feature.business.businessrecipients.domain.model.RecipientFilterType

sealed interface BusinessRecipientsEvent {
    data class SetInitial(val model: BusinessRecipientsModel) : BusinessRecipientsEvent
    data class ToggleExclude(val exclude: Boolean) : BusinessRecipientsEvent
    data class ToggleFilter(val filter: RecipientFilterType, val enabled: Boolean) : BusinessRecipientsEvent
    data class AddSelectedUsers(val userIds: Collection<Long>) : BusinessRecipientsEvent
    data class RemoveSelectedUser(val userId: Long) : BusinessRecipientsEvent
    data class AddExcludedUsers(val userIds: Collection<Long>) : BusinessRecipientsEvent
    data class RemoveExcludedUser(val userId: Long) : BusinessRecipientsEvent
    object Validate : BusinessRecipientsEvent
    object Reset : BusinessRecipientsEvent
}
