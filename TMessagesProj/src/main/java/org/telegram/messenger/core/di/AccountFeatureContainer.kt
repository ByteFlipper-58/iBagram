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
import org.telegram.messenger.feature.folders.data.repository.LegacyFoldersRepository
import org.telegram.messenger.feature.folders.domain.repository.FoldersRepository
import org.telegram.messenger.feature.folders.domain.usecase.CreateFolderUseCase
import org.telegram.messenger.feature.folders.domain.usecase.DeleteFolderUseCase
import org.telegram.messenger.feature.folders.domain.usecase.GetFolderUseCase
import org.telegram.messenger.feature.folders.domain.usecase.GetFoldersUseCase
import org.telegram.messenger.feature.folders.domain.usecase.GetSuggestedFoldersUseCase
import org.telegram.messenger.feature.folders.domain.usecase.ObserveFoldersUseCase
import org.telegram.messenger.feature.folders.domain.usecase.ReorderFoldersUseCase
import org.telegram.messenger.feature.folders.domain.usecase.UpdateFolderUseCase
import org.telegram.messenger.feature.folders.presentation.FoldersViewModel
import org.telegram.messenger.feature.stickers.data.repository.LegacyStickersRepository
import org.telegram.messenger.feature.stickers.domain.repository.StickersRepository
import org.telegram.messenger.feature.stickers.domain.usecase.GetRecentStickersUseCase
import org.telegram.messenger.feature.stickers.domain.usecase.GetStickerSetUseCase
import org.telegram.messenger.feature.stickers.domain.usecase.GetStickerSetsUseCase
import org.telegram.messenger.feature.stickers.domain.usecase.GetStickersForEmojiUseCase
import org.telegram.messenger.feature.stickers.domain.usecase.ObserveStickerSetsUseCase
import org.telegram.messenger.feature.stickers.domain.usecase.ToggleStickerSetArchivedUseCase
import org.telegram.messenger.feature.stickers.domain.usecase.ToggleStickerSetInstalledUseCase
import org.telegram.messenger.feature.stickers.presentation.StickersViewModel
import org.telegram.messenger.feature.fileloader.data.repository.LegacyFileLoaderRepository
import org.telegram.messenger.feature.fileloader.domain.repository.FileLoaderRepository
import org.telegram.messenger.feature.fileloader.domain.usecase.CancelAllDownloadsUseCase
import org.telegram.messenger.feature.fileloader.domain.usecase.CancelFileUploadUseCase
import org.telegram.messenger.feature.fileloader.domain.usecase.CancelLoadFileUseCase
import org.telegram.messenger.feature.fileloader.domain.usecase.GetActiveDownloadsUseCase
import org.telegram.messenger.feature.fileloader.domain.usecase.GetRecentDownloadsUseCase
import org.telegram.messenger.feature.fileloader.domain.usecase.LoadFileUseCase
import org.telegram.messenger.feature.fileloader.domain.usecase.ObserveTransferUseCase
import org.telegram.messenger.feature.fileloader.domain.usecase.ObserveTransfersUseCase
import org.telegram.messenger.feature.fileloader.domain.usecase.UploadFileUseCase
import org.telegram.messenger.feature.fileloader.presentation.FileLoaderViewModel
import org.telegram.messenger.feature.search.data.repository.LegacySearchRepository
import org.telegram.messenger.feature.search.domain.repository.SearchRepository
import org.telegram.messenger.feature.search.domain.usecase.ClearRecentHashtagsUseCase
import org.telegram.messenger.feature.search.domain.usecase.ClearRecentSearchesUseCase
import org.telegram.messenger.feature.search.domain.usecase.GetRecentHashtagsUseCase
import org.telegram.messenger.feature.search.domain.usecase.GetRecentSearchesUseCase
import org.telegram.messenger.feature.search.domain.usecase.PutRecentHashtagUseCase
import org.telegram.messenger.feature.search.domain.usecase.RemoveRecentSearchUseCase
import org.telegram.messenger.feature.search.domain.usecase.SearchGlobalUseCase
import org.telegram.messenger.feature.search.domain.usecase.SearchLocalUseCase
import org.telegram.messenger.feature.search.presentation.SearchViewModel
import org.telegram.messenger.feature.notifications.data.repository.LegacyNotificationsRepository
import org.telegram.messenger.feature.notifications.domain.repository.NotificationsRepository
import org.telegram.messenger.feature.notifications.domain.usecase.GetBadgeSettingsUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.GetBadgeUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.GetNotificationSettingsUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.IsDialogMutedUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.MuteDialogUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.ObserveBadgeSettingsUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.ObserveBadgeUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.ObserveNotificationSettingsUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.RefreshBadgeUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.ToggleContactJoinedNotificationsUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.ToggleInAppPreviewUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.ToggleInAppSoundsUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.ToggleInAppVibrateUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.ToggleInChatSoundUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.TogglePeerNotificationsUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.TogglePinnedMessagesNotificationsUseCase
import org.telegram.messenger.feature.notifications.domain.usecase.UpdateBadgeSettingsUseCase
import org.telegram.messenger.feature.notifications.presentation.NotificationsViewModel
import org.telegram.messenger.feature.privacy.data.repository.LegacyPrivacyRepository
import org.telegram.messenger.feature.privacy.domain.repository.PrivacyRepository
import org.telegram.messenger.feature.privacy.domain.usecase.BlockPrivacyPeerUseCase
import org.telegram.messenger.feature.privacy.domain.usecase.CheckPasscodeUseCase
import org.telegram.messenger.feature.privacy.domain.usecase.ClearPasscodeUseCase
import org.telegram.messenger.feature.privacy.domain.usecase.GetBlockedPeersUseCase
import org.telegram.messenger.feature.privacy.domain.usecase.GetPasscodeSettingsUseCase
import org.telegram.messenger.feature.privacy.domain.usecase.GetPrivacyRulesUseCase
import org.telegram.messenger.feature.privacy.domain.usecase.LoadPrivacyRulesUseCase
import org.telegram.messenger.feature.privacy.domain.usecase.LoadTwoStepVerificationUseCase
import org.telegram.messenger.feature.privacy.domain.usecase.ObserveBlockedPeersUseCase
import org.telegram.messenger.feature.privacy.domain.usecase.ObservePrivacyRulesUseCase
import org.telegram.messenger.feature.privacy.domain.usecase.ObserveTwoStepVerificationUseCase
import org.telegram.messenger.feature.privacy.domain.usecase.SetPasscodeUseCase
import org.telegram.messenger.feature.privacy.domain.usecase.SetPrivacyRuleUseCase
import org.telegram.messenger.feature.privacy.domain.usecase.UnblockPrivacyPeerUseCase
import org.telegram.messenger.feature.privacy.presentation.PrivacyViewModel
import org.telegram.messenger.feature.themes.data.repository.LegacyThemeRepository
import org.telegram.messenger.feature.themes.domain.repository.ThemeRepository
import org.telegram.messenger.feature.themes.domain.usecase.ApplyThemeUseCase
import org.telegram.messenger.feature.themes.domain.usecase.GetAppearanceSettingsUseCase
import org.telegram.messenger.feature.themes.domain.usecase.GetAvailableThemesUseCase
import org.telegram.messenger.feature.themes.domain.usecase.ObserveAppearanceSettingsUseCase
import org.telegram.messenger.feature.themes.domain.usecase.ObserveAvailableThemesUseCase
import org.telegram.messenger.feature.themes.domain.usecase.ObserveNightModeUseCase
import org.telegram.messenger.feature.themes.domain.usecase.ResetAppearanceSettingsUseCase
import org.telegram.messenger.feature.themes.domain.usecase.SetBubbleRadiusUseCase
import org.telegram.messenger.feature.themes.domain.usecase.SetNightModeSettingsUseCase
import org.telegram.messenger.feature.themes.domain.usecase.SetNightModeTypeUseCase
import org.telegram.messenger.feature.themes.domain.usecase.SetThemeAccentUseCase
import org.telegram.messenger.feature.themes.presentation.ThemeViewModel
import org.telegram.messenger.feature.stories.data.repository.LegacyStoriesRepository
import org.telegram.messenger.feature.stories.domain.repository.StoriesRepository
import org.telegram.messenger.feature.stories.domain.usecase.ActivateStealthModeUseCase
import org.telegram.messenger.feature.stories.domain.usecase.DeleteStoryUseCase
import org.telegram.messenger.feature.stories.domain.usecase.GetPeerStoriesUseCase
import org.telegram.messenger.feature.stories.domain.usecase.GetStoryLimitUseCase
import org.telegram.messenger.feature.stories.domain.usecase.MarkStoryAsReadUseCase
import org.telegram.messenger.feature.stories.domain.usecase.ObserveHiddenStoriesUseCase
import org.telegram.messenger.feature.stories.domain.usecase.ObserveSelfStoriesUseCase
import org.telegram.messenger.feature.stories.domain.usecase.ObserveStealthModeUseCase
import org.telegram.messenger.feature.stories.domain.usecase.ObserveStoriesUseCase
import org.telegram.messenger.feature.stories.domain.usecase.RefreshStoriesUseCase
import org.telegram.messenger.feature.stories.domain.usecase.ToggleStoryHiddenUseCase
import org.telegram.messenger.feature.stories.domain.usecase.ToggleStoryPinUseCase
import org.telegram.messenger.feature.stories.presentation.StoriesViewModel
import org.telegram.messenger.feature.payments.data.repository.LegacyPaymentsRepository
import org.telegram.messenger.feature.payments.domain.repository.PaymentsRepository
import org.telegram.messenger.feature.payments.domain.usecase.GetStarSubscriptionsUseCase
import org.telegram.messenger.feature.payments.domain.usecase.GetStarTopupOptionsUseCase
import org.telegram.messenger.feature.payments.domain.usecase.GetStarTransactionsUseCase
import org.telegram.messenger.feature.payments.domain.usecase.GetStarsBalanceUseCase
import org.telegram.messenger.feature.payments.domain.usecase.ObserveStarSubscriptionsUseCase
import org.telegram.messenger.feature.payments.domain.usecase.ObserveStarTransactionsUseCase
import org.telegram.messenger.feature.payments.domain.usecase.ObserveStarsBalanceUseCase
import org.telegram.messenger.feature.payments.domain.usecase.RefreshStarSubscriptionsUseCase
import org.telegram.messenger.feature.payments.domain.usecase.RefreshStarTransactionsUseCase
import org.telegram.messenger.feature.payments.domain.usecase.RefreshStarsBalanceUseCase
import org.telegram.messenger.feature.payments.presentation.PaymentsViewModel
import org.telegram.messenger.feature.datastorage.data.repository.LegacyDataStorageRepository
import org.telegram.messenger.feature.datastorage.domain.repository.DataStorageRepository
import org.telegram.messenger.feature.datastorage.domain.usecase.ClearCacheUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.ClearDatabaseUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.GetAutoDownloadPresetUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.GetKeepMediaSettingsUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.GetNetworkUsageUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.GetStorageUsageUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.ObserveAutoDownloadPresetUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.ObserveKeepMediaSettingsUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.ObserveNetworkUsageUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.ObserveStorageUsageUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.RefreshStorageUsageUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.ResetNetworkUsageUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.UpdateAutoDownloadPresetUseCase
import org.telegram.messenger.feature.datastorage.domain.usecase.UpdateKeepMediaUseCase
import org.telegram.messenger.feature.datastorage.presentation.DataStorageViewModel
import org.telegram.messenger.feature.topics.data.repository.LegacyTopicsRepository
import org.telegram.messenger.feature.topics.domain.repository.TopicsRepository
import org.telegram.messenger.feature.topics.domain.usecase.DeleteTopicsUseCase
import org.telegram.messenger.feature.topics.domain.usecase.GetForumUnreadCountUseCase
import org.telegram.messenger.feature.topics.domain.usecase.GetTopicUseCase
import org.telegram.messenger.feature.topics.domain.usecase.GetTopicsUseCase
import org.telegram.messenger.feature.topics.domain.usecase.LoadTopicsUseCase
import org.telegram.messenger.feature.topics.domain.usecase.MarkTopicReactionsAsReadUseCase
import org.telegram.messenger.feature.topics.domain.usecase.ObserveForumUnreadCountUseCase
import org.telegram.messenger.feature.topics.domain.usecase.ObserveTopicsUseCase
import org.telegram.messenger.feature.topics.domain.usecase.ReloadTopicsUseCase
import org.telegram.messenger.feature.topics.domain.usecase.ReorderPinnedTopicsUseCase
import org.telegram.messenger.feature.topics.domain.usecase.ToggleCloseTopicUseCase
import org.telegram.messenger.feature.topics.domain.usecase.TogglePinTopicUseCase
import org.telegram.messenger.feature.topics.domain.usecase.ToggleShowTopicUseCase
import org.telegram.messenger.feature.topics.presentation.TopicsViewModel
import org.telegram.messenger.feature.location.data.repository.LegacyLocationRepository
import org.telegram.messenger.feature.location.domain.repository.LocationRepository
import org.telegram.messenger.feature.location.domain.usecase.GetActiveSharingsUseCase
import org.telegram.messenger.feature.location.domain.usecase.GetLastKnownLocationUseCase
import org.telegram.messenger.feature.location.domain.usecase.GetSharingInfoUseCase
import org.telegram.messenger.feature.location.domain.usecase.IsSharingLocationUseCase
import org.telegram.messenger.feature.location.domain.usecase.LoadPeerLiveLocationsUseCase
import org.telegram.messenger.feature.location.domain.usecase.MarkLiveLocationsAsReadUseCase
import org.telegram.messenger.feature.location.domain.usecase.ObserveActiveSharingsUseCase
import org.telegram.messenger.feature.location.domain.usecase.ObserveLastKnownLocationUseCase
import org.telegram.messenger.feature.location.domain.usecase.ObservePeerLocationsUseCase
import org.telegram.messenger.feature.location.domain.usecase.SendLiveLocationUseCase
import org.telegram.messenger.feature.location.domain.usecase.SendStaticLocationUseCase
import org.telegram.messenger.feature.location.domain.usecase.SetProximityAlertUseCase
import org.telegram.messenger.feature.location.domain.usecase.StopAllLocationSharingsUseCase
import org.telegram.messenger.feature.location.domain.usecase.StopLocationSharingUseCase
import org.telegram.messenger.feature.location.presentation.LocationViewModel
import org.telegram.messenger.feature.sessions.data.repository.LegacySessionsRepository
import org.telegram.messenger.feature.sessions.domain.repository.SessionsRepository
import org.telegram.messenger.feature.sessions.domain.usecase.AcceptQrLoginUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.GetSessionsUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.GetWebSessionsUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.LoadSessionsUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.LoadWebSessionsUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.ObserveSessionsUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.ObserveWebSessionsUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.SetSessionsTtlUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.TerminateAllOtherSessionsUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.TerminateAllWebSessionsUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.TerminateSessionUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.TerminateWebSessionUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.UpdateSessionSettingsUseCase
import org.telegram.messenger.feature.sessions.presentation.SessionsViewModel
import org.telegram.messenger.feature.translate.data.repository.LegacyTranslationRepository
import org.telegram.messenger.feature.translate.domain.repository.TranslationRepository
import org.telegram.messenger.feature.translate.domain.usecase.AddDoNotTranslateLanguageUseCase
import org.telegram.messenger.feature.translate.domain.usecase.ApplyAppLanguageUseCase
import org.telegram.messenger.feature.translate.domain.usecase.GetAvailableLanguagesUseCase
import org.telegram.messenger.feature.translate.domain.usecase.GetDialogTranslationStateUseCase
import org.telegram.messenger.feature.translate.domain.usecase.GetTranslateSettingsUseCase
import org.telegram.messenger.feature.translate.domain.usecase.ObserveDialogTranslationStateUseCase
import org.telegram.messenger.feature.translate.domain.usecase.ObserveTranslateSettingsUseCase
import org.telegram.messenger.feature.translate.domain.usecase.RemoveDoNotTranslateLanguageUseCase
import org.telegram.messenger.feature.translate.domain.usecase.SetChatTranslateEnabledUseCase
import org.telegram.messenger.feature.translate.domain.usecase.SetContextTranslateEnabledUseCase
import org.telegram.messenger.feature.translate.domain.usecase.SetDialogTargetLanguageUseCase
import org.telegram.messenger.feature.translate.domain.usecase.SetDoNotTranslateLanguagesUseCase
import org.telegram.messenger.feature.translate.domain.usecase.ToggleDialogTranslatingUseCase
import org.telegram.messenger.feature.translate.domain.usecase.TranslateTextUseCase
import org.telegram.messenger.feature.translate.presentation.TranslateViewModel
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

    private var customFoldersRepository: FoldersRepository? = null

    var foldersRepository: FoldersRepository
        get() = customFoldersRepository ?: LegacyFoldersRepository(account)
        set(value) {
            customFoldersRepository = value
        }

    val observeFoldersUseCase: ObserveFoldersUseCase
        get() = ObserveFoldersUseCase(foldersRepository)

    val getFoldersUseCase: GetFoldersUseCase
        get() = GetFoldersUseCase(foldersRepository)

    val getFolderUseCase: GetFolderUseCase
        get() = GetFolderUseCase(foldersRepository)

    val createFolderUseCase: CreateFolderUseCase
        get() = CreateFolderUseCase(foldersRepository)

    val updateFolderUseCase: UpdateFolderUseCase
        get() = UpdateFolderUseCase(foldersRepository)

    val deleteFolderUseCase: DeleteFolderUseCase
        get() = DeleteFolderUseCase(foldersRepository)

    val reorderFoldersUseCase: ReorderFoldersUseCase
        get() = ReorderFoldersUseCase(foldersRepository)

    val getSuggestedFoldersUseCase: GetSuggestedFoldersUseCase
        get() = GetSuggestedFoldersUseCase(foldersRepository)

    private var cachedFoldersViewModel: FoldersViewModel? = null

    val foldersViewModel: FoldersViewModel
        get() {
            var vm = cachedFoldersViewModel
            if (vm == null) {
                vm = createFoldersViewModel()
                cachedFoldersViewModel = vm
            }
            return vm
        }

    fun createFoldersViewModel(): FoldersViewModel {
        return FoldersViewModel(
            observeFoldersUseCase = observeFoldersUseCase,
            getFoldersUseCase = getFoldersUseCase,
            getFolderUseCase = getFolderUseCase,
            createFolderUseCase = createFolderUseCase,
            updateFolderUseCase = updateFolderUseCase,
            deleteFolderUseCase = deleteFolderUseCase,
            reorderFoldersUseCase = reorderFoldersUseCase,
            getSuggestedFoldersUseCase = getSuggestedFoldersUseCase
        )
    }

    private var customStickersRepository: StickersRepository? = null

    var stickersRepository: StickersRepository
        get() = customStickersRepository ?: LegacyStickersRepository(account)
        set(value) {
            customStickersRepository = value
        }

    val observeStickerSetsUseCase: ObserveStickerSetsUseCase
        get() = ObserveStickerSetsUseCase(stickersRepository)

    val getStickerSetsUseCase: GetStickerSetsUseCase
        get() = GetStickerSetsUseCase(stickersRepository)

    val getStickerSetUseCase: GetStickerSetUseCase
        get() = GetStickerSetUseCase(stickersRepository)

    val getRecentStickersUseCase: GetRecentStickersUseCase
        get() = GetRecentStickersUseCase(stickersRepository)

    val getStickersForEmojiUseCase: GetStickersForEmojiUseCase
        get() = GetStickersForEmojiUseCase(stickersRepository)

    val toggleStickerSetInstalledUseCase: ToggleStickerSetInstalledUseCase
        get() = ToggleStickerSetInstalledUseCase(stickersRepository)

    val toggleStickerSetArchivedUseCase: ToggleStickerSetArchivedUseCase
        get() = ToggleStickerSetArchivedUseCase(stickersRepository)

    private val cachedStickersViewModels = ConcurrentHashMap<Int, StickersViewModel>()

    val stickersViewModel: StickersViewModel
        get() = getStickersViewModel(0)

    fun getStickersViewModel(type: Int = 0): StickersViewModel {
        return cachedStickersViewModels.computeIfAbsent(type) { createStickersViewModel(it) }
    }

    fun createStickersViewModel(type: Int = 0): StickersViewModel {
        return StickersViewModel(
            observeStickerSetsUseCase = observeStickerSetsUseCase,
            getStickerSetsUseCase = getStickerSetsUseCase,
            getStickerSetUseCase = getStickerSetUseCase,
            getRecentStickersUseCase = getRecentStickersUseCase,
            getStickersForEmojiUseCase = getStickersForEmojiUseCase,
            toggleStickerSetInstalledUseCase = toggleStickerSetInstalledUseCase,
            toggleStickerSetArchivedUseCase = toggleStickerSetArchivedUseCase,
            stickerType = type
        )
    }

    private var customFileLoaderRepository: FileLoaderRepository? = null

    var fileLoaderRepository: FileLoaderRepository
        get() = customFileLoaderRepository ?: LegacyFileLoaderRepository(account)
        set(value) {
            customFileLoaderRepository = value
        }

    val observeTransfersUseCase: ObserveTransfersUseCase
        get() = ObserveTransfersUseCase(fileLoaderRepository)

    val observeTransferUseCase: ObserveTransferUseCase
        get() = ObserveTransferUseCase(fileLoaderRepository)

    val getActiveDownloadsUseCase: GetActiveDownloadsUseCase
        get() = GetActiveDownloadsUseCase(fileLoaderRepository)

    val getRecentDownloadsUseCase: GetRecentDownloadsUseCase
        get() = GetRecentDownloadsUseCase(fileLoaderRepository)

    val loadFileUseCase: LoadFileUseCase
        get() = LoadFileUseCase(fileLoaderRepository)

    val cancelLoadFileUseCase: CancelLoadFileUseCase
        get() = CancelLoadFileUseCase(fileLoaderRepository)

    val cancelAllDownloadsUseCase: CancelAllDownloadsUseCase
        get() = CancelAllDownloadsUseCase(fileLoaderRepository)

    val uploadFileUseCase: UploadFileUseCase
        get() = UploadFileUseCase(fileLoaderRepository)

    val cancelFileUploadUseCase: CancelFileUploadUseCase
        get() = CancelFileUploadUseCase(fileLoaderRepository)

    private var cachedFileLoaderViewModel: FileLoaderViewModel? = null

    val fileLoaderViewModel: FileLoaderViewModel
        get() {
            var vm = cachedFileLoaderViewModel
            if (vm == null) {
                vm = createFileLoaderViewModel()
                cachedFileLoaderViewModel = vm
            }
            return vm
        }

    fun createFileLoaderViewModel(): FileLoaderViewModel {
        return FileLoaderViewModel(
            observeTransfersUseCase = observeTransfersUseCase,
            getActiveDownloadsUseCase = getActiveDownloadsUseCase,
            getRecentDownloadsUseCase = getRecentDownloadsUseCase,
            loadFileUseCase = loadFileUseCase,
            cancelLoadFileUseCase = cancelLoadFileUseCase,
            cancelAllDownloadsUseCase = cancelAllDownloadsUseCase,
            uploadFileUseCase = uploadFileUseCase,
            cancelFileUploadUseCase = cancelFileUploadUseCase
        )
    }

    // --- Search ---
    private var customSearchRepository: SearchRepository? = null

    var searchRepository: SearchRepository
        get() = customSearchRepository ?: LegacySearchRepository(account)
        set(value) {
            customSearchRepository = value
        }

    val searchGlobalUseCase: SearchGlobalUseCase
        get() = SearchGlobalUseCase(searchRepository)

    val searchLocalUseCase: SearchLocalUseCase
        get() = SearchLocalUseCase(searchRepository)

    val getRecentSearchesUseCase: GetRecentSearchesUseCase
        get() = GetRecentSearchesUseCase(searchRepository)

    val clearRecentSearchesUseCase: ClearRecentSearchesUseCase
        get() = ClearRecentSearchesUseCase(searchRepository)

    val removeRecentSearchUseCase: RemoveRecentSearchUseCase
        get() = RemoveRecentSearchUseCase(searchRepository)

    val getRecentHashtagsUseCase: GetRecentHashtagsUseCase
        get() = GetRecentHashtagsUseCase(searchRepository)

    val putRecentHashtagUseCase: PutRecentHashtagUseCase
        get() = PutRecentHashtagUseCase(searchRepository)

    val clearRecentHashtagsUseCase: ClearRecentHashtagsUseCase
        get() = ClearRecentHashtagsUseCase(searchRepository)

    private var cachedSearchViewModel: SearchViewModel? = null

    val searchViewModel: SearchViewModel
        get() {
            var vm = cachedSearchViewModel
            if (vm == null) {
                vm = createSearchViewModel()
                cachedSearchViewModel = vm
            }
            return vm
        }

    fun createSearchViewModel(): SearchViewModel {
        return SearchViewModel(
            searchGlobalUseCase = searchGlobalUseCase,
            searchLocalUseCase = searchLocalUseCase,
            getRecentSearchesUseCase = getRecentSearchesUseCase,
            clearRecentSearchesUseCase = clearRecentSearchesUseCase,
            removeRecentSearchUseCase = removeRecentSearchUseCase,
            getRecentHashtagsUseCase = getRecentHashtagsUseCase,
            putRecentHashtagUseCase = putRecentHashtagUseCase,
            clearRecentHashtagsUseCase = clearRecentHashtagsUseCase
        )
    }

    private var customNotificationsRepository: NotificationsRepository? = null

    var notificationsRepository: NotificationsRepository
        get() = customNotificationsRepository ?: LegacyNotificationsRepository(account)
        set(value) {
            customNotificationsRepository = value
        }

    val observeNotificationSettingsUseCase: ObserveNotificationSettingsUseCase
        get() = ObserveNotificationSettingsUseCase(notificationsRepository)

    val getNotificationSettingsUseCase: GetNotificationSettingsUseCase
        get() = GetNotificationSettingsUseCase(notificationsRepository)

    val observeBadgeUseCase: ObserveBadgeUseCase
        get() = ObserveBadgeUseCase(notificationsRepository)

    val getBadgeUseCase: GetBadgeUseCase
        get() = GetBadgeUseCase(notificationsRepository)

    val observeBadgeSettingsUseCase: ObserveBadgeSettingsUseCase
        get() = ObserveBadgeSettingsUseCase(notificationsRepository)

    val getBadgeSettingsUseCase: GetBadgeSettingsUseCase
        get() = GetBadgeSettingsUseCase(notificationsRepository)

    val togglePeerNotificationsUseCase: TogglePeerNotificationsUseCase
        get() = TogglePeerNotificationsUseCase(notificationsRepository)

    val toggleInChatSoundUseCase: ToggleInChatSoundUseCase
        get() = ToggleInChatSoundUseCase(notificationsRepository)

    val toggleInAppSoundsUseCase: ToggleInAppSoundsUseCase
        get() = ToggleInAppSoundsUseCase(notificationsRepository)

    val toggleInAppVibrateUseCase: ToggleInAppVibrateUseCase
        get() = ToggleInAppVibrateUseCase(notificationsRepository)

    val toggleInAppPreviewUseCase: ToggleInAppPreviewUseCase
        get() = ToggleInAppPreviewUseCase(notificationsRepository)

    val toggleContactJoinedNotificationsUseCase: ToggleContactJoinedNotificationsUseCase
        get() = ToggleContactJoinedNotificationsUseCase(notificationsRepository)

    val togglePinnedMessagesNotificationsUseCase: TogglePinnedMessagesNotificationsUseCase
        get() = TogglePinnedMessagesNotificationsUseCase(notificationsRepository)

    val updateBadgeSettingsUseCase: UpdateBadgeSettingsUseCase
        get() = UpdateBadgeSettingsUseCase(notificationsRepository)

    val muteDialogUseCase: MuteDialogUseCase
        get() = MuteDialogUseCase(notificationsRepository)

    val isDialogMutedUseCase: IsDialogMutedUseCase
        get() = IsDialogMutedUseCase(notificationsRepository)

    val refreshBadgeUseCase: RefreshBadgeUseCase
        get() = RefreshBadgeUseCase(notificationsRepository)

    private var cachedNotificationsViewModel: NotificationsViewModel? = null

    val notificationsViewModel: NotificationsViewModel
        get() {
            var vm = cachedNotificationsViewModel
            if (vm == null) {
                vm = createNotificationsViewModel()
                cachedNotificationsViewModel = vm
            }
            return vm
        }

    fun createNotificationsViewModel(): NotificationsViewModel {
        return NotificationsViewModel(
            observeNotificationSettingsUseCase = observeNotificationSettingsUseCase,
            getNotificationSettingsUseCase = getNotificationSettingsUseCase,
            observeBadgeUseCase = observeBadgeUseCase,
            getBadgeUseCase = getBadgeUseCase,
            observeBadgeSettingsUseCase = observeBadgeSettingsUseCase,
            getBadgeSettingsUseCase = getBadgeSettingsUseCase,
            togglePeerNotificationsUseCase = togglePeerNotificationsUseCase,
            toggleInChatSoundUseCase = toggleInChatSoundUseCase,
            toggleInAppSoundsUseCase = toggleInAppSoundsUseCase,
            toggleInAppVibrateUseCase = toggleInAppVibrateUseCase,
            toggleInAppPreviewUseCase = toggleInAppPreviewUseCase,
            toggleContactJoinedNotificationsUseCase = toggleContactJoinedNotificationsUseCase,
            togglePinnedMessagesNotificationsUseCase = togglePinnedMessagesNotificationsUseCase,
            updateBadgeSettingsUseCase = updateBadgeSettingsUseCase,
            muteDialogUseCase = muteDialogUseCase,
            refreshBadgeUseCase = refreshBadgeUseCase
        )
    }

    val privacyRepository: PrivacyRepository by lazy {
        LegacyPrivacyRepository(account)
    }

    val observePrivacyRulesUseCase: ObservePrivacyRulesUseCase
        get() = ObservePrivacyRulesUseCase(privacyRepository)

    val getPrivacyRulesUseCase: GetPrivacyRulesUseCase
        get() = GetPrivacyRulesUseCase(privacyRepository)

    val setPrivacyRuleUseCase: SetPrivacyRuleUseCase
        get() = SetPrivacyRuleUseCase(privacyRepository)

    val loadPrivacyRulesUseCase: LoadPrivacyRulesUseCase
        get() = LoadPrivacyRulesUseCase(privacyRepository)

    val observeBlockedPeersUseCase: ObserveBlockedPeersUseCase
        get() = ObserveBlockedPeersUseCase(privacyRepository)

    val getBlockedPeersUseCase: GetBlockedPeersUseCase
        get() = GetBlockedPeersUseCase(privacyRepository)

    val blockPrivacyPeerUseCase: BlockPrivacyPeerUseCase
        get() = BlockPrivacyPeerUseCase(privacyRepository)

    val unblockPrivacyPeerUseCase: UnblockPrivacyPeerUseCase
        get() = UnblockPrivacyPeerUseCase(privacyRepository)

    val getPasscodeSettingsUseCase: GetPasscodeSettingsUseCase
        get() = GetPasscodeSettingsUseCase(privacyRepository)

    val setPasscodeUseCase: SetPasscodeUseCase
        get() = SetPasscodeUseCase(privacyRepository)

    val checkPasscodeUseCase: CheckPasscodeUseCase
        get() = CheckPasscodeUseCase(privacyRepository)

    val clearPasscodeUseCase: ClearPasscodeUseCase
        get() = ClearPasscodeUseCase(privacyRepository)

    val observeTwoStepVerificationUseCase: ObserveTwoStepVerificationUseCase
        get() = ObserveTwoStepVerificationUseCase(privacyRepository)

    val loadTwoStepVerificationUseCase: LoadTwoStepVerificationUseCase
        get() = LoadTwoStepVerificationUseCase(privacyRepository)

    private var cachedPrivacyViewModel: PrivacyViewModel? = null

    val privacyViewModel: PrivacyViewModel
        get() {
            var vm = cachedPrivacyViewModel
            if (vm == null) {
                vm = createPrivacyViewModel()
                cachedPrivacyViewModel = vm
            }
            return vm
        }

    fun createPrivacyViewModel(): PrivacyViewModel {
        return PrivacyViewModel(
            privacyRepository = privacyRepository,
            observePrivacyRulesUseCase = observePrivacyRulesUseCase,
            getPrivacyRulesUseCase = getPrivacyRulesUseCase,
            setPrivacyRuleUseCase = setPrivacyRuleUseCase,
            loadPrivacyRulesUseCase = loadPrivacyRulesUseCase,
            observeBlockedPeersUseCase = observeBlockedPeersUseCase,
            getBlockedPeersUseCase = getBlockedPeersUseCase,
            blockPrivacyPeerUseCase = blockPrivacyPeerUseCase,
            unblockPrivacyPeerUseCase = unblockPrivacyPeerUseCase,
            getPasscodeSettingsUseCase = getPasscodeSettingsUseCase,
            setPasscodeUseCase = setPasscodeUseCase,
            checkPasscodeUseCase = checkPasscodeUseCase,
            clearPasscodeUseCase = clearPasscodeUseCase,
            observeTwoStepVerificationUseCase = observeTwoStepVerificationUseCase,
            loadTwoStepVerificationUseCase = loadTwoStepVerificationUseCase
        )
    }

    val themeRepository: ThemeRepository by lazy {
        LegacyThemeRepository(account)
    }

    val observeAppearanceSettingsUseCase: ObserveAppearanceSettingsUseCase
        get() = ObserveAppearanceSettingsUseCase(themeRepository)

    val getAppearanceSettingsUseCase: GetAppearanceSettingsUseCase
        get() = GetAppearanceSettingsUseCase(themeRepository)

    val observeAvailableThemesUseCase: ObserveAvailableThemesUseCase
        get() = ObserveAvailableThemesUseCase(themeRepository)

    val getAvailableThemesUseCase: GetAvailableThemesUseCase
        get() = GetAvailableThemesUseCase(themeRepository)

    val applyThemeUseCase: ApplyThemeUseCase
        get() = ApplyThemeUseCase(themeRepository)

    val observeNightModeUseCase: ObserveNightModeUseCase
        get() = ObserveNightModeUseCase(themeRepository)

    val setNightModeTypeUseCase: SetNightModeTypeUseCase
        get() = SetNightModeTypeUseCase(themeRepository)

    val setNightModeSettingsUseCase: SetNightModeSettingsUseCase
        get() = SetNightModeSettingsUseCase(themeRepository)

    val setThemeAccentUseCase: SetThemeAccentUseCase
        get() = SetThemeAccentUseCase(themeRepository)

    val setBubbleRadiusUseCase: SetBubbleRadiusUseCase
        get() = SetBubbleRadiusUseCase(themeRepository)

    val resetAppearanceSettingsUseCase: ResetAppearanceSettingsUseCase
        get() = ResetAppearanceSettingsUseCase(themeRepository)

    private var cachedThemeViewModel: ThemeViewModel? = null

    val themeViewModel: ThemeViewModel
        get() {
            var vm = cachedThemeViewModel
            if (vm == null) {
                vm = createThemeViewModel()
                cachedThemeViewModel = vm
            }
            return vm
        }

    fun createThemeViewModel(): ThemeViewModel {
        return ThemeViewModel(
            themeRepository = themeRepository,
            observeAppearanceSettingsUseCase = observeAppearanceSettingsUseCase,
            getAppearanceSettingsUseCase = getAppearanceSettingsUseCase,
            observeAvailableThemesUseCase = observeAvailableThemesUseCase,
            getAvailableThemesUseCase = getAvailableThemesUseCase,
            applyThemeUseCase = applyThemeUseCase,
            observeNightModeUseCase = observeNightModeUseCase,
            setNightModeTypeUseCase = setNightModeTypeUseCase,
            setNightModeSettingsUseCase = setNightModeSettingsUseCase,
            setThemeAccentUseCase = setThemeAccentUseCase,
            setBubbleRadiusUseCase = setBubbleRadiusUseCase,
            resetAppearanceSettingsUseCase = resetAppearanceSettingsUseCase
        )
    }

    private var customStoriesRepository: StoriesRepository? = null

    var storiesRepository: StoriesRepository
        get() = customStoriesRepository ?: LegacyStoriesRepository(account)
        set(value) {
            customStoriesRepository = value
        }

    val observeStoriesUseCase: ObserveStoriesUseCase
        get() = ObserveStoriesUseCase(storiesRepository)

    val observeHiddenStoriesUseCase: ObserveHiddenStoriesUseCase
        get() = ObserveHiddenStoriesUseCase(storiesRepository)

    val observeStealthModeUseCase: ObserveStealthModeUseCase
        get() = ObserveStealthModeUseCase(storiesRepository)

    val observeSelfStoriesUseCase: ObserveSelfStoriesUseCase
        get() = ObserveSelfStoriesUseCase(storiesRepository)

    val getPeerStoriesUseCase: GetPeerStoriesUseCase
        get() = GetPeerStoriesUseCase(storiesRepository)

    val markStoryAsReadUseCase: MarkStoryAsReadUseCase
        get() = MarkStoryAsReadUseCase(storiesRepository)

    val deleteStoryUseCase: DeleteStoryUseCase
        get() = DeleteStoryUseCase(storiesRepository)

    val toggleStoryPinUseCase: ToggleStoryPinUseCase
        get() = ToggleStoryPinUseCase(storiesRepository)

    val toggleStoryHiddenUseCase: ToggleStoryHiddenUseCase
        get() = ToggleStoryHiddenUseCase(storiesRepository)

    val activateStealthModeUseCase: ActivateStealthModeUseCase
        get() = ActivateStealthModeUseCase(storiesRepository)

    val getStoryLimitUseCase: GetStoryLimitUseCase
        get() = GetStoryLimitUseCase(storiesRepository)

    val refreshStoriesUseCase: RefreshStoriesUseCase
        get() = RefreshStoriesUseCase(storiesRepository)

    private var cachedStoriesViewModel: StoriesViewModel? = null

    val storiesViewModel: StoriesViewModel
        get() {
            var vm = cachedStoriesViewModel
            if (vm == null) {
                vm = createStoriesViewModel()
                cachedStoriesViewModel = vm
            }
            return vm
        }

    fun createStoriesViewModel(): StoriesViewModel {
        return StoriesViewModel(
            observeStoriesUseCase = observeStoriesUseCase,
            observeHiddenStoriesUseCase = observeHiddenStoriesUseCase,
            observeStealthModeUseCase = observeStealthModeUseCase,
            observeSelfStoriesUseCase = observeSelfStoriesUseCase,
            markStoryAsReadUseCase = markStoryAsReadUseCase,
            deleteStoryUseCase = deleteStoryUseCase,
            toggleStoryPinUseCase = toggleStoryPinUseCase,
            toggleStoryHiddenUseCase = toggleStoryHiddenUseCase,
            activateStealthModeUseCase = activateStealthModeUseCase,
            getStoryLimitUseCase = getStoryLimitUseCase,
            refreshStoriesUseCase = refreshStoriesUseCase
        )
    }

    private var customPaymentsRepository: PaymentsRepository? = null

    var paymentsRepository: PaymentsRepository
        get() = customPaymentsRepository ?: LegacyPaymentsRepository(account)
        set(value) {
            customPaymentsRepository = value
        }

    val observeStarsBalanceUseCase: ObserveStarsBalanceUseCase
        get() = ObserveStarsBalanceUseCase(paymentsRepository)

    val observeStarTransactionsUseCase: ObserveStarTransactionsUseCase
        get() = ObserveStarTransactionsUseCase(paymentsRepository)

    val observeStarSubscriptionsUseCase: ObserveStarSubscriptionsUseCase
        get() = ObserveStarSubscriptionsUseCase(paymentsRepository)

    val getStarsBalanceUseCase: GetStarsBalanceUseCase
        get() = GetStarsBalanceUseCase(paymentsRepository)

    val getStarTransactionsUseCase: GetStarTransactionsUseCase
        get() = GetStarTransactionsUseCase(paymentsRepository)

    val getStarSubscriptionsUseCase: GetStarSubscriptionsUseCase
        get() = GetStarSubscriptionsUseCase(paymentsRepository)

    val getStarTopupOptionsUseCase: GetStarTopupOptionsUseCase
        get() = GetStarTopupOptionsUseCase(paymentsRepository)

    val refreshStarsBalanceUseCase: RefreshStarsBalanceUseCase
        get() = RefreshStarsBalanceUseCase(paymentsRepository)

    val refreshStarTransactionsUseCase: RefreshStarTransactionsUseCase
        get() = RefreshStarTransactionsUseCase(paymentsRepository)

    val refreshStarSubscriptionsUseCase: RefreshStarSubscriptionsUseCase
        get() = RefreshStarSubscriptionsUseCase(paymentsRepository)

    private var cachedPaymentsViewModel: PaymentsViewModel? = null

    val paymentsViewModel: PaymentsViewModel
        get() {
            var vm = cachedPaymentsViewModel
            if (vm == null) {
                vm = createPaymentsViewModel()
                cachedPaymentsViewModel = vm
            }
            return vm
        }

    fun createPaymentsViewModel(): PaymentsViewModel {
        return PaymentsViewModel(
            observeStarsBalanceUseCase = observeStarsBalanceUseCase,
            observeStarTransactionsUseCase = observeStarTransactionsUseCase,
            observeStarSubscriptionsUseCase = observeStarSubscriptionsUseCase,
            getStarTopupOptionsUseCase = getStarTopupOptionsUseCase,
            refreshStarsBalanceUseCase = refreshStarsBalanceUseCase,
            refreshStarTransactionsUseCase = refreshStarTransactionsUseCase,
            refreshStarSubscriptionsUseCase = refreshStarSubscriptionsUseCase
        )
    }

    private var customDataStorageRepository: DataStorageRepository? = null

    var dataStorageRepository: DataStorageRepository
        get() = customDataStorageRepository ?: LegacyDataStorageRepository(account)
        set(value) {
            customDataStorageRepository = value
        }

    val observeNetworkUsageUseCase: ObserveNetworkUsageUseCase
        get() = ObserveNetworkUsageUseCase(dataStorageRepository)

    val observeStorageUsageUseCase: ObserveStorageUsageUseCase
        get() = ObserveStorageUsageUseCase(dataStorageRepository)

    val observeAutoDownloadPresetUseCase: ObserveAutoDownloadPresetUseCase
        get() = ObserveAutoDownloadPresetUseCase(dataStorageRepository)

    val observeKeepMediaSettingsUseCase: ObserveKeepMediaSettingsUseCase
        get() = ObserveKeepMediaSettingsUseCase(dataStorageRepository)

    val getNetworkUsageUseCase: GetNetworkUsageUseCase
        get() = GetNetworkUsageUseCase(dataStorageRepository)

    val resetNetworkUsageUseCase: ResetNetworkUsageUseCase
        get() = ResetNetworkUsageUseCase(dataStorageRepository)

    val getStorageUsageUseCase: GetStorageUsageUseCase
        get() = GetStorageUsageUseCase(dataStorageRepository)

    val clearCacheUseCase: ClearCacheUseCase
        get() = ClearCacheUseCase(dataStorageRepository)

    val clearDatabaseUseCase: ClearDatabaseUseCase
        get() = ClearDatabaseUseCase(dataStorageRepository)

    val getAutoDownloadPresetUseCase: GetAutoDownloadPresetUseCase
        get() = GetAutoDownloadPresetUseCase(dataStorageRepository)

    val updateAutoDownloadPresetUseCase: UpdateAutoDownloadPresetUseCase
        get() = UpdateAutoDownloadPresetUseCase(dataStorageRepository)

    val getKeepMediaSettingsUseCase: GetKeepMediaSettingsUseCase
        get() = GetKeepMediaSettingsUseCase(dataStorageRepository)

    val updateKeepMediaUseCase: UpdateKeepMediaUseCase
        get() = UpdateKeepMediaUseCase(dataStorageRepository)

    val refreshStorageUsageUseCase: RefreshStorageUsageUseCase
        get() = RefreshStorageUsageUseCase(dataStorageRepository)

    private var cachedDataStorageViewModel: DataStorageViewModel? = null

    val dataStorageViewModel: DataStorageViewModel
        get() {
            var vm = cachedDataStorageViewModel
            if (vm == null) {
                vm = createDataStorageViewModel()
                cachedDataStorageViewModel = vm
            }
            return vm
        }

    fun createDataStorageViewModel(): DataStorageViewModel {
        return DataStorageViewModel(
            observeNetworkUsageUseCase = observeNetworkUsageUseCase,
            observeStorageUsageUseCase = observeStorageUsageUseCase,
            observeAutoDownloadPresetUseCase = observeAutoDownloadPresetUseCase,
            observeKeepMediaSettingsUseCase = observeKeepMediaSettingsUseCase,
            getNetworkUsageUseCase = getNetworkUsageUseCase,
            resetNetworkUsageUseCase = resetNetworkUsageUseCase,
            getStorageUsageUseCase = getStorageUsageUseCase,
            clearCacheUseCase = clearCacheUseCase,
            clearDatabaseUseCase = clearDatabaseUseCase,
            getAutoDownloadPresetUseCase = getAutoDownloadPresetUseCase,
            updateAutoDownloadPresetUseCase = updateAutoDownloadPresetUseCase,
            getKeepMediaSettingsUseCase = getKeepMediaSettingsUseCase,
            updateKeepMediaUseCase = updateKeepMediaUseCase,
            refreshStorageUsageUseCase = refreshStorageUsageUseCase
        )
    }

    private var customTopicsRepository: TopicsRepository? = null

    var topicsRepository: TopicsRepository
        get() = customTopicsRepository ?: LegacyTopicsRepository(account)
        set(value) {
            customTopicsRepository = value
        }

    val observeTopicsUseCase: ObserveTopicsUseCase
        get() = ObserveTopicsUseCase(topicsRepository)

    val observeForumUnreadCountUseCase: ObserveForumUnreadCountUseCase
        get() = ObserveForumUnreadCountUseCase(topicsRepository)

    val getTopicsUseCase: GetTopicsUseCase
        get() = GetTopicsUseCase(topicsRepository)

    val getTopicUseCase: GetTopicUseCase
        get() = GetTopicUseCase(topicsRepository)

    val loadTopicsUseCase: LoadTopicsUseCase
        get() = LoadTopicsUseCase(topicsRepository)

    val reloadTopicsUseCase: ReloadTopicsUseCase
        get() = ReloadTopicsUseCase(topicsRepository)

    val toggleCloseTopicUseCase: ToggleCloseTopicUseCase
        get() = ToggleCloseTopicUseCase(topicsRepository)

    val togglePinTopicUseCase: TogglePinTopicUseCase
        get() = TogglePinTopicUseCase(topicsRepository)

    val toggleShowTopicUseCase: ToggleShowTopicUseCase
        get() = ToggleShowTopicUseCase(topicsRepository)

    val deleteTopicsUseCase: DeleteTopicsUseCase
        get() = DeleteTopicsUseCase(topicsRepository)

    val reorderPinnedTopicsUseCase: ReorderPinnedTopicsUseCase
        get() = ReorderPinnedTopicsUseCase(topicsRepository)

    val markTopicReactionsAsReadUseCase: MarkTopicReactionsAsReadUseCase
        get() = MarkTopicReactionsAsReadUseCase(topicsRepository)

    val getForumUnreadCountUseCase: GetForumUnreadCountUseCase
        get() = GetForumUnreadCountUseCase(topicsRepository)

    private var cachedTopicsViewModel: TopicsViewModel? = null

    val topicsViewModel: TopicsViewModel
        get() {
            var vm = cachedTopicsViewModel
            if (vm == null) {
                vm = createTopicsViewModel()
                cachedTopicsViewModel = vm
            }
            return vm
        }

    fun createTopicsViewModel(): TopicsViewModel {
        return TopicsViewModel(
            observeTopicsUseCase = observeTopicsUseCase,
            observeForumUnreadCountUseCase = observeForumUnreadCountUseCase,
            getTopicsUseCase = getTopicsUseCase,
            getTopicUseCase = getTopicUseCase,
            loadTopicsUseCase = loadTopicsUseCase,
            reloadTopicsUseCase = reloadTopicsUseCase,
            toggleCloseTopicUseCase = toggleCloseTopicUseCase,
            togglePinTopicUseCase = togglePinTopicUseCase,
            toggleShowTopicUseCase = toggleShowTopicUseCase,
            deleteTopicsUseCase = deleteTopicsUseCase,
            reorderPinnedTopicsUseCase = reorderPinnedTopicsUseCase,
            markTopicReactionsAsReadUseCase = markTopicReactionsAsReadUseCase,
            getForumUnreadCountUseCase = getForumUnreadCountUseCase
        )
    }

    private var customLocationRepository: LocationRepository? = null

    var locationRepository: LocationRepository
        get() = customLocationRepository ?: LegacyLocationRepository(account)
        set(value) {
            customLocationRepository = value
        }

    val observeActiveSharingsUseCase: ObserveActiveSharingsUseCase
        get() = ObserveActiveSharingsUseCase(locationRepository)

    val observePeerLocationsUseCase: ObservePeerLocationsUseCase
        get() = ObservePeerLocationsUseCase(locationRepository)

    val observeLastKnownLocationUseCase: ObserveLastKnownLocationUseCase
        get() = ObserveLastKnownLocationUseCase(locationRepository)

    val getActiveSharingsUseCase: GetActiveSharingsUseCase
        get() = GetActiveSharingsUseCase(locationRepository)

    val isSharingLocationUseCase: IsSharingLocationUseCase
        get() = IsSharingLocationUseCase(locationRepository)

    val getSharingInfoUseCase: GetSharingInfoUseCase
        get() = GetSharingInfoUseCase(locationRepository)

    val getLastKnownLocationUseCase: GetLastKnownLocationUseCase
        get() = GetLastKnownLocationUseCase(locationRepository)

    val loadPeerLiveLocationsUseCase: LoadPeerLiveLocationsUseCase
        get() = LoadPeerLiveLocationsUseCase(locationRepository)

    val stopLocationSharingUseCase: StopLocationSharingUseCase
        get() = StopLocationSharingUseCase(locationRepository)

    val stopAllLocationSharingsUseCase: StopAllLocationSharingsUseCase
        get() = StopAllLocationSharingsUseCase(locationRepository)

    val setProximityAlertUseCase: SetProximityAlertUseCase
        get() = SetProximityAlertUseCase(locationRepository)

    val sendStaticLocationUseCase: SendStaticLocationUseCase
        get() = SendStaticLocationUseCase(locationRepository)

    val sendLiveLocationUseCase: SendLiveLocationUseCase
        get() = SendLiveLocationUseCase(locationRepository)

    val markLiveLocationsAsReadUseCase: MarkLiveLocationsAsReadUseCase
        get() = MarkLiveLocationsAsReadUseCase(locationRepository)

    private var cachedLocationViewModel: LocationViewModel? = null

    val locationViewModel: LocationViewModel
        get() {
            var vm = cachedLocationViewModel
            if (vm == null) {
                vm = createLocationViewModel()
                cachedLocationViewModel = vm
            }
            return vm
        }

    fun createLocationViewModel(): LocationViewModel {
        return LocationViewModel(
            observeActiveSharingsUseCase = observeActiveSharingsUseCase,
            observePeerLocationsUseCase = observePeerLocationsUseCase,
            observeLastKnownLocationUseCase = observeLastKnownLocationUseCase,
            getActiveSharingsUseCase = getActiveSharingsUseCase,
            isSharingLocationUseCase = isSharingLocationUseCase,
            getSharingInfoUseCase = getSharingInfoUseCase,
            getLastKnownLocationUseCase = getLastKnownLocationUseCase,
            loadPeerLiveLocationsUseCase = loadPeerLiveLocationsUseCase,
            stopLocationSharingUseCase = stopLocationSharingUseCase,
            stopAllLocationSharingsUseCase = stopAllLocationSharingsUseCase,
            setProximityAlertUseCase = setProximityAlertUseCase,
            sendStaticLocationUseCase = sendStaticLocationUseCase,
            sendLiveLocationUseCase = sendLiveLocationUseCase,
            markLiveLocationsAsReadUseCase = markLiveLocationsAsReadUseCase
        )
    }

    private var customSessionsRepository: SessionsRepository? = null

    var sessionsRepository: SessionsRepository
        get() = customSessionsRepository ?: LegacySessionsRepository(account)
        set(value) {
            customSessionsRepository = value
        }

    val observeSessionsUseCase: ObserveSessionsUseCase
        get() = ObserveSessionsUseCase(sessionsRepository)

    val observeWebSessionsUseCase: ObserveWebSessionsUseCase
        get() = ObserveWebSessionsUseCase(sessionsRepository)

    val getSessionsUseCase: GetSessionsUseCase
        get() = GetSessionsUseCase(sessionsRepository)

    val loadSessionsUseCase: LoadSessionsUseCase
        get() = LoadSessionsUseCase(sessionsRepository)

    val getWebSessionsUseCase: GetWebSessionsUseCase
        get() = GetWebSessionsUseCase(sessionsRepository)

    val loadWebSessionsUseCase: LoadWebSessionsUseCase
        get() = LoadWebSessionsUseCase(sessionsRepository)

    val terminateSessionUseCase: TerminateSessionUseCase
        get() = TerminateSessionUseCase(sessionsRepository)

    val terminateAllOtherSessionsUseCase: TerminateAllOtherSessionsUseCase
        get() = TerminateAllOtherSessionsUseCase(sessionsRepository)

    val terminateWebSessionUseCase: TerminateWebSessionUseCase
        get() = TerminateWebSessionUseCase(sessionsRepository)

    val terminateAllWebSessionsUseCase: TerminateAllWebSessionsUseCase
        get() = TerminateAllWebSessionsUseCase(sessionsRepository)

    val updateSessionSettingsUseCase: UpdateSessionSettingsUseCase
        get() = UpdateSessionSettingsUseCase(sessionsRepository)

    val setSessionsTtlUseCase: SetSessionsTtlUseCase
        get() = SetSessionsTtlUseCase(sessionsRepository)

    val acceptQrLoginUseCase: AcceptQrLoginUseCase
        get() = AcceptQrLoginUseCase(sessionsRepository)

    private var cachedSessionsViewModel: SessionsViewModel? = null

    val sessionsViewModel: SessionsViewModel
        get() {
            var vm = cachedSessionsViewModel
            if (vm == null) {
                vm = createSessionsViewModel()
                cachedSessionsViewModel = vm
            }
            return vm
        }

    fun createSessionsViewModel(): SessionsViewModel {
        return SessionsViewModel(
            observeSessionsUseCase = observeSessionsUseCase,
            observeWebSessionsUseCase = observeWebSessionsUseCase,
            loadSessionsUseCase = loadSessionsUseCase,
            loadWebSessionsUseCase = loadWebSessionsUseCase,
            terminateSessionUseCase = terminateSessionUseCase,
            terminateAllOtherSessionsUseCase = terminateAllOtherSessionsUseCase,
            terminateWebSessionUseCase = terminateWebSessionUseCase,
            terminateAllWebSessionsUseCase = terminateAllWebSessionsUseCase,
            updateSessionSettingsUseCase = updateSessionSettingsUseCase,
            setSessionsTtlUseCase = setSessionsTtlUseCase,
            acceptQrLoginUseCase = acceptQrLoginUseCase
        )
    }

    private var customTranslationRepository: TranslationRepository? = null

    var translationRepository: TranslationRepository
        get() = customTranslationRepository ?: LegacyTranslationRepository(account)
        set(value) {
            customTranslationRepository = value
        }

    val observeTranslateSettingsUseCase: ObserveTranslateSettingsUseCase
        get() = ObserveTranslateSettingsUseCase(translationRepository)

    val getTranslateSettingsUseCase: GetTranslateSettingsUseCase
        get() = GetTranslateSettingsUseCase(translationRepository)

    val setChatTranslateEnabledUseCase: SetChatTranslateEnabledUseCase
        get() = SetChatTranslateEnabledUseCase(translationRepository)

    val setContextTranslateEnabledUseCase: SetContextTranslateEnabledUseCase
        get() = SetContextTranslateEnabledUseCase(translationRepository)

    val setDoNotTranslateLanguagesUseCase: SetDoNotTranslateLanguagesUseCase
        get() = SetDoNotTranslateLanguagesUseCase(translationRepository)

    val addDoNotTranslateLanguageUseCase: AddDoNotTranslateLanguageUseCase
        get() = AddDoNotTranslateLanguageUseCase(translationRepository)

    val removeDoNotTranslateLanguageUseCase: RemoveDoNotTranslateLanguageUseCase
        get() = RemoveDoNotTranslateLanguageUseCase(translationRepository)

    val observeDialogTranslationStateUseCase: ObserveDialogTranslationStateUseCase
        get() = ObserveDialogTranslationStateUseCase(translationRepository)

    val getDialogTranslationStateUseCase: GetDialogTranslationStateUseCase
        get() = GetDialogTranslationStateUseCase(translationRepository)

    val toggleDialogTranslatingUseCase: ToggleDialogTranslatingUseCase
        get() = ToggleDialogTranslatingUseCase(translationRepository)

    val setDialogTargetLanguageUseCase: SetDialogTargetLanguageUseCase
        get() = SetDialogTargetLanguageUseCase(translationRepository)

    val translateTextUseCase: TranslateTextUseCase
        get() = TranslateTextUseCase(translationRepository)

    val getAvailableLanguagesUseCase: GetAvailableLanguagesUseCase
        get() = GetAvailableLanguagesUseCase(translationRepository)

    val applyAppLanguageUseCase: ApplyAppLanguageUseCase
        get() = ApplyAppLanguageUseCase(translationRepository)

    private var cachedTranslateViewModel: TranslateViewModel? = null

    val translateViewModel: TranslateViewModel
        get() {
            var vm = cachedTranslateViewModel
            if (vm == null) {
                vm = createTranslateViewModel()
                cachedTranslateViewModel = vm
            }
            return vm
        }

    fun createTranslateViewModel(): TranslateViewModel {
        return TranslateViewModel(
            observeTranslateSettingsUseCase = observeTranslateSettingsUseCase,
            getTranslateSettingsUseCase = getTranslateSettingsUseCase,
            setChatTranslateEnabledUseCase = setChatTranslateEnabledUseCase,
            setContextTranslateEnabledUseCase = setContextTranslateEnabledUseCase,
            setDoNotTranslateLanguagesUseCase = setDoNotTranslateLanguagesUseCase,
            addDoNotTranslateLanguageUseCase = addDoNotTranslateLanguageUseCase,
            removeDoNotTranslateLanguageUseCase = removeDoNotTranslateLanguageUseCase,
            observeDialogTranslationStateUseCase = observeDialogTranslationStateUseCase,
            getDialogTranslationStateUseCase = getDialogTranslationStateUseCase,
            toggleDialogTranslatingUseCase = toggleDialogTranslatingUseCase,
            setDialogTargetLanguageUseCase = setDialogTargetLanguageUseCase,
            translateTextUseCase = translateTextUseCase,
            getAvailableLanguagesUseCase = getAvailableLanguagesUseCase,
            applyAppLanguageUseCase = applyAppLanguageUseCase
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
