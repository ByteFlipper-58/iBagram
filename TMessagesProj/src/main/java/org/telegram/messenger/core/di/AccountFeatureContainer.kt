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
import org.telegram.messenger.feature.media.data.repository.LegacyMediaRepository
import org.telegram.messenger.feature.media.domain.repository.MediaRepository
import org.telegram.messenger.feature.media.domain.usecase.GetAlbumMediaUseCase
import org.telegram.messenger.feature.media.domain.usecase.GetAllMediaUseCase
import org.telegram.messenger.feature.media.domain.usecase.GetMediaAlbumsUseCase
import org.telegram.messenger.feature.media.domain.usecase.ObserveMediaAlbumsUseCase
import org.telegram.messenger.feature.media.presentation.MediaViewModel
import org.telegram.messenger.feature.voip.data.repository.LegacyVoIPRepository
import org.telegram.messenger.feature.voip.domain.repository.VoIPRepository
import org.telegram.messenger.feature.voip.domain.usecase.AcceptCallUseCase
import org.telegram.messenger.feature.voip.domain.usecase.DeclineCallUseCase
import org.telegram.messenger.feature.voip.domain.usecase.GetCurrentCallUseCase
import org.telegram.messenger.feature.voip.domain.usecase.HangUpCallUseCase
import org.telegram.messenger.feature.voip.domain.usecase.ObserveCurrentCallUseCase
import org.telegram.messenger.feature.voip.domain.usecase.StartCallUseCase
import org.telegram.messenger.feature.voip.domain.usecase.ToggleMuteUseCase
import org.telegram.messenger.feature.voip.domain.usecase.ToggleSpeakerphoneUseCase
import org.telegram.messenger.feature.voip.presentation.CallViewModel
import org.telegram.messenger.feature.secretchat.data.repository.LegacySecretChatRepository
import org.telegram.messenger.feature.secretchat.domain.repository.SecretChatRepository
import org.telegram.messenger.feature.secretchat.domain.usecase.AcceptSecretChatUseCase
import org.telegram.messenger.feature.secretchat.domain.usecase.DeclineSecretChatUseCase
import org.telegram.messenger.feature.secretchat.domain.usecase.GetSecretChatUseCase
import org.telegram.messenger.feature.secretchat.domain.usecase.ObserveSecretChatUseCase
import org.telegram.messenger.feature.secretchat.domain.usecase.ObserveSecretChatsUseCase
import org.telegram.messenger.feature.secretchat.domain.usecase.SendScreenshotNotificationUseCase
import org.telegram.messenger.feature.secretchat.domain.usecase.SetSecretChatTtlUseCase
import org.telegram.messenger.feature.secretchat.domain.usecase.StartSecretChatUseCase
import org.telegram.messenger.feature.secretchat.presentation.SecretChatViewModel
import org.telegram.messenger.feature.contacts.data.repository.LegacyContactsRepository
import org.telegram.messenger.feature.contacts.domain.repository.ContactsRepository
import org.telegram.messenger.feature.contacts.domain.usecase.AddContactUseCase
import org.telegram.messenger.feature.contacts.domain.usecase.DeleteContactUseCase
import org.telegram.messenger.feature.contacts.domain.usecase.GetContactUseCase
import org.telegram.messenger.feature.contacts.domain.usecase.GetContactsUseCase
import org.telegram.messenger.feature.contacts.domain.usecase.ObserveContactsUseCase
import org.telegram.messenger.feature.contacts.domain.usecase.SearchContactsUseCase
import org.telegram.messenger.feature.contacts.presentation.ContactsViewModel
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

    private var customMediaRepository: MediaRepository? = null

    var mediaRepository: MediaRepository
        get() = customMediaRepository ?: LegacyMediaRepository()
        set(value) {
            customMediaRepository = value
        }

    val observeMediaAlbumsUseCase: ObserveMediaAlbumsUseCase
        get() = ObserveMediaAlbumsUseCase(mediaRepository)

    val getMediaAlbumsUseCase: GetMediaAlbumsUseCase
        get() = GetMediaAlbumsUseCase(mediaRepository)

    val getAlbumMediaUseCase: GetAlbumMediaUseCase
        get() = GetAlbumMediaUseCase(mediaRepository)

    val getAllMediaUseCase: GetAllMediaUseCase
        get() = GetAllMediaUseCase(mediaRepository)

    private var cachedMediaViewModel: MediaViewModel? = null

    val mediaViewModel: MediaViewModel
        get() {
            var vm = cachedMediaViewModel
            if (vm == null) {
                vm = createMediaViewModel()
                cachedMediaViewModel = vm
            }
            return vm
        }

    fun createMediaViewModel(): MediaViewModel {
        return MediaViewModel(
            observeMediaAlbumsUseCase = observeMediaAlbumsUseCase,
            getMediaAlbumsUseCase = getMediaAlbumsUseCase,
            getAlbumMediaUseCase = getAlbumMediaUseCase,
            getAllMediaUseCase = getAllMediaUseCase
        )
    }

    private var customVoIPRepository: VoIPRepository? = null

    var voipRepository: VoIPRepository
        get() = customVoIPRepository ?: LegacyVoIPRepository(account)
        set(value) {
            customVoIPRepository = value
        }

    val observeCurrentCallUseCase: ObserveCurrentCallUseCase
        get() = ObserveCurrentCallUseCase(voipRepository)

    val getCurrentCallUseCase: GetCurrentCallUseCase
        get() = GetCurrentCallUseCase(voipRepository)

    val startCallUseCase: StartCallUseCase
        get() = StartCallUseCase(voipRepository)

    val acceptCallUseCase: AcceptCallUseCase
        get() = AcceptCallUseCase(voipRepository)

    val declineCallUseCase: DeclineCallUseCase
        get() = DeclineCallUseCase(voipRepository)

    val hangUpCallUseCase: HangUpCallUseCase
        get() = HangUpCallUseCase(voipRepository)

    val toggleMuteUseCase: ToggleMuteUseCase
        get() = ToggleMuteUseCase(voipRepository)

    val toggleSpeakerphoneUseCase: ToggleSpeakerphoneUseCase
        get() = ToggleSpeakerphoneUseCase(voipRepository)

    private var cachedCallViewModel: CallViewModel? = null

    val callViewModel: CallViewModel
        get() {
            var vm = cachedCallViewModel
            if (vm == null) {
                vm = createCallViewModel()
                cachedCallViewModel = vm
            }
            return vm
        }

    fun createCallViewModel(): CallViewModel {
        return CallViewModel(
            observeCurrentCallUseCase = observeCurrentCallUseCase,
            getCurrentCallUseCase = getCurrentCallUseCase,
            startCallUseCase = startCallUseCase,
            acceptCallUseCase = acceptCallUseCase,
            declineCallUseCase = declineCallUseCase,
            hangUpCallUseCase = hangUpCallUseCase,
            toggleMuteUseCase = toggleMuteUseCase,
            toggleSpeakerphoneUseCase = toggleSpeakerphoneUseCase
        )
    }

    private var customSecretChatRepository: SecretChatRepository? = null

    var secretChatRepository: SecretChatRepository
        get() = customSecretChatRepository ?: LegacySecretChatRepository(account)
        set(value) {
            customSecretChatRepository = value
        }

    val observeSecretChatUseCase: ObserveSecretChatUseCase
        get() = ObserveSecretChatUseCase(secretChatRepository)

    val observeSecretChatsUseCase: ObserveSecretChatsUseCase
        get() = ObserveSecretChatsUseCase(secretChatRepository)

    val getSecretChatUseCase: GetSecretChatUseCase
        get() = GetSecretChatUseCase(secretChatRepository)

    val startSecretChatUseCase: StartSecretChatUseCase
        get() = StartSecretChatUseCase(secretChatRepository)

    val acceptSecretChatUseCase: AcceptSecretChatUseCase
        get() = AcceptSecretChatUseCase(secretChatRepository)

    val declineSecretChatUseCase: DeclineSecretChatUseCase
        get() = DeclineSecretChatUseCase(secretChatRepository)

    val setSecretChatTtlUseCase: SetSecretChatTtlUseCase
        get() = SetSecretChatTtlUseCase(secretChatRepository)

    val sendScreenshotNotificationUseCase: SendScreenshotNotificationUseCase
        get() = SendScreenshotNotificationUseCase(secretChatRepository)

    private val cachedSecretChatViewModels = ConcurrentHashMap<Int, SecretChatViewModel>()

    fun getSecretChatViewModel(chatId: Int): SecretChatViewModel {
        return cachedSecretChatViewModels.computeIfAbsent(chatId) { createSecretChatViewModel(it) }
    }

    fun createSecretChatViewModel(chatId: Int): SecretChatViewModel {
        return SecretChatViewModel(
            chatId = chatId,
            observeSecretChatUseCase = observeSecretChatUseCase,
            getSecretChatUseCase = getSecretChatUseCase,
            acceptSecretChatUseCase = acceptSecretChatUseCase,
            declineSecretChatUseCase = declineSecretChatUseCase,
            setSecretChatTtlUseCase = setSecretChatTtlUseCase,
            sendScreenshotNotificationUseCase = sendScreenshotNotificationUseCase
        )
    }

    private var customContactsRepository: ContactsRepository? = null

    var contactsRepository: ContactsRepository
        get() = customContactsRepository ?: LegacyContactsRepository(account)
        set(value) {
            customContactsRepository = value
        }

    val observeContactsUseCase: ObserveContactsUseCase
        get() = ObserveContactsUseCase(contactsRepository)

    val getContactsUseCase: GetContactsUseCase
        get() = GetContactsUseCase(contactsRepository)

    val getContactUseCase: GetContactUseCase
        get() = GetContactUseCase(contactsRepository)

    val addContactUseCase: AddContactUseCase
        get() = AddContactUseCase(contactsRepository)

    val deleteContactUseCase: DeleteContactUseCase
        get() = DeleteContactUseCase(contactsRepository)

    val searchContactsUseCase: SearchContactsUseCase
        get() = SearchContactsUseCase(contactsRepository)

    private var cachedContactsViewModel: ContactsViewModel? = null

    val contactsViewModel: ContactsViewModel
        get() {
            var vm = cachedContactsViewModel
            if (vm == null) {
                vm = createContactsViewModel()
                cachedContactsViewModel = vm
            }
            return vm
        }

    fun createContactsViewModel(): ContactsViewModel {
        return ContactsViewModel(
            observeContactsUseCase = observeContactsUseCase,
            getContactsUseCase = getContactsUseCase,
            getContactUseCase = getContactUseCase,
            addContactUseCase = addContactUseCase,
            deleteContactUseCase = deleteContactUseCase,
            searchContactsUseCase = searchContactsUseCase
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
