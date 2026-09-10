package org.telegram.messenger.feature.network.pushlistener.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushActionType
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushDecryptStatus
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushListenerState
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushPayloadModel
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushProcessResult
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushType
import org.telegram.messenger.feature.network.pushlistener.domain.repository.PushListenerRepository

class ObservePushListenerStateUseCase(
    private val repository: PushListenerRepository
) {
    operator fun invoke(): Flow<PushListenerState> = repository.observeState()
}

class ObserveIncomingPushesUseCase(
    private val repository: PushListenerRepository
) {
    operator fun invoke(): Flow<PushPayloadModel> = repository.observeIncomingPushes()
}

class GetPushListenerStateUseCase(
    private val repository: PushListenerRepository
) {
    suspend operator fun invoke(): PushListenerState = repository.getState()
}

class ProcessIncomingPushUseCase(
    private val repository: PushListenerRepository
) {
    suspend operator fun invoke(
        pushType: PushType,
        rawData: String,
        timestamp: Long = System.currentTimeMillis()
    ): PushProcessResult {
        return repository.processPush(pushType, rawData, timestamp)
    }
}

class RegisterPushListenerTokenUseCase(
    private val repository: PushListenerRepository
) {
    suspend operator fun invoke(pushType: PushType, token: String) {
        if (token.isNotBlank()) {
            repository.registerToken(pushType, token)
        }
    }
}

class TogglePushListeningUseCase(
    private val repository: PushListenerRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        repository.setListening(enabled)
    }
}

class DeterminePushActionTypeUseCase {
    operator fun invoke(locKey: String): PushActionType {
        return when (locKey) {
            "DC_UPDATE" -> PushActionType.DC_UPDATE
            "MESSAGE_ANNOUNCEMENT" -> PushActionType.MESSAGE_ANNOUNCEMENT
            "SESSION_REVOKE" -> PushActionType.SESSION_REVOKE
            "GEO_LIVE_PENDING" -> PushActionType.GEO_LIVE_PENDING
            "OAUTH_REQUEST" -> PushActionType.OAUTH_REQUEST
            "CONF_CALL_REQUEST", "CONF_VIDEOCALL_REQUEST" -> PushActionType.VOIP_CALL
            "READ_HISTORY" -> PushActionType.READ_HISTORY
            "READ_STORIES" -> PushActionType.READ_STORIES
            "STORY_DELETED" -> PushActionType.STORY_DELETED
            "MESSAGE_DELETED" -> PushActionType.MESSAGE_DELETED
            "READ_REACTION" -> PushActionType.READ_REACTION
            "CHAT_MESSAGE", "CHAT_CREATED", "ENCRYPTED_MESSAGE", "PINNED" -> PushActionType.NEW_MESSAGE
            else -> if (locKey.contains("MESSAGE", ignoreCase = true)) PushActionType.NEW_MESSAGE else PushActionType.UNKNOWN
        }
    }
}

class ParsePushJsonPayloadUseCase(
    private val determinePushActionType: DeterminePushActionTypeUseCase = DeterminePushActionTypeUseCase()
) {
    private val locKeyRegex = Regex(""""loc_key"\s*:\s*"([^"]*)"""")
    private val channelIdRegex = Regex(""""channel_id"\s*:\s*(\d+)""")
    private val chatIdRegex = Regex(""""chat_id"\s*:\s*(\d+)""")
    private val fromIdRegex = Regex(""""from_id"\s*:\s*(\d+)""")
    private val topicIdRegex = Regex(""""topic_id"\s*:\s*(\d+)""")
    private val msgIdRegex = Regex(""""msg_id"\s*:\s*(\d+)""")
    private val silentRegex = Regex(""""silent"\s*:\s*(\d+)""")
    private val scheduleRegex = Regex(""""schedule"\s*:\s*(\d+)""")

    operator fun invoke(
        pushType: PushType,
        jsonString: String,
        timestamp: Long
    ): PushPayloadModel {
        var locKey = ""
        var channelId = 0L
        var chatId = 0L
        var fromId = 0L
        var topicId = 0
        var msgId = 0
        var isScheduled = false
        var isSilent = false
        var userIdOpt: Long? = null
        val extraMap = mutableMapOf<String, String>()

        try {
            val json = JSONObject(jsonString)
            locKey = json.optString("loc_key", "")
            val customObj = json.optJSONObject("custom") ?: JSONObject()
            val customKeys = customObj.keys()
            while (customKeys.hasNext()) {
                val key = customKeys.next()
                extraMap[key] = customObj.optString(key, "")
            }

            channelId = customObj.optLong("channel_id", 0L)
            chatId = customObj.optLong("chat_id", 0L)
            fromId = customObj.optLong("from_id", 0L)
            topicId = customObj.optInt("topic_id", 0)
            msgId = if (customObj.has("msg_id")) customObj.optInt("msg_id", 0) else customObj.optInt("story_id", 0)
            isScheduled = customObj.optInt("schedule", 0) == 1
            isSilent = customObj.optInt("silent", 0) != 0
            if (json.has("user_id")) {
                userIdOpt = json.optLong("user_id", 0L)
            }
        } catch (t: Throwable) {
            // Safe fallback using Regex parsing (essential for headless JVM test environments)
            locKey = locKeyRegex.find(jsonString)?.groupValues?.get(1) ?: ""
            channelId = channelIdRegex.find(jsonString)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            chatId = chatIdRegex.find(jsonString)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            fromId = fromIdRegex.find(jsonString)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            topicId = topicIdRegex.find(jsonString)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            msgId = msgIdRegex.find(jsonString)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            isSilent = (silentRegex.find(jsonString)?.groupValues?.get(1)?.toIntOrNull() ?: 0) != 0
            isScheduled = (scheduleRegex.find(jsonString)?.groupValues?.get(1)?.toIntOrNull() ?: 0) == 1
        }

        val actionType = determinePushActionType(locKey)

        var calculatedDialogId = 0L
        if (channelId != 0L) {
            calculatedDialogId = -channelId
        } else if (chatId != 0L) {
            calculatedDialogId = -chatId
        } else if (fromId != 0L) {
            calculatedDialogId = fromId
        }

        return PushPayloadModel(
            pushType = pushType,
            locKey = locKey,
            actionType = actionType,
            accountUserId = userIdOpt,
            dialogId = calculatedDialogId,
            topicId = topicId,
            messageId = msgId,
            channelId = channelId,
            chatId = chatId,
            fromId = fromId,
            isScheduled = isScheduled,
            isSilent = isSilent,
            rawJson = jsonString,
            receiveTimeMs = timestamp,
            extraParams = extraMap
        )
    }
}
