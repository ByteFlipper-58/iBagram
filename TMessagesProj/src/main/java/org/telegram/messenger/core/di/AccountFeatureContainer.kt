package org.telegram.messenger.core.di

import org.telegram.messenger.UserConfig
import org.telegram.messenger.feature.chat.data.repository.LegacyChatRepository
import org.telegram.messenger.feature.chat.domain.repository.ChatRepository
import org.telegram.messenger.feature.chat.domain.usecase.DeleteMessagesUseCase
import org.telegram.messenger.feature.chat.domain.usecase.GetMessagesUseCase
import org.telegram.messenger.feature.chat.domain.usecase.LoadHistoryUseCase
import org.telegram.messenger.feature.chat.domain.usecase.ObserveMessagesUseCase
import org.telegram.messenger.feature.chat.domain.usecase.SendMessageUseCase
import org.telegram.messenger.feature.chat.presentation.ChatViewModel
import org.telegram.messenger.feature.dialogs.data.repository.LegacyDialogsRepository
import org.telegram.messenger.feature.dialogs.domain.repository.DialogsRepository
import org.telegram.messenger.feature.dialogs.domain.usecase.DeleteDialogUseCase
import org.telegram.messenger.feature.dialogs.domain.usecase.GetDialogsUseCase
import org.telegram.messenger.feature.dialogs.domain.usecase.LoadMoreDialogsUseCase
import org.telegram.messenger.feature.dialogs.domain.usecase.MarkDialogAsReadUseCase
import org.telegram.messenger.feature.dialogs.domain.usecase.PinDialogUseCase
import org.telegram.messenger.feature.dialogs.presentation.DialogsViewModel
import org.telegram.messenger.feature.savedmessages.data.repository.LegacySavedMessagesRepository
import org.telegram.messenger.feature.savedmessages.domain.repository.SavedMessagesRepository
import org.telegram.messenger.feature.savedmessages.domain.usecase.DeleteSavedDialogUseCase
import org.telegram.messenger.feature.savedmessages.domain.usecase.GetSavedDialogsUseCase
import org.telegram.messenger.feature.savedmessages.domain.usecase.GetSavedTagsUseCase
import org.telegram.messenger.feature.savedmessages.domain.usecase.SearchSavedDialogsUseCase
import org.telegram.messenger.feature.savedmessages.domain.usecase.TogglePinSavedDialogUseCase
import org.telegram.messenger.feature.savedmessages.presentation.SavedMessagesViewModel
import java.util.concurrent.ConcurrentHashMap

/**
 * Scoped service container that manages feature dependencies per [account].
 * Ensures clean lifecycle isolation between multi-account instances without heavyweight reflection.
 */
class AccountFeatureContainer private constructor(val account: Int) {

    private var customSavedMessagesRepository: SavedMessagesRepository? = null

    /**
     * Repository providing Saved Messages operations.
     * Can be replaced or mocked via custom setter for testing.
     */
    var savedMessagesRepository: SavedMessagesRepository
        get() = customSavedMessagesRepository ?: LegacySavedMessagesRepository(account)
        set(value) {
            customSavedMessagesRepository = value
        }

    val getSavedDialogsUseCase: GetSavedDialogsUseCase
        get() = GetSavedDialogsUseCase(savedMessagesRepository)

    val togglePinSavedDialogUseCase: TogglePinSavedDialogUseCase
        get() = TogglePinSavedDialogUseCase(savedMessagesRepository)

    val deleteSavedDialogUseCase: DeleteSavedDialogUseCase
        get() = DeleteSavedDialogUseCase(savedMessagesRepository)

    val getSavedTagsUseCase: GetSavedTagsUseCase
        get() = GetSavedTagsUseCase(savedMessagesRepository)

    val searchSavedDialogsUseCase: SearchSavedDialogsUseCase
        get() = SearchSavedDialogsUseCase(savedMessagesRepository)

    private var cachedSavedMessagesViewModel: SavedMessagesViewModel? = null

    fun getSavedMessagesViewModel(): SavedMessagesViewModel {
        return cachedSavedMessagesViewModel ?: createSavedMessagesViewModel().also {
            cachedSavedMessagesViewModel = it
        }
    }

    fun createSavedMessagesViewModel(): SavedMessagesViewModel {
        return SavedMessagesViewModel(
            account = account,
            getSavedDialogsUseCase = getSavedDialogsUseCase,
            togglePinSavedDialogUseCase = togglePinSavedDialogUseCase,
            deleteSavedDialogUseCase = deleteSavedDialogUseCase,
            getSavedTagsUseCase = getSavedTagsUseCase,
            searchSavedDialogsUseCase = searchSavedDialogsUseCase
        )
    }

    private var customDialogsRepository: DialogsRepository? = null

    var dialogsRepository: DialogsRepository
        get() = customDialogsRepository ?: LegacyDialogsRepository(account)
        set(value) {
            customDialogsRepository = value
        }

    val getDialogsUseCase: GetDialogsUseCase
        get() = GetDialogsUseCase(dialogsRepository)

    val loadMoreDialogsUseCase: LoadMoreDialogsUseCase
        get() = LoadMoreDialogsUseCase(dialogsRepository)

    val pinDialogUseCase: PinDialogUseCase
        get() = PinDialogUseCase(dialogsRepository)

    val deleteDialogUseCase: DeleteDialogUseCase
        get() = DeleteDialogUseCase(dialogsRepository)

    val markDialogAsReadUseCase: MarkDialogAsReadUseCase
        get() = MarkDialogAsReadUseCase(dialogsRepository)

    private var cachedDialogsViewModel: DialogsViewModel? = null

    fun getDialogsViewModel(): DialogsViewModel {
        return cachedDialogsViewModel ?: createDialogsViewModel().also {
            cachedDialogsViewModel = it
        }
    }

    fun createDialogsViewModel(): DialogsViewModel {
        return DialogsViewModel(
            account = account,
            getDialogsUseCase = getDialogsUseCase,
            loadMoreDialogsUseCase = loadMoreDialogsUseCase,
            pinDialogUseCase = pinDialogUseCase,
            deleteDialogUseCase = deleteDialogUseCase,
            markDialogAsReadUseCase = markDialogAsReadUseCase
        )
    }

    private var customChatRepository: ChatRepository? = null

    var chatRepository: ChatRepository
        get() = customChatRepository ?: LegacyChatRepository(account)
        set(value) {
            customChatRepository = value
        }

    val observeMessagesUseCase: ObserveMessagesUseCase
        get() = ObserveMessagesUseCase(chatRepository)

    val getMessagesUseCase: GetMessagesUseCase
        get() = GetMessagesUseCase(chatRepository)

    val loadHistoryUseCase: LoadHistoryUseCase
        get() = LoadHistoryUseCase(chatRepository)

    val sendMessageUseCase: SendMessageUseCase
        get() = SendMessageUseCase(chatRepository)

    val deleteMessagesUseCase: DeleteMessagesUseCase
        get() = DeleteMessagesUseCase(chatRepository)

    private val cachedChatViewModels = ConcurrentHashMap<Long, ChatViewModel>()

    fun getChatViewModel(dialogId: Long): ChatViewModel {
        return cachedChatViewModels.computeIfAbsent(dialogId) { createChatViewModel(it) }
    }

    fun createChatViewModel(dialogId: Long): ChatViewModel {
        return ChatViewModel(
            account = account,
            dialogId = dialogId,
            observeMessagesUseCase = observeMessagesUseCase,
            loadHistoryUseCase = loadHistoryUseCase,
            sendMessageUseCase = sendMessageUseCase,
            deleteMessagesUseCase = deleteMessagesUseCase
        )
    }

    companion object {
        private val instances = ConcurrentHashMap<Int, AccountFeatureContainer>()

        @JvmStatic
        fun get(account: Int): AccountFeatureContainer {
            val safeAccount = if (account in 0 until UserConfig.MAX_ACCOUNT_COUNT) account else 0
            return instances.computeIfAbsent(safeAccount) { AccountFeatureContainer(it) }
        }

        @JvmStatic
        fun reset(account: Int) {
            instances.remove(account)
        }

        @JvmStatic
        fun resetAll() {
            instances.clear()
        }
    }
}
