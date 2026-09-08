package org.telegram.messenger.feature.contacts.domain.model

/**
 * Pure Kotlin immutable domain entity representing a user's contact.
 * Decouples presentation and domain layers from legacy [org.telegram.tgnet.TLRPC.TL_contact] and [org.telegram.tgnet.TLRPC.User].
 */
data class ContactModel(
    val id: Long,
    val firstName: String = "",
    val lastName: String = "",
    val displayName: String = "",
    val username: String = "",
    val phone: String = "",
    val isMutual: Boolean = false,
    val isOnline: Boolean = false
)
