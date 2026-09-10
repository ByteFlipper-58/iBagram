package org.telegram.messenger.feature.pushlistener.domain.model

enum class PushType(val id: Int) {
    FIREBASE(2),
    HUAWEI(13);

    companion object {
        fun fromId(id: Int): PushType = values().firstOrNull { it.id == id } ?: FIREBASE
    }
}

enum class PushActionType {
    DC_UPDATE,
    MESSAGE_ANNOUNCEMENT,
    SESSION_REVOKE,
    GEO_LIVE_PENDING,
    OAUTH_REQUEST,
    VOIP_CALL,
    READ_HISTORY,
    READ_STORIES,
    STORY_DELETED,
    MESSAGE_DELETED,
    READ_REACTION,
    NEW_MESSAGE,
    UNKNOWN
}

enum class PushDecryptStatus {
    SUCCESS,
    INVALID_KEY_ID,
    INVALID_MAC,
    DECODE_ERROR,
    PAYLOAD_CORRUPTED
}

data class PushPayloadModel(
    val pushType: PushType,
    val locKey: String,
    val actionType: PushActionType,
    val accountUserId: Long? = null,
    val dialogId: Long = 0L,
    val topicId: Int = 0,
    val messageId: Int = 0,
    val channelId: Long = 0L,
    val chatId: Long = 0L,
    val fromId: Long = 0L,
    val isScheduled: Boolean = false,
    val isSilent: Boolean = false,
    val rawJson: String = "",
    val receiveTimeMs: Long = 0L,
    val extraParams: Map<String, String> = emptyMap()
)

data class PushProcessResult(
    val payload: PushPayloadModel? = null,
    val status: PushDecryptStatus = PushDecryptStatus.SUCCESS,
    val isHandled: Boolean = false,
    val errorMessage: String? = null
)

data class PushListenerState(
    val isListening: Boolean = true,
    val registeredTokens: Map<PushType, String> = emptyMap(),
    val lastProcessedPush: PushPayloadModel? = null,
    val totalReceivedPushes: Int = 0,
    val totalDecryptErrors: Int = 0,
    val lastError: String? = null
)
