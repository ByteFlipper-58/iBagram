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
import org.telegram.messenger.feature.reactions.data.repository.LegacyReactionsRepository
import org.telegram.messenger.feature.reactions.domain.repository.ReactionsRepository
import org.telegram.messenger.feature.reactions.domain.usecase.ClearReactionsUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.GetAvailableReactionsUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.GetDoubleTapReactionUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.GetReactionsSettingsUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.GetRecentReactionsUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.LoadAvailableReactionsUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.ObserveAvailableReactionsUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.ObserveRecentReactionsUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.SendReactionUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.SendVoteUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.SetDoubleTapReactionUseCase
import org.telegram.messenger.feature.reactions.presentation.ReactionsViewModel
import org.telegram.messenger.feature.boosts.data.repository.LegacyBoostsRepository
import org.telegram.messenger.feature.boosts.domain.repository.BoostsRepository
import org.telegram.messenger.feature.boosts.domain.usecase.ApplyBoostUseCase
import org.telegram.messenger.feature.boosts.domain.usecase.CheckCanApplyBoostUseCase
import org.telegram.messenger.feature.boosts.domain.usecase.GetBoostsStatusUseCase
import org.telegram.messenger.feature.boosts.domain.usecase.GetMyBoostsUseCase
import org.telegram.messenger.feature.boosts.presentation.BoostsViewModel
import org.telegram.messenger.feature.quickreplies.data.repository.LegacyQuickRepliesRepository
import org.telegram.messenger.feature.quickreplies.domain.repository.QuickRepliesRepository
import org.telegram.messenger.feature.quickreplies.domain.usecase.CanAddNewQuickReplyUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.CheckQuickReplyNameBusyUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.DeleteQuickRepliesUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.FindQuickReplyUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.GetQuickRepliesUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.LoadQuickRepliesUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.ObserveQuickRepliesUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.RenameQuickReplyUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.ReorderQuickRepliesUseCase
import org.telegram.messenger.feature.quickreplies.domain.usecase.SendQuickReplyUseCase
import org.telegram.messenger.feature.quickreplies.presentation.QuickRepliesViewModel
import org.telegram.messenger.feature.joinrequests.data.repository.LegacyJoinRequestsRepository
import org.telegram.messenger.feature.joinrequests.domain.repository.JoinRequestsRepository
import org.telegram.messenger.feature.joinrequests.domain.usecase.ApproveAllJoinRequestsUseCase
import org.telegram.messenger.feature.joinrequests.domain.usecase.ApproveJoinRequestUseCase
import org.telegram.messenger.feature.joinrequests.domain.usecase.DismissAllJoinRequestsUseCase
import org.telegram.messenger.feature.joinrequests.domain.usecase.DismissJoinRequestUseCase
import org.telegram.messenger.feature.joinrequests.domain.usecase.GetCachedJoinRequestsUseCase
import org.telegram.messenger.feature.joinrequests.domain.usecase.GetPendingRequestsCountUseCase
import org.telegram.messenger.feature.joinrequests.domain.usecase.LoadJoinRequestsUseCase
import org.telegram.messenger.feature.joinrequests.domain.usecase.ObservePendingRequestsUseCase
import org.telegram.messenger.feature.joinrequests.presentation.JoinRequestsViewModel
import org.telegram.messenger.feature.factcheck.data.repository.LegacyFactCheckRepository
import org.telegram.messenger.feature.factcheck.domain.repository.FactCheckRepository
import org.telegram.messenger.feature.factcheck.domain.usecase.ApplyFactCheckUseCase
import org.telegram.messenger.feature.factcheck.domain.usecase.DeleteFactCheckUseCase
import org.telegram.messenger.feature.factcheck.domain.usecase.GetFactCheckLimitUseCase
import org.telegram.messenger.feature.factcheck.domain.usecase.GetFactCheckUseCase
import org.telegram.messenger.feature.factcheck.domain.usecase.LoadFactCheckUseCase
import org.telegram.messenger.feature.factcheck.domain.usecase.ObserveFactCheckLoadedUseCase
import org.telegram.messenger.feature.factcheck.presentation.FactCheckViewModel
import org.telegram.messenger.feature.birthdays.data.repository.LegacyBirthdaysRepository
import org.telegram.messenger.feature.birthdays.domain.repository.BirthdaysRepository
import org.telegram.messenger.feature.birthdays.domain.usecase.CheckBirthdaysUseCase
import org.telegram.messenger.feature.birthdays.domain.usecase.GetBirthdaysStateUseCase
import org.telegram.messenger.feature.birthdays.domain.usecase.HasBirthdaysTodayUseCase
import org.telegram.messenger.feature.birthdays.domain.usecase.HideTodayBirthdaysUseCase
import org.telegram.messenger.feature.birthdays.domain.usecase.IsBirthdayTodayUseCase
import org.telegram.messenger.feature.birthdays.domain.usecase.ObserveBirthdaysUseCase
import org.telegram.messenger.feature.birthdays.presentation.BirthdaysViewModel
import org.telegram.messenger.feature.chattheme.data.repository.LegacyChatThemeRepository
import org.telegram.messenger.feature.chattheme.domain.repository.ChatThemeRepository
import org.telegram.messenger.feature.chattheme.domain.usecase.GetAvailableChatThemesUseCase
import org.telegram.messenger.feature.chattheme.domain.usecase.GetDialogThemeStateUseCase
import org.telegram.messenger.feature.chattheme.domain.usecase.ObserveDialogThemeUseCase
import org.telegram.messenger.feature.chattheme.domain.usecase.ResetDialogThemeUseCase
import org.telegram.messenger.feature.chattheme.domain.usecase.SaveChatWallpaperUseCase
import org.telegram.messenger.feature.chattheme.domain.usecase.SetDialogThemeUseCase
import org.telegram.messenger.feature.chattheme.presentation.ChatThemeViewModel
import org.telegram.messenger.feature.passkeys.data.repository.LegacyPasskeysRepository
import org.telegram.messenger.feature.passkeys.domain.repository.PasskeysRepository
import org.telegram.messenger.feature.passkeys.domain.usecase.CheckCanAddPasskeyUseCase
import org.telegram.messenger.feature.passkeys.domain.usecase.DeletePasskeyUseCase
import org.telegram.messenger.feature.passkeys.domain.usecase.GetPasskeysUseCase
import org.telegram.messenger.feature.passkeys.domain.usecase.IsPasskeysSupportedUseCase
import org.telegram.messenger.feature.passkeys.domain.usecase.ObservePasskeysUseCase
import org.telegram.messenger.feature.passkeys.presentation.PasskeysViewModel
import org.telegram.messenger.feature.proxy.data.repository.LegacyProxyRepository
import org.telegram.messenger.feature.proxy.domain.repository.ProxyRepository
import org.telegram.messenger.feature.proxy.domain.usecase.AddProxyUseCase
import org.telegram.messenger.feature.proxy.domain.usecase.CheckProxyPingUseCase
import org.telegram.messenger.feature.proxy.domain.usecase.DeleteProxyUseCase
import org.telegram.messenger.feature.proxy.domain.usecase.DisableProxyUseCase
import org.telegram.messenger.feature.proxy.domain.usecase.EnableProxyUseCase
import org.telegram.messenger.feature.proxy.domain.usecase.GetProxySettingsUseCase
import org.telegram.messenger.feature.proxy.domain.usecase.ObserveProxySettingsUseCase
import org.telegram.messenger.feature.proxy.domain.usecase.ToggleProxyRotationUseCase
import org.telegram.messenger.feature.proxy.presentation.ProxyViewModel
import org.telegram.messenger.feature.autodelete.data.repository.LegacyAutoDeleteRepository
import org.telegram.messenger.feature.autodelete.domain.repository.AutoDeleteRepository
import org.telegram.messenger.feature.autodelete.domain.usecase.GetChatAutoDeleteUseCase
import org.telegram.messenger.feature.autodelete.domain.usecase.GetGlobalAutoDeleteUseCase
import org.telegram.messenger.feature.autodelete.domain.usecase.ObserveGlobalAutoDeleteUseCase
import org.telegram.messenger.feature.autodelete.domain.usecase.SetChatAutoDeleteUseCase
import org.telegram.messenger.feature.autodelete.domain.usecase.SetChatsAutoDeleteBatchUseCase
import org.telegram.messenger.feature.autodelete.domain.usecase.SetGlobalAutoDeleteUseCase
import org.telegram.messenger.feature.autodelete.presentation.AutoDeleteViewModel
import org.telegram.messenger.feature.unconfirmedauth.data.repository.LegacyUnconfirmedAuthRepository
import org.telegram.messenger.feature.unconfirmedauth.domain.repository.UnconfirmedAuthRepository
import org.telegram.messenger.feature.unconfirmedauth.domain.usecase.ClearUnconfirmedAuthsUseCase
import org.telegram.messenger.feature.unconfirmedauth.domain.usecase.ConfirmAllAuthsUseCase
import org.telegram.messenger.feature.unconfirmedauth.domain.usecase.ConfirmAuthUseCase
import org.telegram.messenger.feature.unconfirmedauth.domain.usecase.DenyAllAuthsUseCase
import org.telegram.messenger.feature.unconfirmedauth.domain.usecase.DenyAuthUseCase
import org.telegram.messenger.feature.unconfirmedauth.domain.usecase.GetUnconfirmedAuthsUseCase
import org.telegram.messenger.feature.unconfirmedauth.domain.usecase.ObserveUnconfirmedAuthsUseCase
import org.telegram.messenger.feature.unconfirmedauth.presentation.UnconfirmedAuthViewModel
import org.telegram.messenger.feature.stargifts.data.repository.LegacyStarGiftsRepository
import org.telegram.messenger.feature.stargifts.domain.repository.StarGiftsRepository
import org.telegram.messenger.feature.stargifts.domain.usecase.GetStarGiftByIdUseCase
import org.telegram.messenger.feature.stargifts.domain.usecase.GetStarGiftsCatalogUseCase
import org.telegram.messenger.feature.stargifts.domain.usecase.LoadProfileGiftsUseCase
import org.telegram.messenger.feature.stargifts.domain.usecase.ObserveProfileGiftsUseCase
import org.telegram.messenger.feature.stargifts.domain.usecase.ObserveStarGiftsCatalogUseCase
import org.telegram.messenger.feature.stargifts.domain.usecase.ToggleHideProfileGiftUseCase
import org.telegram.messenger.feature.stargifts.domain.usecase.TogglePinProfileGiftUseCase
import org.telegram.messenger.feature.stargifts.presentation.StarGiftsViewModel
import org.telegram.messenger.feature.aitones.data.repository.LegacyAiTonesRepository
import org.telegram.messenger.feature.aitones.domain.repository.AiTonesRepository
import org.telegram.messenger.feature.aitones.domain.usecase.AddAiToneUseCase
import org.telegram.messenger.feature.aitones.domain.usecase.EditAiToneUseCase
import org.telegram.messenger.feature.aitones.domain.usecase.GetAiTonesStateUseCase
import org.telegram.messenger.feature.aitones.domain.usecase.LoadAiTonesUseCase
import org.telegram.messenger.feature.aitones.domain.usecase.ObserveAiTonesUseCase
import org.telegram.messenger.feature.aitones.domain.usecase.RemoveAiToneUseCase
import org.telegram.messenger.feature.aitones.domain.usecase.UnsaveAiToneUseCase
import org.telegram.messenger.feature.aitones.presentation.AiTonesViewModel
import org.telegram.messenger.feature.captcha.data.repository.LegacyCaptchaRepository
import org.telegram.messenger.feature.captcha.domain.repository.CaptchaRepository
import org.telegram.messenger.feature.captcha.domain.usecase.CancelCaptchaUseCase
import org.telegram.messenger.feature.captcha.domain.usecase.GetActiveCaptchaRequestsUseCase
import org.telegram.messenger.feature.captcha.domain.usecase.ObserveActiveCaptchaRequestsUseCase
import org.telegram.messenger.feature.captcha.domain.usecase.SubmitCaptchaResultUseCase
import org.telegram.messenger.feature.captcha.domain.usecase.VerifyCaptchaUseCase
import org.telegram.messenger.feature.captcha.presentation.CaptchaViewModel
import org.telegram.messenger.feature.hashtagsearch.data.repository.LegacyHashtagSearchRepository
import org.telegram.messenger.feature.hashtagsearch.domain.repository.HashtagSearchRepository
import org.telegram.messenger.feature.hashtagsearch.domain.usecase.AddHashtagToHistoryUseCase
import org.telegram.messenger.feature.hashtagsearch.domain.usecase.ClearHashtagHistoryUseCase
import org.telegram.messenger.feature.hashtagsearch.domain.usecase.ClearHashtagSearchResultsUseCase
import org.telegram.messenger.feature.hashtagsearch.domain.usecase.GetHashtagHistoryUseCase
import org.telegram.messenger.feature.hashtagsearch.domain.usecase.JumpToHashtagMessageUseCase
import org.telegram.messenger.feature.hashtagsearch.domain.usecase.ObserveHashtagHistoryUseCase
import org.telegram.messenger.feature.hashtagsearch.domain.usecase.ObserveHashtagSearchResultUseCase
import org.telegram.messenger.feature.hashtagsearch.domain.usecase.RemoveHashtagFromHistoryUseCase
import org.telegram.messenger.feature.hashtagsearch.domain.usecase.SearchHashtagUseCase
import org.telegram.messenger.feature.hashtagsearch.presentation.HashtagSearchViewModel
import org.telegram.messenger.feature.biometrics.data.repository.LegacyBiometricsRepository
import org.telegram.messenger.feature.biometrics.domain.repository.BiometricsRepository
import org.telegram.messenger.feature.biometrics.domain.usecase.CheckBiometricKeyReadyUseCase
import org.telegram.messenger.feature.biometrics.domain.usecase.DeleteInvalidBiometricKeyUseCase
import org.telegram.messenger.feature.biometrics.domain.usecase.GetBiometricKeyStateUseCase
import org.telegram.messenger.feature.biometrics.domain.usecase.HasDeviceBiometricsChangedUseCase
import org.telegram.messenger.feature.biometrics.domain.usecase.IsBiometricKeyReadyUseCase
import org.telegram.messenger.feature.biometrics.domain.usecase.ObserveBiometricKeyStateUseCase
import org.telegram.messenger.feature.biometrics.presentation.BiometricsViewModel
import org.telegram.messenger.feature.giftauctions.data.repository.LegacyGiftAuctionsRepository
import org.telegram.messenger.feature.giftauctions.domain.repository.GiftAuctionsRepository
import org.telegram.messenger.feature.giftauctions.domain.usecase.GetActiveAuctionsUseCase
import org.telegram.messenger.feature.giftauctions.domain.usecase.GetAuctionByIdUseCase
import org.telegram.messenger.feature.giftauctions.domain.usecase.GetAuctionBySlugUseCase
import org.telegram.messenger.feature.giftauctions.domain.usecase.LoadAuctionAcquiredGiftsUseCase
import org.telegram.messenger.feature.giftauctions.domain.usecase.ObserveActiveAuctionsUseCase
import org.telegram.messenger.feature.giftauctions.domain.usecase.ObserveAuctionUseCase
import org.telegram.messenger.feature.giftauctions.domain.usecase.RefreshActiveAuctionsUseCase
import org.telegram.messenger.feature.giftauctions.domain.usecase.SendAuctionBidUseCase
import org.telegram.messenger.feature.giftauctions.presentation.GiftAuctionsViewModel
import org.telegram.messenger.feature.businesslinks.data.repository.LegacyBusinessLinksRepository
import org.telegram.messenger.feature.businesslinks.domain.repository.BusinessLinksRepository
import org.telegram.messenger.feature.businesslinks.domain.usecase.CanAddNewBusinessLinkUseCase
import org.telegram.messenger.feature.businesslinks.domain.usecase.CreateBusinessLinkUseCase
import org.telegram.messenger.feature.businesslinks.domain.usecase.DeleteBusinessLinkUseCase
import org.telegram.messenger.feature.businesslinks.domain.usecase.EditBusinessLinkUseCase
import org.telegram.messenger.feature.businesslinks.domain.usecase.FindBusinessLinkUseCase
import org.telegram.messenger.feature.businesslinks.domain.usecase.GetBusinessLinksUseCase
import org.telegram.messenger.feature.businesslinks.domain.usecase.LoadBusinessLinksUseCase
import org.telegram.messenger.feature.businesslinks.domain.usecase.ObserveBusinessLinksUseCase
import org.telegram.messenger.feature.businesslinks.presentation.BusinessLinksViewModel
import org.telegram.messenger.feature.businessbots.data.repository.LegacyBusinessBotsRepository
import org.telegram.messenger.feature.businessbots.domain.repository.BusinessBotsRepository
import org.telegram.messenger.feature.businessbots.domain.usecase.DeleteConnectedBotUseCase
import org.telegram.messenger.feature.businessbots.domain.usecase.FindConnectedBotUseCase
import org.telegram.messenger.feature.businessbots.domain.usecase.GetConnectedBotsUseCase
import org.telegram.messenger.feature.businessbots.domain.usecase.LoadConnectedBotsUseCase
import org.telegram.messenger.feature.businessbots.domain.usecase.ObserveConnectedBotsUseCase
import org.telegram.messenger.feature.businessbots.domain.usecase.UpdateConnectedBotUseCase
import org.telegram.messenger.feature.businessbots.presentation.BusinessBotsViewModel
import org.telegram.messenger.feature.timezones.data.repository.LegacyTimezonesRepository
import org.telegram.messenger.feature.timezones.domain.repository.TimezonesRepository
import org.telegram.messenger.feature.timezones.domain.usecase.FindTimezoneUseCase
import org.telegram.messenger.feature.timezones.domain.usecase.GetSystemTimezoneIdUseCase
import org.telegram.messenger.feature.timezones.domain.usecase.GetTimezoneNameUseCase
import org.telegram.messenger.feature.timezones.domain.usecase.GetTimezonesUseCase
import org.telegram.messenger.feature.timezones.domain.usecase.LoadTimezonesUseCase
import org.telegram.messenger.feature.timezones.domain.usecase.ObserveTimezonesUseCase
import org.telegram.messenger.feature.timezones.presentation.TimezonesViewModel
import org.telegram.messenger.feature.botstars.data.repository.LegacyBotStarsRepository
import org.telegram.messenger.feature.botstars.domain.repository.BotStarsRepository
import org.telegram.messenger.feature.botstars.domain.usecase.GetAdminedBotsAndChannelsUseCase
import org.telegram.messenger.feature.botstars.domain.usecase.GetBotStarsStatsUseCase
import org.telegram.messenger.feature.botstars.domain.usecase.GetTonStatsUseCase
import org.telegram.messenger.feature.botstars.domain.usecase.LoadBotTransactionsUseCase
import org.telegram.messenger.feature.botstars.domain.usecase.LoadConnectedStarBotsUseCase
import org.telegram.messenger.feature.botstars.domain.usecase.LoadSuggestedStarBotsUseCase
import org.telegram.messenger.feature.botstars.domain.usecase.ObserveBotStarsStatsUseCase
import org.telegram.messenger.feature.botstars.domain.usecase.ObserveBotTransactionsUseCase
import org.telegram.messenger.feature.botstars.domain.usecase.ObserveConnectedStarBotsUseCase
import org.telegram.messenger.feature.botstars.domain.usecase.ObserveTonStatsUseCase
import org.telegram.messenger.feature.botstars.presentation.BotStarsViewModel
import org.telegram.messenger.feature.billing.data.repository.LegacyBillingRepository
import org.telegram.messenger.feature.billing.domain.repository.BillingRepository
import org.telegram.messenger.feature.billing.domain.usecase.FormatCurrencyUseCase
import org.telegram.messenger.feature.billing.domain.usecase.GetBillingStateUseCase
import org.telegram.messenger.feature.billing.domain.usecase.GetCurrencyExpUseCase
import org.telegram.messenger.feature.billing.domain.usecase.GetPremiumProductUseCase
import org.telegram.messenger.feature.billing.domain.usecase.ManageSubscriptionUseCase
import org.telegram.messenger.feature.billing.domain.usecase.ObserveBillingStateUseCase
import org.telegram.messenger.feature.billing.domain.usecase.QueryBillingPurchasesUseCase
import org.telegram.messenger.feature.billing.domain.usecase.StartBillingConnectionUseCase
import org.telegram.messenger.feature.billing.presentation.BillingViewModel
import org.telegram.messenger.feature.launchericon.data.repository.LegacyLauncherIconRepository
import org.telegram.messenger.feature.launchericon.domain.repository.LauncherIconRepository
import org.telegram.messenger.feature.launchericon.domain.usecase.FixLauncherIconIfNeededUseCase
import org.telegram.messenger.feature.launchericon.domain.usecase.GetActiveLauncherIconUseCase
import org.telegram.messenger.feature.launchericon.domain.usecase.GetLauncherIconsUseCase
import org.telegram.messenger.feature.launchericon.domain.usecase.IsLauncherIconEnabledUseCase
import org.telegram.messenger.feature.launchericon.domain.usecase.ObserveLauncherIconsUseCase
import org.telegram.messenger.feature.launchericon.domain.usecase.SetLauncherIconUseCase
import org.telegram.messenger.feature.launchericon.presentation.LauncherIconViewModel
import org.telegram.messenger.feature.push.data.repository.LegacyPushRepository
import org.telegram.messenger.feature.push.domain.repository.PushRepository
import org.telegram.messenger.feature.push.domain.usecase.GetPushStatusUseCase
import org.telegram.messenger.feature.push.domain.usecase.IsPushAvailableUseCase
import org.telegram.messenger.feature.push.domain.usecase.ObservePushStatusUseCase
import org.telegram.messenger.feature.push.domain.usecase.RegisterPushTokenUseCase
import org.telegram.messenger.feature.push.domain.usecase.RequestPushTokenUseCase
import org.telegram.messenger.feature.push.domain.usecase.ResetPushTokenUseCase
import org.telegram.messenger.feature.push.presentation.PushViewModel
import org.telegram.messenger.feature.chromecast.data.repository.LegacyChromecastRepository
import org.telegram.messenger.feature.chromecast.domain.repository.ChromecastRepository
import org.telegram.messenger.feature.chromecast.domain.usecase.CastMediaUseCase
import org.telegram.messenger.feature.chromecast.domain.usecase.GetChromecastStateUseCase
import org.telegram.messenger.feature.chromecast.domain.usecase.IsCastingUseCase
import org.telegram.messenger.feature.chromecast.domain.usecase.IsMediaPlayingOnCastUseCase
import org.telegram.messenger.feature.chromecast.domain.usecase.ObserveChromecastStateUseCase
import org.telegram.messenger.feature.chromecast.domain.usecase.SetCastCoverFileUseCase
import org.telegram.messenger.feature.chromecast.domain.usecase.StopCastingUseCase
import org.telegram.messenger.feature.chromecast.presentation.ChromecastViewModel
import org.telegram.messenger.feature.hints.data.repository.LegacyHintsRepository
import org.telegram.messenger.feature.hints.domain.repository.HintsRepository
import org.telegram.messenger.feature.hints.domain.usecase.DoNotShowAgainHintUseCase
import org.telegram.messenger.feature.hints.domain.usecase.GetHintUseCase
import org.telegram.messenger.feature.hints.domain.usecase.GetHintsStateUseCase
import org.telegram.messenger.feature.hints.domain.usecase.IncrementHintUseCase
import org.telegram.messenger.feature.hints.domain.usecase.ObserveHintsUseCase
import org.telegram.messenger.feature.hints.domain.usecase.ResetAllHintsUseCase
import org.telegram.messenger.feature.hints.domain.usecase.ResetHintUseCase
import org.telegram.messenger.feature.hints.domain.usecase.ShouldShowHintUseCase
import org.telegram.messenger.feature.hints.presentation.HintsViewModel
import org.telegram.messenger.feature.groupcallmsg.data.repository.LegacyGroupCallMessagesRepository
import org.telegram.messenger.feature.groupcallmsg.domain.repository.GroupCallMessagesRepository
import org.telegram.messenger.feature.groupcallmsg.domain.usecase.ClearGroupCallMessagesUseCase
import org.telegram.messenger.feature.groupcallmsg.domain.usecase.GetGroupCallMessagesUseCase
import org.telegram.messenger.feature.groupcallmsg.domain.usecase.ObserveGroupCallMessagesUseCase
import org.telegram.messenger.feature.groupcallmsg.domain.usecase.PopGroupCallMessageUseCase
import org.telegram.messenger.feature.groupcallmsg.domain.usecase.SendGroupCallMessageUseCase
import org.telegram.messenger.feature.groupcallmsg.presentation.GroupCallMessagesViewModel
import org.telegram.messenger.feature.gallerysave.data.repository.LegacyGallerySaveRepository
import org.telegram.messenger.feature.gallerysave.domain.repository.GallerySaveRepository
import org.telegram.messenger.feature.gallerysave.domain.usecase.GetGallerySaveConfigUseCase
import org.telegram.messenger.feature.gallerysave.domain.usecase.GetGallerySaveExceptionsUseCase
import org.telegram.messenger.feature.gallerysave.domain.usecase.GetGallerySaveSettingsUseCase
import org.telegram.messenger.feature.gallerysave.domain.usecase.ObserveGallerySaveConfigUseCase
import org.telegram.messenger.feature.gallerysave.domain.usecase.RemoveAllGallerySaveExceptionsUseCase
import org.telegram.messenger.feature.gallerysave.domain.usecase.RemoveGallerySaveExceptionUseCase
import org.telegram.messenger.feature.gallerysave.domain.usecase.SetGallerySaveExceptionUseCase
import org.telegram.messenger.feature.gallerysave.domain.usecase.SetGallerySaveVideoLimitUseCase
import org.telegram.messenger.feature.gallerysave.domain.usecase.ToggleGallerySavePeerTypeUseCase
import org.telegram.messenger.feature.gallerysave.domain.usecase.UpdateGallerySaveSettingsUseCase
import org.telegram.messenger.feature.gallerysave.presentation.GallerySaveViewModel
import org.telegram.messenger.feature.refreshrate.data.repository.LegacyRefreshRateRepository
import org.telegram.messenger.feature.refreshrate.domain.repository.RefreshRateRepository
import org.telegram.messenger.feature.refreshrate.domain.usecase.GetDisplayRefreshModesUseCase
import org.telegram.messenger.feature.refreshrate.domain.usecase.GetRefreshRateStateUseCase
import org.telegram.messenger.feature.refreshrate.domain.usecase.ObserveRefreshRateStateUseCase
import org.telegram.messenger.feature.refreshrate.domain.usecase.RecordFrameMetricUseCase
import org.telegram.messenger.feature.refreshrate.domain.usecase.ResetRefreshRateStatsUseCase
import org.telegram.messenger.feature.refreshrate.domain.usecase.SetPreferredRefreshRateModeUseCase
import org.telegram.messenger.feature.refreshrate.domain.usecase.StartRefreshRateTrackingUseCase
import org.telegram.messenger.feature.refreshrate.domain.usecase.StopRefreshRateTrackingUseCase
import org.telegram.messenger.feature.refreshrate.domain.usecase.ToggleAdaptiveRefreshRateUseCase
import org.telegram.messenger.feature.refreshrate.presentation.RefreshRateViewModel
import org.telegram.messenger.feature.chatmeta.data.repository.LegacyChatMessagesMetadataRepository
import org.telegram.messenger.feature.chatmeta.domain.repository.ChatMessagesMetadataRepository
import org.telegram.messenger.feature.chatmeta.domain.usecase.CancelPendingMetadataRequestsUseCase
import org.telegram.messenger.feature.chatmeta.domain.usecase.CheckMessagesMetadataUseCase
import org.telegram.messenger.feature.chatmeta.domain.usecase.GetChatMetadataStatsUseCase
import org.telegram.messenger.feature.chatmeta.domain.usecase.LoadMessagesExtendedMediaUseCase
import org.telegram.messenger.feature.chatmeta.domain.usecase.LoadMessagesReactionsUseCase
import org.telegram.messenger.feature.chatmeta.domain.usecase.ObserveChatMetadataStatsUseCase
import org.telegram.messenger.feature.chatmeta.presentation.ChatMetadataViewModel
import org.telegram.messenger.feature.pip.data.repository.LegacyPipRepository
import org.telegram.messenger.feature.pip.domain.repository.PipRepository
import org.telegram.messenger.feature.pip.domain.usecase.DispatchPipStateUseCase
import org.telegram.messenger.feature.pip.domain.usecase.EvaluatePipEligibilityUseCase
import org.telegram.messenger.feature.pip.domain.usecase.GetPipSessionUseCase
import org.telegram.messenger.feature.pip.domain.usecase.ObservePipSessionUseCase
import org.telegram.messenger.feature.pip.domain.usecase.RegisterPipSourceUseCase
import org.telegram.messenger.feature.pip.domain.usecase.TriggerPipActionUseCase
import org.telegram.messenger.feature.pip.domain.usecase.UnregisterPipSourceUseCase
import org.telegram.messenger.feature.pip.domain.usecase.UpdatePipSourceStateUseCase
import org.telegram.messenger.feature.pip.presentation.PipViewModel
import org.telegram.messenger.feature.drafts.data.repository.LegacyDraftsRepository
import org.telegram.messenger.feature.drafts.domain.repository.DraftsRepository
import org.telegram.messenger.feature.drafts.domain.usecase.CleanupExpiredDraftsUseCase
import org.telegram.messenger.feature.drafts.domain.usecase.DeleteDraftUseCase
import org.telegram.messenger.feature.drafts.domain.usecase.DeleteForEditUseCase
import org.telegram.messenger.feature.drafts.domain.usecase.GetDraftForEditUseCase
import org.telegram.messenger.feature.drafts.domain.usecase.GetDraftsStateUseCase
import org.telegram.messenger.feature.drafts.domain.usecase.LoadDraftsUseCase
import org.telegram.messenger.feature.drafts.domain.usecase.ObserveDraftsStateUseCase
import org.telegram.messenger.feature.drafts.domain.usecase.SaveDraftUseCase
import org.telegram.messenger.feature.drafts.presentation.DraftsViewModel
import org.telegram.messenger.feature.fileref.data.repository.LegacyFileRefRepository
import org.telegram.messenger.feature.fileref.domain.repository.FileRefRepository
import org.telegram.messenger.feature.fileref.domain.usecase.CancelFileRefRequestUseCase
import org.telegram.messenger.feature.fileref.domain.usecase.ClearFileRefCacheUseCase
import org.telegram.messenger.feature.fileref.domain.usecase.GetFileRefStatsUseCase
import org.telegram.messenger.feature.fileref.domain.usecase.NotifyReferenceRenewedUseCase
import org.telegram.messenger.feature.fileref.domain.usecase.ObserveFileRefStatsUseCase
import org.telegram.messenger.feature.fileref.domain.usecase.RequestReferenceRenewalUseCase
import org.telegram.messenger.feature.fileref.presentation.FileRefViewModel
import org.telegram.messenger.feature.camera.data.repository.LegacyCameraRepository
import org.telegram.messenger.feature.camera.domain.repository.CameraRepository
import org.telegram.messenger.feature.camera.domain.usecase.ChooseOptimalResolutionUseCase
import org.telegram.messenger.feature.camera.domain.usecase.GetCameraStateUseCase
import org.telegram.messenger.feature.camera.domain.usecase.InitCamerasUseCase
import org.telegram.messenger.feature.camera.domain.usecase.NotifyCameraRecordingUseCase
import org.telegram.messenger.feature.camera.domain.usecase.ObserveCameraStateUseCase
import org.telegram.messenger.feature.camera.domain.usecase.SelectCameraUseCase
import org.telegram.messenger.feature.camera.domain.usecase.SetCameraFlashModeUseCase
import org.telegram.messenger.feature.camera.domain.usecase.SwitchCameraUseCase
import org.telegram.messenger.feature.camera.domain.usecase.ToggleMirrorFrontCameraUseCase
import org.telegram.messenger.feature.camera.presentation.CameraViewModel
import org.telegram.messenger.feature.cachebychats.data.repository.LegacyCacheByChatsRepository
import org.telegram.messenger.feature.cachebychats.domain.repository.CacheByChatsRepository
import org.telegram.messenger.feature.cachebychats.domain.usecase.ClearKeepMediaExceptionsUseCase
import org.telegram.messenger.feature.cachebychats.domain.usecase.GetCacheByChatsConfigUseCase
import org.telegram.messenger.feature.cachebychats.domain.usecase.ObserveCacheByChatsConfigUseCase
import org.telegram.messenger.feature.cachebychats.domain.usecase.RemoveKeepMediaExceptionUseCase
import org.telegram.messenger.feature.cachebychats.domain.usecase.SetKeepMediaDurationUseCase
import org.telegram.messenger.feature.cachebychats.domain.usecase.SetKeepMediaExceptionUseCase
import org.telegram.messenger.feature.cachebychats.presentation.CacheByChatsViewModel
import org.telegram.messenger.feature.draftmeasure.data.repository.LegacyDraftMeasureRepository
import org.telegram.messenger.feature.draftmeasure.domain.repository.DraftMeasureRepository
import org.telegram.messenger.feature.draftmeasure.domain.usecase.CalculateDraftMeasureOverrideUseCase
import org.telegram.messenger.feature.draftmeasure.domain.usecase.GetDraftMeasureConfigUseCase
import org.telegram.messenger.feature.draftmeasure.domain.usecase.ObserveDraftMeasureConfigUseCase
import org.telegram.messenger.feature.draftmeasure.domain.usecase.OnDraftMessageIdChangedUseCase
import org.telegram.messenger.feature.draftmeasure.domain.usecase.ResetDraftMeasureTargetUseCase
import org.telegram.messenger.feature.draftmeasure.domain.usecase.SetDraftMeasureTargetUseCase
import org.telegram.messenger.feature.draftmeasure.domain.usecase.SetPreviousMessageHeightUseCase
import org.telegram.messenger.feature.draftmeasure.presentation.DraftMeasureViewModel
import org.telegram.ui.Components.chat.ChatActivityDraftMessageMeasureController
import org.telegram.messenger.feature.bottomviews.data.repository.LegacyBottomViewsVisibilityRepository
import org.telegram.messenger.feature.bottomviews.domain.repository.BottomViewsVisibilityRepository
import org.telegram.messenger.feature.bottomviews.domain.usecase.GetBottomViewVisibilityUseCase
import org.telegram.messenger.feature.bottomviews.domain.usecase.GetBottomViewsStateUseCase
import org.telegram.messenger.feature.bottomviews.domain.usecase.GetPriorityBottomContainerUseCase
import org.telegram.messenger.feature.bottomviews.domain.usecase.ObserveBottomViewsVisibilityUseCase
import org.telegram.messenger.feature.bottomviews.domain.usecase.SetBottomViewVisibleUseCase
import org.telegram.messenger.feature.bottomviews.presentation.BottomViewsViewModel
import org.telegram.ui.Components.chat.ChatActivityBottomViewsVisibilityController
import org.telegram.messenger.feature.floatingdebug.data.repository.LegacyFloatingDebugRepository
import org.telegram.messenger.feature.floatingdebug.domain.repository.FloatingDebugRepository
import org.telegram.messenger.feature.floatingdebug.domain.usecase.ClearFloatingDebugItemsUseCase
import org.telegram.messenger.feature.floatingdebug.domain.usecase.GetFloatingDebugItemsUseCase
import org.telegram.messenger.feature.floatingdebug.domain.usecase.GetFloatingDebugStateUseCase
import org.telegram.messenger.feature.floatingdebug.domain.usecase.IsFloatingDebugActiveUseCase
import org.telegram.messenger.feature.floatingdebug.domain.usecase.ObserveFloatingDebugStateUseCase
import org.telegram.messenger.feature.floatingdebug.domain.usecase.RegisterFloatingDebugItemsUseCase
import org.telegram.messenger.feature.floatingdebug.domain.usecase.SetFloatingDebugActiveUseCase
import org.telegram.messenger.feature.floatingdebug.domain.usecase.ToggleFloatingDebugActiveUseCase
import org.telegram.messenger.feature.floatingdebug.presentation.FloatingDebugViewModel
import org.telegram.ui.LaunchActivity
import org.telegram.messenger.feature.keyboardinsets.data.repository.LegacyKeyboardInsetsRepository
import org.telegram.messenger.feature.keyboardinsets.domain.repository.KeyboardInsetsRepository
import org.telegram.messenger.feature.keyboardinsets.domain.usecase.GetKeyboardInsetsUseCase
import org.telegram.messenger.feature.keyboardinsets.domain.usecase.ObserveKeyboardInsetsUseCase
import org.telegram.messenger.feature.keyboardinsets.domain.usecase.RequestInAppKeyboardHeightUseCase
import org.telegram.messenger.feature.keyboardinsets.domain.usecase.RequestInAppKeyboardHeightWithNavbarUseCase
import org.telegram.messenger.feature.keyboardinsets.domain.usecase.ResetInAppKeyboardHeightUseCase
import org.telegram.messenger.feature.keyboardinsets.domain.usecase.UpdateSystemInsetsUseCase
import org.telegram.messenger.feature.keyboardinsets.presentation.KeyboardInsetsViewModel
import org.telegram.ui.Components.inset.WindowInsetsInAppController
import org.telegram.messenger.feature.maintabs.data.repository.LegacyMainTabsRepository
import org.telegram.messenger.feature.maintabs.domain.repository.MainTabsRepository
import org.telegram.messenger.feature.maintabs.domain.usecase.GetMainTabsConfigUseCase
import org.telegram.messenger.feature.maintabs.domain.usecase.ObserveMainTabsConfigUseCase
import org.telegram.messenger.feature.maintabs.domain.usecase.SelectMainTabUseCase
import org.telegram.messenger.feature.maintabs.domain.usecase.SetContactsPermissionWarningUseCase
import org.telegram.messenger.feature.maintabs.domain.usecase.SetMainTabsVisibleUseCase
import org.telegram.messenger.feature.maintabs.domain.usecase.SetShowCallsTabUseCase
import org.telegram.messenger.feature.maintabs.domain.usecase.UpdateChatsUnreadCountUseCase
import org.telegram.messenger.feature.maintabs.presentation.MainTabsViewModel
import org.telegram.ui.MainTabsActivityController
import org.telegram.messenger.feature.richcaption.data.repository.LegacyRichCaptionRepository
import org.telegram.messenger.feature.richcaption.domain.repository.RichCaptionRepository
import org.telegram.messenger.feature.richcaption.domain.usecase.CalculateCaptionMeasureWidthUseCase
import org.telegram.messenger.feature.richcaption.domain.usecase.CheckCaptionPressHitUseCase
import org.telegram.messenger.feature.richcaption.domain.usecase.ClearRichCaptionUseCase
import org.telegram.messenger.feature.richcaption.domain.usecase.GetRichCaptionUseCase
import org.telegram.messenger.feature.richcaption.domain.usecase.ObserveRichCaptionUseCase
import org.telegram.messenger.feature.richcaption.domain.usecase.SetRichCaptionCreditUseCase
import org.telegram.messenger.feature.richcaption.domain.usecase.SetRichCaptionLockedUseCase
import org.telegram.messenger.feature.richcaption.domain.usecase.SetRichCaptionTextUseCase
import org.telegram.messenger.feature.richcaption.presentation.RichCaptionViewModel
import org.telegram.messenger.feature.adjustpan.data.repository.LegacyAdjustPanRepository
import org.telegram.messenger.feature.adjustpan.domain.repository.AdjustPanRepository
import org.telegram.messenger.feature.adjustpan.domain.usecase.CalculatePanTransitionPlanUseCase
import org.telegram.messenger.feature.adjustpan.domain.usecase.ComputePanProgressUseCase
import org.telegram.messenger.feature.adjustpan.domain.usecase.GetAdjustPanStateUseCase
import org.telegram.messenger.feature.adjustpan.domain.usecase.ObserveAdjustPanStateUseCase
import org.telegram.messenger.feature.adjustpan.domain.usecase.ResetAdjustPanUseCase
import org.telegram.messenger.feature.adjustpan.domain.usecase.SetAdjustPanEnabledUseCase
import org.telegram.messenger.feature.adjustpan.domain.usecase.StartAdjustPanTransitionUseCase
import org.telegram.messenger.feature.adjustpan.domain.usecase.StopAdjustPanTransitionUseCase
import org.telegram.messenger.feature.adjustpan.domain.usecase.UpdateAdjustPanTransitionUseCase
import org.telegram.messenger.feature.adjustpan.presentation.AdjustPanViewModel
import org.telegram.messenger.feature.keyboardhide.data.repository.LegacyKeyboardHideRepository
import org.telegram.messenger.feature.keyboardhide.domain.repository.KeyboardHideRepository
import org.telegram.messenger.feature.keyboardhide.domain.usecase.CalculateKeyboardHideProgressUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.EndKeyboardHideMovingUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.EvaluateKeyboardDismissDecisionUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.FinishKeyboardHideDismissUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.GetKeyboardHideStateUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.ObserveKeyboardHideStateUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.ResetKeyboardHideUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.SetKeyboardHideEnabledUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.StartKeyboardHideMovingUseCase
import org.telegram.messenger.feature.keyboardhide.domain.usecase.UpdateKeyboardHideMovingUseCase
import org.telegram.messenger.feature.keyboardhide.presentation.KeyboardHideViewModel
import org.telegram.messenger.feature.businessrecipients.data.repository.LegacyBusinessRecipientsRepository
import org.telegram.messenger.feature.businessrecipients.domain.repository.BusinessRecipientsRepository
import org.telegram.messenger.feature.businessrecipients.domain.usecase.AddExcludedUsersUseCase
import org.telegram.messenger.feature.businessrecipients.domain.usecase.AddSelectedUsersUseCase
import org.telegram.messenger.feature.businessrecipients.domain.usecase.CheckRecipientsChangesUseCase
import org.telegram.messenger.feature.businessrecipients.domain.usecase.GetBusinessRecipientsUseCase
import org.telegram.messenger.feature.businessrecipients.domain.usecase.ObserveBusinessRecipientsUseCase
import org.telegram.messenger.feature.businessrecipients.domain.usecase.RemoveExcludedUserUseCase
import org.telegram.messenger.feature.businessrecipients.domain.usecase.RemoveSelectedUserUseCase
import org.telegram.messenger.feature.businessrecipients.domain.usecase.ResetBusinessRecipientsUseCase
import org.telegram.messenger.feature.businessrecipients.domain.usecase.SetBusinessRecipientsUseCase
import org.telegram.messenger.feature.businessrecipients.domain.usecase.ToggleExcludeSelectedUseCase
import org.telegram.messenger.feature.businessrecipients.domain.usecase.ToggleRecipientFilterUseCase
import org.telegram.messenger.feature.businessrecipients.domain.usecase.ValidateBusinessRecipientsUseCase
import org.telegram.messenger.feature.businessrecipients.presentation.BusinessRecipientsViewModel
import org.telegram.messenger.feature.pinchtozoom.data.repository.LegacyPinchToZoomRepository
import org.telegram.messenger.feature.pinchtozoom.domain.repository.PinchToZoomRepository
import org.telegram.messenger.feature.pinchtozoom.domain.usecase.CalculatePinchImageBoundsUseCase
import org.telegram.messenger.feature.pinchtozoom.domain.usecase.CalculatePinchScaleUseCase
import org.telegram.messenger.feature.pinchtozoom.domain.usecase.CalculatePinchTransformUseCase
import org.telegram.messenger.feature.pinchtozoom.domain.usecase.CalculatePinchTranslationUseCase
import org.telegram.messenger.feature.pinchtozoom.domain.usecase.EvaluatePinchGestureUseCase
import org.telegram.messenger.feature.pinchtozoom.domain.usecase.FinishPinchZoomUseCase
import org.telegram.messenger.feature.pinchtozoom.domain.usecase.GetPinchZoomStateUseCase
import org.telegram.messenger.feature.pinchtozoom.domain.usecase.ObservePinchZoomStateUseCase
import org.telegram.messenger.feature.pinchtozoom.domain.usecase.ResetPinchZoomUseCase
import org.telegram.messenger.feature.pinchtozoom.domain.usecase.StartPinchZoomUseCase
import org.telegram.messenger.feature.pinchtozoom.domain.usecase.UpdatePinchZoomUseCase
import org.telegram.messenger.feature.pinchtozoom.presentation.PinchToZoomViewModel
import org.telegram.messenger.feature.recyclerscroll.data.repository.LegacyRecyclerScrollRepository
import org.telegram.messenger.feature.recyclerscroll.domain.repository.RecyclerScrollRepository
import org.telegram.messenger.feature.recyclerscroll.domain.usecase.CalculateScrollAnimationPlanUseCase
import org.telegram.messenger.feature.recyclerscroll.domain.usecase.CalculateScrollLengthUseCase
import org.telegram.messenger.feature.recyclerscroll.domain.usecase.CancelRecyclerScrollUseCase
import org.telegram.messenger.feature.recyclerscroll.domain.usecase.ComputeScrollViewTranslationsUseCase
import org.telegram.messenger.feature.recyclerscroll.domain.usecase.EvaluateScrollEligibilityUseCase
import org.telegram.messenger.feature.recyclerscroll.domain.usecase.FinishRecyclerScrollUseCase
import org.telegram.messenger.feature.recyclerscroll.domain.usecase.GetRecyclerScrollStateUseCase
import org.telegram.messenger.feature.recyclerscroll.domain.usecase.ObserveRecyclerScrollStateUseCase
import org.telegram.messenger.feature.recyclerscroll.domain.usecase.ResetRecyclerScrollUseCase
import org.telegram.messenger.feature.recyclerscroll.domain.usecase.StartRecyclerScrollUseCase
import org.telegram.messenger.feature.recyclerscroll.domain.usecase.UpdateRecyclerScrollProgressUseCase
import org.telegram.messenger.feature.recyclerscroll.presentation.RecyclerScrollViewModel
import org.telegram.messenger.feature.emojieffects.data.repository.LegacyEmojiEffectsRepository
import org.telegram.messenger.feature.emojieffects.domain.repository.EmojiEffectsRepository
import org.telegram.messenger.feature.emojieffects.domain.usecase.CalculateEmojiBoundsUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.CalculateEmojiOverlayPositionUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.ClearEmojiEffectsUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.DecodeEmojiInteractionsJsonUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.DismissEmojiEffectUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.EncodeEmojiInteractionsJsonUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.EvaluateAnimationQuotaUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.EvaluateEmojiSupportUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.GetEmojiEffectsStateUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.NormalizeEmojiUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.ObserveEmojiEffectsStateUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.RecordEmojiTapUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.StartEmojiEffectUseCase
import org.telegram.messenger.feature.emojieffects.domain.usecase.UpdateEmojiEffectProgressUseCase
import org.telegram.messenger.feature.emojieffects.presentation.EmojiEffectsViewModel
import org.telegram.messenger.feature.mentions.data.repository.LegacyMentionsRepository
import org.telegram.messenger.feature.mentions.domain.repository.MentionsRepository
import org.telegram.messenger.feature.mentions.domain.usecase.ClearMentionsUseCase
import org.telegram.messenger.feature.mentions.domain.usecase.DismissMentionsUseCase
import org.telegram.messenger.feature.mentions.domain.usecase.FilterMentionsUseCase
import org.telegram.messenger.feature.mentions.domain.usecase.FormatMentionReplacementUseCase
import org.telegram.messenger.feature.mentions.domain.usecase.GetMentionsStateUseCase
import org.telegram.messenger.feature.mentions.domain.usecase.ObserveMentionsStateUseCase
import org.telegram.messenger.feature.mentions.domain.usecase.ParseMentionQueryUseCase
import org.telegram.messenger.feature.mentions.domain.usecase.SetMentionCandidatesUseCase
import org.telegram.messenger.feature.mentions.domain.usecase.UpdateMentionQueryUseCase
import org.telegram.messenger.feature.mentions.domain.usecase.ValidateUsernameUseCase
import org.telegram.messenger.feature.mentions.presentation.MentionsViewModel
import org.telegram.messenger.feature.sharedmedia.data.repository.LegacySharedMediaRepository
import org.telegram.messenger.feature.sharedmedia.domain.repository.SharedMediaRepository
import org.telegram.messenger.feature.sharedmedia.domain.usecase.CalculateMediaSelectionUseCase
import org.telegram.messenger.feature.sharedmedia.domain.usecase.ClearMediaSelectionUseCase
import org.telegram.messenger.feature.sharedmedia.domain.usecase.FilterSharedMediaUseCase
import org.telegram.messenger.feature.sharedmedia.domain.usecase.GetSharedMediaStateUseCase
import org.telegram.messenger.feature.sharedmedia.domain.usecase.GroupMediaByMonthUseCase
import org.telegram.messenger.feature.sharedmedia.domain.usecase.ObserveSharedMediaStateUseCase
import org.telegram.messenger.feature.sharedmedia.domain.usecase.ResolveAvailableTabsUseCase
import org.telegram.messenger.feature.sharedmedia.domain.usecase.SelectSharedMediaTabUseCase
import org.telegram.messenger.feature.sharedmedia.domain.usecase.SetSharedMediaFilterUseCase
import org.telegram.messenger.feature.sharedmedia.domain.usecase.ToggleMediaSelectionUseCase
import org.telegram.messenger.feature.sharedmedia.presentation.SharedMediaViewModel
import org.telegram.messenger.feature.contentpreview.data.repository.LegacyContentPreviewRepository
import org.telegram.messenger.feature.contentpreview.domain.repository.ContentPreviewRepository
import org.telegram.messenger.feature.contentpreview.domain.usecase.CalculatePreviewDragUseCase
import org.telegram.messenger.feature.contentpreview.domain.usecase.ClearContentPreviewUseCase
import org.telegram.messenger.feature.contentpreview.domain.usecase.DismissContentPreviewUseCase
import org.telegram.messenger.feature.contentpreview.domain.usecase.EvaluatePreviewEligibilityUseCase
import org.telegram.messenger.feature.contentpreview.domain.usecase.GetContentPreviewStateUseCase
import org.telegram.messenger.feature.contentpreview.domain.usecase.ObserveContentPreviewStateUseCase
import org.telegram.messenger.feature.contentpreview.domain.usecase.OpenContentPreviewUseCase
import org.telegram.messenger.feature.contentpreview.domain.usecase.ResolvePreviewActionsUseCase
import org.telegram.messenger.feature.contentpreview.domain.usecase.TriggerPreviewActionUseCase
import org.telegram.messenger.feature.contentpreview.domain.usecase.UpdatePreviewDragUseCase
import org.telegram.messenger.feature.contentpreview.presentation.ContentPreviewViewModel
import org.telegram.messenger.feature.emojipicker.data.repository.LegacyEmojiPickerRepository
import org.telegram.messenger.feature.emojipicker.domain.repository.EmojiPickerRepository
import org.telegram.messenger.feature.emojipicker.domain.usecase.ClearRecentPickerItemsUseCase
import org.telegram.messenger.feature.emojipicker.domain.usecase.FilterEmojiItemsUseCase
import org.telegram.messenger.feature.emojipicker.domain.usecase.FilterGifsUseCase
import org.telegram.messenger.feature.emojipicker.domain.usecase.FilterStickersUseCase
import org.telegram.messenger.feature.emojipicker.domain.usecase.GetEmojiPickerStateUseCase
import org.telegram.messenger.feature.emojipicker.domain.usecase.ObserveEmojiPickerStateUseCase
import org.telegram.messenger.feature.emojipicker.domain.usecase.ResolveAvailablePickerTabsUseCase
import org.telegram.messenger.feature.emojipicker.domain.usecase.SelectPickerTabUseCase
import org.telegram.messenger.feature.emojipicker.domain.usecase.ToggleStickerFavoriteUseCase
import org.telegram.messenger.feature.emojipicker.domain.usecase.UpdatePickerSearchQueryUseCase
import org.telegram.messenger.feature.emojipicker.presentation.EmojiPickerViewModel
import org.telegram.messenger.feature.chatattach.data.repository.LegacyChatAttachRepository
import org.telegram.messenger.feature.chatattach.domain.repository.ChatAttachRepository
import org.telegram.messenger.feature.chatattach.domain.usecase.CalculateAttachCaptionLimitUseCase
import org.telegram.messenger.feature.chatattach.domain.usecase.ClearAttachSelectionUseCase
import org.telegram.messenger.feature.chatattach.domain.usecase.GetChatAttachStateUseCase
import org.telegram.messenger.feature.chatattach.domain.usecase.ObserveChatAttachStateUseCase
import org.telegram.messenger.feature.chatattach.domain.usecase.OpenChatAttachAlertUseCase
import org.telegram.messenger.feature.chatattach.domain.usecase.ResolveAvailableAttachLayoutsUseCase
import org.telegram.messenger.feature.chatattach.domain.usecase.SelectAttachLayoutUseCase
import org.telegram.messenger.feature.chatattach.domain.usecase.ToggleAttachItemSelectionUseCase
import org.telegram.messenger.feature.chatattach.domain.usecase.UpdateAttachSendOptionsUseCase
import org.telegram.messenger.feature.chatattach.domain.usecase.ValidateSendOptionsUseCase
import org.telegram.messenger.feature.chatattach.presentation.ChatAttachViewModel
import org.telegram.messenger.feature.photoviewer.data.repository.LegacyPhotoViewerRepository
import org.telegram.messenger.feature.photoviewer.domain.repository.PhotoViewerRepository
import org.telegram.messenger.feature.photoviewer.domain.usecase.CalculateMediaPagingUseCase
import org.telegram.messenger.feature.photoviewer.domain.usecase.CalculateZoomTransformUseCase
import org.telegram.messenger.feature.photoviewer.domain.usecase.ClosePhotoViewerUseCase
import org.telegram.messenger.feature.photoviewer.domain.usecase.GetPhotoViewerStateUseCase
import org.telegram.messenger.feature.photoviewer.domain.usecase.NavigatePhotoViewerUseCase
import org.telegram.messenger.feature.photoviewer.domain.usecase.ObservePhotoViewerStateUseCase
import org.telegram.messenger.feature.photoviewer.domain.usecase.OpenPhotoViewerUseCase
import org.telegram.messenger.feature.photoviewer.domain.usecase.ResolveMediaQualityUseCase
import org.telegram.messenger.feature.photoviewer.domain.usecase.UpdatePlaybackStateUseCase
import org.telegram.messenger.feature.photoviewer.domain.usecase.ValidateViewerActionsUseCase
import org.telegram.messenger.feature.photoviewer.presentation.PhotoViewerViewModel
import org.telegram.messenger.feature.chatinput.data.repository.LegacyChatInputRepository
import org.telegram.messenger.feature.chatinput.domain.repository.ChatInputRepository
import org.telegram.messenger.feature.chatinput.domain.usecase.CalculateSendButtonStateUseCase
import org.telegram.messenger.feature.chatinput.domain.usecase.ClearChatInputReplyUseCase
import org.telegram.messenger.feature.chatinput.domain.usecase.FormatTextSelectionUseCase
import org.telegram.messenger.feature.chatinput.domain.usecase.GetChatInputStateUseCase
import org.telegram.messenger.feature.chatinput.domain.usecase.ObserveChatInputStateUseCase
import org.telegram.messenger.feature.chatinput.domain.usecase.ResolvePanelVisibilityUseCase
import org.telegram.messenger.feature.chatinput.domain.usecase.SetChatInputPanelModeUseCase
import org.telegram.messenger.feature.chatinput.domain.usecase.SetChatInputReplyUseCase
import org.telegram.messenger.feature.chatinput.domain.usecase.SetChatInputTextUseCase
import org.telegram.messenger.feature.chatinput.domain.usecase.ValidateVoiceRecordActionUseCase
import org.telegram.messenger.feature.chatinput.presentation.ChatInputViewModel
import org.telegram.messenger.feature.audioplayer.data.repository.LegacyAudioPlayerRepository
import org.telegram.messenger.feature.audioplayer.domain.repository.AudioPlayerRepository
import org.telegram.messenger.feature.audioplayer.domain.usecase.ConfigureEqualizerUseCase
import org.telegram.messenger.feature.audioplayer.domain.usecase.CyclePlaybackSpeedUseCase
import org.telegram.messenger.feature.audioplayer.domain.usecase.CycleRepeatModeUseCase
import org.telegram.messenger.feature.audioplayer.domain.usecase.GetPlaybackStateUseCase
import org.telegram.messenger.feature.audioplayer.domain.usecase.HandleProximitySensorUseCase
import org.telegram.messenger.feature.audioplayer.domain.usecase.NavigatePlaylistUseCase
import org.telegram.messenger.feature.audioplayer.domain.usecase.ObservePlaybackStateUseCase
import org.telegram.messenger.feature.audioplayer.domain.usecase.PlayTrackUseCase
import org.telegram.messenger.feature.audioplayer.domain.usecase.SeekAudioUseCase
import org.telegram.messenger.feature.audioplayer.domain.usecase.TogglePlayPauseUseCase
import org.telegram.messenger.feature.audioplayer.domain.usecase.ToggleShuffleUseCase
import org.telegram.messenger.feature.audioplayer.presentation.AudioPlayerViewModel
import org.telegram.messenger.feature.sendmessages.data.repository.LegacySendMessagesRepository
import org.telegram.messenger.feature.sendmessages.domain.repository.SendMessagesRepository
import org.telegram.messenger.feature.sendmessages.domain.usecase.CancelSendMessageUseCase
import org.telegram.messenger.feature.sendmessages.domain.usecase.ForwardMessagesUseCase
import org.telegram.messenger.feature.sendmessages.domain.usecase.ObservePendingSendsUseCase
import org.telegram.messenger.feature.sendmessages.domain.usecase.RetrySendMessageUseCase
import org.telegram.messenger.feature.sendmessages.domain.usecase.SendMediaAlbumUseCase
import org.telegram.messenger.feature.sendmessages.domain.usecase.SendMediaMessageUseCase
import org.telegram.messenger.feature.sendmessages.domain.usecase.SendTextMessageUseCase
import org.telegram.messenger.feature.sendmessages.presentation.SendMessagesViewModel
import org.telegram.messenger.feature.imageloader.data.repository.LegacyImageLoaderRepository
import org.telegram.messenger.feature.imageloader.domain.repository.ImageLoaderRepository
import org.telegram.messenger.feature.imageloader.domain.usecase.BuildImageCacheKeyUseCase
import org.telegram.messenger.feature.imageloader.domain.usecase.CalculateImageDownscaleUseCase
import org.telegram.messenger.feature.imageloader.domain.usecase.CancelImageRequestUseCase
import org.telegram.messenger.feature.imageloader.domain.usecase.ClearImageCacheUseCase
import org.telegram.messenger.feature.imageloader.domain.usecase.EnqueueImageRequestUseCase
import org.telegram.messenger.feature.imageloader.domain.usecase.EvaluateImageCacheEligibilityUseCase
import org.telegram.messenger.feature.imageloader.domain.usecase.FormatImageFilterUseCase
import org.telegram.messenger.feature.imageloader.domain.usecase.GetImageLoaderStateUseCase
import org.telegram.messenger.feature.imageloader.domain.usecase.ObserveImageLoaderStateUseCase
import org.telegram.messenger.feature.imageloader.domain.usecase.ParseImageFilterUseCase
import org.telegram.messenger.feature.imageloader.domain.usecase.TrimImageMemoryUseCase
import org.telegram.messenger.feature.imageloader.presentation.ImageLoaderViewModel
import org.telegram.messenger.feature.downloadmanager.data.repository.LegacyDownloadManagerRepository
import org.telegram.messenger.feature.downloadmanager.domain.repository.DownloadManagerRepository
import org.telegram.messenger.feature.downloadmanager.domain.usecase.CancelDownloadUseCase
import org.telegram.messenger.feature.downloadmanager.domain.usecase.ClearRecentDownloadsUseCase
import org.telegram.messenger.feature.downloadmanager.domain.usecase.EnqueueDownloadUseCase
import org.telegram.messenger.feature.downloadmanager.domain.usecase.EvaluateAutoDownloadEligibilityUseCase
import org.telegram.messenger.feature.downloadmanager.domain.usecase.GetDownloadManagerStateUseCase
import org.telegram.messenger.feature.downloadmanager.domain.usecase.MarkDownloadsAsViewedUseCase
import org.telegram.messenger.feature.downloadmanager.domain.usecase.ObserveDownloadManagerStateUseCase
import org.telegram.messenger.feature.downloadmanager.domain.usecase.PauseDownloadUseCase
import org.telegram.messenger.feature.downloadmanager.domain.usecase.ResumeDownloadUseCase
import org.telegram.messenger.feature.downloadmanager.domain.usecase.RetryDownloadUseCase
import org.telegram.messenger.feature.downloadmanager.domain.usecase.SetDownloadNetworkTypeUseCase
import org.telegram.messenger.feature.downloadmanager.domain.usecase.UpdateDownloadPresetUseCase
import org.telegram.messenger.feature.downloadmanager.domain.usecase.UpdateDownloadProgressUseCase
import org.telegram.messenger.feature.downloadmanager.presentation.DownloadManagerViewModel
import org.telegram.messenger.feature.localization.data.repository.LegacyLocalizationRepository
import org.telegram.messenger.feature.localization.domain.repository.LocalizationRepository
import org.telegram.messenger.feature.localization.domain.usecase.ApplyLocaleUseCase
import org.telegram.messenger.feature.localization.domain.usecase.DetectRtlLanguageUseCase
import org.telegram.messenger.feature.localization.domain.usecase.FormatFullNameUseCase
import org.telegram.messenger.feature.localization.domain.usecase.FormatNumberWithSuffixUseCase
import org.telegram.messenger.feature.localization.domain.usecase.FormatRelativeTimestampUseCase
import org.telegram.messenger.feature.localization.domain.usecase.GetLocalizationStateUseCase
import org.telegram.messenger.feature.localization.domain.usecase.ObserveLocalizationStateUseCase
import org.telegram.messenger.feature.localization.domain.usecase.ResolvePluralQuantityUseCase
import org.telegram.messenger.feature.localization.domain.usecase.SetNameDisplayOrderUseCase
import org.telegram.messenger.feature.localization.domain.usecase.Toggle24HourFormatUseCase
import org.telegram.messenger.feature.localization.presentation.LocalizationViewModel
import org.telegram.messenger.feature.ringtones.data.repository.LegacyRingtoneRepository
import org.telegram.messenger.feature.ringtones.domain.repository.RingtoneRepository
import org.telegram.messenger.feature.ringtones.domain.usecase.AddRingtoneUseCase
import org.telegram.messenger.feature.ringtones.domain.usecase.CancelRingtoneUploadUseCase
import org.telegram.messenger.feature.ringtones.domain.usecase.GetRingtoneByIdUseCase
import org.telegram.messenger.feature.ringtones.domain.usecase.GetRingtoneSoundPathUseCase
import org.telegram.messenger.feature.ringtones.domain.usecase.GetRingtonesUseCase
import org.telegram.messenger.feature.ringtones.domain.usecase.ObserveRingtoneStateUseCase
import org.telegram.messenger.feature.ringtones.domain.usecase.ObserveRingtonesUseCase
import org.telegram.messenger.feature.ringtones.domain.usecase.RefreshRingtonesUseCase
import org.telegram.messenger.feature.ringtones.domain.usecase.RemoveRingtoneUseCase
import org.telegram.messenger.feature.ringtones.domain.usecase.SaveRingtoneFromDocumentUseCase
import org.telegram.messenger.feature.ringtones.domain.usecase.SelectRingtoneUseCase
import org.telegram.messenger.feature.ringtones.domain.usecase.UploadRingtoneUseCase
import org.telegram.messenger.feature.ringtones.domain.usecase.ValidateRingtoneEligibilityUseCase
import org.telegram.messenger.feature.ringtones.presentation.RingtoneViewModel
import org.telegram.messenger.feature.networkstats.data.repository.LegacyNetworkStatsRepository
import org.telegram.messenger.feature.networkstats.domain.repository.NetworkStatsRepository
import org.telegram.messenger.feature.networkstats.domain.usecase.CalculateMessagesTrafficUseCase
import org.telegram.messenger.feature.networkstats.domain.usecase.FormatCallsDurationUseCase
import org.telegram.messenger.feature.networkstats.domain.usecase.FormatTrafficBytesUseCase
import org.telegram.messenger.feature.networkstats.domain.usecase.GetAllNetworkStatsUseCase
import org.telegram.messenger.feature.networkstats.domain.usecase.GetNetworkStatsUseCase
import org.telegram.messenger.feature.networkstats.domain.usecase.IncrementCallsTimeUseCase
import org.telegram.messenger.feature.networkstats.domain.usecase.IncrementTrafficBytesUseCase
import org.telegram.messenger.feature.networkstats.domain.usecase.IncrementTrafficItemsUseCase
import org.telegram.messenger.feature.networkstats.domain.usecase.ObserveAllNetworkStatsUseCase
import org.telegram.messenger.feature.networkstats.domain.usecase.ObserveNetworkStatsUseCase
import org.telegram.messenger.feature.networkstats.domain.usecase.RefreshNetworkStatsUseCase
import org.telegram.messenger.feature.networkstats.domain.usecase.ResetNetworkStatsUseCase
import org.telegram.messenger.feature.networkstats.presentation.NetworkStatsViewModel
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

    private var customReactionsRepository: ReactionsRepository? = null

    var reactionsRepository: ReactionsRepository
        get() = customReactionsRepository ?: LegacyReactionsRepository(account)
        set(value) {
            customReactionsRepository = value
        }

    val observeAvailableReactionsUseCase: ObserveAvailableReactionsUseCase
        get() = ObserveAvailableReactionsUseCase(reactionsRepository)

    val getAvailableReactionsUseCase: GetAvailableReactionsUseCase
        get() = GetAvailableReactionsUseCase(reactionsRepository)

    val loadAvailableReactionsUseCase: LoadAvailableReactionsUseCase
        get() = LoadAvailableReactionsUseCase(reactionsRepository)

    val observeRecentReactionsUseCase: ObserveRecentReactionsUseCase
        get() = ObserveRecentReactionsUseCase(reactionsRepository)

    val getRecentReactionsUseCase: GetRecentReactionsUseCase
        get() = GetRecentReactionsUseCase(reactionsRepository)

    val getReactionsSettingsUseCase: GetReactionsSettingsUseCase
        get() = GetReactionsSettingsUseCase(reactionsRepository)

    val getDoubleTapReactionUseCase: GetDoubleTapReactionUseCase
        get() = GetDoubleTapReactionUseCase(reactionsRepository)

    val setDoubleTapReactionUseCase: SetDoubleTapReactionUseCase
        get() = SetDoubleTapReactionUseCase(reactionsRepository)

    val sendReactionUseCase: SendReactionUseCase
        get() = SendReactionUseCase(reactionsRepository)

    val clearReactionsUseCase: ClearReactionsUseCase
        get() = ClearReactionsUseCase(reactionsRepository)

    val sendVoteUseCase: SendVoteUseCase
        get() = SendVoteUseCase(reactionsRepository)

    private var cachedReactionsViewModel: ReactionsViewModel? = null

    val reactionsViewModel: ReactionsViewModel
        get() {
            var vm = cachedReactionsViewModel
            if (vm == null) {
                vm = createReactionsViewModel()
                cachedReactionsViewModel = vm
            }
            return vm
        }

    fun createReactionsViewModel(): ReactionsViewModel {
        return ReactionsViewModel(
            observeAvailableReactionsUseCase = observeAvailableReactionsUseCase,
            getAvailableReactionsUseCase = getAvailableReactionsUseCase,
            loadAvailableReactionsUseCase = loadAvailableReactionsUseCase,
            observeRecentReactionsUseCase = observeRecentReactionsUseCase,
            getRecentReactionsUseCase = getRecentReactionsUseCase,
            getReactionsSettingsUseCase = getReactionsSettingsUseCase,
            getDoubleTapReactionUseCase = getDoubleTapReactionUseCase,
            setDoubleTapReactionUseCase = setDoubleTapReactionUseCase,
            sendReactionUseCase = sendReactionUseCase,
            clearReactionsUseCase = clearReactionsUseCase,
            sendVoteUseCase = sendVoteUseCase
        )
    }

    private var customBoostsRepository: BoostsRepository? = null

    var boostsRepository: BoostsRepository
        get() = customBoostsRepository ?: LegacyBoostsRepository(account)
        set(value) {
            customBoostsRepository = value
        }

    val getBoostsStatusUseCase: GetBoostsStatusUseCase
        get() = GetBoostsStatusUseCase(boostsRepository)

    val getMyBoostsUseCase: GetMyBoostsUseCase
        get() = GetMyBoostsUseCase(boostsRepository)

    val checkCanApplyBoostUseCase: CheckCanApplyBoostUseCase
        get() = CheckCanApplyBoostUseCase(boostsRepository)

    val applyBoostUseCase: ApplyBoostUseCase
        get() = ApplyBoostUseCase(boostsRepository)

    private var cachedBoostsViewModel: BoostsViewModel? = null

    val boostsViewModel: BoostsViewModel
        get() {
            var vm = cachedBoostsViewModel
            if (vm == null) {
                vm = createBoostsViewModel()
                cachedBoostsViewModel = vm
            }
            return vm
        }

    fun createBoostsViewModel(): BoostsViewModel {
        return BoostsViewModel(
            getBoostsStatusUseCase = getBoostsStatusUseCase,
            getMyBoostsUseCase = getMyBoostsUseCase,
            checkCanApplyBoostUseCase = checkCanApplyBoostUseCase,
            applyBoostUseCase = applyBoostUseCase
        )
    }

    private var customQuickRepliesRepository: QuickRepliesRepository? = null

    var quickRepliesRepository: QuickRepliesRepository
        get() = customQuickRepliesRepository ?: LegacyQuickRepliesRepository(account)
        set(value) {
            customQuickRepliesRepository = value
        }

    val observeQuickRepliesUseCase: ObserveQuickRepliesUseCase
        get() = ObserveQuickRepliesUseCase(quickRepliesRepository)

    val getQuickRepliesUseCase: GetQuickRepliesUseCase
        get() = GetQuickRepliesUseCase(quickRepliesRepository)

    val loadQuickRepliesUseCase: LoadQuickRepliesUseCase
        get() = LoadQuickRepliesUseCase(quickRepliesRepository)

    val findQuickReplyUseCase: FindQuickReplyUseCase
        get() = FindQuickReplyUseCase(quickRepliesRepository)

    val checkQuickReplyNameBusyUseCase: CheckQuickReplyNameBusyUseCase
        get() = CheckQuickReplyNameBusyUseCase(quickRepliesRepository)

    val canAddNewQuickReplyUseCase: CanAddNewQuickReplyUseCase
        get() = CanAddNewQuickReplyUseCase(quickRepliesRepository)

    val renameQuickReplyUseCase: RenameQuickReplyUseCase
        get() = RenameQuickReplyUseCase(quickRepliesRepository)

    val reorderQuickRepliesUseCase: ReorderQuickRepliesUseCase
        get() = ReorderQuickRepliesUseCase(quickRepliesRepository)

    val deleteQuickRepliesUseCase: DeleteQuickRepliesUseCase
        get() = DeleteQuickRepliesUseCase(quickRepliesRepository)

    val sendQuickReplyUseCase: SendQuickReplyUseCase
        get() = SendQuickReplyUseCase(quickRepliesRepository)

    private var cachedQuickRepliesViewModel: QuickRepliesViewModel? = null

    val quickRepliesViewModel: QuickRepliesViewModel
        get() {
            var vm = cachedQuickRepliesViewModel
            if (vm == null) {
                vm = createQuickRepliesViewModel()
                cachedQuickRepliesViewModel = vm
            }
            return vm
        }

    fun createQuickRepliesViewModel(): QuickRepliesViewModel {
        return QuickRepliesViewModel(
            observeQuickRepliesUseCase = observeQuickRepliesUseCase,
            loadQuickRepliesUseCase = loadQuickRepliesUseCase,
            canAddNewQuickReplyUseCase = canAddNewQuickReplyUseCase,
            renameQuickReplyUseCase = renameQuickReplyUseCase,
            reorderQuickRepliesUseCase = reorderQuickRepliesUseCase,
            deleteQuickRepliesUseCase = deleteQuickRepliesUseCase,
            sendQuickReplyUseCase = sendQuickReplyUseCase
        )
    }

    private var customJoinRequestsRepository: JoinRequestsRepository? = null

    var joinRequestsRepository: JoinRequestsRepository
        get() = customJoinRequestsRepository ?: LegacyJoinRequestsRepository(account)
        set(value) {
            customJoinRequestsRepository = value
        }

    val observePendingRequestsUseCase: ObservePendingRequestsUseCase
        get() = ObservePendingRequestsUseCase(joinRequestsRepository)

    val getPendingRequestsCountUseCase: GetPendingRequestsCountUseCase
        get() = GetPendingRequestsCountUseCase(joinRequestsRepository)

    val getCachedJoinRequestsUseCase: GetCachedJoinRequestsUseCase
        get() = GetCachedJoinRequestsUseCase(joinRequestsRepository)

    val loadJoinRequestsUseCase: LoadJoinRequestsUseCase
        get() = LoadJoinRequestsUseCase(joinRequestsRepository)

    val approveJoinRequestUseCase: ApproveJoinRequestUseCase
        get() = ApproveJoinRequestUseCase(joinRequestsRepository)

    val dismissJoinRequestUseCase: DismissJoinRequestUseCase
        get() = DismissJoinRequestUseCase(joinRequestsRepository)

    val approveAllJoinRequestsUseCase: ApproveAllJoinRequestsUseCase
        get() = ApproveAllJoinRequestsUseCase(joinRequestsRepository)

    val dismissAllJoinRequestsUseCase: DismissAllJoinRequestsUseCase
        get() = DismissAllJoinRequestsUseCase(joinRequestsRepository)

    private var cachedJoinRequestsViewModel: JoinRequestsViewModel? = null

    val joinRequestsViewModel: JoinRequestsViewModel
        get() {
            var vm = cachedJoinRequestsViewModel
            if (vm == null) {
                vm = createJoinRequestsViewModel()
                cachedJoinRequestsViewModel = vm
            }
            return vm
        }

    fun createJoinRequestsViewModel(): JoinRequestsViewModel {
        return JoinRequestsViewModel(
            observePendingRequestsUseCase = observePendingRequestsUseCase,
            getCachedJoinRequestsUseCase = getCachedJoinRequestsUseCase,
            loadJoinRequestsUseCase = loadJoinRequestsUseCase,
            approveJoinRequestUseCase = approveJoinRequestUseCase,
            dismissJoinRequestUseCase = dismissJoinRequestUseCase,
            approveAllJoinRequestsUseCase = approveAllJoinRequestsUseCase,
            dismissAllJoinRequestsUseCase = dismissAllJoinRequestsUseCase
        )
    }

    private var customFactCheckRepository: FactCheckRepository? = null

    var factCheckRepository: FactCheckRepository
        get() = customFactCheckRepository ?: LegacyFactCheckRepository(account)
        set(value) {
            customFactCheckRepository = value
        }

    val observeFactCheckLoadedUseCase: ObserveFactCheckLoadedUseCase
        get() = ObserveFactCheckLoadedUseCase(factCheckRepository)

    val getFactCheckUseCase: GetFactCheckUseCase
        get() = GetFactCheckUseCase(factCheckRepository)

    val loadFactCheckUseCase: LoadFactCheckUseCase
        get() = LoadFactCheckUseCase(factCheckRepository)

    val applyFactCheckUseCase: ApplyFactCheckUseCase
        get() = ApplyFactCheckUseCase(factCheckRepository)

    val deleteFactCheckUseCase: DeleteFactCheckUseCase
        get() = DeleteFactCheckUseCase(factCheckRepository)

    val getFactCheckLimitUseCase: GetFactCheckLimitUseCase
        get() = GetFactCheckLimitUseCase(factCheckRepository)

    private var cachedFactCheckViewModel: FactCheckViewModel? = null

    val factCheckViewModel: FactCheckViewModel
        get() {
            var vm = cachedFactCheckViewModel
            if (vm == null) {
                vm = createFactCheckViewModel()
                cachedFactCheckViewModel = vm
            }
            return vm
        }

    fun createFactCheckViewModel(): FactCheckViewModel {
        return FactCheckViewModel(
            observeFactCheckLoadedUseCase = observeFactCheckLoadedUseCase,
            getFactCheckUseCase = getFactCheckUseCase,
            loadFactCheckUseCase = loadFactCheckUseCase,
            applyFactCheckUseCase = applyFactCheckUseCase,
            deleteFactCheckUseCase = deleteFactCheckUseCase,
            getFactCheckLimitUseCase = getFactCheckLimitUseCase
        )
    }

    private var customBirthdaysRepository: BirthdaysRepository? = null

    var birthdaysRepository: BirthdaysRepository
        get() = customBirthdaysRepository ?: LegacyBirthdaysRepository(account)
        set(value) {
            customBirthdaysRepository = value
        }

    val observeBirthdaysUseCase: ObserveBirthdaysUseCase
        get() = ObserveBirthdaysUseCase(birthdaysRepository)

    val getBirthdaysStateUseCase: GetBirthdaysStateUseCase
        get() = GetBirthdaysStateUseCase(birthdaysRepository)

    val checkBirthdaysUseCase: CheckBirthdaysUseCase
        get() = CheckBirthdaysUseCase(birthdaysRepository)

    val hideTodayBirthdaysUseCase: HideTodayBirthdaysUseCase
        get() = HideTodayBirthdaysUseCase(birthdaysRepository)

    val isBirthdayTodayUseCase: IsBirthdayTodayUseCase
        get() = IsBirthdayTodayUseCase(birthdaysRepository)

    val hasBirthdaysTodayUseCase: HasBirthdaysTodayUseCase
        get() = HasBirthdaysTodayUseCase(birthdaysRepository)

    private var cachedBirthdaysViewModel: BirthdaysViewModel? = null

    val birthdaysViewModel: BirthdaysViewModel
        get() {
            var vm = cachedBirthdaysViewModel
            if (vm == null) {
                vm = createBirthdaysViewModel()
                cachedBirthdaysViewModel = vm
            }
            return vm
        }

    fun createBirthdaysViewModel(): BirthdaysViewModel {
        return BirthdaysViewModel(
            observeBirthdaysUseCase = observeBirthdaysUseCase,
            getBirthdaysStateUseCase = getBirthdaysStateUseCase,
            checkBirthdaysUseCase = checkBirthdaysUseCase,
            hideTodayBirthdaysUseCase = hideTodayBirthdaysUseCase,
            isBirthdayTodayUseCase = isBirthdayTodayUseCase,
            hasBirthdaysTodayUseCase = hasBirthdaysTodayUseCase
        )
    }

    private var customChatThemeRepository: ChatThemeRepository? = null

    var chatThemeRepository: ChatThemeRepository
        get() = customChatThemeRepository ?: LegacyChatThemeRepository(account)
        set(value) {
            customChatThemeRepository = value
        }

    val observeDialogThemeUseCase: ObserveDialogThemeUseCase
        get() = ObserveDialogThemeUseCase(chatThemeRepository)

    val getDialogThemeStateUseCase: GetDialogThemeStateUseCase
        get() = GetDialogThemeStateUseCase(chatThemeRepository)

    val getAvailableChatThemesUseCase: GetAvailableChatThemesUseCase
        get() = GetAvailableChatThemesUseCase(chatThemeRepository)

    val setDialogThemeUseCase: SetDialogThemeUseCase
        get() = SetDialogThemeUseCase(chatThemeRepository)

    val resetDialogThemeUseCase: ResetDialogThemeUseCase
        get() = ResetDialogThemeUseCase(chatThemeRepository)

    val saveChatWallpaperUseCase: SaveChatWallpaperUseCase
        get() = SaveChatWallpaperUseCase(chatThemeRepository)

    private var cachedChatThemeViewModel: ChatThemeViewModel? = null

    val chatThemeViewModel: ChatThemeViewModel
        get() {
            var vm = cachedChatThemeViewModel
            if (vm == null) {
                vm = createChatThemeViewModel()
                cachedChatThemeViewModel = vm
            }
            return vm
        }

    fun createChatThemeViewModel(): ChatThemeViewModel {
        return ChatThemeViewModel(
            observeDialogThemeUseCase = observeDialogThemeUseCase,
            getDialogThemeStateUseCase = getDialogThemeStateUseCase,
            getAvailableThemesUseCase = getAvailableChatThemesUseCase,
            setDialogThemeUseCase = setDialogThemeUseCase,
            resetDialogThemeUseCase = resetDialogThemeUseCase,
            saveChatWallpaperUseCase = saveChatWallpaperUseCase
        )
    }

    private var customPasskeysRepository: PasskeysRepository? = null

    var passkeysRepository: PasskeysRepository
        get() = customPasskeysRepository ?: LegacyPasskeysRepository(account)
        set(value) {
            customPasskeysRepository = value
        }

    val observePasskeysUseCase: ObservePasskeysUseCase
        get() = ObservePasskeysUseCase(passkeysRepository)

    val getPasskeysUseCase: GetPasskeysUseCase
        get() = GetPasskeysUseCase(passkeysRepository)

    val deletePasskeyUseCase: DeletePasskeyUseCase
        get() = DeletePasskeyUseCase(passkeysRepository)

    val checkCanAddPasskeyUseCase: CheckCanAddPasskeyUseCase
        get() = CheckCanAddPasskeyUseCase(passkeysRepository)

    val isPasskeysSupportedUseCase: IsPasskeysSupportedUseCase
        get() = IsPasskeysSupportedUseCase(passkeysRepository)

    private var cachedPasskeysViewModel: PasskeysViewModel? = null

    val passkeysViewModel: PasskeysViewModel
        get() {
            var vm = cachedPasskeysViewModel
            if (vm == null) {
                vm = createPasskeysViewModel()
                cachedPasskeysViewModel = vm
            }
            return vm
        }

    fun createPasskeysViewModel(): PasskeysViewModel {
        return PasskeysViewModel(
            observePasskeysUseCase = observePasskeysUseCase,
            getPasskeysUseCase = getPasskeysUseCase,
            deletePasskeyUseCase = deletePasskeyUseCase,
            checkCanAddPasskeyUseCase = checkCanAddPasskeyUseCase,
            isPasskeysSupportedUseCase = isPasskeysSupportedUseCase
        )
    }

    private var customProxyRepository: ProxyRepository? = null

    var proxyRepository: ProxyRepository
        get() = customProxyRepository ?: LegacyProxyRepository(account)
        set(value) {
            customProxyRepository = value
        }

    val observeProxySettingsUseCase: ObserveProxySettingsUseCase
        get() = ObserveProxySettingsUseCase(proxyRepository)

    val getProxySettingsUseCase: GetProxySettingsUseCase
        get() = GetProxySettingsUseCase(proxyRepository)

    val addProxyUseCase: AddProxyUseCase
        get() = AddProxyUseCase(proxyRepository)

    val deleteProxyUseCase: DeleteProxyUseCase
        get() = DeleteProxyUseCase(proxyRepository)

    val enableProxyUseCase: EnableProxyUseCase
        get() = EnableProxyUseCase(proxyRepository)

    val disableProxyUseCase: DisableProxyUseCase
        get() = DisableProxyUseCase(proxyRepository)

    val toggleProxyRotationUseCase: ToggleProxyRotationUseCase
        get() = ToggleProxyRotationUseCase(proxyRepository)

    val checkProxyPingUseCase: CheckProxyPingUseCase
        get() = CheckProxyPingUseCase(proxyRepository)

    private var cachedProxyViewModel: ProxyViewModel? = null

    val proxyViewModel: ProxyViewModel
        get() {
            var vm = cachedProxyViewModel
            if (vm == null) {
                vm = createProxyViewModel()
                cachedProxyViewModel = vm
            }
            return vm
        }

    fun createProxyViewModel(): ProxyViewModel {
        return ProxyViewModel(
            observeProxySettingsUseCase = observeProxySettingsUseCase,
            getProxySettingsUseCase = getProxySettingsUseCase,
            addProxyUseCase = addProxyUseCase,
            deleteProxyUseCase = deleteProxyUseCase,
            enableProxyUseCase = enableProxyUseCase,
            disableProxyUseCase = disableProxyUseCase,
            toggleProxyRotationUseCase = toggleProxyRotationUseCase,
            checkProxyPingUseCase = checkProxyPingUseCase
        )
    }

    private var customAutoDeleteRepository: AutoDeleteRepository? = null

    var autoDeleteRepository: AutoDeleteRepository
        get() = customAutoDeleteRepository ?: LegacyAutoDeleteRepository(account)
        set(value) {
            customAutoDeleteRepository = value
        }

    val observeGlobalAutoDeleteUseCase: ObserveGlobalAutoDeleteUseCase
        get() = ObserveGlobalAutoDeleteUseCase(autoDeleteRepository)

    val getGlobalAutoDeleteUseCase: GetGlobalAutoDeleteUseCase
        get() = GetGlobalAutoDeleteUseCase(autoDeleteRepository)

    val setGlobalAutoDeleteUseCase: SetGlobalAutoDeleteUseCase
        get() = SetGlobalAutoDeleteUseCase(autoDeleteRepository)

    val getChatAutoDeleteUseCase: GetChatAutoDeleteUseCase
        get() = GetChatAutoDeleteUseCase(autoDeleteRepository)

    val setChatAutoDeleteUseCase: SetChatAutoDeleteUseCase
        get() = SetChatAutoDeleteUseCase(autoDeleteRepository)

    val setChatsAutoDeleteBatchUseCase: SetChatsAutoDeleteBatchUseCase
        get() = SetChatsAutoDeleteBatchUseCase(autoDeleteRepository)

    private var cachedAutoDeleteViewModel: AutoDeleteViewModel? = null

    val autoDeleteViewModel: AutoDeleteViewModel
        get() {
            var vm = cachedAutoDeleteViewModel
            if (vm == null) {
                vm = createAutoDeleteViewModel()
                cachedAutoDeleteViewModel = vm
            }
            return vm
        }

    fun createAutoDeleteViewModel(): AutoDeleteViewModel {
        return AutoDeleteViewModel(
            observeGlobalAutoDeleteUseCase = observeGlobalAutoDeleteUseCase,
            getGlobalAutoDeleteUseCase = getGlobalAutoDeleteUseCase,
            setGlobalAutoDeleteUseCase = setGlobalAutoDeleteUseCase,
            getChatAutoDeleteUseCase = getChatAutoDeleteUseCase,
            setChatAutoDeleteUseCase = setChatAutoDeleteUseCase,
            setChatsAutoDeleteBatchUseCase = setChatsAutoDeleteBatchUseCase
        )
    }

    private var customUnconfirmedAuthRepository: UnconfirmedAuthRepository? = null

    var unconfirmedAuthRepository: UnconfirmedAuthRepository
        get() = customUnconfirmedAuthRepository ?: LegacyUnconfirmedAuthRepository(account)
        set(value) {
            customUnconfirmedAuthRepository = value
        }

    val observeUnconfirmedAuthsUseCase: ObserveUnconfirmedAuthsUseCase
        get() = ObserveUnconfirmedAuthsUseCase(unconfirmedAuthRepository)

    val getUnconfirmedAuthsUseCase: GetUnconfirmedAuthsUseCase
        get() = GetUnconfirmedAuthsUseCase(unconfirmedAuthRepository)

    val confirmAuthUseCase: ConfirmAuthUseCase
        get() = ConfirmAuthUseCase(unconfirmedAuthRepository)

    val denyAuthUseCase: DenyAuthUseCase
        get() = DenyAuthUseCase(unconfirmedAuthRepository)

    val confirmAllAuthsUseCase: ConfirmAllAuthsUseCase
        get() = ConfirmAllAuthsUseCase(unconfirmedAuthRepository)

    val denyAllAuthsUseCase: DenyAllAuthsUseCase
        get() = DenyAllAuthsUseCase(unconfirmedAuthRepository)

    val clearUnconfirmedAuthsUseCase: ClearUnconfirmedAuthsUseCase
        get() = ClearUnconfirmedAuthsUseCase(unconfirmedAuthRepository)

    private var cachedUnconfirmedAuthViewModel: UnconfirmedAuthViewModel? = null

    val unconfirmedAuthViewModel: UnconfirmedAuthViewModel
        get() {
            var vm = cachedUnconfirmedAuthViewModel
            if (vm == null) {
                vm = createUnconfirmedAuthViewModel()
                cachedUnconfirmedAuthViewModel = vm
            }
            return vm
        }

    fun createUnconfirmedAuthViewModel(): UnconfirmedAuthViewModel {
        return UnconfirmedAuthViewModel(
            observeUnconfirmedAuthsUseCase = observeUnconfirmedAuthsUseCase,
            getUnconfirmedAuthsUseCase = getUnconfirmedAuthsUseCase,
            confirmAuthUseCase = confirmAuthUseCase,
            denyAuthUseCase = denyAuthUseCase,
            confirmAllAuthsUseCase = confirmAllAuthsUseCase,
            denyAllAuthsUseCase = denyAllAuthsUseCase,
            clearUnconfirmedAuthsUseCase = clearUnconfirmedAuthsUseCase
        )
    }

    private var customStarGiftsRepository: StarGiftsRepository? = null

    var starGiftsRepository: StarGiftsRepository
        get() = customStarGiftsRepository ?: LegacyStarGiftsRepository(account)
        set(value) {
            customStarGiftsRepository = value
        }

    val observeStarGiftsCatalogUseCase: ObserveStarGiftsCatalogUseCase
        get() = ObserveStarGiftsCatalogUseCase(starGiftsRepository)

    val getStarGiftsCatalogUseCase: GetStarGiftsCatalogUseCase
        get() = GetStarGiftsCatalogUseCase(starGiftsRepository)

    val getStarGiftByIdUseCase: GetStarGiftByIdUseCase
        get() = GetStarGiftByIdUseCase(starGiftsRepository)

    val observeProfileGiftsUseCase: ObserveProfileGiftsUseCase
        get() = ObserveProfileGiftsUseCase(starGiftsRepository)

    val loadProfileGiftsUseCase: LoadProfileGiftsUseCase
        get() = LoadProfileGiftsUseCase(starGiftsRepository)

    val togglePinProfileGiftUseCase: TogglePinProfileGiftUseCase
        get() = TogglePinProfileGiftUseCase(starGiftsRepository)

    val toggleHideProfileGiftUseCase: ToggleHideProfileGiftUseCase
        get() = ToggleHideProfileGiftUseCase(starGiftsRepository)

    private var cachedStarGiftsViewModel: StarGiftsViewModel? = null

    val starGiftsViewModel: StarGiftsViewModel
        get() {
            var vm = cachedStarGiftsViewModel
            if (vm == null) {
                vm = createStarGiftsViewModel()
                cachedStarGiftsViewModel = vm
            }
            return vm
        }

    fun createStarGiftsViewModel(): StarGiftsViewModel {
        return StarGiftsViewModel(
            observeStarGiftsCatalogUseCase = observeStarGiftsCatalogUseCase,
            getStarGiftsCatalogUseCase = getStarGiftsCatalogUseCase,
            getStarGiftByIdUseCase = getStarGiftByIdUseCase,
            observeProfileGiftsUseCase = observeProfileGiftsUseCase,
            loadProfileGiftsUseCase = loadProfileGiftsUseCase,
            togglePinProfileGiftUseCase = togglePinProfileGiftUseCase,
            toggleHideProfileGiftUseCase = toggleHideProfileGiftUseCase
        )
    }

    private var customAiTonesRepository: AiTonesRepository? = null

    var aiTonesRepository: AiTonesRepository
        get() = customAiTonesRepository ?: LegacyAiTonesRepository(account)
        set(value) {
            customAiTonesRepository = value
        }

    val observeAiTonesUseCase: ObserveAiTonesUseCase
        get() = ObserveAiTonesUseCase(aiTonesRepository)

    val getAiTonesStateUseCase: GetAiTonesStateUseCase
        get() = GetAiTonesStateUseCase(aiTonesRepository)

    val loadAiTonesUseCase: LoadAiTonesUseCase
        get() = LoadAiTonesUseCase(aiTonesRepository)

    val addAiToneUseCase: AddAiToneUseCase
        get() = AddAiToneUseCase(aiTonesRepository)

    val removeAiToneUseCase: RemoveAiToneUseCase
        get() = RemoveAiToneUseCase(aiTonesRepository)

    val unsaveAiToneUseCase: UnsaveAiToneUseCase
        get() = UnsaveAiToneUseCase(aiTonesRepository)

    val editAiToneUseCase: EditAiToneUseCase
        get() = EditAiToneUseCase(aiTonesRepository)

    private var cachedAiTonesViewModel: AiTonesViewModel? = null

    val aiTonesViewModel: AiTonesViewModel
        get() {
            var vm = cachedAiTonesViewModel
            if (vm == null) {
                vm = createAiTonesViewModel()
                cachedAiTonesViewModel = vm
            }
            return vm
        }

    fun createAiTonesViewModel(): AiTonesViewModel {
        return AiTonesViewModel(
            observeAiTonesUseCase = observeAiTonesUseCase,
            getAiTonesStateUseCase = getAiTonesStateUseCase,
            loadAiTonesUseCase = loadAiTonesUseCase,
            addAiToneUseCase = addAiToneUseCase,
            removeAiToneUseCase = removeAiToneUseCase,
            unsaveAiToneUseCase = unsaveAiToneUseCase,
            editAiToneUseCase = editAiToneUseCase
        )
    }

    private var customCaptchaRepository: CaptchaRepository? = null

    var captchaRepository: CaptchaRepository
        get() = customCaptchaRepository ?: LegacyCaptchaRepository(account)
        set(value) {
            customCaptchaRepository = value
        }

    val observeActiveCaptchaRequestsUseCase: ObserveActiveCaptchaRequestsUseCase
        get() = ObserveActiveCaptchaRequestsUseCase(captchaRepository)

    val getActiveCaptchaRequestsUseCase: GetActiveCaptchaRequestsUseCase
        get() = GetActiveCaptchaRequestsUseCase(captchaRepository)

    val verifyCaptchaUseCase: VerifyCaptchaUseCase
        get() = VerifyCaptchaUseCase(captchaRepository)

    val submitCaptchaResultUseCase: SubmitCaptchaResultUseCase
        get() = SubmitCaptchaResultUseCase(captchaRepository)

    val cancelCaptchaUseCase: CancelCaptchaUseCase
        get() = CancelCaptchaUseCase(captchaRepository)

    private var cachedCaptchaViewModel: CaptchaViewModel? = null

    val captchaViewModel: CaptchaViewModel
        get() {
            var vm = cachedCaptchaViewModel
            if (vm == null) {
                vm = createCaptchaViewModel()
                cachedCaptchaViewModel = vm
            }
            return vm
        }

    fun createCaptchaViewModel(): CaptchaViewModel {
        return CaptchaViewModel(
            observeActiveCaptchaRequestsUseCase = observeActiveCaptchaRequestsUseCase,
            getActiveCaptchaRequestsUseCase = getActiveCaptchaRequestsUseCase,
            verifyCaptchaUseCase = verifyCaptchaUseCase,
            submitCaptchaResultUseCase = submitCaptchaResultUseCase,
            cancelCaptchaUseCase = cancelCaptchaUseCase
        )
    }

    private var customHashtagSearchRepository: HashtagSearchRepository? = null

    var hashtagSearchRepository: HashtagSearchRepository
        get() = customHashtagSearchRepository ?: LegacyHashtagSearchRepository(account)
        set(value) {
            customHashtagSearchRepository = value
        }

    val observeHashtagHistoryUseCase: ObserveHashtagHistoryUseCase
        get() = ObserveHashtagHistoryUseCase(hashtagSearchRepository)

    val getHashtagHistoryUseCase: GetHashtagHistoryUseCase
        get() = GetHashtagHistoryUseCase(hashtagSearchRepository)

    val addHashtagToHistoryUseCase: AddHashtagToHistoryUseCase
        get() = AddHashtagToHistoryUseCase(hashtagSearchRepository)

    val removeHashtagFromHistoryUseCase: RemoveHashtagFromHistoryUseCase
        get() = RemoveHashtagFromHistoryUseCase(hashtagSearchRepository)

    val clearHashtagHistoryUseCase: ClearHashtagHistoryUseCase
        get() = ClearHashtagHistoryUseCase(hashtagSearchRepository)

    val observeHashtagSearchResultUseCase: ObserveHashtagSearchResultUseCase
        get() = ObserveHashtagSearchResultUseCase(hashtagSearchRepository)

    val searchHashtagUseCase: SearchHashtagUseCase
        get() = SearchHashtagUseCase(hashtagSearchRepository)

    val jumpToHashtagMessageUseCase: JumpToHashtagMessageUseCase
        get() = JumpToHashtagMessageUseCase(hashtagSearchRepository)

    val clearHashtagSearchResultsUseCase: ClearHashtagSearchResultsUseCase
        get() = ClearHashtagSearchResultsUseCase(hashtagSearchRepository)

    private var cachedHashtagSearchViewModel: HashtagSearchViewModel? = null

    val hashtagSearchViewModel: HashtagSearchViewModel
        get() {
            var vm = cachedHashtagSearchViewModel
            if (vm == null) {
                vm = createHashtagSearchViewModel()
                cachedHashtagSearchViewModel = vm
            }
            return vm
        }

    fun createHashtagSearchViewModel(): HashtagSearchViewModel {
        return HashtagSearchViewModel(
            observeHashtagHistoryUseCase = observeHashtagHistoryUseCase,
            getHashtagHistoryUseCase = getHashtagHistoryUseCase,
            addHashtagToHistoryUseCase = addHashtagToHistoryUseCase,
            removeHashtagFromHistoryUseCase = removeHashtagFromHistoryUseCase,
            clearHashtagHistoryUseCase = clearHashtagHistoryUseCase,
            observeHashtagSearchResultUseCase = observeHashtagSearchResultUseCase,
            searchHashtagUseCase = searchHashtagUseCase,
            jumpToHashtagMessageUseCase = jumpToHashtagMessageUseCase,
            clearHashtagSearchResultsUseCase = clearHashtagSearchResultsUseCase
        )
    }

    private var customBiometricsRepository: BiometricsRepository? = null

    var biometricsRepository: BiometricsRepository
        get() = customBiometricsRepository ?: LegacyBiometricsRepository()
        set(value) {
            customBiometricsRepository = value
        }

    val observeBiometricKeyStateUseCase: ObserveBiometricKeyStateUseCase
        get() = ObserveBiometricKeyStateUseCase(biometricsRepository)

    val getBiometricKeyStateUseCase: GetBiometricKeyStateUseCase
        get() = GetBiometricKeyStateUseCase(biometricsRepository)

    val checkBiometricKeyReadyUseCase: CheckBiometricKeyReadyUseCase
        get() = CheckBiometricKeyReadyUseCase(biometricsRepository)

    val deleteInvalidBiometricKeyUseCase: DeleteInvalidBiometricKeyUseCase
        get() = DeleteInvalidBiometricKeyUseCase(biometricsRepository)

    val isBiometricKeyReadyUseCase: IsBiometricKeyReadyUseCase
        get() = IsBiometricKeyReadyUseCase(biometricsRepository)

    val hasDeviceBiometricsChangedUseCase: HasDeviceBiometricsChangedUseCase
        get() = HasDeviceBiometricsChangedUseCase(biometricsRepository)

    private var cachedBiometricsViewModel: BiometricsViewModel? = null

    val biometricsViewModel: BiometricsViewModel
        get() {
            var vm = cachedBiometricsViewModel
            if (vm == null) {
                vm = createBiometricsViewModel()
                cachedBiometricsViewModel = vm
            }
            return vm
        }

    fun createBiometricsViewModel(): BiometricsViewModel {
        return BiometricsViewModel(
            observeBiometricKeyStateUseCase = observeBiometricKeyStateUseCase,
            getBiometricKeyStateUseCase = getBiometricKeyStateUseCase,
            checkBiometricKeyReadyUseCase = checkBiometricKeyReadyUseCase,
            deleteInvalidBiometricKeyUseCase = deleteInvalidBiometricKeyUseCase,
            isBiometricKeyReadyUseCase = isBiometricKeyReadyUseCase,
            hasDeviceBiometricsChangedUseCase = hasDeviceBiometricsChangedUseCase
        )
    }

    private var customGiftAuctionsRepository: GiftAuctionsRepository? = null

    var giftAuctionsRepository: GiftAuctionsRepository
        get() = customGiftAuctionsRepository ?: LegacyGiftAuctionsRepository(account)
        set(value) {
            customGiftAuctionsRepository = value
        }

    val observeActiveAuctionsUseCase: ObserveActiveAuctionsUseCase
        get() = ObserveActiveAuctionsUseCase(giftAuctionsRepository)

    val observeAuctionUseCase: ObserveAuctionUseCase
        get() = ObserveAuctionUseCase(giftAuctionsRepository)

    val getActiveAuctionsUseCase: GetActiveAuctionsUseCase
        get() = GetActiveAuctionsUseCase(giftAuctionsRepository)

    val getAuctionByIdUseCase: GetAuctionByIdUseCase
        get() = GetAuctionByIdUseCase(giftAuctionsRepository)

    val getAuctionBySlugUseCase: GetAuctionBySlugUseCase
        get() = GetAuctionBySlugUseCase(giftAuctionsRepository)

    val sendAuctionBidUseCase: SendAuctionBidUseCase
        get() = SendAuctionBidUseCase(giftAuctionsRepository)

    val loadAuctionAcquiredGiftsUseCase: LoadAuctionAcquiredGiftsUseCase
        get() = LoadAuctionAcquiredGiftsUseCase(giftAuctionsRepository)

    val refreshActiveAuctionsUseCase: RefreshActiveAuctionsUseCase
        get() = RefreshActiveAuctionsUseCase(giftAuctionsRepository)

    private var cachedGiftAuctionsViewModel: GiftAuctionsViewModel? = null

    val giftAuctionsViewModel: GiftAuctionsViewModel
        get() {
            var vm = cachedGiftAuctionsViewModel
            if (vm == null) {
                vm = createGiftAuctionsViewModel()
                cachedGiftAuctionsViewModel = vm
            }
            return vm
        }

    fun createGiftAuctionsViewModel(): GiftAuctionsViewModel {
        return GiftAuctionsViewModel(
            observeActiveAuctionsUseCase = observeActiveAuctionsUseCase,
            observeAuctionUseCase = observeAuctionUseCase,
            getActiveAuctionsUseCase = getActiveAuctionsUseCase,
            getAuctionByIdUseCase = getAuctionByIdUseCase,
            getAuctionBySlugUseCase = getAuctionBySlugUseCase,
            sendAuctionBidUseCase = sendAuctionBidUseCase,
            loadAuctionAcquiredGiftsUseCase = loadAuctionAcquiredGiftsUseCase,
            refreshActiveAuctionsUseCase = refreshActiveAuctionsUseCase
        )
    }

    private var customBusinessLinksRepository: BusinessLinksRepository? = null

    var businessLinksRepository: BusinessLinksRepository
        get() = customBusinessLinksRepository ?: LegacyBusinessLinksRepository(account)
        set(value) {
            customBusinessLinksRepository = value
        }

    val observeBusinessLinksUseCase: ObserveBusinessLinksUseCase
        get() = ObserveBusinessLinksUseCase(businessLinksRepository)

    val getBusinessLinksUseCase: GetBusinessLinksUseCase
        get() = GetBusinessLinksUseCase(businessLinksRepository)

    val loadBusinessLinksUseCase: LoadBusinessLinksUseCase
        get() = LoadBusinessLinksUseCase(businessLinksRepository)

    val createBusinessLinkUseCase: CreateBusinessLinkUseCase
        get() = CreateBusinessLinkUseCase(businessLinksRepository)

    val editBusinessLinkUseCase: EditBusinessLinkUseCase
        get() = EditBusinessLinkUseCase(businessLinksRepository)

    val deleteBusinessLinkUseCase: DeleteBusinessLinkUseCase
        get() = DeleteBusinessLinkUseCase(businessLinksRepository)

    val findBusinessLinkUseCase: FindBusinessLinkUseCase
        get() = FindBusinessLinkUseCase(businessLinksRepository)

    val canAddNewBusinessLinkUseCase: CanAddNewBusinessLinkUseCase
        get() = CanAddNewBusinessLinkUseCase(businessLinksRepository)

    private var cachedBusinessLinksViewModel: BusinessLinksViewModel? = null

    val businessLinksViewModel: BusinessLinksViewModel
        get() {
            var vm = cachedBusinessLinksViewModel
            if (vm == null) {
                vm = createBusinessLinksViewModel()
                cachedBusinessLinksViewModel = vm
            }
            return vm
        }

    fun createBusinessLinksViewModel(): BusinessLinksViewModel {
        return BusinessLinksViewModel(
            observeBusinessLinksUseCase = observeBusinessLinksUseCase,
            loadBusinessLinksUseCase = loadBusinessLinksUseCase,
            createBusinessLinkUseCase = createBusinessLinkUseCase,
            editBusinessLinkUseCase = editBusinessLinkUseCase,
            deleteBusinessLinkUseCase = deleteBusinessLinkUseCase,
            canAddNewBusinessLinkUseCase = canAddNewBusinessLinkUseCase
        )
    }

    private var customBusinessBotsRepository: BusinessBotsRepository? = null

    var businessBotsRepository: BusinessBotsRepository
        get() = customBusinessBotsRepository ?: LegacyBusinessBotsRepository(account)
        set(value) {
            customBusinessBotsRepository = value
        }

    val observeConnectedBotsUseCase: ObserveConnectedBotsUseCase
        get() = ObserveConnectedBotsUseCase(businessBotsRepository)

    val getConnectedBotsUseCase: GetConnectedBotsUseCase
        get() = GetConnectedBotsUseCase(businessBotsRepository)

    val loadConnectedBotsUseCase: LoadConnectedBotsUseCase
        get() = LoadConnectedBotsUseCase(businessBotsRepository)

    val updateConnectedBotUseCase: UpdateConnectedBotUseCase
        get() = UpdateConnectedBotUseCase(businessBotsRepository)

    val deleteConnectedBotUseCase: DeleteConnectedBotUseCase
        get() = DeleteConnectedBotUseCase(businessBotsRepository)

    val findConnectedBotUseCase: FindConnectedBotUseCase
        get() = FindConnectedBotUseCase(businessBotsRepository)

    private var cachedBusinessBotsViewModel: BusinessBotsViewModel? = null

    val businessBotsViewModel: BusinessBotsViewModel
        get() {
            var vm = cachedBusinessBotsViewModel
            if (vm == null) {
                vm = createBusinessBotsViewModel()
                cachedBusinessBotsViewModel = vm
            }
            return vm
        }

    fun createBusinessBotsViewModel(): BusinessBotsViewModel {
        return BusinessBotsViewModel(
            observeConnectedBotsUseCase = observeConnectedBotsUseCase,
            loadConnectedBotsUseCase = loadConnectedBotsUseCase,
            updateConnectedBotUseCase = updateConnectedBotUseCase,
            deleteConnectedBotUseCase = deleteConnectedBotUseCase
        )
    }

    // --- Timezones ---
    private var customTimezonesRepository: TimezonesRepository? = null

    var timezonesRepository: TimezonesRepository
        get() = customTimezonesRepository ?: LegacyTimezonesRepository(account)
        set(value) {
            customTimezonesRepository = value
        }

    val observeTimezonesUseCase: ObserveTimezonesUseCase
        get() = ObserveTimezonesUseCase(timezonesRepository)

    val getTimezonesUseCase: GetTimezonesUseCase
        get() = GetTimezonesUseCase(timezonesRepository)

    val loadTimezonesUseCase: LoadTimezonesUseCase
        get() = LoadTimezonesUseCase(timezonesRepository)

    val findTimezoneUseCase: FindTimezoneUseCase
        get() = FindTimezoneUseCase(timezonesRepository)

    val getSystemTimezoneIdUseCase: GetSystemTimezoneIdUseCase
        get() = GetSystemTimezoneIdUseCase(timezonesRepository)

    val getTimezoneNameUseCase: GetTimezoneNameUseCase
        get() = GetTimezoneNameUseCase(timezonesRepository)

    private var cachedTimezonesViewModel: TimezonesViewModel? = null

    val timezonesViewModel: TimezonesViewModel
        get() {
            var vm = cachedTimezonesViewModel
            if (vm == null) {
                vm = createTimezonesViewModel()
                cachedTimezonesViewModel = vm
            }
            return vm
        }

    fun createTimezonesViewModel(): TimezonesViewModel {
        return TimezonesViewModel(
            observeTimezonesUseCase = observeTimezonesUseCase,
            getTimezonesUseCase = getTimezonesUseCase,
            loadTimezonesUseCase = loadTimezonesUseCase,
            findTimezoneUseCase = findTimezoneUseCase,
            getSystemTimezoneIdUseCase = getSystemTimezoneIdUseCase,
            getTimezoneNameUseCase = getTimezoneNameUseCase
        )
    }

    // --- Bot Stars ---
    private var customBotStarsRepository: BotStarsRepository? = null

    var botStarsRepository: BotStarsRepository
        get() = customBotStarsRepository ?: LegacyBotStarsRepository(account)
        set(value) {
            customBotStarsRepository = value
        }

    val observeBotStarsStatsUseCase: ObserveBotStarsStatsUseCase
        get() = ObserveBotStarsStatsUseCase(botStarsRepository)

    val getBotStarsStatsUseCase: GetBotStarsStatsUseCase
        get() = GetBotStarsStatsUseCase(botStarsRepository)

    val observeTonStatsUseCase: ObserveTonStatsUseCase
        get() = ObserveTonStatsUseCase(botStarsRepository)

    val getTonStatsUseCase: GetTonStatsUseCase
        get() = GetTonStatsUseCase(botStarsRepository)

    val observeBotTransactionsUseCase: ObserveBotTransactionsUseCase
        get() = ObserveBotTransactionsUseCase(botStarsRepository)

    val loadBotTransactionsUseCase: LoadBotTransactionsUseCase
        get() = LoadBotTransactionsUseCase(botStarsRepository)

    val observeConnectedStarBotsUseCase: ObserveConnectedStarBotsUseCase
        get() = ObserveConnectedStarBotsUseCase(botStarsRepository)

    val loadConnectedStarBotsUseCase: LoadConnectedStarBotsUseCase
        get() = LoadConnectedStarBotsUseCase(botStarsRepository)

    val loadSuggestedStarBotsUseCase: LoadSuggestedStarBotsUseCase
        get() = LoadSuggestedStarBotsUseCase(botStarsRepository)

    val getAdminedBotsAndChannelsUseCase: GetAdminedBotsAndChannelsUseCase
        get() = GetAdminedBotsAndChannelsUseCase(botStarsRepository)

    private var cachedBotStarsViewModel: BotStarsViewModel? = null

    val botStarsViewModel: BotStarsViewModel
        get() {
            var vm = cachedBotStarsViewModel
            if (vm == null) {
                vm = createBotStarsViewModel()
                cachedBotStarsViewModel = vm
            }
            return vm
        }

    fun createBotStarsViewModel(): BotStarsViewModel {
        return BotStarsViewModel(
            observeBotStarsStatsUseCase = observeBotStarsStatsUseCase,
            getBotStarsStatsUseCase = getBotStarsStatsUseCase,
            observeTonStatsUseCase = observeTonStatsUseCase,
            getTonStatsUseCase = getTonStatsUseCase,
            observeBotTransactionsUseCase = observeBotTransactionsUseCase,
            loadBotTransactionsUseCase = loadBotTransactionsUseCase,
            observeConnectedStarBotsUseCase = observeConnectedStarBotsUseCase,
            loadConnectedStarBotsUseCase = loadConnectedStarBotsUseCase,
            loadSuggestedStarBotsUseCase = loadSuggestedStarBotsUseCase,
            getAdminedBotsAndChannelsUseCase = getAdminedBotsAndChannelsUseCase
        )
    }

    private var customBillingRepository: BillingRepository? = null

    var billingRepository: BillingRepository
        get() = customBillingRepository ?: LegacyBillingRepository()
        set(value) {
            customBillingRepository = value
        }

    val observeBillingStateUseCase: ObserveBillingStateUseCase
        get() = ObserveBillingStateUseCase(billingRepository)

    val getBillingStateUseCase: GetBillingStateUseCase
        get() = GetBillingStateUseCase(billingRepository)

    val startBillingConnectionUseCase: StartBillingConnectionUseCase
        get() = StartBillingConnectionUseCase(billingRepository)

    val getPremiumProductUseCase: GetPremiumProductUseCase
        get() = GetPremiumProductUseCase(billingRepository)

    val formatCurrencyUseCase: FormatCurrencyUseCase
        get() = FormatCurrencyUseCase(billingRepository)

    val getCurrencyExpUseCase: GetCurrencyExpUseCase
        get() = GetCurrencyExpUseCase(billingRepository)

    val queryBillingPurchasesUseCase: QueryBillingPurchasesUseCase
        get() = QueryBillingPurchasesUseCase(billingRepository)

    val manageSubscriptionUseCase: ManageSubscriptionUseCase
        get() = ManageSubscriptionUseCase(billingRepository)

    private var cachedBillingViewModel: BillingViewModel? = null

    val billingViewModel: BillingViewModel
        get() {
            var vm = cachedBillingViewModel
            if (vm == null) {
                vm = createBillingViewModel()
                cachedBillingViewModel = vm
            }
            return vm
        }

    fun createBillingViewModel(): BillingViewModel {
        return BillingViewModel(
            observeBillingStateUseCase = observeBillingStateUseCase,
            getBillingStateUseCase = getBillingStateUseCase,
            startBillingConnectionUseCase = startBillingConnectionUseCase,
            queryBillingPurchasesUseCase = queryBillingPurchasesUseCase,
            manageSubscriptionUseCase = manageSubscriptionUseCase
        )
    }

    private var customLauncherIconRepository: LauncherIconRepository? = null

    var launcherIconRepository: LauncherIconRepository
        get() = customLauncherIconRepository ?: LegacyLauncherIconRepository()
        set(value) {
            customLauncherIconRepository = value
        }

    val observeLauncherIconsUseCase: ObserveLauncherIconsUseCase
        get() = ObserveLauncherIconsUseCase(launcherIconRepository)

    val getLauncherIconsUseCase: GetLauncherIconsUseCase
        get() = GetLauncherIconsUseCase(launcherIconRepository)

    val getActiveLauncherIconUseCase: GetActiveLauncherIconUseCase
        get() = GetActiveLauncherIconUseCase(launcherIconRepository)

    val isLauncherIconEnabledUseCase: IsLauncherIconEnabledUseCase
        get() = IsLauncherIconEnabledUseCase(launcherIconRepository)

    val setLauncherIconUseCase: SetLauncherIconUseCase
        get() = SetLauncherIconUseCase(launcherIconRepository)

    val fixLauncherIconIfNeededUseCase: FixLauncherIconIfNeededUseCase
        get() = FixLauncherIconIfNeededUseCase(launcherIconRepository)

    private var cachedLauncherIconViewModel: LauncherIconViewModel? = null

    val launcherIconViewModel: LauncherIconViewModel
        get() {
            var vm = cachedLauncherIconViewModel
            if (vm == null) {
                vm = createLauncherIconViewModel()
                cachedLauncherIconViewModel = vm
            }
            return vm
        }

    fun createLauncherIconViewModel(): LauncherIconViewModel {
        return LauncherIconViewModel(
            observeLauncherIconsUseCase = observeLauncherIconsUseCase,
            getLauncherIconsUseCase = getLauncherIconsUseCase,
            getActiveLauncherIconUseCase = getActiveLauncherIconUseCase,
            setLauncherIconUseCase = setLauncherIconUseCase,
            fixLauncherIconIfNeededUseCase = fixLauncherIconIfNeededUseCase
        )
    }

    private var customPushRepository: PushRepository? = null

    var pushRepository: PushRepository
        get() = customPushRepository ?: LegacyPushRepository(account)
        set(value) {
            customPushRepository = value
        }

    val observePushStatusUseCase: ObservePushStatusUseCase
        get() = ObservePushStatusUseCase(pushRepository)

    val getPushStatusUseCase: GetPushStatusUseCase
        get() = GetPushStatusUseCase(pushRepository)

    val isPushAvailableUseCase: IsPushAvailableUseCase
        get() = IsPushAvailableUseCase(pushRepository)

    val requestPushTokenUseCase: RequestPushTokenUseCase
        get() = RequestPushTokenUseCase(pushRepository)

    val registerPushTokenUseCase: RegisterPushTokenUseCase
        get() = RegisterPushTokenUseCase(pushRepository)

    val resetPushTokenUseCase: ResetPushTokenUseCase
        get() = ResetPushTokenUseCase(pushRepository)

    private var cachedPushViewModel: PushViewModel? = null

    val pushViewModel: PushViewModel
        get() {
            var vm = cachedPushViewModel
            if (vm == null) {
                vm = createPushViewModel()
                cachedPushViewModel = vm
            }
            return vm
        }

    fun createPushViewModel(): PushViewModel {
        return PushViewModel(
            observePushStatusUseCase = observePushStatusUseCase,
            getPushStatusUseCase = getPushStatusUseCase,
            isPushAvailableUseCase = isPushAvailableUseCase,
            requestPushTokenUseCase = requestPushTokenUseCase,
            registerPushTokenUseCase = registerPushTokenUseCase,
            resetPushTokenUseCase = resetPushTokenUseCase
        )
    }

    private var customChromecastRepository: ChromecastRepository? = null

    var chromecastRepository: ChromecastRepository
        get() = customChromecastRepository ?: LegacyChromecastRepository()
        set(value) {
            customChromecastRepository = value
        }

    val observeChromecastStateUseCase: ObserveChromecastStateUseCase
        get() = ObserveChromecastStateUseCase(chromecastRepository)

    val getChromecastStateUseCase: GetChromecastStateUseCase
        get() = GetChromecastStateUseCase(chromecastRepository)

    val isCastingUseCase: IsCastingUseCase
        get() = IsCastingUseCase(chromecastRepository)

    val isMediaPlayingOnCastUseCase: IsMediaPlayingOnCastUseCase
        get() = IsMediaPlayingOnCastUseCase(chromecastRepository)

    val castMediaUseCase: CastMediaUseCase
        get() = CastMediaUseCase(chromecastRepository)

    val stopCastingUseCase: StopCastingUseCase
        get() = StopCastingUseCase(chromecastRepository)

    val setCastCoverFileUseCase: SetCastCoverFileUseCase
        get() = SetCastCoverFileUseCase(chromecastRepository)

    private var cachedChromecastViewModel: ChromecastViewModel? = null

    val chromecastViewModel: ChromecastViewModel
        get() {
            var vm = cachedChromecastViewModel
            if (vm == null) {
                vm = createChromecastViewModel()
                cachedChromecastViewModel = vm
            }
            return vm
        }

    fun createChromecastViewModel(): ChromecastViewModel {
        return ChromecastViewModel(
            observeChromecastStateUseCase = observeChromecastStateUseCase,
            getChromecastStateUseCase = getChromecastStateUseCase,
            isCastingUseCase = isCastingUseCase,
            isMediaPlayingOnCastUseCase = isMediaPlayingOnCastUseCase,
            castMediaUseCase = castMediaUseCase,
            stopCastingUseCase = stopCastingUseCase,
            setCastCoverFileUseCase = setCastCoverFileUseCase
        )
    }

    private var customHintsRepository: HintsRepository? = null

    var hintsRepository: HintsRepository
        get() = customHintsRepository ?: LegacyHintsRepository()
        set(value) {
            customHintsRepository = value
        }

    val observeHintsUseCase: ObserveHintsUseCase
        get() = ObserveHintsUseCase(hintsRepository)

    val getHintsStateUseCase: GetHintsStateUseCase
        get() = GetHintsStateUseCase(hintsRepository)

    val getHintUseCase: GetHintUseCase
        get() = GetHintUseCase(hintsRepository)

    val shouldShowHintUseCase: ShouldShowHintUseCase
        get() = ShouldShowHintUseCase(hintsRepository)

    val incrementHintUseCase: IncrementHintUseCase
        get() = IncrementHintUseCase(hintsRepository)

    val doNotShowAgainHintUseCase: DoNotShowAgainHintUseCase
        get() = DoNotShowAgainHintUseCase(hintsRepository)

    val resetHintUseCase: ResetHintUseCase
        get() = ResetHintUseCase(hintsRepository)

    val resetAllHintsUseCase: ResetAllHintsUseCase
        get() = ResetAllHintsUseCase(hintsRepository)

    private var cachedHintsViewModel: HintsViewModel? = null

    val hintsViewModel: HintsViewModel
        get() {
            var vm = cachedHintsViewModel
            if (vm == null) {
                vm = createHintsViewModel()
                cachedHintsViewModel = vm
            }
            return vm
        }

    fun createHintsViewModel(): HintsViewModel {
        return HintsViewModel(
            observeHintsUseCase = observeHintsUseCase,
            getHintsStateUseCase = getHintsStateUseCase,
            shouldShowHintUseCase = shouldShowHintUseCase,
            incrementHintUseCase = incrementHintUseCase,
            doNotShowAgainHintUseCase = doNotShowAgainHintUseCase,
            resetHintUseCase = resetHintUseCase,
            resetAllHintsUseCase = resetAllHintsUseCase
        )
    }

    private var customGroupCallMessagesRepository: GroupCallMessagesRepository? = null

    var groupCallMessagesRepository: GroupCallMessagesRepository
        get() = customGroupCallMessagesRepository ?: LegacyGroupCallMessagesRepository(account)
        set(value) {
            customGroupCallMessagesRepository = value
        }

    val observeGroupCallMessagesUseCase: ObserveGroupCallMessagesUseCase
        get() = ObserveGroupCallMessagesUseCase(groupCallMessagesRepository)

    val getGroupCallMessagesUseCase: GetGroupCallMessagesUseCase
        get() = GetGroupCallMessagesUseCase(groupCallMessagesRepository)

    val sendGroupCallMessageUseCase: SendGroupCallMessageUseCase
        get() = SendGroupCallMessageUseCase(groupCallMessagesRepository)

    val popGroupCallMessageUseCase: PopGroupCallMessageUseCase
        get() = PopGroupCallMessageUseCase(groupCallMessagesRepository)

    val clearGroupCallMessagesUseCase: ClearGroupCallMessagesUseCase
        get() = ClearGroupCallMessagesUseCase(groupCallMessagesRepository)

    private val cachedGroupCallMessagesViewModels = ConcurrentHashMap<Long, GroupCallMessagesViewModel>()

    fun getGroupCallMessagesViewModel(callId: Long = 0L): GroupCallMessagesViewModel {
        return cachedGroupCallMessagesViewModels.computeIfAbsent(callId) { createGroupCallMessagesViewModel(it) }
    }

    fun createGroupCallMessagesViewModel(callId: Long = 0L): GroupCallMessagesViewModel {
        return GroupCallMessagesViewModel(
            observeGroupCallMessagesUseCase = observeGroupCallMessagesUseCase,
            getGroupCallMessagesUseCase = getGroupCallMessagesUseCase,
            sendGroupCallMessageUseCase = sendGroupCallMessageUseCase,
            popGroupCallMessageUseCase = popGroupCallMessageUseCase,
            clearGroupCallMessagesUseCase = clearGroupCallMessagesUseCase,
            initialCallId = callId
        )
    }

    private var customGallerySaveRepository: GallerySaveRepository? = null

    var gallerySaveRepository: GallerySaveRepository
        get() = customGallerySaveRepository ?: LegacyGallerySaveRepository(account)
        set(value) {
            customGallerySaveRepository = value
        }

    val observeGallerySaveConfigUseCase: ObserveGallerySaveConfigUseCase
        get() = ObserveGallerySaveConfigUseCase(gallerySaveRepository)

    val getGallerySaveConfigUseCase: GetGallerySaveConfigUseCase
        get() = GetGallerySaveConfigUseCase(gallerySaveRepository)

    val getGallerySaveSettingsUseCase: GetGallerySaveSettingsUseCase
        get() = GetGallerySaveSettingsUseCase(gallerySaveRepository)

    val updateGallerySaveSettingsUseCase: UpdateGallerySaveSettingsUseCase
        get() = UpdateGallerySaveSettingsUseCase(gallerySaveRepository)

    val toggleGallerySavePeerTypeUseCase: ToggleGallerySavePeerTypeUseCase
        get() = ToggleGallerySavePeerTypeUseCase(gallerySaveRepository)

    val setGallerySaveVideoLimitUseCase: SetGallerySaveVideoLimitUseCase
        get() = SetGallerySaveVideoLimitUseCase(gallerySaveRepository)

    val getGallerySaveExceptionsUseCase: GetGallerySaveExceptionsUseCase
        get() = GetGallerySaveExceptionsUseCase(gallerySaveRepository)

    val setGallerySaveExceptionUseCase: SetGallerySaveExceptionUseCase
        get() = SetGallerySaveExceptionUseCase(gallerySaveRepository)

    val removeGallerySaveExceptionUseCase: RemoveGallerySaveExceptionUseCase
        get() = RemoveGallerySaveExceptionUseCase(gallerySaveRepository)

    val removeAllGallerySaveExceptionsUseCase: RemoveAllGallerySaveExceptionsUseCase
        get() = RemoveAllGallerySaveExceptionsUseCase(gallerySaveRepository)

    private var cachedGallerySaveViewModel: GallerySaveViewModel? = null

    val gallerySaveViewModel: GallerySaveViewModel
        get() {
            var vm = cachedGallerySaveViewModel
            if (vm == null) {
                vm = createGallerySaveViewModel()
                cachedGallerySaveViewModel = vm
            }
            return vm
        }

    fun createGallerySaveViewModel(): GallerySaveViewModel {
        return GallerySaveViewModel(
            observeGallerySaveConfigUseCase = observeGallerySaveConfigUseCase,
            getGallerySaveConfigUseCase = getGallerySaveConfigUseCase,
            getGallerySaveSettingsUseCase = getGallerySaveSettingsUseCase,
            updateGallerySaveSettingsUseCase = updateGallerySaveSettingsUseCase,
            toggleGallerySavePeerTypeUseCase = toggleGallerySavePeerTypeUseCase,
            setGallerySaveVideoLimitUseCase = setGallerySaveVideoLimitUseCase,
            setGallerySaveExceptionUseCase = setGallerySaveExceptionUseCase,
            removeGallerySaveExceptionUseCase = removeGallerySaveExceptionUseCase,
            removeAllGallerySaveExceptionsUseCase = removeAllGallerySaveExceptionsUseCase
        )
    }

    private var customRefreshRateRepository: RefreshRateRepository? = null

    var refreshRateRepository: RefreshRateRepository
        get() = customRefreshRateRepository ?: LegacyRefreshRateRepository()
        set(value) {
            customRefreshRateRepository = value
        }

    val observeRefreshRateStateUseCase: ObserveRefreshRateStateUseCase
        get() = ObserveRefreshRateStateUseCase(refreshRateRepository)

    val getRefreshRateStateUseCase: GetRefreshRateStateUseCase
        get() = GetRefreshRateStateUseCase(refreshRateRepository)

    val startRefreshRateTrackingUseCase: StartRefreshRateTrackingUseCase
        get() = StartRefreshRateTrackingUseCase(refreshRateRepository)

    val stopRefreshRateTrackingUseCase: StopRefreshRateTrackingUseCase
        get() = StopRefreshRateTrackingUseCase(refreshRateRepository)

    val toggleAdaptiveRefreshRateUseCase: ToggleAdaptiveRefreshRateUseCase
        get() = ToggleAdaptiveRefreshRateUseCase(refreshRateRepository)

    val setPreferredRefreshRateModeUseCase: SetPreferredRefreshRateModeUseCase
        get() = SetPreferredRefreshRateModeUseCase(refreshRateRepository)

    val recordFrameMetricUseCase: RecordFrameMetricUseCase
        get() = RecordFrameMetricUseCase(refreshRateRepository)

    val resetRefreshRateStatsUseCase: ResetRefreshRateStatsUseCase
        get() = ResetRefreshRateStatsUseCase(refreshRateRepository)

    val getDisplayRefreshModesUseCase: GetDisplayRefreshModesUseCase
        get() = GetDisplayRefreshModesUseCase(refreshRateRepository)

    private var cachedRefreshRateViewModel: RefreshRateViewModel? = null

    val refreshRateViewModel: RefreshRateViewModel
        get() {
            var vm = cachedRefreshRateViewModel
            if (vm == null) {
                vm = createRefreshRateViewModel()
                cachedRefreshRateViewModel = vm
            }
            return vm
        }

    fun createRefreshRateViewModel(): RefreshRateViewModel {
        return RefreshRateViewModel(
            observeRefreshRateStateUseCase = observeRefreshRateStateUseCase,
            getRefreshRateStateUseCase = getRefreshRateStateUseCase,
            startRefreshRateTrackingUseCase = startRefreshRateTrackingUseCase,
            stopRefreshRateTrackingUseCase = stopRefreshRateTrackingUseCase,
            toggleAdaptiveRefreshRateUseCase = toggleAdaptiveRefreshRateUseCase,
            setPreferredRefreshRateModeUseCase = setPreferredRefreshRateModeUseCase,
            recordFrameMetricUseCase = recordFrameMetricUseCase,
            resetRefreshRateStatsUseCase = resetRefreshRateStatsUseCase
        )
    }

    private var customChatMessagesMetadataRepository: ChatMessagesMetadataRepository? = null

    var chatMessagesMetadataRepository: ChatMessagesMetadataRepository
        get() = customChatMessagesMetadataRepository ?: LegacyChatMessagesMetadataRepository(account)
        set(value) {
            customChatMessagesMetadataRepository = value
        }

    val observeChatMetadataStatsUseCase: ObserveChatMetadataStatsUseCase
        get() = ObserveChatMetadataStatsUseCase(chatMessagesMetadataRepository)

    val getChatMetadataStatsUseCase: GetChatMetadataStatsUseCase
        get() = GetChatMetadataStatsUseCase(chatMessagesMetadataRepository)

    val checkMessagesMetadataUseCase: CheckMessagesMetadataUseCase
        get() = CheckMessagesMetadataUseCase(chatMessagesMetadataRepository)

    val loadMessagesReactionsUseCase: LoadMessagesReactionsUseCase
        get() = LoadMessagesReactionsUseCase(chatMessagesMetadataRepository)

    val loadMessagesExtendedMediaUseCase: LoadMessagesExtendedMediaUseCase
        get() = LoadMessagesExtendedMediaUseCase(chatMessagesMetadataRepository)

    val cancelPendingMetadataRequestsUseCase: CancelPendingMetadataRequestsUseCase
        get() = CancelPendingMetadataRequestsUseCase(chatMessagesMetadataRepository)

    private var cachedChatMetadataViewModel: ChatMetadataViewModel? = null

    val chatMetadataViewModel: ChatMetadataViewModel
        get() {
            var vm = cachedChatMetadataViewModel
            if (vm == null) {
                vm = createChatMetadataViewModel()
                cachedChatMetadataViewModel = vm
            }
            return vm
        }

    fun createChatMetadataViewModel(): ChatMetadataViewModel {
        return ChatMetadataViewModel(
            observeChatMetadataStatsUseCase = observeChatMetadataStatsUseCase,
            getChatMetadataStatsUseCase = getChatMetadataStatsUseCase,
            checkMessagesMetadataUseCase = checkMessagesMetadataUseCase,
            loadMessagesReactionsUseCase = loadMessagesReactionsUseCase,
            loadMessagesExtendedMediaUseCase = loadMessagesExtendedMediaUseCase,
            cancelPendingMetadataRequestsUseCase = cancelPendingMetadataRequestsUseCase
        )
    }

    private var customPipRepository: PipRepository? = null

    var pipRepository: PipRepository
        get() = customPipRepository ?: LegacyPipRepository()
        set(value) {
            customPipRepository = value
        }

    val observePipSessionUseCase: ObservePipSessionUseCase
        get() = ObservePipSessionUseCase(pipRepository)

    val getPipSessionUseCase: GetPipSessionUseCase
        get() = GetPipSessionUseCase(pipRepository)

    val registerPipSourceUseCase: RegisterPipSourceUseCase
        get() = RegisterPipSourceUseCase(pipRepository)

    val unregisterPipSourceUseCase: UnregisterPipSourceUseCase
        get() = UnregisterPipSourceUseCase(pipRepository)

    val updatePipSourceStateUseCase: UpdatePipSourceStateUseCase
        get() = UpdatePipSourceStateUseCase(pipRepository)

    val dispatchPipStateUseCase: DispatchPipStateUseCase
        get() = DispatchPipStateUseCase(pipRepository)

    val triggerPipActionUseCase: TriggerPipActionUseCase
        get() = TriggerPipActionUseCase(pipRepository)

    val evaluatePipEligibilityUseCase: EvaluatePipEligibilityUseCase
        get() = EvaluatePipEligibilityUseCase(pipRepository)

    private var cachedPipViewModel: PipViewModel? = null

    val pipViewModel: PipViewModel
        get() {
            var vm = cachedPipViewModel
            if (vm == null) {
                vm = createPipViewModel()
                cachedPipViewModel = vm
            }
            return vm
        }

    fun createPipViewModel(): PipViewModel {
        return PipViewModel(
            observePipSessionUseCase = observePipSessionUseCase,
            getPipSessionUseCase = getPipSessionUseCase,
            registerPipSourceUseCase = registerPipSourceUseCase,
            unregisterPipSourceUseCase = unregisterPipSourceUseCase,
            updatePipSourceStateUseCase = updatePipSourceStateUseCase,
            dispatchPipStateUseCase = dispatchPipStateUseCase,
            triggerPipActionUseCase = triggerPipActionUseCase,
            evaluatePipEligibilityUseCase = evaluatePipEligibilityUseCase
        )
    }

    private var customDraftsRepository: DraftsRepository? = null

    var draftsRepository: DraftsRepository
        get() = customDraftsRepository ?: LegacyDraftsRepository(account)
        set(value) {
            customDraftsRepository = value
        }

    val observeDraftsStateUseCase: ObserveDraftsStateUseCase
        get() = ObserveDraftsStateUseCase(draftsRepository)

    val getDraftsStateUseCase: GetDraftsStateUseCase
        get() = GetDraftsStateUseCase(draftsRepository)

    val loadDraftsUseCase: LoadDraftsUseCase
        get() = LoadDraftsUseCase(draftsRepository)

    val saveDraftUseCase: SaveDraftUseCase
        get() = SaveDraftUseCase(draftsRepository)

    val deleteDraftUseCase: DeleteDraftUseCase
        get() = DeleteDraftUseCase(draftsRepository)

    val deleteForEditUseCase: DeleteForEditUseCase
        get() = DeleteForEditUseCase(draftsRepository)

    val getDraftForEditUseCase: GetDraftForEditUseCase
        get() = GetDraftForEditUseCase(draftsRepository)

    val cleanupExpiredDraftsUseCase: CleanupExpiredDraftsUseCase
        get() = CleanupExpiredDraftsUseCase(draftsRepository)

    private var cachedDraftsViewModel: DraftsViewModel? = null

    val draftsViewModel: DraftsViewModel
        get() {
            var vm = cachedDraftsViewModel
            if (vm == null) {
                vm = createDraftsViewModel()
                cachedDraftsViewModel = vm
            }
            return vm
        }

    fun createDraftsViewModel(): DraftsViewModel {
        return DraftsViewModel(
            observeDraftsStateUseCase = observeDraftsStateUseCase,
            getDraftsStateUseCase = getDraftsStateUseCase,
            loadDraftsUseCase = loadDraftsUseCase,
            saveDraftUseCase = saveDraftUseCase,
            deleteDraftUseCase = deleteDraftUseCase,
            deleteForEditUseCase = deleteForEditUseCase,
            getDraftForEditUseCase = getDraftForEditUseCase,
            cleanupExpiredDraftsUseCase = cleanupExpiredDraftsUseCase
        )
    }

    // ==========================================
    // Feature: FileRef (MTProto File Reference Renewal)
    // ==========================================

    private var customFileRefRepository: FileRefRepository? = null

    var fileRefRepository: FileRefRepository
        get() = customFileRefRepository ?: LegacyFileRefRepository(account)
        set(value) { customFileRefRepository = value }

    val observeFileRefStatsUseCase: ObserveFileRefStatsUseCase
        get() = ObserveFileRefStatsUseCase(fileRefRepository)

    val getFileRefStatsUseCase: GetFileRefStatsUseCase
        get() = GetFileRefStatsUseCase(fileRefRepository)

    val requestReferenceRenewalUseCase: RequestReferenceRenewalUseCase
        get() = RequestReferenceRenewalUseCase(fileRefRepository)

    val notifyReferenceRenewedUseCase: NotifyReferenceRenewedUseCase
        get() = NotifyReferenceRenewedUseCase(fileRefRepository)

    val cancelFileRefRequestUseCase: CancelFileRefRequestUseCase
        get() = CancelFileRefRequestUseCase(fileRefRepository)

    val clearFileRefCacheUseCase: ClearFileRefCacheUseCase
        get() = ClearFileRefCacheUseCase(fileRefRepository)

    private var cachedFileRefViewModel: FileRefViewModel? = null

    val fileRefViewModel: FileRefViewModel
        get() {
            var vm = cachedFileRefViewModel
            if (vm == null) {
                vm = createFileRefViewModel()
                cachedFileRefViewModel = vm
            }
            return vm
        }

    fun createFileRefViewModel(): FileRefViewModel {
        return FileRefViewModel(
            observeFileRefStatsUseCase = observeFileRefStatsUseCase,
            getFileRefStatsUseCase = getFileRefStatsUseCase,
            requestReferenceRenewalUseCase = requestReferenceRenewalUseCase,
            notifyReferenceRenewedUseCase = notifyReferenceRenewedUseCase,
            cancelFileRefRequestUseCase = cancelFileRefRequestUseCase,
            clearFileRefCacheUseCase = clearFileRefCacheUseCase
        )
    }

    // ==========================================
    // Feature: Camera (Hardware Camera & Video Recording)
    // ==========================================

    private var customCameraRepository: CameraRepository? = null

    var cameraRepository: CameraRepository
        get() = customCameraRepository ?: LegacyCameraRepository()
        set(value) { customCameraRepository = value }

    val observeCameraStateUseCase: ObserveCameraStateUseCase
        get() = ObserveCameraStateUseCase(cameraRepository)

    val getCameraStateUseCase: GetCameraStateUseCase
        get() = GetCameraStateUseCase(cameraRepository)

    val initCamerasUseCase: InitCamerasUseCase
        get() = InitCamerasUseCase(cameraRepository)

    val selectCameraUseCase: SelectCameraUseCase
        get() = SelectCameraUseCase(cameraRepository)

    val switchCameraUseCase: SwitchCameraUseCase
        get() = SwitchCameraUseCase(cameraRepository)

    val setCameraFlashModeUseCase: SetCameraFlashModeUseCase
        get() = SetCameraFlashModeUseCase(cameraRepository)

    val toggleMirrorFrontCameraUseCase: ToggleMirrorFrontCameraUseCase
        get() = ToggleMirrorFrontCameraUseCase(cameraRepository)

    val chooseOptimalResolutionUseCase: ChooseOptimalResolutionUseCase
        get() = ChooseOptimalResolutionUseCase(cameraRepository)

    val notifyCameraRecordingUseCase: NotifyCameraRecordingUseCase
        get() = NotifyCameraRecordingUseCase(cameraRepository)

    private var cachedCameraViewModel: CameraViewModel? = null

    val cameraViewModel: CameraViewModel
        get() {
            var vm = cachedCameraViewModel
            if (vm == null) {
                vm = createCameraViewModel()
                cachedCameraViewModel = vm
            }
            return vm
        }

    fun createCameraViewModel(): CameraViewModel {
        return CameraViewModel(
            observeCameraStateUseCase = observeCameraStateUseCase,
            getCameraStateUseCase = getCameraStateUseCase,
            initCamerasUseCase = initCamerasUseCase,
            selectCameraUseCase = selectCameraUseCase,
            switchCameraUseCase = switchCameraUseCase,
            setCameraFlashModeUseCase = setCameraFlashModeUseCase,
            toggleMirrorFrontCameraUseCase = toggleMirrorFrontCameraUseCase,
            chooseOptimalResolutionUseCase = chooseOptimalResolutionUseCase,
            notifyCameraRecordingUseCase = notifyCameraRecordingUseCase
        )
    }

    // ==========================================
    // Feature: CacheByChats (Keep-Media Cache Retention & Exceptions)
    // ==========================================

    private var customCacheByChatsRepository: CacheByChatsRepository? = null

    var cacheByChatsRepository: CacheByChatsRepository
        get() = customCacheByChatsRepository ?: LegacyCacheByChatsRepository(account)
        set(value) { customCacheByChatsRepository = value }

    val observeCacheByChatsConfigUseCase: ObserveCacheByChatsConfigUseCase
        get() = ObserveCacheByChatsConfigUseCase(cacheByChatsRepository)

    val getCacheByChatsConfigUseCase: GetCacheByChatsConfigUseCase
        get() = GetCacheByChatsConfigUseCase(cacheByChatsRepository)

    val setKeepMediaDurationUseCase: SetKeepMediaDurationUseCase
        get() = SetKeepMediaDurationUseCase(cacheByChatsRepository)

    val setKeepMediaExceptionUseCase: SetKeepMediaExceptionUseCase
        get() = SetKeepMediaExceptionUseCase(cacheByChatsRepository)

    val removeKeepMediaExceptionUseCase: RemoveKeepMediaExceptionUseCase
        get() = RemoveKeepMediaExceptionUseCase(cacheByChatsRepository)

    val clearKeepMediaExceptionsUseCase: ClearKeepMediaExceptionsUseCase
        get() = ClearKeepMediaExceptionsUseCase(cacheByChatsRepository)

    private var cachedCacheByChatsViewModel: CacheByChatsViewModel? = null

    val cacheByChatsViewModel: CacheByChatsViewModel
        get() {
            var vm = cachedCacheByChatsViewModel
            if (vm == null) {
                vm = createCacheByChatsViewModel()
                cachedCacheByChatsViewModel = vm
            }
            return vm
        }

    fun createCacheByChatsViewModel(): CacheByChatsViewModel {
        return CacheByChatsViewModel(
            observeCacheByChatsConfigUseCase = observeCacheByChatsConfigUseCase,
            getCacheByChatsConfigUseCase = getCacheByChatsConfigUseCase,
            setKeepMediaDurationUseCase = setKeepMediaDurationUseCase,
            setKeepMediaExceptionUseCase = setKeepMediaExceptionUseCase,
            removeKeepMediaExceptionUseCase = removeKeepMediaExceptionUseCase,
            clearKeepMediaExceptionsUseCase = clearKeepMediaExceptionsUseCase
        )
    }

    fun createDraftMeasureRepository(legacyController: ChatActivityDraftMessageMeasureController? = null): DraftMeasureRepository {
        return LegacyDraftMeasureRepository(legacyController)
    }

    val draftMeasureRepository: DraftMeasureRepository by lazy {
        LegacyDraftMeasureRepository()
    }

    val calculateDraftMeasureOverrideUseCase: CalculateDraftMeasureOverrideUseCase
        get() = CalculateDraftMeasureOverrideUseCase(draftMeasureRepository)

    val setDraftMeasureTargetUseCase: SetDraftMeasureTargetUseCase
        get() = SetDraftMeasureTargetUseCase(draftMeasureRepository)

    val onDraftMessageIdChangedUseCase: OnDraftMessageIdChangedUseCase
        get() = OnDraftMessageIdChangedUseCase(draftMeasureRepository)

    val setPreviousMessageHeightUseCase: SetPreviousMessageHeightUseCase
        get() = SetPreviousMessageHeightUseCase(draftMeasureRepository)

    val resetDraftMeasureTargetUseCase: ResetDraftMeasureTargetUseCase
        get() = ResetDraftMeasureTargetUseCase(draftMeasureRepository)

    val observeDraftMeasureConfigUseCase: ObserveDraftMeasureConfigUseCase
        get() = ObserveDraftMeasureConfigUseCase(draftMeasureRepository)

    val getDraftMeasureConfigUseCase: GetDraftMeasureConfigUseCase
        get() = GetDraftMeasureConfigUseCase(draftMeasureRepository)

    private var cachedDraftMeasureViewModel: DraftMeasureViewModel? = null

    val draftMeasureViewModel: DraftMeasureViewModel
        get() {
            var vm = cachedDraftMeasureViewModel
            if (vm == null) {
                vm = createDraftMeasureViewModel()
                cachedDraftMeasureViewModel = vm
            }
            return vm
        }

    fun createDraftMeasureViewModel(legacyController: ChatActivityDraftMessageMeasureController? = null): DraftMeasureViewModel {
        val repo = if (legacyController != null) createDraftMeasureRepository(legacyController) else draftMeasureRepository
        return DraftMeasureViewModel(
            calculateOverrideUseCase = CalculateDraftMeasureOverrideUseCase(repo),
            setTargetUseCase = SetDraftMeasureTargetUseCase(repo),
            onMessageIdChangedUseCase = OnDraftMessageIdChangedUseCase(repo),
            setPreviousHeightUseCase = SetPreviousMessageHeightUseCase(repo),
            resetTargetUseCase = ResetDraftMeasureTargetUseCase(repo),
            observeConfigUseCase = ObserveDraftMeasureConfigUseCase(repo),
            getConfigUseCase = GetDraftMeasureConfigUseCase(repo)
        )
    }

    fun createBottomViewsRepository(legacyController: ChatActivityBottomViewsVisibilityController? = null): BottomViewsVisibilityRepository {
        return LegacyBottomViewsVisibilityRepository(legacyController)
    }

    val bottomViewsRepository: BottomViewsVisibilityRepository by lazy {
        LegacyBottomViewsVisibilityRepository()
    }

    val getBottomViewVisibilityUseCase: GetBottomViewVisibilityUseCase
        get() = GetBottomViewVisibilityUseCase(bottomViewsRepository)

    val setBottomViewVisibleUseCase: SetBottomViewVisibleUseCase
        get() = SetBottomViewVisibleUseCase(bottomViewsRepository)

    val getPriorityBottomContainerUseCase: GetPriorityBottomContainerUseCase
        get() = GetPriorityBottomContainerUseCase(bottomViewsRepository)

    val getBottomViewsStateUseCase: GetBottomViewsStateUseCase
        get() = GetBottomViewsStateUseCase(bottomViewsRepository)

    val observeBottomViewsVisibilityUseCase: ObserveBottomViewsVisibilityUseCase
        get() = ObserveBottomViewsVisibilityUseCase(bottomViewsRepository)

    private var cachedBottomViewsViewModel: BottomViewsViewModel? = null

    val bottomViewsViewModel: BottomViewsViewModel
        get() {
            var vm = cachedBottomViewsViewModel
            if (vm == null) {
                vm = createBottomViewsViewModel()
                cachedBottomViewsViewModel = vm
            }
            return vm
        }

    fun createBottomViewsViewModel(legacyController: ChatActivityBottomViewsVisibilityController? = null): BottomViewsViewModel {
        val repo = if (legacyController != null) createBottomViewsRepository(legacyController) else bottomViewsRepository
        return BottomViewsViewModel(
            observeBottomViewsVisibilityUseCase = ObserveBottomViewsVisibilityUseCase(repo),
            getBottomViewsStateUseCase = GetBottomViewsStateUseCase(repo),
            setBottomViewVisibleUseCase = SetBottomViewVisibleUseCase(repo)
        )
    }

    fun createFloatingDebugRepository(activityProvider: (() -> LaunchActivity?)? = null): FloatingDebugRepository {
        return LegacyFloatingDebugRepository(activityProvider)
    }

    val floatingDebugRepository: FloatingDebugRepository by lazy {
        LegacyFloatingDebugRepository()
    }

    val isFloatingDebugActiveUseCase: IsFloatingDebugActiveUseCase
        get() = IsFloatingDebugActiveUseCase(floatingDebugRepository)

    val setFloatingDebugActiveUseCase: SetFloatingDebugActiveUseCase
        get() = SetFloatingDebugActiveUseCase(floatingDebugRepository)

    val toggleFloatingDebugActiveUseCase: ToggleFloatingDebugActiveUseCase
        get() = ToggleFloatingDebugActiveUseCase(floatingDebugRepository)

    val getFloatingDebugItemsUseCase: GetFloatingDebugItemsUseCase
        get() = GetFloatingDebugItemsUseCase(floatingDebugRepository)

    val registerFloatingDebugItemsUseCase: RegisterFloatingDebugItemsUseCase
        get() = RegisterFloatingDebugItemsUseCase(floatingDebugRepository)

    val clearFloatingDebugItemsUseCase: ClearFloatingDebugItemsUseCase
        get() = ClearFloatingDebugItemsUseCase(floatingDebugRepository)

    val observeFloatingDebugStateUseCase: ObserveFloatingDebugStateUseCase
        get() = ObserveFloatingDebugStateUseCase(floatingDebugRepository)

    val getFloatingDebugStateUseCase: GetFloatingDebugStateUseCase
        get() = GetFloatingDebugStateUseCase(floatingDebugRepository)

    private var cachedFloatingDebugViewModel: FloatingDebugViewModel? = null

    val floatingDebugViewModel: FloatingDebugViewModel
        get() {
            var vm = cachedFloatingDebugViewModel
            if (vm == null) {
                vm = createFloatingDebugViewModel()
                cachedFloatingDebugViewModel = vm
            }
            return vm
        }

    fun createFloatingDebugViewModel(activityProvider: (() -> LaunchActivity?)? = null): FloatingDebugViewModel {
        val repo = if (activityProvider != null) createFloatingDebugRepository(activityProvider) else floatingDebugRepository
        return FloatingDebugViewModel(
            observeFloatingDebugStateUseCase = ObserveFloatingDebugStateUseCase(repo),
            getFloatingDebugStateUseCase = GetFloatingDebugStateUseCase(repo),
            setFloatingDebugActiveUseCase = SetFloatingDebugActiveUseCase(repo),
            toggleFloatingDebugActiveUseCase = ToggleFloatingDebugActiveUseCase(repo),
            registerFloatingDebugItemsUseCase = RegisterFloatingDebugItemsUseCase(repo),
            clearFloatingDebugItemsUseCase = ClearFloatingDebugItemsUseCase(repo)
        )
    }

    fun createKeyboardInsetsRepository(inAppController: WindowInsetsInAppController? = null): KeyboardInsetsRepository {
        return LegacyKeyboardInsetsRepository(inAppController)
    }

    val keyboardInsetsRepository: KeyboardInsetsRepository by lazy {
        LegacyKeyboardInsetsRepository()
    }

    val requestInAppKeyboardHeightUseCase: RequestInAppKeyboardHeightUseCase
        get() = RequestInAppKeyboardHeightUseCase(keyboardInsetsRepository)

    val resetInAppKeyboardHeightUseCase: ResetInAppKeyboardHeightUseCase
        get() = ResetInAppKeyboardHeightUseCase(keyboardInsetsRepository)

    val requestInAppKeyboardHeightWithNavbarUseCase: RequestInAppKeyboardHeightWithNavbarUseCase
        get() = RequestInAppKeyboardHeightWithNavbarUseCase(keyboardInsetsRepository)

    val updateSystemInsetsUseCase: UpdateSystemInsetsUseCase
        get() = UpdateSystemInsetsUseCase(keyboardInsetsRepository)

    val getKeyboardInsetsUseCase: GetKeyboardInsetsUseCase
        get() = GetKeyboardInsetsUseCase(keyboardInsetsRepository)

    val observeKeyboardInsetsUseCase: ObserveKeyboardInsetsUseCase
        get() = ObserveKeyboardInsetsUseCase(keyboardInsetsRepository)

    private var cachedKeyboardInsetsViewModel: KeyboardInsetsViewModel? = null

    val keyboardInsetsViewModel: KeyboardInsetsViewModel
        get() {
            var vm = cachedKeyboardInsetsViewModel
            if (vm == null) {
                vm = createKeyboardInsetsViewModel()
                cachedKeyboardInsetsViewModel = vm
            }
            return vm
        }

    fun createKeyboardInsetsViewModel(inAppController: WindowInsetsInAppController? = null): KeyboardInsetsViewModel {
        val repo = if (inAppController != null) createKeyboardInsetsRepository(inAppController) else keyboardInsetsRepository
        return KeyboardInsetsViewModel(
            observeKeyboardInsetsUseCase = ObserveKeyboardInsetsUseCase(repo),
            getKeyboardInsetsUseCase = GetKeyboardInsetsUseCase(repo),
            requestInAppKeyboardHeightUseCase = RequestInAppKeyboardHeightUseCase(repo),
            resetInAppKeyboardHeightUseCase = ResetInAppKeyboardHeightUseCase(repo),
            requestInAppKeyboardHeightWithNavbarUseCase = RequestInAppKeyboardHeightWithNavbarUseCase(repo),
            updateSystemInsetsUseCase = UpdateSystemInsetsUseCase(repo)
        )
    }

    fun createMainTabsRepository(controller: MainTabsActivityController? = null): MainTabsRepository {
        return LegacyMainTabsRepository(account, controller)
    }

    val mainTabsRepository: MainTabsRepository by lazy {
        LegacyMainTabsRepository(account)
    }

    val observeMainTabsConfigUseCase: ObserveMainTabsConfigUseCase
        get() = ObserveMainTabsConfigUseCase(mainTabsRepository)

    val getMainTabsConfigUseCase: GetMainTabsConfigUseCase
        get() = GetMainTabsConfigUseCase(mainTabsRepository)

    val setMainTabsVisibleUseCase: SetMainTabsVisibleUseCase
        get() = SetMainTabsVisibleUseCase(mainTabsRepository)

    val selectMainTabUseCase: SelectMainTabUseCase
        get() = SelectMainTabUseCase(mainTabsRepository)

    val setShowCallsTabUseCase: SetShowCallsTabUseCase
        get() = SetShowCallsTabUseCase(mainTabsRepository)

    val updateChatsUnreadCountUseCase: UpdateChatsUnreadCountUseCase
        get() = UpdateChatsUnreadCountUseCase(mainTabsRepository)

    val setContactsPermissionWarningUseCase: SetContactsPermissionWarningUseCase
        get() = SetContactsPermissionWarningUseCase(mainTabsRepository)

    private var cachedMainTabsViewModel: MainTabsViewModel? = null

    val mainTabsViewModel: MainTabsViewModel
        get() {
            var vm = cachedMainTabsViewModel
            if (vm == null) {
                vm = createMainTabsViewModel()
                cachedMainTabsViewModel = vm
            }
            return vm
        }

    fun createMainTabsViewModel(controller: MainTabsActivityController? = null): MainTabsViewModel {
        val repo = if (controller != null) createMainTabsRepository(controller) else mainTabsRepository
        return MainTabsViewModel(
            observeConfigUseCase = ObserveMainTabsConfigUseCase(repo),
            getConfigUseCase = GetMainTabsConfigUseCase(repo),
            setTabsVisibleUseCase = SetMainTabsVisibleUseCase(repo),
            selectMainTabUseCase = SelectMainTabUseCase(repo),
            setShowCallsTabUseCase = SetShowCallsTabUseCase(repo),
            updateChatsUnreadCountUseCase = UpdateChatsUnreadCountUseCase(repo),
            setContactsPermissionWarningUseCase = SetContactsPermissionWarningUseCase(repo)
        )
    }

    fun createRichCaptionRepository(): RichCaptionRepository {
        return LegacyRichCaptionRepository()
    }

    val richCaptionRepository: RichCaptionRepository by lazy {
        LegacyRichCaptionRepository()
    }

    val observeRichCaptionUseCase: ObserveRichCaptionUseCase
        get() = ObserveRichCaptionUseCase(richCaptionRepository)

    val getRichCaptionUseCase: GetRichCaptionUseCase
        get() = GetRichCaptionUseCase(richCaptionRepository)

    val setRichCaptionTextUseCase: SetRichCaptionTextUseCase
        get() = SetRichCaptionTextUseCase(richCaptionRepository)

    val setRichCaptionCreditUseCase: SetRichCaptionCreditUseCase
        get() = SetRichCaptionCreditUseCase(richCaptionRepository)

    val setRichCaptionLockedUseCase: SetRichCaptionLockedUseCase
        get() = SetRichCaptionLockedUseCase(richCaptionRepository)

    val calculateCaptionMeasureWidthUseCase: CalculateCaptionMeasureWidthUseCase
        get() = CalculateCaptionMeasureWidthUseCase(richCaptionRepository)

    val checkCaptionPressHitUseCase: CheckCaptionPressHitUseCase
        get() = CheckCaptionPressHitUseCase(richCaptionRepository)

    val clearRichCaptionUseCase: ClearRichCaptionUseCase
        get() = ClearRichCaptionUseCase(richCaptionRepository)

    private var cachedRichCaptionViewModel: RichCaptionViewModel? = null

    val richCaptionViewModel: RichCaptionViewModel
        get() {
            var vm = cachedRichCaptionViewModel
            if (vm == null) {
                vm = createRichCaptionViewModel()
                cachedRichCaptionViewModel = vm
            }
            return vm
        }

    fun createRichCaptionViewModel(): RichCaptionViewModel {
        return RichCaptionViewModel(
            observeRichCaptionUseCase = observeRichCaptionUseCase,
            getRichCaptionUseCase = getRichCaptionUseCase,
            setRichCaptionTextUseCase = setRichCaptionTextUseCase,
            setRichCaptionCreditUseCase = setRichCaptionCreditUseCase,
            setRichCaptionLockedUseCase = setRichCaptionLockedUseCase,
            clearRichCaptionUseCase = clearRichCaptionUseCase
        )
    }

    fun createAdjustPanRepository(): AdjustPanRepository {
        return LegacyAdjustPanRepository()
    }

    val adjustPanRepository: AdjustPanRepository by lazy {
        LegacyAdjustPanRepository()
    }

    val calculatePanTransitionPlanUseCase: CalculatePanTransitionPlanUseCase
        get() = CalculatePanTransitionPlanUseCase(adjustPanRepository)

    val computePanProgressUseCase: ComputePanProgressUseCase
        get() = ComputePanProgressUseCase(adjustPanRepository)

    val observeAdjustPanStateUseCase: ObserveAdjustPanStateUseCase
        get() = ObserveAdjustPanStateUseCase(adjustPanRepository)

    val getAdjustPanStateUseCase: GetAdjustPanStateUseCase
        get() = GetAdjustPanStateUseCase(adjustPanRepository)

    val setAdjustPanEnabledUseCase: SetAdjustPanEnabledUseCase
        get() = SetAdjustPanEnabledUseCase(adjustPanRepository)

    val startAdjustPanTransitionUseCase: StartAdjustPanTransitionUseCase
        get() = StartAdjustPanTransitionUseCase(adjustPanRepository)

    val updateAdjustPanTransitionUseCase: UpdateAdjustPanTransitionUseCase
        get() = UpdateAdjustPanTransitionUseCase(adjustPanRepository)

    val stopAdjustPanTransitionUseCase: StopAdjustPanTransitionUseCase
        get() = StopAdjustPanTransitionUseCase(adjustPanRepository)

    val resetAdjustPanUseCase: ResetAdjustPanUseCase
        get() = ResetAdjustPanUseCase(adjustPanRepository)

    private var cachedAdjustPanViewModel: AdjustPanViewModel? = null

    val adjustPanViewModel: AdjustPanViewModel
        get() {
            var vm = cachedAdjustPanViewModel
            if (vm == null) {
                vm = createAdjustPanViewModel()
                cachedAdjustPanViewModel = vm
            }
            return vm
        }

    fun createAdjustPanViewModel(): AdjustPanViewModel {
        return AdjustPanViewModel(
            calculatePlanUseCase = calculatePanTransitionPlanUseCase,
            observeStateUseCase = observeAdjustPanStateUseCase,
            getAdjustPanStateUseCase = getAdjustPanStateUseCase,
            setEnabledUseCase = setAdjustPanEnabledUseCase,
            startTransitionUseCase = startAdjustPanTransitionUseCase,
            updateTransitionUseCase = updateAdjustPanTransitionUseCase,
            stopTransitionUseCase = stopAdjustPanTransitionUseCase,
            resetUseCase = resetAdjustPanUseCase
        )
    }

    fun createKeyboardHideRepository(): KeyboardHideRepository {
        return LegacyKeyboardHideRepository()
    }

    val keyboardHideRepository: KeyboardHideRepository by lazy {
        LegacyKeyboardHideRepository()
    }

    val calculateKeyboardHideProgressUseCase: CalculateKeyboardHideProgressUseCase
        get() = CalculateKeyboardHideProgressUseCase(keyboardHideRepository)

    val evaluateKeyboardDismissDecisionUseCase: EvaluateKeyboardDismissDecisionUseCase
        get() = EvaluateKeyboardDismissDecisionUseCase(keyboardHideRepository)

    val observeKeyboardHideStateUseCase: ObserveKeyboardHideStateUseCase
        get() = ObserveKeyboardHideStateUseCase(keyboardHideRepository)

    val getKeyboardHideStateUseCase: GetKeyboardHideStateUseCase
        get() = GetKeyboardHideStateUseCase(keyboardHideRepository)

    val setKeyboardHideEnabledUseCase: SetKeyboardHideEnabledUseCase
        get() = SetKeyboardHideEnabledUseCase(keyboardHideRepository)

    val startKeyboardHideMovingUseCase: StartKeyboardHideMovingUseCase
        get() = StartKeyboardHideMovingUseCase(keyboardHideRepository)

    val updateKeyboardHideMovingUseCase: UpdateKeyboardHideMovingUseCase
        get() = UpdateKeyboardHideMovingUseCase(keyboardHideRepository)

    val endKeyboardHideMovingUseCase: EndKeyboardHideMovingUseCase
        get() = EndKeyboardHideMovingUseCase(keyboardHideRepository)

    val finishKeyboardHideDismissUseCase: FinishKeyboardHideDismissUseCase
        get() = FinishKeyboardHideDismissUseCase(keyboardHideRepository)

    val resetKeyboardHideUseCase: ResetKeyboardHideUseCase
        get() = ResetKeyboardHideUseCase(keyboardHideRepository)

    private var cachedKeyboardHideViewModel: KeyboardHideViewModel? = null

    val keyboardHideViewModel: KeyboardHideViewModel
        get() {
            var vm = cachedKeyboardHideViewModel
            if (vm == null) {
                vm = createKeyboardHideViewModel()
                cachedKeyboardHideViewModel = vm
            }
            return vm
        }

    fun createKeyboardHideViewModel(): KeyboardHideViewModel {
        return KeyboardHideViewModel(
            calculateProgressUseCase = calculateKeyboardHideProgressUseCase,
            evaluateDismissDecisionUseCase = evaluateKeyboardDismissDecisionUseCase,
            observeStateUseCase = observeKeyboardHideStateUseCase,
            getStateUseCase = getKeyboardHideStateUseCase,
            setEnabledUseCase = setKeyboardHideEnabledUseCase,
            startMovingUseCase = startKeyboardHideMovingUseCase,
            updateMovingUseCase = updateKeyboardHideMovingUseCase,
            endMovingUseCase = endKeyboardHideMovingUseCase,
            finishDismissUseCase = finishKeyboardHideDismissUseCase,
            resetUseCase = resetKeyboardHideUseCase
        )
    }

    fun createBusinessRecipientsRepository(): BusinessRecipientsRepository {
        return LegacyBusinessRecipientsRepository()
    }

    val businessRecipientsRepository: BusinessRecipientsRepository by lazy {
        LegacyBusinessRecipientsRepository()
    }

    val observeBusinessRecipientsUseCase: ObserveBusinessRecipientsUseCase
        get() = ObserveBusinessRecipientsUseCase(businessRecipientsRepository)

    val getBusinessRecipientsUseCase: GetBusinessRecipientsUseCase
        get() = GetBusinessRecipientsUseCase(businessRecipientsRepository)

    val setBusinessRecipientsUseCase: SetBusinessRecipientsUseCase
        get() = SetBusinessRecipientsUseCase(businessRecipientsRepository)

    val toggleExcludeSelectedUseCase: ToggleExcludeSelectedUseCase
        get() = ToggleExcludeSelectedUseCase(businessRecipientsRepository)

    val toggleRecipientFilterUseCase: ToggleRecipientFilterUseCase
        get() = ToggleRecipientFilterUseCase(businessRecipientsRepository)

    val addSelectedUsersUseCase: AddSelectedUsersUseCase
        get() = AddSelectedUsersUseCase(businessRecipientsRepository)

    val removeSelectedUserUseCase: RemoveSelectedUserUseCase
        get() = RemoveSelectedUserUseCase(businessRecipientsRepository)

    val addExcludedUsersUseCase: AddExcludedUsersUseCase
        get() = AddExcludedUsersUseCase(businessRecipientsRepository)

    val removeExcludedUserUseCase: RemoveExcludedUserUseCase
        get() = RemoveExcludedUserUseCase(businessRecipientsRepository)

    val checkRecipientsChangesUseCase: CheckRecipientsChangesUseCase
        get() = CheckRecipientsChangesUseCase(businessRecipientsRepository)

    val validateBusinessRecipientsUseCase: ValidateBusinessRecipientsUseCase
        get() = ValidateBusinessRecipientsUseCase(businessRecipientsRepository)

    val resetBusinessRecipientsUseCase: ResetBusinessRecipientsUseCase
        get() = ResetBusinessRecipientsUseCase(businessRecipientsRepository)

    private var cachedBusinessRecipientsViewModel: BusinessRecipientsViewModel? = null

    val businessRecipientsViewModel: BusinessRecipientsViewModel
        get() {
            var vm = cachedBusinessRecipientsViewModel
            if (vm == null) {
                vm = createBusinessRecipientsViewModel()
                cachedBusinessRecipientsViewModel = vm
            }
            return vm
        }

    fun createBusinessRecipientsViewModel(): BusinessRecipientsViewModel {
        return BusinessRecipientsViewModel(
            observeRecipientsUseCase = observeBusinessRecipientsUseCase,
            getRecipientsUseCase = getBusinessRecipientsUseCase,
            setRecipientsUseCase = setBusinessRecipientsUseCase,
            toggleExcludeSelectedUseCase = toggleExcludeSelectedUseCase,
            toggleRecipientFilterUseCase = toggleRecipientFilterUseCase,
            addSelectedUsersUseCase = addSelectedUsersUseCase,
            removeSelectedUserUseCase = removeSelectedUserUseCase,
            addExcludedUsersUseCase = addExcludedUsersUseCase,
            removeExcludedUserUseCase = removeExcludedUserUseCase,
            checkChangesUseCase = checkRecipientsChangesUseCase,
            validateUseCase = validateBusinessRecipientsUseCase,
            resetUseCase = resetBusinessRecipientsUseCase
        )
    }

    fun createPinchToZoomRepository(): PinchToZoomRepository {
        return LegacyPinchToZoomRepository()
    }

    val pinchToZoomRepository: PinchToZoomRepository by lazy {
        LegacyPinchToZoomRepository()
    }

    val observePinchZoomStateUseCase: ObservePinchZoomStateUseCase
        get() = ObservePinchZoomStateUseCase(pinchToZoomRepository)

    val getPinchZoomStateUseCase: GetPinchZoomStateUseCase
        get() = GetPinchZoomStateUseCase(pinchToZoomRepository)

    val calculatePinchScaleUseCase: CalculatePinchScaleUseCase
        get() = CalculatePinchScaleUseCase(pinchToZoomRepository)

    val calculatePinchTranslationUseCase: CalculatePinchTranslationUseCase
        get() = CalculatePinchTranslationUseCase(pinchToZoomRepository)

    val calculatePinchTransformUseCase: CalculatePinchTransformUseCase
        get() = CalculatePinchTransformUseCase(pinchToZoomRepository)

    val calculatePinchImageBoundsUseCase: CalculatePinchImageBoundsUseCase
        get() = CalculatePinchImageBoundsUseCase(pinchToZoomRepository)

    val evaluatePinchGestureUseCase: EvaluatePinchGestureUseCase
        get() = EvaluatePinchGestureUseCase(pinchToZoomRepository)

    val startPinchZoomUseCase: StartPinchZoomUseCase
        get() = StartPinchZoomUseCase(pinchToZoomRepository)

    val updatePinchZoomUseCase: UpdatePinchZoomUseCase
        get() = UpdatePinchZoomUseCase(pinchToZoomRepository)

    val finishPinchZoomUseCase: FinishPinchZoomUseCase
        get() = FinishPinchZoomUseCase(pinchToZoomRepository)

    val resetPinchZoomUseCase: ResetPinchZoomUseCase
        get() = ResetPinchZoomUseCase(pinchToZoomRepository)

    private var cachedPinchToZoomViewModel: PinchToZoomViewModel? = null

    val pinchToZoomViewModel: PinchToZoomViewModel
        get() {
            var vm = cachedPinchToZoomViewModel
            if (vm == null) {
                vm = createPinchToZoomViewModel()
                cachedPinchToZoomViewModel = vm
            }
            return vm
        }

    fun createPinchToZoomViewModel(): PinchToZoomViewModel {
        return PinchToZoomViewModel(
            observeZoomStateUseCase = observePinchZoomStateUseCase,
            getZoomStateUseCase = getPinchZoomStateUseCase,
            calculateScaleUseCase = calculatePinchScaleUseCase,
            calculateTranslationUseCase = calculatePinchTranslationUseCase,
            calculateTransformUseCase = calculatePinchTransformUseCase,
            calculateImageBoundsUseCase = calculatePinchImageBoundsUseCase,
            evaluatePinchGestureUseCase = evaluatePinchGestureUseCase,
            startZoomUseCase = startPinchZoomUseCase,
            updateZoomUseCase = updatePinchZoomUseCase,
            finishZoomUseCase = finishPinchZoomUseCase,
            resetUseCase = resetPinchZoomUseCase
        )
    }

    fun createRecyclerScrollRepository(): RecyclerScrollRepository {
        return LegacyRecyclerScrollRepository()
    }

    val recyclerScrollRepository: RecyclerScrollRepository by lazy {
        LegacyRecyclerScrollRepository()
    }

    val observeRecyclerScrollStateUseCase: ObserveRecyclerScrollStateUseCase
        get() = ObserveRecyclerScrollStateUseCase(recyclerScrollRepository)

    val getRecyclerScrollStateUseCase: GetRecyclerScrollStateUseCase
        get() = GetRecyclerScrollStateUseCase(recyclerScrollRepository)

    val evaluateScrollEligibilityUseCase: EvaluateScrollEligibilityUseCase
        get() = EvaluateScrollEligibilityUseCase(recyclerScrollRepository)

    val calculateScrollAnimationPlanUseCase: CalculateScrollAnimationPlanUseCase
        get() = CalculateScrollAnimationPlanUseCase(recyclerScrollRepository)

    val calculateScrollLengthUseCase: CalculateScrollLengthUseCase
        get() = CalculateScrollLengthUseCase(recyclerScrollRepository)

    val computeScrollViewTranslationsUseCase: ComputeScrollViewTranslationsUseCase
        get() = ComputeScrollViewTranslationsUseCase(recyclerScrollRepository)

    val startRecyclerScrollUseCase: StartRecyclerScrollUseCase
        get() = StartRecyclerScrollUseCase(recyclerScrollRepository)

    val updateRecyclerScrollProgressUseCase: UpdateRecyclerScrollProgressUseCase
        get() = UpdateRecyclerScrollProgressUseCase(recyclerScrollRepository)

    val finishRecyclerScrollUseCase: FinishRecyclerScrollUseCase
        get() = FinishRecyclerScrollUseCase(recyclerScrollRepository)

    val cancelRecyclerScrollUseCase: CancelRecyclerScrollUseCase
        get() = CancelRecyclerScrollUseCase(recyclerScrollRepository)

    val resetRecyclerScrollUseCase: ResetRecyclerScrollUseCase
        get() = ResetRecyclerScrollUseCase(recyclerScrollRepository)

    private var cachedRecyclerScrollViewModel: RecyclerScrollViewModel? = null

    val recyclerScrollViewModel: RecyclerScrollViewModel
        get() {
            var vm = cachedRecyclerScrollViewModel
            if (vm == null) {
                vm = createRecyclerScrollViewModel()
                cachedRecyclerScrollViewModel = vm
            }
            return vm
        }

    fun createRecyclerScrollViewModel(): RecyclerScrollViewModel {
        return RecyclerScrollViewModel(
            observeScrollStateUseCase = observeRecyclerScrollStateUseCase,
            getScrollStateUseCase = getRecyclerScrollStateUseCase,
            evaluateScrollEligibilityUseCase = evaluateScrollEligibilityUseCase,
            calculateScrollAnimationPlanUseCase = calculateScrollAnimationPlanUseCase,
            calculateScrollLengthUseCase = calculateScrollLengthUseCase,
            computeScrollViewTranslationsUseCase = computeScrollViewTranslationsUseCase,
            startRecyclerScrollUseCase = startRecyclerScrollUseCase,
            updateRecyclerScrollProgressUseCase = updateRecyclerScrollProgressUseCase,
            finishRecyclerScrollUseCase = finishRecyclerScrollUseCase,
            cancelRecyclerScrollUseCase = cancelRecyclerScrollUseCase,
            resetRecyclerScrollUseCase = resetRecyclerScrollUseCase
        )
    }

    fun createEmojiEffectsRepository(): EmojiEffectsRepository {
        return LegacyEmojiEffectsRepository()
    }

    val emojiEffectsRepository: EmojiEffectsRepository by lazy {
        LegacyEmojiEffectsRepository()
    }

    val normalizeEmojiUseCase: NormalizeEmojiUseCase
        get() = NormalizeEmojiUseCase()

    val evaluateEmojiSupportUseCase: EvaluateEmojiSupportUseCase
        get() = EvaluateEmojiSupportUseCase(normalizeEmojiUseCase)

    val recordEmojiTapUseCase: RecordEmojiTapUseCase
        get() = RecordEmojiTapUseCase(emojiEffectsRepository, normalizeEmojiUseCase)

    val encodeEmojiInteractionsJsonUseCase: EncodeEmojiInteractionsJsonUseCase
        get() = EncodeEmojiInteractionsJsonUseCase()

    val decodeEmojiInteractionsJsonUseCase: DecodeEmojiInteractionsJsonUseCase
        get() = DecodeEmojiInteractionsJsonUseCase()

    val calculateEmojiBoundsUseCase: CalculateEmojiBoundsUseCase
        get() = CalculateEmojiBoundsUseCase()

    val calculateEmojiOverlayPositionUseCase: CalculateEmojiOverlayPositionUseCase
        get() = CalculateEmojiOverlayPositionUseCase()

    val evaluateAnimationQuotaUseCase: EvaluateAnimationQuotaUseCase
        get() = EvaluateAnimationQuotaUseCase()

    val observeEmojiEffectsStateUseCase: ObserveEmojiEffectsStateUseCase
        get() = ObserveEmojiEffectsStateUseCase(emojiEffectsRepository)

    val getEmojiEffectsStateUseCase: GetEmojiEffectsStateUseCase
        get() = GetEmojiEffectsStateUseCase(emojiEffectsRepository)

    val startEmojiEffectUseCase: StartEmojiEffectUseCase
        get() = StartEmojiEffectUseCase(emojiEffectsRepository, evaluateAnimationQuotaUseCase)

    val updateEmojiEffectProgressUseCase: UpdateEmojiEffectProgressUseCase
        get() = UpdateEmojiEffectProgressUseCase(emojiEffectsRepository)

    val dismissEmojiEffectUseCase: DismissEmojiEffectUseCase
        get() = DismissEmojiEffectUseCase(emojiEffectsRepository)

    val clearEmojiEffectsUseCase: ClearEmojiEffectsUseCase
        get() = ClearEmojiEffectsUseCase(emojiEffectsRepository)

    private var cachedEmojiEffectsViewModel: EmojiEffectsViewModel? = null

    val emojiEffectsViewModel: EmojiEffectsViewModel
        get() {
            var vm = cachedEmojiEffectsViewModel
            if (vm == null) {
                vm = createEmojiEffectsViewModel()
                cachedEmojiEffectsViewModel = vm
            }
            return vm
        }

    fun createEmojiEffectsViewModel(): EmojiEffectsViewModel {
        return EmojiEffectsViewModel(
            observeEmojiEffectsStateUseCase = observeEmojiEffectsStateUseCase,
            recordEmojiTapUseCase = recordEmojiTapUseCase,
            startEmojiEffectUseCase = startEmojiEffectUseCase,
            updateEmojiEffectProgressUseCase = updateEmojiEffectProgressUseCase,
            dismissEmojiEffectUseCase = dismissEmojiEffectUseCase,
            clearEmojiEffectsUseCase = clearEmojiEffectsUseCase,
            repository = emojiEffectsRepository
        )
    }

    fun createMentionsRepository(): MentionsRepository {
        return LegacyMentionsRepository()
    }

    val mentionsRepository: MentionsRepository by lazy {
        LegacyMentionsRepository()
    }

    val validateUsernameUseCase: ValidateUsernameUseCase
        get() = ValidateUsernameUseCase()

    val parseMentionQueryUseCase: ParseMentionQueryUseCase
        get() = ParseMentionQueryUseCase(validateUsernameUseCase)

    val filterMentionsUseCase: FilterMentionsUseCase
        get() = FilterMentionsUseCase()

    val formatMentionReplacementUseCase: FormatMentionReplacementUseCase
        get() = FormatMentionReplacementUseCase()

    val observeMentionsStateUseCase: ObserveMentionsStateUseCase
        get() = ObserveMentionsStateUseCase(mentionsRepository)

    val getMentionsStateUseCase: GetMentionsStateUseCase
        get() = GetMentionsStateUseCase(mentionsRepository)

    val updateMentionQueryUseCase: UpdateMentionQueryUseCase
        get() = UpdateMentionQueryUseCase(mentionsRepository, parseMentionQueryUseCase)

    val setMentionCandidatesUseCase: SetMentionCandidatesUseCase
        get() = SetMentionCandidatesUseCase(mentionsRepository, filterMentionsUseCase)

    val dismissMentionsUseCase: DismissMentionsUseCase
        get() = DismissMentionsUseCase(mentionsRepository)

    val clearMentionsUseCase: ClearMentionsUseCase
        get() = ClearMentionsUseCase(mentionsRepository)

    private var cachedMentionsViewModel: MentionsViewModel? = null

    val mentionsViewModel: MentionsViewModel
        get() {
            var vm = cachedMentionsViewModel
            if (vm == null) {
                vm = createMentionsViewModel()
                cachedMentionsViewModel = vm
            }
            return vm
        }

    fun createMentionsViewModel(): MentionsViewModel {
        return MentionsViewModel(
            observeMentionsStateUseCase = observeMentionsStateUseCase,
            updateMentionQueryUseCase = updateMentionQueryUseCase,
            setMentionCandidatesUseCase = setMentionCandidatesUseCase,
            formatMentionReplacementUseCase = formatMentionReplacementUseCase,
            dismissMentionsUseCase = dismissMentionsUseCase,
            clearMentionsUseCase = clearMentionsUseCase
        )
    }

    fun createSharedMediaRepository(): SharedMediaRepository {
        return LegacySharedMediaRepository(
            groupMediaByMonthUseCase = groupMediaByMonthUseCase,
            calculateMediaSelectionUseCase = calculateMediaSelectionUseCase,
            filterSharedMediaUseCase = filterSharedMediaUseCase
        )
    }

    val sharedMediaRepository: SharedMediaRepository by lazy {
        LegacySharedMediaRepository(
            groupMediaByMonthUseCase = groupMediaByMonthUseCase,
            calculateMediaSelectionUseCase = calculateMediaSelectionUseCase,
            filterSharedMediaUseCase = filterSharedMediaUseCase
        )
    }

    val resolveAvailableTabsUseCase: ResolveAvailableTabsUseCase
        get() = ResolveAvailableTabsUseCase()

    val filterSharedMediaUseCase: FilterSharedMediaUseCase
        get() = FilterSharedMediaUseCase()

    val groupMediaByMonthUseCase: GroupMediaByMonthUseCase
        get() = GroupMediaByMonthUseCase()

    val calculateMediaSelectionUseCase: CalculateMediaSelectionUseCase
        get() = CalculateMediaSelectionUseCase()

    val observeSharedMediaStateUseCase: ObserveSharedMediaStateUseCase
        get() = ObserveSharedMediaStateUseCase(sharedMediaRepository)

    val getSharedMediaStateUseCase: GetSharedMediaStateUseCase
        get() = GetSharedMediaStateUseCase(sharedMediaRepository)

    val selectSharedMediaTabUseCase: SelectSharedMediaTabUseCase
        get() = SelectSharedMediaTabUseCase(sharedMediaRepository)

    val setSharedMediaFilterUseCase: SetSharedMediaFilterUseCase
        get() = SetSharedMediaFilterUseCase(sharedMediaRepository)

    val toggleMediaSelectionUseCase: ToggleMediaSelectionUseCase
        get() = ToggleMediaSelectionUseCase(sharedMediaRepository)

    val clearMediaSelectionUseCase: ClearMediaSelectionUseCase
        get() = ClearMediaSelectionUseCase(sharedMediaRepository)

    private var cachedSharedMediaViewModel: SharedMediaViewModel? = null

    val sharedMediaViewModel: SharedMediaViewModel
        get() {
            var vm = cachedSharedMediaViewModel
            if (vm == null) {
                vm = createSharedMediaViewModel()
                cachedSharedMediaViewModel = vm
            }
            return vm
        }

    fun createSharedMediaViewModel(): SharedMediaViewModel {
        return SharedMediaViewModel(
            observeSharedMediaStateUseCase = observeSharedMediaStateUseCase,
            selectSharedMediaTabUseCase = selectSharedMediaTabUseCase,
            setSharedMediaFilterUseCase = setSharedMediaFilterUseCase,
            toggleMediaSelectionUseCase = toggleMediaSelectionUseCase,
            clearMediaSelectionUseCase = clearMediaSelectionUseCase,
            repository = sharedMediaRepository
        )
    }

    fun createContentPreviewRepository(): ContentPreviewRepository {
        return LegacyContentPreviewRepository()
    }

    val contentPreviewRepository: ContentPreviewRepository by lazy {
        LegacyContentPreviewRepository()
    }

    val evaluatePreviewEligibilityUseCase: EvaluatePreviewEligibilityUseCase
        get() = EvaluatePreviewEligibilityUseCase()

    val calculatePreviewDragUseCase: CalculatePreviewDragUseCase
        get() = CalculatePreviewDragUseCase()

    val resolvePreviewActionsUseCase: ResolvePreviewActionsUseCase
        get() = ResolvePreviewActionsUseCase()

    val observeContentPreviewStateUseCase: ObserveContentPreviewStateUseCase
        get() = ObserveContentPreviewStateUseCase(contentPreviewRepository)

    val getContentPreviewStateUseCase: GetContentPreviewStateUseCase
        get() = GetContentPreviewStateUseCase(contentPreviewRepository)

    val openContentPreviewUseCase: OpenContentPreviewUseCase
        get() = OpenContentPreviewUseCase(
            contentPreviewRepository,
            evaluatePreviewEligibilityUseCase,
            resolvePreviewActionsUseCase
        )

    val updatePreviewDragUseCase: UpdatePreviewDragUseCase
        get() = UpdatePreviewDragUseCase(contentPreviewRepository, calculatePreviewDragUseCase)

    val triggerPreviewActionUseCase: TriggerPreviewActionUseCase
        get() = TriggerPreviewActionUseCase(contentPreviewRepository)

    val dismissContentPreviewUseCase: DismissContentPreviewUseCase
        get() = DismissContentPreviewUseCase(contentPreviewRepository)

    val clearContentPreviewUseCase: ClearContentPreviewUseCase
        get() = ClearContentPreviewUseCase(contentPreviewRepository)

    private var cachedContentPreviewViewModel: ContentPreviewViewModel? = null

    val contentPreviewViewModel: ContentPreviewViewModel
        get() {
            var vm = cachedContentPreviewViewModel
            if (vm == null) {
                vm = createContentPreviewViewModel()
                cachedContentPreviewViewModel = vm
            }
            return vm
        }

    fun createContentPreviewViewModel(): ContentPreviewViewModel {
        return ContentPreviewViewModel(
            observeContentPreviewStateUseCase = observeContentPreviewStateUseCase,
            openContentPreviewUseCase = openContentPreviewUseCase,
            updatePreviewDragUseCase = updatePreviewDragUseCase,
            triggerPreviewActionUseCase = triggerPreviewActionUseCase,
            dismissContentPreviewUseCase = dismissContentPreviewUseCase,
            clearContentPreviewUseCase = clearContentPreviewUseCase
        )
    }

    fun createEmojiPickerRepository(): EmojiPickerRepository {
        return LegacyEmojiPickerRepository(resolveAvailablePickerTabsUseCase)
    }

    val emojiPickerRepository: EmojiPickerRepository by lazy {
        createEmojiPickerRepository()
    }

    val resolveAvailablePickerTabsUseCase: ResolveAvailablePickerTabsUseCase
        get() = ResolveAvailablePickerTabsUseCase()

    val filterEmojiItemsUseCase: FilterEmojiItemsUseCase
        get() = FilterEmojiItemsUseCase()

    val filterStickersUseCase: FilterStickersUseCase
        get() = FilterStickersUseCase()

    val filterGifsUseCase: FilterGifsUseCase
        get() = FilterGifsUseCase()

    val observeEmojiPickerStateUseCase: ObserveEmojiPickerStateUseCase
        get() = ObserveEmojiPickerStateUseCase(emojiPickerRepository)

    val getEmojiPickerStateUseCase: GetEmojiPickerStateUseCase
        get() = GetEmojiPickerStateUseCase(emojiPickerRepository)

    val selectPickerTabUseCase: SelectPickerTabUseCase
        get() = SelectPickerTabUseCase(emojiPickerRepository)

    val updatePickerSearchQueryUseCase: UpdatePickerSearchQueryUseCase
        get() = UpdatePickerSearchQueryUseCase(emojiPickerRepository)

    val toggleStickerFavoriteUseCase: ToggleStickerFavoriteUseCase
        get() = ToggleStickerFavoriteUseCase(emojiPickerRepository)

    val clearRecentPickerItemsUseCase: ClearRecentPickerItemsUseCase
        get() = ClearRecentPickerItemsUseCase(emojiPickerRepository)

    private var cachedEmojiPickerViewModel: EmojiPickerViewModel? = null

    val emojiPickerViewModel: EmojiPickerViewModel
        get() {
            var vm = cachedEmojiPickerViewModel
            if (vm == null) {
                vm = createEmojiPickerViewModel()
                cachedEmojiPickerViewModel = vm
            }
            return vm
        }

    fun createEmojiPickerViewModel(): EmojiPickerViewModel {
        return EmojiPickerViewModel(
            observeStateUseCase = observeEmojiPickerStateUseCase,
            getStateUseCase = getEmojiPickerStateUseCase,
            selectTabUseCase = selectPickerTabUseCase,
            updateSearchQueryUseCase = updatePickerSearchQueryUseCase,
            toggleFavoriteUseCase = toggleStickerFavoriteUseCase,
            clearRecentUseCase = clearRecentPickerItemsUseCase,
            repository = emojiPickerRepository
        )
    }

    fun createChatAttachRepository(): ChatAttachRepository {
        return LegacyChatAttachRepository()
    }

    val chatAttachRepository: ChatAttachRepository by lazy {
        createChatAttachRepository()
    }

    val resolveAvailableAttachLayoutsUseCase: ResolveAvailableAttachLayoutsUseCase
        get() = ResolveAvailableAttachLayoutsUseCase()

    val calculateAttachCaptionLimitUseCase: CalculateAttachCaptionLimitUseCase
        get() = CalculateAttachCaptionLimitUseCase()

    val toggleAttachItemSelectionUseCase: ToggleAttachItemSelectionUseCase
        get() = ToggleAttachItemSelectionUseCase(chatAttachRepository)

    val validateSendOptionsUseCase: ValidateSendOptionsUseCase
        get() = ValidateSendOptionsUseCase()

    val observeChatAttachStateUseCase: ObserveChatAttachStateUseCase
        get() = ObserveChatAttachStateUseCase(chatAttachRepository)

    val getChatAttachStateUseCase: GetChatAttachStateUseCase
        get() = GetChatAttachStateUseCase(chatAttachRepository)

    val selectAttachLayoutUseCase: SelectAttachLayoutUseCase
        get() = SelectAttachLayoutUseCase(chatAttachRepository)

    val updateAttachSendOptionsUseCase: UpdateAttachSendOptionsUseCase
        get() = UpdateAttachSendOptionsUseCase(chatAttachRepository)

    val clearAttachSelectionUseCase: ClearAttachSelectionUseCase
        get() = ClearAttachSelectionUseCase(chatAttachRepository)

    val openChatAttachAlertUseCase: OpenChatAttachAlertUseCase
        get() = OpenChatAttachAlertUseCase(chatAttachRepository, resolveAvailableAttachLayoutsUseCase)

    private var cachedChatAttachViewModel: ChatAttachViewModel? = null

    val chatAttachViewModel: ChatAttachViewModel
        get() {
            var vm = cachedChatAttachViewModel
            if (vm == null) {
                vm = createChatAttachViewModel()
                cachedChatAttachViewModel = vm
            }
            return vm
        }

    fun createChatAttachViewModel(): ChatAttachViewModel {
        return ChatAttachViewModel(
            observeStateUseCase = observeChatAttachStateUseCase,
            openAlertUseCase = openChatAttachAlertUseCase,
            selectLayoutUseCase = selectAttachLayoutUseCase,
            toggleSelectionUseCase = toggleAttachItemSelectionUseCase,
            updateSendOptionsUseCase = updateAttachSendOptionsUseCase,
            clearSelectionUseCase = clearAttachSelectionUseCase,
            calculateCaptionLimitUseCase = calculateAttachCaptionLimitUseCase,
            repository = chatAttachRepository
        )
    }

    private var customPhotoViewerRepository: PhotoViewerRepository? = null

    var photoViewerRepository: PhotoViewerRepository
        get() = customPhotoViewerRepository ?: LegacyPhotoViewerRepository(
            pagingUseCase = calculateMediaPagingUseCase,
            zoomUseCase = calculateZoomTransformUseCase,
            actionsUseCase = validateViewerActionsUseCase
        )
        set(value) {
            customPhotoViewerRepository = value
        }

    val calculateMediaPagingUseCase: CalculateMediaPagingUseCase
        get() = CalculateMediaPagingUseCase()

    val calculateZoomTransformUseCase: CalculateZoomTransformUseCase
        get() = CalculateZoomTransformUseCase()

    val validateViewerActionsUseCase: ValidateViewerActionsUseCase
        get() = ValidateViewerActionsUseCase()

    val resolveMediaQualityUseCase: ResolveMediaQualityUseCase
        get() = ResolveMediaQualityUseCase()

    val observePhotoViewerStateUseCase: ObservePhotoViewerStateUseCase
        get() = ObservePhotoViewerStateUseCase(photoViewerRepository)

    val getPhotoViewerStateUseCase: GetPhotoViewerStateUseCase
        get() = GetPhotoViewerStateUseCase(photoViewerRepository)

    val openPhotoViewerUseCase: OpenPhotoViewerUseCase
        get() = OpenPhotoViewerUseCase(photoViewerRepository)

    val navigatePhotoViewerUseCase: NavigatePhotoViewerUseCase
        get() = NavigatePhotoViewerUseCase(photoViewerRepository)

    val updatePlaybackStateUseCase: UpdatePlaybackStateUseCase
        get() = UpdatePlaybackStateUseCase(photoViewerRepository)

    val closePhotoViewerUseCase: ClosePhotoViewerUseCase
        get() = ClosePhotoViewerUseCase(photoViewerRepository)

    private var cachedPhotoViewerViewModel: PhotoViewerViewModel? = null

    val photoViewerViewModel: PhotoViewerViewModel
        get() {
            var vm = cachedPhotoViewerViewModel
            if (vm == null) {
                vm = createPhotoViewerViewModel()
                cachedPhotoViewerViewModel = vm
            }
            return vm
        }

    fun createPhotoViewerViewModel(): PhotoViewerViewModel {
        return PhotoViewerViewModel(
            repository = photoViewerRepository
        )
    }

    private var customChatInputRepository: ChatInputRepository? = null

    var chatInputRepository: ChatInputRepository
        get() = customChatInputRepository ?: LegacyChatInputRepository(
            sendButtonStateUseCase = calculateSendButtonStateUseCase
        )
        set(value) {
            customChatInputRepository = value
        }

    val calculateSendButtonStateUseCase: CalculateSendButtonStateUseCase
        get() = CalculateSendButtonStateUseCase()

    val formatTextSelectionUseCase: FormatTextSelectionUseCase
        get() = FormatTextSelectionUseCase()

    val validateVoiceRecordActionUseCase: ValidateVoiceRecordActionUseCase
        get() = ValidateVoiceRecordActionUseCase()

    val resolvePanelVisibilityUseCase: ResolvePanelVisibilityUseCase
        get() = ResolvePanelVisibilityUseCase()

    val observeChatInputStateUseCase: ObserveChatInputStateUseCase
        get() = ObserveChatInputStateUseCase(chatInputRepository)

    val getChatInputStateUseCase: GetChatInputStateUseCase
        get() = GetChatInputStateUseCase(chatInputRepository)

    val setChatInputTextUseCase: SetChatInputTextUseCase
        get() = SetChatInputTextUseCase(chatInputRepository)

    val setChatInputPanelModeUseCase: SetChatInputPanelModeUseCase
        get() = SetChatInputPanelModeUseCase(chatInputRepository)

    val setChatInputReplyUseCase: SetChatInputReplyUseCase
        get() = SetChatInputReplyUseCase(chatInputRepository)

    val clearChatInputReplyUseCase: ClearChatInputReplyUseCase
        get() = ClearChatInputReplyUseCase(chatInputRepository)

    private var cachedChatInputViewModel: ChatInputViewModel? = null

    val chatInputViewModel: ChatInputViewModel
        get() {
            var vm = cachedChatInputViewModel
            if (vm == null) {
                vm = createChatInputViewModel()
                cachedChatInputViewModel = vm
            }
            return vm
        }

    fun createChatInputViewModel(): ChatInputViewModel {
        return ChatInputViewModel(
            repository = chatInputRepository,
            formatUseCase = formatTextSelectionUseCase,
            resolvePanelUseCase = resolvePanelVisibilityUseCase
        )
    }

    val audioPlayerRepository: AudioPlayerRepository by lazy {
        LegacyAudioPlayerRepository(account)
    }

    val observeAudioPlaybackStateUseCase: ObservePlaybackStateUseCase
        get() = ObservePlaybackStateUseCase(audioPlayerRepository)

    val getAudioPlaybackStateUseCase: GetPlaybackStateUseCase
        get() = GetPlaybackStateUseCase(audioPlayerRepository)

    val playTrackUseCase: PlayTrackUseCase
        get() = PlayTrackUseCase(audioPlayerRepository)

    val toggleAudioPlayPauseUseCase: TogglePlayPauseUseCase
        get() = TogglePlayPauseUseCase(audioPlayerRepository)

    val seekAudioUseCase: SeekAudioUseCase
        get() = SeekAudioUseCase(audioPlayerRepository)

    val navigatePlaylistUseCase: NavigatePlaylistUseCase
        get() = NavigatePlaylistUseCase(audioPlayerRepository)

    val cyclePlaybackSpeedUseCase: CyclePlaybackSpeedUseCase
        get() = CyclePlaybackSpeedUseCase(audioPlayerRepository)

    val cycleRepeatModeUseCase: CycleRepeatModeUseCase
        get() = CycleRepeatModeUseCase(audioPlayerRepository)

    val toggleShuffleUseCase: ToggleShuffleUseCase
        get() = ToggleShuffleUseCase(audioPlayerRepository)

    val handleProximitySensorUseCase: HandleProximitySensorUseCase
        get() = HandleProximitySensorUseCase(audioPlayerRepository)

    val configureEqualizerUseCase: ConfigureEqualizerUseCase
        get() = ConfigureEqualizerUseCase(audioPlayerRepository)

    private var cachedAudioPlayerViewModel: AudioPlayerViewModel? = null

    val audioPlayerViewModel: AudioPlayerViewModel
        get() {
            var vm = cachedAudioPlayerViewModel
            if (vm == null) {
                vm = createAudioPlayerViewModel()
                cachedAudioPlayerViewModel = vm
            }
            return vm
        }

    fun createAudioPlayerViewModel(): AudioPlayerViewModel {
        return AudioPlayerViewModel(
            observePlaybackStateUseCase = observeAudioPlaybackStateUseCase,
            getPlaybackStateUseCase = getAudioPlaybackStateUseCase,
            playTrackUseCase = playTrackUseCase,
            togglePlayPauseUseCase = toggleAudioPlayPauseUseCase,
            seekAudioUseCase = seekAudioUseCase,
            navigatePlaylistUseCase = navigatePlaylistUseCase,
            cyclePlaybackSpeedUseCase = cyclePlaybackSpeedUseCase,
            cycleRepeatModeUseCase = cycleRepeatModeUseCase,
            toggleShuffleUseCase = toggleShuffleUseCase,
            handleProximitySensorUseCase = handleProximitySensorUseCase,
            configureEqualizerUseCase = configureEqualizerUseCase
        )
    }

    val sendMessagesRepository: SendMessagesRepository by lazy {
        LegacySendMessagesRepository(account)
    }

    val sendTextMessageUseCase: SendTextMessageUseCase
        get() = SendTextMessageUseCase(sendMessagesRepository)

    val sendMediaMessageUseCase: SendMediaMessageUseCase
        get() = SendMediaMessageUseCase(sendMessagesRepository)

    val sendMediaAlbumUseCase: SendMediaAlbumUseCase
        get() = SendMediaAlbumUseCase(sendMessagesRepository)

    val forwardMessagesUseCase: ForwardMessagesUseCase
        get() = ForwardMessagesUseCase(sendMessagesRepository)

    val retrySendMessageUseCase: RetrySendMessageUseCase
        get() = RetrySendMessageUseCase(sendMessagesRepository)

    val cancelSendMessageUseCase: CancelSendMessageUseCase
        get() = CancelSendMessageUseCase(sendMessagesRepository)

    val observePendingSendsUseCase: ObservePendingSendsUseCase
        get() = ObservePendingSendsUseCase(sendMessagesRepository)

    private var cachedSendMessagesViewModel: SendMessagesViewModel? = null

    val sendMessagesViewModel: SendMessagesViewModel
        get() {
            var vm = cachedSendMessagesViewModel
            if (vm == null) {
                vm = createSendMessagesViewModel()
                cachedSendMessagesViewModel = vm
            }
            return vm
        }

    fun createSendMessagesViewModel(): SendMessagesViewModel {
        return SendMessagesViewModel(
            sendTextMessageUseCase = sendTextMessageUseCase,
            sendMediaMessageUseCase = sendMediaMessageUseCase,
            sendMediaAlbumUseCase = sendMediaAlbumUseCase,
            forwardMessagesUseCase = forwardMessagesUseCase,
            retrySendMessageUseCase = retrySendMessageUseCase,
            cancelSendMessageUseCase = cancelSendMessageUseCase,
            observePendingSendsUseCase = observePendingSendsUseCase
        )
    }

    val imageLoaderRepository: ImageLoaderRepository by lazy {
        LegacyImageLoaderRepository(account)
    }

    val parseImageFilterUseCase: ParseImageFilterUseCase
        get() = ParseImageFilterUseCase()

    val formatImageFilterUseCase: FormatImageFilterUseCase
        get() = FormatImageFilterUseCase()

    val buildImageCacheKeyUseCase: BuildImageCacheKeyUseCase
        get() = BuildImageCacheKeyUseCase()

    val calculateImageDownscaleUseCase: CalculateImageDownscaleUseCase
        get() = CalculateImageDownscaleUseCase()

    val evaluateImageCacheEligibilityUseCase: EvaluateImageCacheEligibilityUseCase
        get() = EvaluateImageCacheEligibilityUseCase()

    val observeImageLoaderStateUseCase: ObserveImageLoaderStateUseCase
        get() = ObserveImageLoaderStateUseCase(imageLoaderRepository)

    val getImageLoaderStateUseCase: GetImageLoaderStateUseCase
        get() = GetImageLoaderStateUseCase(imageLoaderRepository)

    val enqueueImageRequestUseCase: EnqueueImageRequestUseCase
        get() = EnqueueImageRequestUseCase(
            repository = imageLoaderRepository,
            evaluateTier = evaluateImageCacheEligibilityUseCase,
            buildKey = buildImageCacheKeyUseCase
        )

    val cancelImageRequestUseCase: CancelImageRequestUseCase
        get() = CancelImageRequestUseCase(imageLoaderRepository)

    val trimImageMemoryUseCase: TrimImageMemoryUseCase
        get() = TrimImageMemoryUseCase(imageLoaderRepository)

    val clearImageCacheUseCase: ClearImageCacheUseCase
        get() = ClearImageCacheUseCase(imageLoaderRepository)

    private var cachedImageLoaderViewModel: ImageLoaderViewModel? = null

    val imageLoaderViewModel: ImageLoaderViewModel
        get() {
            var vm = cachedImageLoaderViewModel
            if (vm == null) {
                vm = createImageLoaderViewModel()
                cachedImageLoaderViewModel = vm
            }
            return vm
        }

    fun createImageLoaderViewModel(): ImageLoaderViewModel {
        return ImageLoaderViewModel(
            observeImageLoaderStateUseCase = observeImageLoaderStateUseCase,
            enqueueImageRequestUseCase = enqueueImageRequestUseCase,
            cancelImageRequestUseCase = cancelImageRequestUseCase,
            trimImageMemoryUseCase = trimImageMemoryUseCase,
            clearImageCacheUseCase = clearImageCacheUseCase
        )
    }

    val downloadManagerRepository: DownloadManagerRepository by lazy {
        LegacyDownloadManagerRepository(account)
    }

    val evaluateAutoDownloadEligibilityUseCase: EvaluateAutoDownloadEligibilityUseCase
        get() = EvaluateAutoDownloadEligibilityUseCase(downloadManagerRepository)

    val observeDownloadManagerStateUseCase: ObserveDownloadManagerStateUseCase
        get() = ObserveDownloadManagerStateUseCase(downloadManagerRepository)

    val getDownloadManagerStateUseCase: GetDownloadManagerStateUseCase
        get() = GetDownloadManagerStateUseCase(downloadManagerRepository)

    val enqueueDownloadUseCase: EnqueueDownloadUseCase
        get() = EnqueueDownloadUseCase(downloadManagerRepository)

    val pauseDownloadUseCase: PauseDownloadUseCase
        get() = PauseDownloadUseCase(downloadManagerRepository)

    val resumeDownloadUseCase: ResumeDownloadUseCase
        get() = ResumeDownloadUseCase(downloadManagerRepository)

    val cancelDownloadUseCase: CancelDownloadUseCase
        get() = CancelDownloadUseCase(downloadManagerRepository)

    val retryDownloadUseCase: RetryDownloadUseCase
        get() = RetryDownloadUseCase(downloadManagerRepository)

    val clearRecentDownloadsUseCase: ClearRecentDownloadsUseCase
        get() = ClearRecentDownloadsUseCase(downloadManagerRepository)

    val markDownloadsAsViewedUseCase: MarkDownloadsAsViewedUseCase
        get() = MarkDownloadsAsViewedUseCase(downloadManagerRepository)

    val updateDownloadProgressUseCase: UpdateDownloadProgressUseCase
        get() = UpdateDownloadProgressUseCase(downloadManagerRepository)

    val setDownloadNetworkTypeUseCase: SetDownloadNetworkTypeUseCase
        get() = SetDownloadNetworkTypeUseCase(downloadManagerRepository)

    val updateDownloadPresetUseCase: UpdateDownloadPresetUseCase
        get() = UpdateDownloadPresetUseCase(downloadManagerRepository)

    private var cachedDownloadManagerViewModel: DownloadManagerViewModel? = null

    val downloadManagerViewModel: DownloadManagerViewModel
        get() {
            var vm = cachedDownloadManagerViewModel
            if (vm == null) {
                vm = createDownloadManagerViewModel()
                cachedDownloadManagerViewModel = vm
            }
            return vm
        }

    fun createDownloadManagerViewModel(): DownloadManagerViewModel {
        return DownloadManagerViewModel(
            observeDownloadManagerStateUseCase = observeDownloadManagerStateUseCase,
            enqueueDownloadUseCase = enqueueDownloadUseCase,
            pauseDownloadUseCase = pauseDownloadUseCase,
            resumeDownloadUseCase = resumeDownloadUseCase,
            cancelDownloadUseCase = cancelDownloadUseCase,
            retryDownloadUseCase = retryDownloadUseCase,
            clearRecentDownloadsUseCase = clearRecentDownloadsUseCase,
            markDownloadsAsViewedUseCase = markDownloadsAsViewedUseCase,
            setDownloadNetworkTypeUseCase = setDownloadNetworkTypeUseCase,
            updateDownloadPresetUseCase = updateDownloadPresetUseCase
        )
    }

    val localizationRepository: LocalizationRepository by lazy {
        LegacyLocalizationRepository(account)
    }

    val resolvePluralQuantityUseCase: ResolvePluralQuantityUseCase
        get() = ResolvePluralQuantityUseCase()

    val formatRelativeTimestampUseCase: FormatRelativeTimestampUseCase
        get() = FormatRelativeTimestampUseCase()

    val formatFullNameUseCase: FormatFullNameUseCase
        get() = FormatFullNameUseCase()

    val formatNumberWithSuffixUseCase: FormatNumberWithSuffixUseCase
        get() = FormatNumberWithSuffixUseCase()

    val detectRtlLanguageUseCase: DetectRtlLanguageUseCase
        get() = DetectRtlLanguageUseCase()

    val observeLocalizationStateUseCase: ObserveLocalizationStateUseCase
        get() = ObserveLocalizationStateUseCase(localizationRepository)

    val getLocalizationStateUseCase: GetLocalizationStateUseCase
        get() = GetLocalizationStateUseCase(localizationRepository)

    val applyLocaleUseCase: ApplyLocaleUseCase
        get() = ApplyLocaleUseCase(localizationRepository, detectRtlLanguageUseCase)

    val toggle24HourFormatUseCase: Toggle24HourFormatUseCase
        get() = Toggle24HourFormatUseCase(localizationRepository)

    val setNameDisplayOrderUseCase: SetNameDisplayOrderUseCase
        get() = SetNameDisplayOrderUseCase(localizationRepository)

    private var cachedLocalizationViewModel: LocalizationViewModel? = null

    val localizationViewModel: LocalizationViewModel
        get() {
            var vm = cachedLocalizationViewModel
            if (vm == null) {
                vm = createLocalizationViewModel()
                cachedLocalizationViewModel = vm
            }
            return vm
        }

    fun createLocalizationViewModel(): LocalizationViewModel {
        return LocalizationViewModel(
            observeLocalizationStateUseCase = observeLocalizationStateUseCase,
            applyLocaleUseCase = applyLocaleUseCase,
            toggle24HourFormatUseCase = toggle24HourFormatUseCase,
            setNameDisplayOrderUseCase = setNameDisplayOrderUseCase,
            repository = localizationRepository
        )
    }

    val ringtoneRepository: RingtoneRepository by lazy {
        LegacyRingtoneRepository(account)
    }

    val validateRingtoneEligibilityUseCase: ValidateRingtoneEligibilityUseCase
        get() = ValidateRingtoneEligibilityUseCase()

    val observeRingtonesUseCase: ObserveRingtonesUseCase
        get() = ObserveRingtonesUseCase(ringtoneRepository)

    val observeRingtoneStateUseCase: ObserveRingtoneStateUseCase
        get() = ObserveRingtoneStateUseCase(ringtoneRepository)

    val getRingtonesUseCase: GetRingtonesUseCase
        get() = GetRingtonesUseCase(ringtoneRepository)

    val getRingtoneByIdUseCase: GetRingtoneByIdUseCase
        get() = GetRingtoneByIdUseCase(ringtoneRepository)

    val getRingtoneSoundPathUseCase: GetRingtoneSoundPathUseCase
        get() = GetRingtoneSoundPathUseCase(ringtoneRepository)

    val addRingtoneUseCase: AddRingtoneUseCase
        get() = AddRingtoneUseCase(ringtoneRepository, validateRingtoneEligibilityUseCase)

    val removeRingtoneUseCase: RemoveRingtoneUseCase
        get() = RemoveRingtoneUseCase(ringtoneRepository)

    val saveRingtoneFromDocumentUseCase: SaveRingtoneFromDocumentUseCase
        get() = SaveRingtoneFromDocumentUseCase(ringtoneRepository, validateRingtoneEligibilityUseCase)

    val uploadRingtoneUseCase: UploadRingtoneUseCase
        get() = UploadRingtoneUseCase(ringtoneRepository, validateRingtoneEligibilityUseCase)

    val cancelRingtoneUploadUseCase: CancelRingtoneUploadUseCase
        get() = CancelRingtoneUploadUseCase(ringtoneRepository)

    val refreshRingtonesUseCase: RefreshRingtonesUseCase
        get() = RefreshRingtonesUseCase(ringtoneRepository)

    val selectRingtoneUseCase: SelectRingtoneUseCase
        get() = SelectRingtoneUseCase(ringtoneRepository)

    private var cachedRingtoneViewModel: RingtoneViewModel? = null

    val ringtoneViewModel: RingtoneViewModel
        get() {
            var vm = cachedRingtoneViewModel
            if (vm == null) {
                vm = createRingtoneViewModel()
                cachedRingtoneViewModel = vm
            }
            return vm
        }

    fun createRingtoneViewModel(): RingtoneViewModel {
        return RingtoneViewModel(
            observeRingtoneStateUseCase = observeRingtoneStateUseCase,
            refreshRingtonesUseCase = refreshRingtonesUseCase,
            selectRingtoneUseCase = selectRingtoneUseCase,
            removeRingtoneUseCase = removeRingtoneUseCase,
            saveRingtoneFromDocumentUseCase = saveRingtoneFromDocumentUseCase,
            uploadRingtoneUseCase = uploadRingtoneUseCase,
            cancelRingtoneUploadUseCase = cancelRingtoneUploadUseCase
        )
    }

    private var customNetworkStatsRepository: NetworkStatsRepository? = null

    var networkStatsRepository: NetworkStatsRepository
        get() = customNetworkStatsRepository ?: LegacyNetworkStatsRepository(account)
        set(value) {
            customNetworkStatsRepository = value
        }

    val observeNetworkStatsUseCase: ObserveNetworkStatsUseCase
        get() = ObserveNetworkStatsUseCase(networkStatsRepository)

    val observeAllNetworkStatsUseCase: ObserveAllNetworkStatsUseCase
        get() = ObserveAllNetworkStatsUseCase(networkStatsRepository)

    val getNetworkStatsUseCase: GetNetworkStatsUseCase
        get() = GetNetworkStatsUseCase(networkStatsRepository)

    val getAllNetworkStatsUseCase: GetAllNetworkStatsUseCase
        get() = GetAllNetworkStatsUseCase(networkStatsRepository)

    val incrementTrafficBytesUseCase: IncrementTrafficBytesUseCase
        get() = IncrementTrafficBytesUseCase(networkStatsRepository)

    val incrementTrafficItemsUseCase: IncrementTrafficItemsUseCase
        get() = IncrementTrafficItemsUseCase(networkStatsRepository)

    val incrementCallsTimeUseCase: IncrementCallsTimeUseCase
        get() = IncrementCallsTimeUseCase(networkStatsRepository)

    val resetNetworkStatsUseCase: ResetNetworkStatsUseCase
        get() = ResetNetworkStatsUseCase(networkStatsRepository)

    val refreshNetworkStatsUseCase: RefreshNetworkStatsUseCase
        get() = RefreshNetworkStatsUseCase(networkStatsRepository)

    val calculateMessagesTrafficUseCase: CalculateMessagesTrafficUseCase
        get() = CalculateMessagesTrafficUseCase()

    val formatTrafficBytesUseCase: FormatTrafficBytesUseCase
        get() = FormatTrafficBytesUseCase()

    val formatCallsDurationUseCase: FormatCallsDurationUseCase
        get() = FormatCallsDurationUseCase()

    private var cachedNetworkStatsViewModel: NetworkStatsViewModel? = null

    val networkStatsViewModel: NetworkStatsViewModel
        get() {
            var vm = cachedNetworkStatsViewModel
            if (vm == null) {
                vm = createNetworkStatsViewModel()
                cachedNetworkStatsViewModel = vm
            }
            return vm
        }

    fun createNetworkStatsViewModel(): NetworkStatsViewModel {
        return NetworkStatsViewModel(
            observeAllNetworkStatsUseCase = observeAllNetworkStatsUseCase,
            getNetworkStatsUseCase = getNetworkStatsUseCase,
            resetNetworkStatsUseCase = resetNetworkStatsUseCase,
            refreshNetworkStatsUseCase = refreshNetworkStatsUseCase,
            incrementTrafficBytesUseCase = incrementTrafficBytesUseCase,
            incrementTrafficItemsUseCase = incrementTrafficItemsUseCase,
            incrementCallsTimeUseCase = incrementCallsTimeUseCase,
            formatTrafficBytesUseCase = formatTrafficBytesUseCase,
            formatCallsDurationUseCase = formatCallsDurationUseCase
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
