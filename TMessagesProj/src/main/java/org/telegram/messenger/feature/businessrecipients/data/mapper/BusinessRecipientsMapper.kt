package org.telegram.messenger.feature.businessrecipients.data.mapper

import org.telegram.messenger.feature.businessrecipients.domain.model.BusinessRecipientsModel
import org.telegram.messenger.feature.businessrecipients.domain.model.RecipientFilterType
import org.telegram.messenger.feature.businessrecipients.domain.model.RecipientValidationResult

/**
 * Pure mapping and validation engine for business recipients configurations.
 */
object BusinessRecipientsMapper {

    /**
     * Converts boolean flags and categories into Telegram bitmask flags.
     */
    fun toFlags(model: BusinessRecipientsModel): Int {
        return model.flags
    }

    /**
     * Reconstructs domain model from bitmask flags and collections.
     */
    fun fromFlags(
        flags: Int,
        exclude: Boolean,
        isBot: Boolean = false,
        selectedUsers: Set<Long> = emptySet(),
        excludedUsers: Set<Long> = emptySet()
    ): BusinessRecipientsModel {
        val sanitizedFlags = flags and (32 or 16).inv()
        return BusinessRecipientsModel(
            excludeSelected = exclude,
            existingChats = (sanitizedFlags and RecipientFilterType.EXISTING_CHATS.mask) != 0,
            newChats = (sanitizedFlags and RecipientFilterType.NEW_CHATS.mask) != 0,
            contacts = (sanitizedFlags and RecipientFilterType.CONTACTS.mask) != 0,
            nonContacts = (sanitizedFlags and RecipientFilterType.NON_CONTACTS.mask) != 0,
            selectedUserIds = selectedUsers,
            excludedUserIds = excludedUsers,
            isBot = isBot
        )
    }

    /**
     * Change detection matching Telegram's hasChanges() implementation.
     */
    fun hasChanges(initial: BusinessRecipientsModel, current: BusinessRecipientsModel): Boolean {
        if (initial.excludeSelected != current.excludeSelected) return true
        if (initial.flags != current.flags) return true
        if (initial.isBot != current.isBot) return true

        val initialUsers = if (initial.excludeSelected) initial.excludedUserIds else initial.selectedUserIds
        val currentUsers = if (current.excludeSelected) current.excludedUserIds else current.selectedUserIds
        if (initialUsers != currentUsers) return true

        if (current.isBot && !current.excludeSelected) {
            if (initial.excludedUserIds != current.excludedUserIds) return true
        }

        return false
    }

    /**
     * Validates business recipients according to Telegram Business rules.
     * When excludeSelected is false (include mode), user must select at least one filter or user.
     */
    fun validate(model: BusinessRecipientsModel): RecipientValidationResult {
        if (!model.excludeSelected && model.selectedUserIds.isEmpty() && model.flags == 0) {
            return RecipientValidationResult(
                isValid = false,
                errorReason = "At least one chat or category must be included"
            )
        }
        return RecipientValidationResult(isValid = true)
    }
}
