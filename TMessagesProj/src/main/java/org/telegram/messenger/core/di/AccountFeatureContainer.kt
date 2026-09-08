package org.telegram.messenger.core.di

import org.telegram.messenger.UserConfig
import org.telegram.messenger.feature.chat.data.repository.LegacyChatRepository
import org.telegram.messenger.feature.profile.data.repository.LegacyProfileRepository
import org.telegram.messenger.feature.profile.domain.repository.ProfileRepository
import org.telegram.messenger.feature.profile.domain.usecase.BlockPeerUseCase
import org.telegram.messenger.feature.profile.domain.usecase.GetProfileUseCase
import org.telegram.messenger.feature.profile.domain.usecase.LoadFullProfileUseCase
import org.telegram.messenger.feature.profile.domain.usecase.ObserveProfileUseCase
import org.telegram.messenger.feature.profile.domain.usecase.UnblockPeerUseCase
import org.telegram.messenger.feature.profile.presentation.ProfileViewModel
import org.telegram.messenger.feature.settings.data.repository.LegacySettingsRepository
import org.telegram.messenger.feature.settings.domain.repository.SettingsRepository
import org.telegram.messenger.feature.settings.domain.usecase.GetSettingsUseCase
import org.telegram.messenger.feature.settings.domain.usecase.ObserveSettingsUseCase
import org.telegram.messenger.feature.settings.domain.usecase.UpdateBubbleRadiusUseCase
import org.telegram.messenger.feature.settings.domain.usecase.UpdateFontSizeUseCase
import org.telegram.messenger.feature.settings.domain.usecase.UpdateSaveToGalleryUseCase
import org.telegram.messenger.feature.settings.domain.usecase.UpdateStreamMediaUseCase
import org.telegram.messenger.feature.settings.domain.usecase.UpdateSyncContactsUseCase
import org.telegram.messenger.feature.settings.presentation.SettingsViewModel
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

    private var customProfileRepository: ProfileRepository? = null

    var profileRepository: ProfileRepository
        get() = customProfileRepository ?: LegacyProfileRepository(account)
        set(value) {
            customProfileRepository = value
        }

    val observeProfileUseCase: ObserveProfileUseCase
        get() = ObserveProfileUseCase(profileRepository)

    val getProfileUseCase: GetProfileUseCase
        get() = GetProfileUseCase(profileRepository)

    val loadFullProfileUseCase: LoadFullProfileUseCase
        get() = LoadFullProfileUseCase(profileRepository)

    val blockPeerUseCase: BlockPeerUseCase
        get() = BlockPeerUseCase(profileRepository)

    val unblockPeerUseCase: UnblockPeerUseCase
        get() = UnblockPeerUseCase(profileRepository)

    private val cachedProfileViewModels = ConcurrentHashMap<Long, ProfileViewModel>()

    fun getProfileViewModel(peerId: Long): ProfileViewModel {
        return cachedProfileViewModels.computeIfAbsent(peerId) { createProfileViewModel(it) }
    }

    fun createProfileViewModel(peerId: Long): ProfileViewModel {
        return ProfileViewModel(
            account = account,
            peerId = peerId,
            observeProfileUseCase = observeProfileUseCase,
            loadFullProfileUseCase = loadFullProfileUseCase,
            blockPeerUseCase = blockPeerUseCase,
            unblockPeerUseCase = unblockPeerUseCase
        )
    }

    private var customSettingsRepository: SettingsRepository? = null

    var settingsRepository: SettingsRepository
        get() = customSettingsRepository ?: LegacySettingsRepository(account)
        set(value) {
            customSettingsRepository = value
        }

    val observeSettingsUseCase: ObserveSettingsUseCase
        get() = ObserveSettingsUseCase(settingsRepository)

    val getSettingsUseCase: GetSettingsUseCase
        get() = GetSettingsUseCase(settingsRepository)

    val updateFontSizeUseCase: UpdateFontSizeUseCase
        get() = UpdateFontSizeUseCase(settingsRepository)

    val updateBubbleRadiusUseCase: UpdateBubbleRadiusUseCase
        get() = UpdateBubbleRadiusUseCase(settingsRepository)

    val updateSaveToGalleryUseCase: UpdateSaveToGalleryUseCase
        get() = UpdateSaveToGalleryUseCase(settingsRepository)

    val updateStreamMediaUseCase: UpdateStreamMediaUseCase
        get() = UpdateStreamMediaUseCase(settingsRepository)

    val updateSyncContactsUseCase: UpdateSyncContactsUseCase
        get() = UpdateSyncContactsUseCase(settingsRepository)

    private var cachedSettingsViewModel: SettingsViewModel? = null

    val settingsViewModel: SettingsViewModel
        get() {
            var vm = cachedSettingsViewModel
            if (vm == null) {
                vm = createSettingsViewModel()
                cachedSettingsViewModel = vm
            }
            return vm
        }

    fun createSettingsViewModel(): SettingsViewModel {
        return SettingsViewModel(
            observeSettingsUseCase = observeSettingsUseCase,
            getSettingsUseCase = getSettingsUseCase,
            updateFontSizeUseCase = updateFontSizeUseCase,
            updateBubbleRadiusUseCase = updateBubbleRadiusUseCase,
            updateSaveToGalleryUseCase = updateSaveToGalleryUseCase,
            updateStreamMediaUseCase = updateStreamMediaUseCase,
            updateSyncContactsUseCase = updateSyncContactsUseCase
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
