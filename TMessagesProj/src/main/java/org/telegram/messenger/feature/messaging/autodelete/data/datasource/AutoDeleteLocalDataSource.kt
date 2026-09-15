package org.telegram.messenger.feature.messaging.autodelete.data.datasource

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.UserConfig
import org.telegram.messenger.feature.messaging.autodelete.domain.model.AutoDeleteTtlModel
import org.telegram.messenger.feature.messaging.autodelete.domain.model.GlobalAutoDeleteStateModel
import java.util.concurrent.ConcurrentHashMap

class AutoDeleteLocalDataSource(
    private val currentAccount: Int
) {
    private val isLegacyAvailable: Boolean
        get() = ApplicationLoader.applicationContext != null

    private val userConfig: UserConfig?
        get() = if (isLegacyAvailable) UserConfig.getInstance(currentAccount) else null

    private val messagesController: MessagesController?
        get() = if (isLegacyAvailable) MessagesController.getInstance(currentAccount) else null

    private val notificationCenter: NotificationCenter?
        get() = if (isLegacyAvailable) NotificationCenter.getInstance(currentAccount) else null

    private val chatTtls = ConcurrentHashMap<Long, Int>()

    private val _globalAutoDeleteFlow = MutableStateFlow(
        GlobalAutoDeleteStateModel(
            ttl = AutoDeleteTtlModel(userConfig?.globalTTl?.let { it * 60 } ?: 0)
        )
    )
    val globalAutoDeleteFlow: StateFlow<GlobalAutoDeleteStateModel> = _globalAutoDeleteFlow.asStateFlow()

    fun getGlobalTtl(): AutoDeleteTtlModel {
        val seconds = userConfig?.globalTTl?.let { it * 60 } ?: _globalAutoDeleteFlow.value.ttl.periodSeconds
        return AutoDeleteTtlModel(seconds)
    }

    fun setGlobalTtl(periodSeconds: Int) {
        val minutes = periodSeconds / 60
        userConfig?.setGlobalTtl(minutes)
        notificationCenter?.postNotificationName(NotificationCenter.didUpdateGlobalAutoDeleteTimer)
        _globalAutoDeleteFlow.value = GlobalAutoDeleteStateModel(
            ttl = AutoDeleteTtlModel(periodSeconds),
            isLoading = false
        )
    }

    fun setGlobalLoading(isLoading: Boolean) {
        _globalAutoDeleteFlow.value = _globalAutoDeleteFlow.value.copy(isLoading = isLoading)
    }

    fun getChatTtl(chatId: Long): AutoDeleteTtlModel {
        val cached = chatTtls[chatId]
        if (cached != null) {
            return AutoDeleteTtlModel(cached)
        }

        val mc = messagesController
        if (mc != null) {
            val dialog = mc.dialogs_dict.get(chatId)
            val ttl = dialog?.ttl_period ?: run {
                if (chatId > 0) {
                    mc.getUserFull(chatId)?.ttl_period ?: 0
                } else {
                    mc.getChatFull(-chatId)?.ttl_period ?: 0
                }
            }
            chatTtls[chatId] = ttl
            return AutoDeleteTtlModel(ttl)
        }

        return AutoDeleteTtlModel.OFF
    }

    fun setChatTtl(chatId: Long, periodSeconds: Int) {
        chatTtls[chatId] = periodSeconds
        messagesController?.setDialogHistoryTTL(chatId, periodSeconds)
    }

    fun setChatsAutoDeleteBatch(chatIds: List<Long>, periodSeconds: Int) {
        for (id in chatIds) {
            chatTtls[id] = periodSeconds
            messagesController?.setDialogHistoryTTL(id, periodSeconds)
        }
    }
}
