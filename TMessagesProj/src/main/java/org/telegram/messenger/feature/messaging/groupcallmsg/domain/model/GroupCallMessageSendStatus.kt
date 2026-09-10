package org.telegram.messenger.feature.messaging.groupcallmsg.domain.model

/**
 * Status of sending an ephemeral group call message.
 */
enum class GroupCallMessageSendStatus {
    SENDING,
    DELAYED,
    CONFIRMED,
    ERROR
}
