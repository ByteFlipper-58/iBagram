package org.telegram.messenger.feature.businessrecipients.domain.model

/**
 * Filter categories for business chat targeting.
 */
enum class RecipientFilterType(val mask: Int) {
    EXISTING_CHATS(1),
    NEW_CHATS(2),
    CONTACTS(4),
    NON_CONTACTS(8);

    companion object {
        fun fromMask(mask: Int): Set<RecipientFilterType> {
            return entries.filter { (mask and it.mask) != 0 }.toSet()
        }

        fun toMask(filters: Collection<RecipientFilterType>): Int {
            return filters.fold(0) { acc, f -> acc or f.mask }
        }
    }
}

/**
 * Pure domain model representing business recipients configuration for away messages,
 * greeting messages, and business chatbots.
 */
data class BusinessRecipientsModel(
    val excludeSelected: Boolean = true,
    val existingChats: Boolean = false,
    val newChats: Boolean = false,
    val contacts: Boolean = false,
    val nonContacts: Boolean = false,
    val selectedUserIds: Set<Long> = emptySet(),
    val excludedUserIds: Set<Long> = emptySet(),
    val isBot: Boolean = false
) {
    val flags: Int
        get() {
            var f = 0
            if (existingChats) f = f or RecipientFilterType.EXISTING_CHATS.mask
            if (newChats) f = f or RecipientFilterType.NEW_CHATS.mask
            if (contacts) f = f or RecipientFilterType.CONTACTS.mask
            if (nonContacts) f = f or RecipientFilterType.NON_CONTACTS.mask
            return f
        }

    fun hasFilter(filter: RecipientFilterType): Boolean {
        return (flags and filter.mask) != 0
    }
}

/**
 * Validation result for business recipients configuration before committing.
 */
data class RecipientValidationResult(
    val isValid: Boolean,
    val errorReason: String? = null
)
