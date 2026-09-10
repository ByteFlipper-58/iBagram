package org.telegram.messenger.feature.business.businessbots.domain.model

data class BusinessBotRecipientsModel(
    val excludeSelected: Boolean = true,
    val users: List<Long> = emptyList(),
    val existingChats: Boolean = false,
    val newChats: Boolean = false,
    val contacts: Boolean = false,
    val nonContacts: Boolean = false
) {
    val hasFilters: Boolean
        get() = existingChats || newChats || contacts || nonContacts || users.isNotEmpty()
}
