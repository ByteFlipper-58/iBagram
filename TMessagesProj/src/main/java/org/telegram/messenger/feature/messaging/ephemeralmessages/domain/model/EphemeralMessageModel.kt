package org.telegram.messenger.feature.messaging.ephemeralmessages.domain.model

object EphemeralMessageIdHelper {
    private const val EPHEMERAL_ID_MASK: Long = 0x80000000L

    fun pack(id: Int): Int {
        return id or 0x40000000
    }

    fun unpack(packedId: Int): Int {
        return packedId and 0x3FFFFFFF
    }

    fun isEphemeral(id: Int): Boolean {
        return (id and 0x40000000) != 0
    }
}

data class EphemeralBotCommandInfo(
    val command: String,
    val botUsername: String? = null,
    val botId: Long = 0L,
    val isEphemeral: Boolean = false
)

data class EphemeralMessageItem(
    val id: Int,
    val dialogId: Long,
    val fromId: Long,
    val receiverBotId: Long,
    val topMsgId: Int = 0,
    val date: Int = 0,
    val message: String = "",
    val isWelcome: Boolean = false,
    val anchorMsgId: Int = 0,
    val viaBotId: Long = 0L,
    val isOut: Boolean = false,
    val noForwards: Boolean = false,
    val invertMedia: Boolean = false
)

data class WelcomeAnchorBinding(
    val dialogId: Long,
    val messageId: Int,
    val ephemeralMessageId: Int
)

data class EphemeralMessagesState(
    val activeAnchorBindings: Map<Long, Map<Int, Int>> = emptyMap(),
    val lastDetectedCommand: EphemeralBotCommandInfo? = null,
    val pendingEphemeralCount: Int = 0
)
