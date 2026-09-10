package org.telegram.messenger.feature.security.flagsecure.presentation

import org.telegram.messenger.feature.security.flagsecure.domain.model.SecurityReasonType

sealed interface FlagSecureEvent {
    data class SelectWindow(val windowId: String) : FlagSecureEvent
    data class AttachReason(val windowId: String, val reason: SecurityReasonType) : FlagSecureEvent
    data class DetachReason(val windowId: String, val reason: SecurityReasonType) : FlagSecureEvent
    data class InvalidateWindow(val windowId: String) : FlagSecureEvent
    data class ResetWindow(val windowId: String) : FlagSecureEvent
    data object DismissError : FlagSecureEvent
}
