package org.telegram.messenger.core.di

import java.util.concurrent.ConcurrentHashMap
import org.telegram.messenger.UserConfig
import org.telegram.messenger.feature.business.billing.domain.repository.BillingRepository
import org.telegram.messenger.feature.business.billing.domain.usecase.FormatCurrencyUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.GetBillingStateUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.GetCurrencyExpUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.GetPremiumProductUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.ManageSubscriptionUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.ObserveBillingStateUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.QueryBillingPurchasesUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.StartBillingConnectionUseCase
import org.telegram.messenger.feature.business.billing.presentation.BillingViewModel
import org.telegram.messenger.feature.business.botstars.domain.repository.BotStarsRepository
import org.telegram.messenger.feature.business.botstars.domain.usecase.GetAdminedBotsAndChannelsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.GetBotStarsStatsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.GetTonStatsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.LoadBotTransactionsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.LoadConnectedStarBotsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.LoadSuggestedStarBotsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.ObserveBotStarsStatsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.ObserveBotTransactionsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.ObserveConnectedStarBotsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.ObserveTonStatsUseCase
import org.telegram.messenger.feature.business.botstars.presentation.BotStarsViewModel
import org.telegram.messenger.feature.business.businessbots.domain.repository.BusinessBotsRepository
import org.telegram.messenger.feature.business.businessbots.domain.usecase.DeleteConnectedBotUseCase
import org.telegram.messenger.feature.business.businessbots.domain.usecase.FindConnectedBotUseCase
import org.telegram.messenger.feature.business.businessbots.domain.usecase.GetConnectedBotsUseCase
import org.telegram.messenger.feature.business.businessbots.domain.usecase.LoadConnectedBotsUseCase
import org.telegram.messenger.feature.business.businessbots.domain.usecase.ObserveConnectedBotsUseCase
import org.telegram.messenger.feature.business.businessbots.domain.usecase.UpdateConnectedBotUseCase
import org.telegram.messenger.feature.business.businessbots.presentation.BusinessBotsViewModel
import org.telegram.messenger.feature.business.businesslinks.domain.repository.BusinessLinksRepository
import org.telegram.messenger.feature.business.businesslinks.domain.usecase.CanAddNewBusinessLinkUseCase
import org.telegram.messenger.feature.business.businesslinks.domain.usecase.CreateBusinessLinkUseCase
import org.telegram.messenger.feature.business.businesslinks.domain.usecase.DeleteBusinessLinkUseCase
import org.telegram.messenger.feature.business.businesslinks.domain.usecase.EditBusinessLinkUseCase
import org.telegram.messenger.feature.business.businesslinks.domain.usecase.FindBusinessLinkUseCase
import org.telegram.messenger.feature.business.businesslinks.domain.usecase.GetBusinessLinksUseCase
import org.telegram.messenger.feature.business.businesslinks.domain.usecase.LoadBusinessLinksUseCase
import org.telegram.messenger.feature.business.businesslinks.domain.usecase.ObserveBusinessLinksUseCase
import org.telegram.messenger.feature.business.businesslinks.presentation.BusinessLinksViewModel
import org.telegram.messenger.feature.business.businessrecipients.domain.repository.BusinessRecipientsRepository
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.AddExcludedUsersUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.AddSelectedUsersUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.CheckRecipientsChangesUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.GetBusinessRecipientsUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.ObserveBusinessRecipientsUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.RemoveExcludedUserUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.RemoveSelectedUserUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.ResetBusinessRecipientsUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.SetBusinessRecipientsUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.ToggleExcludeSelectedUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.ToggleRecipientFilterUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.ValidateBusinessRecipientsUseCase
import org.telegram.messenger.feature.business.businessrecipients.presentation.BusinessRecipientsViewModel
import org.telegram.messenger.feature.business.di.BusinessContainer
import org.telegram.messenger.feature.business.giftauctions.domain.repository.GiftAuctionsRepository
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.GetActiveAuctionsUseCase
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.GetAuctionByIdUseCase
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.GetAuctionBySlugUseCase
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.LoadAuctionAcquiredGiftsUseCase
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.ObserveActiveAuctionsUseCase
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.ObserveAuctionUseCase
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.RefreshActiveAuctionsUseCase
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.SendAuctionBidUseCase
import org.telegram.messenger.feature.business.giftauctions.presentation.GiftAuctionsViewModel
import org.telegram.messenger.feature.business.payments.domain.repository.PaymentsRepository
import org.telegram.messenger.feature.business.payments.domain.usecase.GetStarSubscriptionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.GetStarTopupOptionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.GetStarTransactionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.GetStarsBalanceUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.ObserveStarSubscriptionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.ObserveStarTransactionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.ObserveStarsBalanceUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.RefreshStarSubscriptionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.RefreshStarTransactionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.RefreshStarsBalanceUseCase
import org.telegram.messenger.feature.business.payments.presentation.PaymentsViewModel
import org.telegram.messenger.feature.business.quickreplies.domain.repository.QuickRepliesRepository
import org.telegram.messenger.feature.business.quickreplies.domain.usecase.CanAddNewQuickReplyUseCase
import org.telegram.messenger.feature.business.quickreplies.domain.usecase.CheckQuickReplyNameBusyUseCase
import org.telegram.messenger.feature.business.quickreplies.domain.usecase.DeleteQuickRepliesUseCase
import org.telegram.messenger.feature.business.quickreplies.domain.usecase.FindQuickReplyUseCase
import org.telegram.messenger.feature.business.quickreplies.domain.usecase.GetQuickRepliesUseCase
import org.telegram.messenger.feature.business.quickreplies.domain.usecase.LoadQuickRepliesUseCase
import org.telegram.messenger.feature.business.quickreplies.domain.usecase.ObserveQuickRepliesUseCase
import org.telegram.messenger.feature.business.quickreplies.domain.usecase.RenameQuickReplyUseCase
import org.telegram.messenger.feature.business.quickreplies.domain.usecase.ReorderQuickRepliesUseCase
import org.telegram.messenger.feature.business.quickreplies.domain.usecase.SendQuickReplyUseCase
import org.telegram.messenger.feature.business.quickreplies.presentation.QuickRepliesViewModel
import org.telegram.messenger.feature.business.stargifts.domain.repository.StarGiftsRepository
import org.telegram.messenger.feature.business.stargifts.domain.usecase.GetStarGiftByIdUseCase
import org.telegram.messenger.feature.business.stargifts.domain.usecase.GetStarGiftsCatalogUseCase
import org.telegram.messenger.feature.business.stargifts.domain.usecase.LoadProfileGiftsUseCase
import org.telegram.messenger.feature.business.stargifts.domain.usecase.ObserveProfileGiftsUseCase
import org.telegram.messenger.feature.business.stargifts.domain.usecase.ObserveStarGiftsCatalogUseCase
import org.telegram.messenger.feature.business.stargifts.domain.usecase.ToggleHideProfileGiftUseCase
import org.telegram.messenger.feature.business.stargifts.domain.usecase.TogglePinProfileGiftUseCase
import org.telegram.messenger.feature.business.stargifts.presentation.StarGiftsViewModel
import org.telegram.messenger.feature.business.timezones.domain.repository.TimezonesRepository
import org.telegram.messenger.feature.business.timezones.domain.usecase.FindTimezoneUseCase
import org.telegram.messenger.feature.business.timezones.domain.usecase.GetSystemTimezoneIdUseCase
import org.telegram.messenger.feature.business.timezones.domain.usecase.GetTimezoneNameUseCase
import org.telegram.messenger.feature.business.timezones.domain.usecase.GetTimezonesUseCase
import org.telegram.messenger.feature.business.timezones.domain.usecase.LoadTimezonesUseCase
import org.telegram.messenger.feature.business.timezones.domain.usecase.ObserveTimezonesUseCase
import org.telegram.messenger.feature.business.timezones.presentation.TimezonesViewModel
import org.telegram.messenger.feature.media.audioplayer.domain.repository.AudioPlayerRepository
import org.telegram.messenger.feature.media.audioplayer.domain.usecase.ConfigureEqualizerUseCase
import org.telegram.messenger.feature.media.audioplayer.domain.usecase.CyclePlaybackSpeedUseCase
import org.telegram.messenger.feature.media.audioplayer.domain.usecase.CycleRepeatModeUseCase
import org.telegram.messenger.feature.media.audioplayer.domain.usecase.GetPlaybackStateUseCase
import org.telegram.messenger.feature.media.audioplayer.domain.usecase.HandleProximitySensorUseCase
import org.telegram.messenger.feature.media.audioplayer.domain.usecase.NavigatePlaylistUseCase
import org.telegram.messenger.feature.media.audioplayer.domain.usecase.ObservePlaybackStateUseCase
import org.telegram.messenger.feature.media.audioplayer.domain.usecase.PlayTrackUseCase
import org.telegram.messenger.feature.media.audioplayer.domain.usecase.SeekAudioUseCase
import org.telegram.messenger.feature.media.audioplayer.domain.usecase.TogglePlayPauseUseCase
import org.telegram.messenger.feature.media.audioplayer.domain.usecase.ToggleShuffleUseCase
import org.telegram.messenger.feature.media.audioplayer.presentation.AudioPlayerViewModel
import org.telegram.messenger.feature.media.autodeletemedia.domain.repository.AutoDeleteMediaRepository
import org.telegram.messenger.feature.media.autodeletemedia.domain.usecase.CalculateEvictionCandidatesUseCase
import org.telegram.messenger.feature.media.autodeletemedia.domain.usecase.CheckShouldRunCleanupUseCase
import org.telegram.messenger.feature.media.autodeletemedia.domain.usecase.GetAutoDeleteStateUseCase
import org.telegram.messenger.feature.media.autodeletemedia.domain.usecase.IsFileLockedUseCase
import org.telegram.messenger.feature.media.autodeletemedia.domain.usecase.LockFileUseCase
import org.telegram.messenger.feature.media.autodeletemedia.domain.usecase.ObserveAutoDeleteStateUseCase
import org.telegram.messenger.feature.media.autodeletemedia.domain.usecase.RunAutoDeleteCleanupUseCase
import org.telegram.messenger.feature.media.autodeletemedia.domain.usecase.UnlockFileUseCase
import org.telegram.messenger.feature.media.autodeletemedia.presentation.AutoDeleteMediaViewModel
import org.telegram.messenger.feature.media.cachebychats.domain.repository.CacheByChatsRepository
import org.telegram.messenger.feature.media.cachebychats.domain.usecase.ClearKeepMediaExceptionsUseCase
import org.telegram.messenger.feature.media.cachebychats.domain.usecase.GetCacheByChatsConfigUseCase
import org.telegram.messenger.feature.media.cachebychats.domain.usecase.ObserveCacheByChatsConfigUseCase
import org.telegram.messenger.feature.media.cachebychats.domain.usecase.RemoveKeepMediaExceptionUseCase
import org.telegram.messenger.feature.media.cachebychats.domain.usecase.SetKeepMediaDurationUseCase
import org.telegram.messenger.feature.media.cachebychats.domain.usecase.SetKeepMediaExceptionUseCase
import org.telegram.messenger.feature.media.cachebychats.presentation.CacheByChatsViewModel
import org.telegram.messenger.feature.media.camera.domain.repository.CameraRepository
import org.telegram.messenger.feature.media.camera.domain.usecase.ChooseOptimalResolutionUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.GetCameraStateUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.InitCamerasUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.NotifyCameraRecordingUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.ObserveCameraStateUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.SelectCameraUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.SetCameraFlashModeUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.SwitchCameraUseCase
import org.telegram.messenger.feature.media.camera.domain.usecase.ToggleMirrorFrontCameraUseCase
import org.telegram.messenger.feature.media.camera.presentation.CameraViewModel
import org.telegram.messenger.feature.media.chromecast.domain.repository.ChromecastRepository
import org.telegram.messenger.feature.media.chromecast.domain.usecase.CastMediaUseCase
import org.telegram.messenger.feature.media.chromecast.domain.usecase.GetChromecastStateUseCase
import org.telegram.messenger.feature.media.chromecast.domain.usecase.IsCastingUseCase
import org.telegram.messenger.feature.media.chromecast.domain.usecase.IsMediaPlayingOnCastUseCase
import org.telegram.messenger.feature.media.chromecast.domain.usecase.ObserveChromecastStateUseCase
import org.telegram.messenger.feature.media.chromecast.domain.usecase.SetCastCoverFileUseCase
import org.telegram.messenger.feature.media.chromecast.domain.usecase.StopCastingUseCase
import org.telegram.messenger.feature.media.chromecast.presentation.ChromecastViewModel
import org.telegram.messenger.feature.media.contentpreview.domain.repository.ContentPreviewRepository
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.CalculatePreviewDragUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.ClearContentPreviewUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.DismissContentPreviewUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.EvaluatePreviewEligibilityUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.GetContentPreviewStateUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.ObserveContentPreviewStateUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.OpenContentPreviewUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.ResolvePreviewActionsUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.TriggerPreviewActionUseCase
import org.telegram.messenger.feature.media.contentpreview.domain.usecase.UpdatePreviewDragUseCase
import org.telegram.messenger.feature.media.contentpreview.presentation.ContentPreviewViewModel
import org.telegram.messenger.feature.media.di.MediaContainer
import org.telegram.messenger.feature.media.downloadmanager.domain.repository.DownloadManagerRepository
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.CancelDownloadUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.ClearRecentDownloadsUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.EnqueueDownloadUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.EvaluateAutoDownloadEligibilityUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.GetDownloadManagerStateUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.MarkDownloadsAsViewedUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.ObserveDownloadManagerStateUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.PauseDownloadUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.ResumeDownloadUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.RetryDownloadUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.SetDownloadNetworkTypeUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.UpdateDownloadPresetUseCase
import org.telegram.messenger.feature.media.downloadmanager.domain.usecase.UpdateDownloadProgressUseCase
import org.telegram.messenger.feature.media.downloadmanager.presentation.DownloadManagerViewModel
import org.telegram.messenger.feature.media.fileloader.domain.repository.FileLoaderRepository
import org.telegram.messenger.feature.media.fileloader.domain.usecase.CancelAllDownloadsUseCase
import org.telegram.messenger.feature.media.fileloader.domain.usecase.CancelFileUploadUseCase
import org.telegram.messenger.feature.media.fileloader.domain.usecase.CancelLoadFileUseCase
import org.telegram.messenger.feature.media.fileloader.domain.usecase.GetActiveDownloadsUseCase
import org.telegram.messenger.feature.media.fileloader.domain.usecase.GetRecentDownloadsUseCase
import org.telegram.messenger.feature.media.fileloader.domain.usecase.LoadFileUseCase
import org.telegram.messenger.feature.media.fileloader.domain.usecase.ObserveTransferUseCase
import org.telegram.messenger.feature.media.fileloader.domain.usecase.ObserveTransfersUseCase
import org.telegram.messenger.feature.media.fileloader.domain.usecase.UploadFileUseCase
import org.telegram.messenger.feature.media.fileloader.presentation.FileLoaderViewModel
import org.telegram.messenger.feature.media.fileref.domain.repository.FileRefRepository
import org.telegram.messenger.feature.media.fileref.domain.usecase.CancelFileRefRequestUseCase
import org.telegram.messenger.feature.media.fileref.domain.usecase.ClearFileRefCacheUseCase
import org.telegram.messenger.feature.media.fileref.domain.usecase.GetFileRefStatsUseCase
import org.telegram.messenger.feature.media.fileref.domain.usecase.NotifyReferenceRenewedUseCase
import org.telegram.messenger.feature.media.fileref.domain.usecase.ObserveFileRefStatsUseCase
import org.telegram.messenger.feature.media.fileref.domain.usecase.RequestReferenceRenewalUseCase
import org.telegram.messenger.feature.media.fileref.presentation.FileRefViewModel
import org.telegram.messenger.feature.media.gallerysave.domain.repository.GallerySaveRepository
import org.telegram.messenger.feature.media.gallerysave.domain.usecase.GetGallerySaveConfigUseCase
import org.telegram.messenger.feature.media.gallerysave.domain.usecase.GetGallerySaveExceptionsUseCase
import org.telegram.messenger.feature.media.gallerysave.domain.usecase.GetGallerySaveSettingsUseCase
import org.telegram.messenger.feature.media.gallerysave.domain.usecase.ObserveGallerySaveConfigUseCase
import org.telegram.messenger.feature.media.gallerysave.domain.usecase.RemoveAllGallerySaveExceptionsUseCase
import org.telegram.messenger.feature.media.gallerysave.domain.usecase.RemoveGallerySaveExceptionUseCase
import org.telegram.messenger.feature.media.gallerysave.domain.usecase.SetGallerySaveExceptionUseCase
import org.telegram.messenger.feature.media.gallerysave.domain.usecase.SetGallerySaveVideoLimitUseCase
import org.telegram.messenger.feature.media.gallerysave.domain.usecase.ToggleGallerySavePeerTypeUseCase
import org.telegram.messenger.feature.media.gallerysave.domain.usecase.UpdateGallerySaveSettingsUseCase
import org.telegram.messenger.feature.media.gallerysave.presentation.GallerySaveViewModel
import org.telegram.messenger.feature.media.imageloader.domain.repository.ImageLoaderRepository
import org.telegram.messenger.feature.media.imageloader.domain.usecase.BuildImageCacheKeyUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.CalculateImageDownscaleUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.CancelImageRequestUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.ClearImageCacheUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.EnqueueImageRequestUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.EvaluateImageCacheEligibilityUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.FormatImageFilterUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.GetImageLoaderStateUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.ObserveImageLoaderStateUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.ParseImageFilterUseCase
import org.telegram.messenger.feature.media.imageloader.domain.usecase.TrimImageMemoryUseCase
import org.telegram.messenger.feature.media.imageloader.presentation.ImageLoaderViewModel
import org.telegram.messenger.feature.media.mediadata.domain.repository.MediaRepository
import org.telegram.messenger.feature.media.mediadata.domain.usecase.GetAlbumMediaUseCase
import org.telegram.messenger.feature.media.mediadata.domain.usecase.GetAllMediaUseCase
import org.telegram.messenger.feature.media.mediadata.domain.usecase.GetMediaAlbumsUseCase
import org.telegram.messenger.feature.media.mediadata.domain.usecase.ObserveMediaAlbumsUseCase
import org.telegram.messenger.feature.media.mediadata.presentation.MediaViewModel
import org.telegram.messenger.feature.media.photoviewer.domain.repository.PhotoViewerRepository
import org.telegram.messenger.feature.media.photoviewer.domain.usecase.CalculateMediaPagingUseCase
import org.telegram.messenger.feature.media.photoviewer.domain.usecase.CalculateZoomTransformUseCase
import org.telegram.messenger.feature.media.photoviewer.domain.usecase.ClosePhotoViewerUseCase
import org.telegram.messenger.feature.media.photoviewer.domain.usecase.GetPhotoViewerStateUseCase
import org.telegram.messenger.feature.media.photoviewer.domain.usecase.NavigatePhotoViewerUseCase
import org.telegram.messenger.feature.media.photoviewer.domain.usecase.ObservePhotoViewerStateUseCase
import org.telegram.messenger.feature.media.photoviewer.domain.usecase.OpenPhotoViewerUseCase
import org.telegram.messenger.feature.media.photoviewer.domain.usecase.ResolveMediaQualityUseCase
import org.telegram.messenger.feature.media.photoviewer.domain.usecase.UpdatePlaybackStateUseCase
import org.telegram.messenger.feature.media.photoviewer.domain.usecase.ValidateViewerActionsUseCase
import org.telegram.messenger.feature.media.photoviewer.presentation.PhotoViewerViewModel
import org.telegram.messenger.feature.media.pip.domain.repository.PipRepository
import org.telegram.messenger.feature.media.pip.domain.usecase.DispatchPipStateUseCase
import org.telegram.messenger.feature.media.pip.domain.usecase.EvaluatePipEligibilityUseCase
import org.telegram.messenger.feature.media.pip.domain.usecase.GetPipSessionUseCase
import org.telegram.messenger.feature.media.pip.domain.usecase.ObservePipSessionUseCase
import org.telegram.messenger.feature.media.pip.domain.usecase.RegisterPipSourceUseCase
import org.telegram.messenger.feature.media.pip.domain.usecase.TriggerPipActionUseCase
import org.telegram.messenger.feature.media.pip.domain.usecase.UnregisterPipSourceUseCase
import org.telegram.messenger.feature.media.pip.domain.usecase.UpdatePipSourceStateUseCase
import org.telegram.messenger.feature.media.pip.presentation.PipViewModel
import org.telegram.messenger.feature.media.sharedmedia.domain.repository.SharedMediaRepository
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.CalculateMediaSelectionUseCase
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.ClearMediaSelectionUseCase
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.FilterSharedMediaUseCase
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.GetSharedMediaStateUseCase
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.GroupMediaByMonthUseCase
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.ObserveSharedMediaStateUseCase
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.ResolveAvailableTabsUseCase
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.SelectSharedMediaTabUseCase
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.SetSharedMediaFilterUseCase
import org.telegram.messenger.feature.media.sharedmedia.domain.usecase.ToggleMediaSelectionUseCase
import org.telegram.messenger.feature.media.sharedmedia.presentation.SharedMediaViewModel
import org.telegram.messenger.feature.media.stories.domain.repository.StoriesRepository
import org.telegram.messenger.feature.media.stories.domain.usecase.ActivateStealthModeUseCase
import org.telegram.messenger.feature.media.stories.domain.usecase.DeleteStoryUseCase
import org.telegram.messenger.feature.media.stories.domain.usecase.GetPeerStoriesUseCase
import org.telegram.messenger.feature.media.stories.domain.usecase.GetStoryLimitUseCase
import org.telegram.messenger.feature.media.stories.domain.usecase.MarkStoryAsReadUseCase
import org.telegram.messenger.feature.media.stories.domain.usecase.ObserveHiddenStoriesUseCase
import org.telegram.messenger.feature.media.stories.domain.usecase.ObserveSelfStoriesUseCase
import org.telegram.messenger.feature.media.stories.domain.usecase.ObserveStealthModeUseCase
import org.telegram.messenger.feature.media.stories.domain.usecase.ObserveStoriesUseCase
import org.telegram.messenger.feature.media.stories.domain.usecase.RefreshStoriesUseCase
import org.telegram.messenger.feature.media.stories.domain.usecase.ToggleStoryHiddenUseCase
import org.telegram.messenger.feature.media.stories.domain.usecase.ToggleStoryPinUseCase
import org.telegram.messenger.feature.media.stories.presentation.StoriesViewModel
import org.telegram.messenger.feature.media.storycustomparams.domain.repository.StoryCustomParamsRepository
import org.telegram.messenger.feature.media.storycustomparams.domain.usecase.CheckStoryCustomParamsEmptyUseCase
import org.telegram.messenger.feature.media.storycustomparams.domain.usecase.ClearAllStoryCustomParamsUseCase
import org.telegram.messenger.feature.media.storycustomparams.domain.usecase.ComputeStoryCustomParamsFlagsUseCase
import org.telegram.messenger.feature.media.storycustomparams.domain.usecase.CopyStoryCustomParamsUseCase
import org.telegram.messenger.feature.media.storycustomparams.domain.usecase.GetStoryCustomParamsStateUseCase
import org.telegram.messenger.feature.media.storycustomparams.domain.usecase.GetStoryCustomParamsUseCase
import org.telegram.messenger.feature.media.storycustomparams.domain.usecase.ObserveStoryCustomParamsStateUseCase
import org.telegram.messenger.feature.media.storycustomparams.domain.usecase.RemoveStoryCustomParamsUseCase
import org.telegram.messenger.feature.media.storycustomparams.domain.usecase.SaveStoryCustomParamsUseCase
import org.telegram.messenger.feature.media.storycustomparams.domain.usecase.UpdateStoryTranslationUseCase
import org.telegram.messenger.feature.media.storycustomparams.presentation.StoryCustomParamsViewModel
import org.telegram.messenger.feature.media.voip.domain.repository.VoIPRepository
import org.telegram.messenger.feature.media.voip.domain.usecase.AcceptCallUseCase
import org.telegram.messenger.feature.media.voip.domain.usecase.DeclineCallUseCase
import org.telegram.messenger.feature.media.voip.domain.usecase.GetCurrentCallUseCase
import org.telegram.messenger.feature.media.voip.domain.usecase.HangUpCallUseCase
import org.telegram.messenger.feature.media.voip.domain.usecase.ObserveCurrentCallUseCase
import org.telegram.messenger.feature.media.voip.domain.usecase.StartCallUseCase
import org.telegram.messenger.feature.media.voip.domain.usecase.ToggleMuteUseCase
import org.telegram.messenger.feature.media.voip.domain.usecase.ToggleSpeakerphoneUseCase
import org.telegram.messenger.feature.media.voip.presentation.CallViewModel
import org.telegram.messenger.feature.messaging.aitones.domain.repository.AiTonesRepository
import org.telegram.messenger.feature.messaging.aitones.domain.usecase.AddAiToneUseCase
import org.telegram.messenger.feature.messaging.aitones.domain.usecase.EditAiToneUseCase
import org.telegram.messenger.feature.messaging.aitones.domain.usecase.GetAiTonesStateUseCase
import org.telegram.messenger.feature.messaging.aitones.domain.usecase.LoadAiTonesUseCase
import org.telegram.messenger.feature.messaging.aitones.domain.usecase.ObserveAiTonesUseCase
import org.telegram.messenger.feature.messaging.aitones.domain.usecase.RemoveAiToneUseCase
import org.telegram.messenger.feature.messaging.aitones.domain.usecase.UnsaveAiToneUseCase
import org.telegram.messenger.feature.messaging.aitones.presentation.AiTonesViewModel
import org.telegram.messenger.feature.messaging.autodelete.domain.repository.AutoDeleteRepository
import org.telegram.messenger.feature.messaging.autodelete.domain.usecase.GetChatAutoDeleteUseCase
import org.telegram.messenger.feature.messaging.autodelete.domain.usecase.GetGlobalAutoDeleteUseCase
import org.telegram.messenger.feature.messaging.autodelete.domain.usecase.ObserveGlobalAutoDeleteUseCase
import org.telegram.messenger.feature.messaging.autodelete.domain.usecase.SetChatAutoDeleteUseCase
import org.telegram.messenger.feature.messaging.autodelete.domain.usecase.SetChatsAutoDeleteBatchUseCase
import org.telegram.messenger.feature.messaging.autodelete.domain.usecase.SetGlobalAutoDeleteUseCase
import org.telegram.messenger.feature.messaging.autodelete.presentation.AutoDeleteViewModel
import org.telegram.messenger.feature.messaging.botforum.domain.repository.BotForumRepository
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.CheckHasBotForumDraftsUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.CheckIsBotForumUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.CheckIsStreamingTopicUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.CheckNewMessageDraftReplacementUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.DeriveTopicNameFromMessageUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.GetBotForumStateUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.GetStreamingSendButtonStateUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.ObserveBotForumStateUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.RemoveMarkedRemovedDraftsUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.ResolveStreamingButtonStateUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.SaveIsStreamingTopicUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.StopStreamingDraftUseCase
import org.telegram.messenger.feature.messaging.botforum.domain.usecase.UpdateBotForumDraftUseCase
import org.telegram.messenger.feature.messaging.botforum.presentation.BotForumViewModel
import org.telegram.messenger.feature.messaging.botkeyboard.domain.repository.BotKeyboardRepository
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.BuildBotKeyboardLayoutUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.CheckIsButtonWebViewUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.CheckIsForceReplyUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.ClearAllKeyboardsUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.GetBotKeyboardStateUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.GetKeyboardForMessageUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.ObserveBotKeyboardStateUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.RecordButtonPressedUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.RemoveKeyboardForMessageUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.ResolveCustomButtonTypeUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.domain.usecase.SetKeyboardForMessageUseCase
import org.telegram.messenger.feature.messaging.botkeyboard.presentation.BotKeyboardViewModel
import org.telegram.messenger.feature.messaging.bottomviews.domain.repository.BottomViewsVisibilityRepository
import org.telegram.messenger.feature.messaging.bottomviews.domain.usecase.GetBottomViewVisibilityUseCase
import org.telegram.messenger.feature.messaging.bottomviews.domain.usecase.GetBottomViewsStateUseCase
import org.telegram.messenger.feature.messaging.bottomviews.domain.usecase.GetPriorityBottomContainerUseCase
import org.telegram.messenger.feature.messaging.bottomviews.domain.usecase.ObserveBottomViewsVisibilityUseCase
import org.telegram.messenger.feature.messaging.bottomviews.domain.usecase.SetBottomViewVisibleUseCase
import org.telegram.messenger.feature.messaging.bottomviews.presentation.BottomViewsViewModel
import org.telegram.messenger.feature.messaging.chat.domain.repository.ChatRepository
import org.telegram.messenger.feature.messaging.chat.domain.usecase.DeleteMessagesUseCase
import org.telegram.messenger.feature.messaging.chat.domain.usecase.GetMessagesUseCase
import org.telegram.messenger.feature.messaging.chat.domain.usecase.LoadHistoryUseCase
import org.telegram.messenger.feature.messaging.chat.domain.usecase.ObserveMessagesUseCase
import org.telegram.messenger.feature.messaging.chat.domain.usecase.SendMessageUseCase
import org.telegram.messenger.feature.messaging.chat.presentation.ChatViewModel
import org.telegram.messenger.feature.messaging.chatattach.domain.repository.ChatAttachRepository
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.CalculateAttachCaptionLimitUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.ClearAttachSelectionUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.GetChatAttachStateUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.ObserveChatAttachStateUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.OpenChatAttachAlertUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.ResolveAvailableAttachLayoutsUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.SelectAttachLayoutUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.ToggleAttachItemSelectionUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.UpdateAttachSendOptionsUseCase
import org.telegram.messenger.feature.messaging.chatattach.domain.usecase.ValidateSendOptionsUseCase
import org.telegram.messenger.feature.messaging.chatattach.presentation.ChatAttachViewModel
import org.telegram.messenger.feature.messaging.chatinput.domain.repository.ChatInputRepository
import org.telegram.messenger.feature.messaging.chatinput.domain.usecase.CalculateSendButtonStateUseCase
import org.telegram.messenger.feature.messaging.chatinput.domain.usecase.ClearChatInputReplyUseCase
import org.telegram.messenger.feature.messaging.chatinput.domain.usecase.FormatTextSelectionUseCase
import org.telegram.messenger.feature.messaging.chatinput.domain.usecase.GetChatInputStateUseCase
import org.telegram.messenger.feature.messaging.chatinput.domain.usecase.ObserveChatInputStateUseCase
import org.telegram.messenger.feature.messaging.chatinput.domain.usecase.ResolvePanelVisibilityUseCase
import org.telegram.messenger.feature.messaging.chatinput.domain.usecase.SetChatInputPanelModeUseCase
import org.telegram.messenger.feature.messaging.chatinput.domain.usecase.SetChatInputReplyUseCase
import org.telegram.messenger.feature.messaging.chatinput.domain.usecase.SetChatInputTextUseCase
import org.telegram.messenger.feature.messaging.chatinput.domain.usecase.ValidateVoiceRecordActionUseCase
import org.telegram.messenger.feature.messaging.chatinput.presentation.ChatInputViewModel
import org.telegram.messenger.feature.messaging.chatmeta.domain.repository.ChatMessagesMetadataRepository
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.CancelPendingMetadataRequestsUseCase
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.CheckMessagesMetadataUseCase
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.GetChatMetadataStatsUseCase
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.LoadMessagesExtendedMediaUseCase
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.LoadMessagesReactionsUseCase
import org.telegram.messenger.feature.messaging.chatmeta.domain.usecase.ObserveChatMetadataStatsUseCase
import org.telegram.messenger.feature.messaging.chatmeta.presentation.ChatMetadataViewModel
import org.telegram.messenger.feature.messaging.chattheme.domain.repository.ChatThemeRepository
import org.telegram.messenger.feature.messaging.chattheme.domain.usecase.GetAvailableChatThemesUseCase
import org.telegram.messenger.feature.messaging.chattheme.domain.usecase.GetDialogThemeStateUseCase
import org.telegram.messenger.feature.messaging.chattheme.domain.usecase.ObserveDialogThemeUseCase
import org.telegram.messenger.feature.messaging.chattheme.domain.usecase.ResetDialogThemeUseCase
import org.telegram.messenger.feature.messaging.chattheme.domain.usecase.SaveChatWallpaperUseCase
import org.telegram.messenger.feature.messaging.chattheme.domain.usecase.SetDialogThemeUseCase
import org.telegram.messenger.feature.messaging.chattheme.presentation.ChatThemeViewModel
import org.telegram.messenger.feature.messaging.di.MessagingContainer
import org.telegram.messenger.feature.messaging.dialogs.domain.repository.DialogsRepository
import org.telegram.messenger.feature.messaging.dialogs.domain.usecase.DeleteDialogUseCase
import org.telegram.messenger.feature.messaging.dialogs.domain.usecase.GetDialogsUseCase
import org.telegram.messenger.feature.messaging.dialogs.domain.usecase.LoadMoreDialogsUseCase
import org.telegram.messenger.feature.messaging.dialogs.domain.usecase.MarkDialogAsReadUseCase
import org.telegram.messenger.feature.messaging.dialogs.domain.usecase.PinDialogUseCase
import org.telegram.messenger.feature.messaging.dialogs.presentation.DialogsViewModel
import org.telegram.messenger.feature.messaging.draftmeasure.domain.repository.DraftMeasureRepository
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.CalculateDraftMeasureOverrideUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.GetDraftMeasureConfigUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.ObserveDraftMeasureConfigUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.OnDraftMessageIdChangedUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.ResetDraftMeasureTargetUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.SetDraftMeasureTargetUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.domain.usecase.SetPreviousMessageHeightUseCase
import org.telegram.messenger.feature.messaging.draftmeasure.presentation.DraftMeasureViewModel
import org.telegram.messenger.feature.messaging.drafts.domain.repository.DraftsRepository
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.CleanupExpiredDraftsUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.DeleteDraftUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.DeleteForEditUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.GetDraftForEditUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.GetDraftsStateUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.LoadDraftsUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.ObserveDraftsStateUseCase
import org.telegram.messenger.feature.messaging.drafts.domain.usecase.SaveDraftUseCase
import org.telegram.messenger.feature.messaging.drafts.presentation.DraftsViewModel
import org.telegram.messenger.feature.messaging.emojieffects.domain.repository.EmojiEffectsRepository
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.CalculateEmojiBoundsUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.CalculateEmojiOverlayPositionUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.ClearEmojiEffectsUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.DecodeEmojiInteractionsJsonUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.DismissEmojiEffectUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.EncodeEmojiInteractionsJsonUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.EvaluateAnimationQuotaUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.EvaluateEmojiSupportUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.GetEmojiEffectsStateUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.NormalizeEmojiUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.ObserveEmojiEffectsStateUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.RecordEmojiTapUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.StartEmojiEffectUseCase
import org.telegram.messenger.feature.messaging.emojieffects.domain.usecase.UpdateEmojiEffectProgressUseCase
import org.telegram.messenger.feature.messaging.emojieffects.presentation.EmojiEffectsViewModel
import org.telegram.messenger.feature.messaging.emojipicker.domain.repository.EmojiPickerRepository
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.ClearRecentPickerItemsUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.FilterEmojiItemsUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.FilterGifsUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.FilterStickersUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.GetEmojiPickerStateUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.ObserveEmojiPickerStateUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.ResolveAvailablePickerTabsUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.SelectPickerTabUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.ToggleStickerFavoriteUseCase
import org.telegram.messenger.feature.messaging.emojipicker.domain.usecase.UpdatePickerSearchQueryUseCase
import org.telegram.messenger.feature.messaging.emojipicker.presentation.EmojiPickerViewModel
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.repository.EphemeralMessagesRepository
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.ClearAllWelcomeAnchorBindingsUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.GetEphemeralCommandBotIdUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.GetEphemeralMessagesStateUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.GetWelcomeAnchorBindingsUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.IsEphemeralCommandUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.IsEphemeralMessageIdUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.ObserveEphemeralMessagesStateUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.PackEphemeralMessageIdUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.ParseBotCommandUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.PutWelcomeAnchorBindingUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.RemoveWelcomeAnchorBindingUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.domain.usecase.UnpackEphemeralMessageIdUseCase
import org.telegram.messenger.feature.messaging.ephemeralmessages.presentation.EphemeralMessagesViewModel
import org.telegram.messenger.feature.messaging.factcheck.domain.repository.FactCheckRepository
import org.telegram.messenger.feature.messaging.factcheck.domain.usecase.ApplyFactCheckUseCase
import org.telegram.messenger.feature.messaging.factcheck.domain.usecase.DeleteFactCheckUseCase
import org.telegram.messenger.feature.messaging.factcheck.domain.usecase.GetFactCheckLimitUseCase
import org.telegram.messenger.feature.messaging.factcheck.domain.usecase.GetFactCheckUseCase
import org.telegram.messenger.feature.messaging.factcheck.domain.usecase.LoadFactCheckUseCase
import org.telegram.messenger.feature.messaging.factcheck.domain.usecase.ObserveFactCheckLoadedUseCase
import org.telegram.messenger.feature.messaging.factcheck.presentation.FactCheckViewModel
import org.telegram.messenger.feature.messaging.folders.domain.repository.FoldersRepository
import org.telegram.messenger.feature.messaging.folders.domain.usecase.CreateFolderUseCase
import org.telegram.messenger.feature.messaging.folders.domain.usecase.DeleteFolderUseCase
import org.telegram.messenger.feature.messaging.folders.domain.usecase.GetFolderUseCase
import org.telegram.messenger.feature.messaging.folders.domain.usecase.GetFoldersUseCase
import org.telegram.messenger.feature.messaging.folders.domain.usecase.GetSuggestedFoldersUseCase
import org.telegram.messenger.feature.messaging.folders.domain.usecase.ObserveFoldersUseCase
import org.telegram.messenger.feature.messaging.folders.domain.usecase.ReorderFoldersUseCase
import org.telegram.messenger.feature.messaging.folders.domain.usecase.UpdateFolderUseCase
import org.telegram.messenger.feature.messaging.folders.presentation.FoldersViewModel
import org.telegram.messenger.feature.messaging.groupcallmsg.domain.repository.GroupCallMessagesRepository
import org.telegram.messenger.feature.messaging.groupcallmsg.domain.usecase.ClearGroupCallMessagesUseCase
import org.telegram.messenger.feature.messaging.groupcallmsg.domain.usecase.GetGroupCallMessagesUseCase
import org.telegram.messenger.feature.messaging.groupcallmsg.domain.usecase.ObserveGroupCallMessagesUseCase
import org.telegram.messenger.feature.messaging.groupcallmsg.domain.usecase.PopGroupCallMessageUseCase
import org.telegram.messenger.feature.messaging.groupcallmsg.domain.usecase.SendGroupCallMessageUseCase
import org.telegram.messenger.feature.messaging.groupcallmsg.presentation.GroupCallMessagesViewModel
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.repository.HashtagSearchRepository
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.AddHashtagToHistoryUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.ClearHashtagHistoryUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.ClearHashtagSearchResultsUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.GetHashtagHistoryUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.JumpToHashtagMessageUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.ObserveHashtagHistoryUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.ObserveHashtagSearchResultUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.RemoveHashtagFromHistoryUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.domain.usecase.SearchHashtagUseCase
import org.telegram.messenger.feature.messaging.hashtagsearch.presentation.HashtagSearchViewModel
import org.telegram.messenger.feature.messaging.mentions.domain.repository.MentionsRepository
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.ClearMentionsUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.DismissMentionsUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.FilterMentionsUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.FormatMentionReplacementUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.GetMentionsStateUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.ObserveMentionsStateUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.ParseMentionQueryUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.SetMentionCandidatesUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.UpdateMentionQueryUseCase
import org.telegram.messenger.feature.messaging.mentions.domain.usecase.ValidateUsernameUseCase
import org.telegram.messenger.feature.messaging.mentions.presentation.MentionsViewModel
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.repository.MessageCustomParamsRepository
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.CheckMessageCustomParamsEmptyUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.ClearAllMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.CopyMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.GetMessageCustomParamsStateUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.GetMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.MergeMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.ObserveMessageCustomParamsStateUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.RemoveMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.SetMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.UpdateMessageSummaryUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.UpdateMessageTranslationUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.UpdateVoiceTranscriptionUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.presentation.MessageCustomParamsViewModel
import org.telegram.messenger.feature.messaging.reactions.domain.repository.ReactionsRepository
import org.telegram.messenger.feature.messaging.reactions.domain.usecase.ClearReactionsUseCase
import org.telegram.messenger.feature.messaging.reactions.domain.usecase.GetAvailableReactionsUseCase
import org.telegram.messenger.feature.messaging.reactions.domain.usecase.GetDoubleTapReactionUseCase
import org.telegram.messenger.feature.messaging.reactions.domain.usecase.GetReactionsSettingsUseCase
import org.telegram.messenger.feature.messaging.reactions.domain.usecase.GetRecentReactionsUseCase
import org.telegram.messenger.feature.messaging.reactions.domain.usecase.LoadAvailableReactionsUseCase
import org.telegram.messenger.feature.messaging.reactions.domain.usecase.ObserveAvailableReactionsUseCase
import org.telegram.messenger.feature.messaging.reactions.domain.usecase.ObserveRecentReactionsUseCase
import org.telegram.messenger.feature.messaging.reactions.domain.usecase.SendReactionUseCase
import org.telegram.messenger.feature.messaging.reactions.domain.usecase.SendVoteUseCase
import org.telegram.messenger.feature.messaging.reactions.domain.usecase.SetDoubleTapReactionUseCase
import org.telegram.messenger.feature.messaging.reactions.presentation.ReactionsViewModel
import org.telegram.messenger.feature.messaging.richcaption.domain.repository.RichCaptionRepository
import org.telegram.messenger.feature.messaging.richcaption.domain.usecase.CalculateCaptionMeasureWidthUseCase
import org.telegram.messenger.feature.messaging.richcaption.domain.usecase.CheckCaptionPressHitUseCase
import org.telegram.messenger.feature.messaging.richcaption.domain.usecase.ClearRichCaptionUseCase
import org.telegram.messenger.feature.messaging.richcaption.domain.usecase.GetRichCaptionUseCase
import org.telegram.messenger.feature.messaging.richcaption.domain.usecase.ObserveRichCaptionUseCase
import org.telegram.messenger.feature.messaging.richcaption.domain.usecase.SetRichCaptionCreditUseCase
import org.telegram.messenger.feature.messaging.richcaption.domain.usecase.SetRichCaptionLockedUseCase
import org.telegram.messenger.feature.messaging.richcaption.domain.usecase.SetRichCaptionTextUseCase
import org.telegram.messenger.feature.messaging.richcaption.presentation.RichCaptionViewModel
import org.telegram.messenger.feature.messaging.savedmessages.domain.repository.SavedMessagesRepository
import org.telegram.messenger.feature.messaging.savedmessages.domain.usecase.DeleteSavedDialogUseCase
import org.telegram.messenger.feature.messaging.savedmessages.domain.usecase.GetSavedDialogsUseCase
import org.telegram.messenger.feature.messaging.savedmessages.domain.usecase.GetSavedTagsUseCase
import org.telegram.messenger.feature.messaging.savedmessages.domain.usecase.SearchSavedDialogsUseCase
import org.telegram.messenger.feature.messaging.savedmessages.domain.usecase.TogglePinSavedDialogUseCase
import org.telegram.messenger.feature.messaging.savedmessages.presentation.SavedMessagesViewModel
import org.telegram.messenger.feature.messaging.search.domain.repository.SearchRepository
import org.telegram.messenger.feature.messaging.search.domain.usecase.ClearRecentHashtagsUseCase
import org.telegram.messenger.feature.messaging.search.domain.usecase.ClearRecentSearchesUseCase
import org.telegram.messenger.feature.messaging.search.domain.usecase.GetRecentHashtagsUseCase
import org.telegram.messenger.feature.messaging.search.domain.usecase.GetRecentSearchesUseCase
import org.telegram.messenger.feature.messaging.search.domain.usecase.PutRecentHashtagUseCase
import org.telegram.messenger.feature.messaging.search.domain.usecase.RemoveRecentSearchUseCase
import org.telegram.messenger.feature.messaging.search.domain.usecase.SearchGlobalUseCase
import org.telegram.messenger.feature.messaging.search.domain.usecase.SearchLocalUseCase
import org.telegram.messenger.feature.messaging.search.presentation.SearchViewModel
import org.telegram.messenger.feature.messaging.sendmessages.domain.repository.SendMessagesRepository
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.CancelSendMessageUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.ForwardMessagesUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.ObservePendingSendsUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.RetrySendMessageUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.SendMediaAlbumUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.SendMediaMessageUseCase
import org.telegram.messenger.feature.messaging.sendmessages.domain.usecase.SendTextMessageUseCase
import org.telegram.messenger.feature.messaging.sendmessages.presentation.SendMessagesViewModel
import org.telegram.messenger.feature.messaging.stickers.domain.repository.StickersRepository
import org.telegram.messenger.feature.messaging.stickers.domain.usecase.GetRecentStickersUseCase
import org.telegram.messenger.feature.messaging.stickers.domain.usecase.GetStickerSetUseCase
import org.telegram.messenger.feature.messaging.stickers.domain.usecase.GetStickerSetsUseCase
import org.telegram.messenger.feature.messaging.stickers.domain.usecase.GetStickersForEmojiUseCase
import org.telegram.messenger.feature.messaging.stickers.domain.usecase.ObserveStickerSetsUseCase
import org.telegram.messenger.feature.messaging.stickers.domain.usecase.ToggleStickerSetArchivedUseCase
import org.telegram.messenger.feature.messaging.stickers.domain.usecase.ToggleStickerSetInstalledUseCase
import org.telegram.messenger.feature.messaging.stickers.presentation.StickersViewModel
import org.telegram.messenger.feature.messaging.topics.domain.repository.TopicsRepository
import org.telegram.messenger.feature.messaging.topics.domain.usecase.DeleteTopicsUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.GetForumUnreadCountUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.GetTopicUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.GetTopicsUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.LoadTopicsUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.MarkTopicReactionsAsReadUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.ObserveForumUnreadCountUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.ObserveTopicsUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.ReloadTopicsUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.ReorderPinnedTopicsUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.ToggleCloseTopicUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.TogglePinTopicUseCase
import org.telegram.messenger.feature.messaging.topics.domain.usecase.ToggleShowTopicUseCase
import org.telegram.messenger.feature.messaging.topics.presentation.TopicsViewModel
import org.telegram.messenger.feature.messaging.translate.domain.repository.TranslationRepository
import org.telegram.messenger.feature.messaging.translate.domain.usecase.AddDoNotTranslateLanguageUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.ApplyAppLanguageUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.GetAvailableLanguagesUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.GetDialogTranslationStateUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.GetTranslateSettingsUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.ObserveDialogTranslationStateUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.ObserveTranslateSettingsUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.RemoveDoNotTranslateLanguageUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.SetChatTranslateEnabledUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.SetContextTranslateEnabledUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.SetDialogTargetLanguageUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.SetDoNotTranslateLanguagesUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.ToggleDialogTranslatingUseCase
import org.telegram.messenger.feature.messaging.translate.domain.usecase.TranslateTextUseCase
import org.telegram.messenger.feature.messaging.translate.presentation.TranslateViewModel
import org.telegram.messenger.feature.network.di.NetworkContainer
import org.telegram.messenger.feature.network.networkstats.domain.repository.NetworkStatsRepository
import org.telegram.messenger.feature.network.networkstats.domain.usecase.CalculateMessagesTrafficUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.FormatCallsDurationUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.FormatTrafficBytesUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.GetAllNetworkStatsUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.GetNetworkStatsUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.IncrementCallsTimeUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.IncrementTrafficBytesUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.IncrementTrafficItemsUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.ObserveAllNetworkStatsUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.ObserveNetworkStatsUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.RefreshNetworkStatsUseCase
import org.telegram.messenger.feature.network.networkstats.domain.usecase.ResetNetworkStatsUseCase
import org.telegram.messenger.feature.network.networkstats.presentation.NetworkStatsViewModel
import org.telegram.messenger.feature.network.proxy.domain.repository.ProxyRepository
import org.telegram.messenger.feature.network.proxy.domain.usecase.AddProxyUseCase
import org.telegram.messenger.feature.network.proxy.domain.usecase.CheckProxyPingUseCase
import org.telegram.messenger.feature.network.proxy.domain.usecase.DeleteProxyUseCase
import org.telegram.messenger.feature.network.proxy.domain.usecase.DisableProxyUseCase
import org.telegram.messenger.feature.network.proxy.domain.usecase.EnableProxyUseCase
import org.telegram.messenger.feature.network.proxy.domain.usecase.GetProxySettingsUseCase
import org.telegram.messenger.feature.network.proxy.domain.usecase.ObserveProxySettingsUseCase
import org.telegram.messenger.feature.network.proxy.domain.usecase.ToggleProxyRotationUseCase
import org.telegram.messenger.feature.network.proxy.presentation.ProxyViewModel
import org.telegram.messenger.feature.network.push.domain.repository.PushRepository
import org.telegram.messenger.feature.network.push.domain.usecase.GetPushStatusUseCase
import org.telegram.messenger.feature.network.push.domain.usecase.IsPushAvailableUseCase
import org.telegram.messenger.feature.network.push.domain.usecase.ObservePushStatusUseCase
import org.telegram.messenger.feature.network.push.domain.usecase.RegisterPushTokenUseCase
import org.telegram.messenger.feature.network.push.domain.usecase.RequestPushTokenUseCase
import org.telegram.messenger.feature.network.push.domain.usecase.ResetPushTokenUseCase
import org.telegram.messenger.feature.network.push.presentation.PushViewModel
import org.telegram.messenger.feature.network.pushlistener.domain.repository.PushListenerRepository
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.DeterminePushActionTypeUseCase
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.GetPushListenerStateUseCase
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.ObserveIncomingPushesUseCase
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.ObservePushListenerStateUseCase
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.ParsePushJsonPayloadUseCase
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.ProcessIncomingPushUseCase
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.RegisterPushListenerTokenUseCase
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.TogglePushListeningUseCase
import org.telegram.messenger.feature.network.pushlistener.presentation.PushListenerViewModel
import org.telegram.messenger.feature.security.authtokens.domain.repository.AuthTokensRepository
import org.telegram.messenger.feature.security.authtokens.domain.usecase.AddLogoutTokenUseCase
import org.telegram.messenger.feature.security.authtokens.domain.usecase.ClearAllTokensUseCase
import org.telegram.messenger.feature.security.authtokens.domain.usecase.GetAuthTokensStateUseCase
import org.telegram.messenger.feature.security.authtokens.domain.usecase.GetSavedLoginTokensUseCase
import org.telegram.messenger.feature.security.authtokens.domain.usecase.GetSavedLogoutTokensUseCase
import org.telegram.messenger.feature.security.authtokens.domain.usecase.ObserveAuthTokensStateUseCase
import org.telegram.messenger.feature.security.authtokens.domain.usecase.PruneTokensListUseCase
import org.telegram.messenger.feature.security.authtokens.domain.usecase.RefreshAuthTokensUseCase
import org.telegram.messenger.feature.security.authtokens.domain.usecase.RemoveTokenUseCase
import org.telegram.messenger.feature.security.authtokens.domain.usecase.SaveLoginTokenUseCase
import org.telegram.messenger.feature.security.authtokens.domain.usecase.SaveLogoutTokensUseCase
import org.telegram.messenger.feature.security.authtokens.domain.usecase.ValidateAuthTokenFormatUseCase
import org.telegram.messenger.feature.security.authtokens.presentation.AuthTokensViewModel
import org.telegram.messenger.feature.security.biometrics.domain.repository.BiometricsRepository
import org.telegram.messenger.feature.security.biometrics.domain.usecase.CheckBiometricKeyReadyUseCase
import org.telegram.messenger.feature.security.biometrics.domain.usecase.DeleteInvalidBiometricKeyUseCase
import org.telegram.messenger.feature.security.biometrics.domain.usecase.GetBiometricKeyStateUseCase
import org.telegram.messenger.feature.security.biometrics.domain.usecase.HasDeviceBiometricsChangedUseCase
import org.telegram.messenger.feature.security.biometrics.domain.usecase.IsBiometricKeyReadyUseCase
import org.telegram.messenger.feature.security.biometrics.domain.usecase.ObserveBiometricKeyStateUseCase
import org.telegram.messenger.feature.security.biometrics.presentation.BiometricsViewModel
import org.telegram.messenger.feature.security.botguard.domain.repository.BotGuardRepository
import org.telegram.messenger.feature.security.botguard.domain.usecase.ClearAllGuardBotSessionsUseCase
import org.telegram.messenger.feature.security.botguard.domain.usecase.CloseGuardBotSessionUseCase
import org.telegram.messenger.feature.security.botguard.domain.usecase.DetermineGuardBotLaunchFlowUseCase
import org.telegram.messenger.feature.security.botguard.domain.usecase.FormatGuardBotBulletinUseCase
import org.telegram.messenger.feature.security.botguard.domain.usecase.GetAllActiveGuardBotSessionsUseCase
import org.telegram.messenger.feature.security.botguard.domain.usecase.GetGuardBotSessionUseCase
import org.telegram.messenger.feature.security.botguard.domain.usecase.IsGuardBotConfirmationNeededUseCase
import org.telegram.messenger.feature.security.botguard.domain.usecase.MapJoinChatBotResultUseCase
import org.telegram.messenger.feature.security.botguard.domain.usecase.ObserveGuardBotDecisionsUseCase
import org.telegram.messenger.feature.security.botguard.domain.usecase.ObserveGuardBotStateUseCase
import org.telegram.messenger.feature.security.botguard.domain.usecase.RegisterGuardBotSessionUseCase
import org.telegram.messenger.feature.security.botguard.domain.usecase.SetGuardBotConfirmationShownUseCase
import org.telegram.messenger.feature.security.botguard.presentation.BotGuardViewModel
import org.telegram.messenger.feature.security.captcha.domain.repository.CaptchaRepository
import org.telegram.messenger.feature.security.captcha.domain.usecase.CancelCaptchaUseCase
import org.telegram.messenger.feature.security.captcha.domain.usecase.GetActiveCaptchaRequestsUseCase
import org.telegram.messenger.feature.security.captcha.domain.usecase.ObserveActiveCaptchaRequestsUseCase
import org.telegram.messenger.feature.security.captcha.domain.usecase.SubmitCaptchaResultUseCase
import org.telegram.messenger.feature.security.captcha.domain.usecase.VerifyCaptchaUseCase
import org.telegram.messenger.feature.security.captcha.presentation.CaptchaViewModel
import org.telegram.messenger.feature.security.di.SecurityContainer
import org.telegram.messenger.feature.security.passkeys.domain.repository.PasskeysRepository
import org.telegram.messenger.feature.security.passkeys.domain.usecase.CheckCanAddPasskeyUseCase
import org.telegram.messenger.feature.security.passkeys.domain.usecase.DeletePasskeyUseCase
import org.telegram.messenger.feature.security.passkeys.domain.usecase.GetPasskeysUseCase
import org.telegram.messenger.feature.security.passkeys.domain.usecase.IsPasskeysSupportedUseCase
import org.telegram.messenger.feature.security.passkeys.domain.usecase.ObservePasskeysUseCase
import org.telegram.messenger.feature.security.passkeys.presentation.PasskeysViewModel
import org.telegram.messenger.feature.security.privacy.domain.repository.PrivacyRepository
import org.telegram.messenger.feature.security.privacy.domain.usecase.BlockPrivacyPeerUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.CheckPasscodeUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.ClearPasscodeUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.GetBlockedPeersUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.GetPasscodeSettingsUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.GetPrivacyRulesUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.LoadPrivacyRulesUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.LoadTwoStepVerificationUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.ObserveBlockedPeersUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.ObservePrivacyRulesUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.ObserveTwoStepVerificationUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.SetPasscodeUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.SetPrivacyRuleUseCase
import org.telegram.messenger.feature.security.privacy.domain.usecase.UnblockPrivacyPeerUseCase
import org.telegram.messenger.feature.security.privacy.presentation.PrivacyViewModel
import org.telegram.messenger.feature.security.secretchat.domain.repository.SecretChatRepository
import org.telegram.messenger.feature.security.secretchat.domain.usecase.AcceptSecretChatUseCase
import org.telegram.messenger.feature.security.secretchat.domain.usecase.DeclineSecretChatUseCase
import org.telegram.messenger.feature.security.secretchat.domain.usecase.GetSecretChatUseCase
import org.telegram.messenger.feature.security.secretchat.domain.usecase.ObserveSecretChatUseCase
import org.telegram.messenger.feature.security.secretchat.domain.usecase.ObserveSecretChatsUseCase
import org.telegram.messenger.feature.security.secretchat.domain.usecase.SendScreenshotNotificationUseCase
import org.telegram.messenger.feature.security.secretchat.domain.usecase.SetSecretChatTtlUseCase
import org.telegram.messenger.feature.security.secretchat.domain.usecase.StartSecretChatUseCase
import org.telegram.messenger.feature.security.secretchat.presentation.SecretChatViewModel
import org.telegram.messenger.feature.security.sessions.domain.repository.SessionsRepository
import org.telegram.messenger.feature.security.sessions.domain.usecase.AcceptQrLoginUseCase
import org.telegram.messenger.feature.security.sessions.domain.usecase.GetSessionsUseCase
import org.telegram.messenger.feature.security.sessions.domain.usecase.GetWebSessionsUseCase
import org.telegram.messenger.feature.security.sessions.domain.usecase.LoadSessionsUseCase
import org.telegram.messenger.feature.security.sessions.domain.usecase.LoadWebSessionsUseCase
import org.telegram.messenger.feature.security.sessions.domain.usecase.ObserveSessionsUseCase
import org.telegram.messenger.feature.security.sessions.domain.usecase.ObserveWebSessionsUseCase
import org.telegram.messenger.feature.security.sessions.domain.usecase.SetSessionsTtlUseCase
import org.telegram.messenger.feature.security.sessions.domain.usecase.TerminateAllOtherSessionsUseCase
import org.telegram.messenger.feature.security.sessions.domain.usecase.TerminateAllWebSessionsUseCase
import org.telegram.messenger.feature.security.sessions.domain.usecase.TerminateSessionUseCase
import org.telegram.messenger.feature.security.sessions.domain.usecase.TerminateWebSessionUseCase
import org.telegram.messenger.feature.security.sessions.domain.usecase.UpdateSessionSettingsUseCase
import org.telegram.messenger.feature.security.sessions.presentation.SessionsViewModel
import org.telegram.messenger.feature.security.unconfirmedauth.domain.repository.UnconfirmedAuthRepository
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.ClearUnconfirmedAuthsUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.ConfirmAllAuthsUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.ConfirmAuthUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.DenyAllAuthsUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.DenyAuthUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.GetUnconfirmedAuthsUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.ObserveUnconfirmedAuthsUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.presentation.UnconfirmedAuthViewModel
import org.telegram.messenger.feature.social.birthdays.domain.repository.BirthdaysRepository
import org.telegram.messenger.feature.social.birthdays.domain.usecase.CheckBirthdaysUseCase
import org.telegram.messenger.feature.social.birthdays.domain.usecase.GetBirthdaysStateUseCase
import org.telegram.messenger.feature.social.birthdays.domain.usecase.HasBirthdaysTodayUseCase
import org.telegram.messenger.feature.social.birthdays.domain.usecase.HideTodayBirthdaysUseCase
import org.telegram.messenger.feature.social.birthdays.domain.usecase.IsBirthdayTodayUseCase
import org.telegram.messenger.feature.social.birthdays.domain.usecase.ObserveBirthdaysUseCase
import org.telegram.messenger.feature.social.birthdays.presentation.BirthdaysViewModel
import org.telegram.messenger.feature.social.boosts.domain.repository.BoostsRepository
import org.telegram.messenger.feature.social.boosts.domain.usecase.ApplyBoostUseCase
import org.telegram.messenger.feature.social.boosts.domain.usecase.CheckCanApplyBoostUseCase
import org.telegram.messenger.feature.social.boosts.domain.usecase.GetBoostsStatusUseCase
import org.telegram.messenger.feature.social.boosts.domain.usecase.GetMyBoostsUseCase
import org.telegram.messenger.feature.social.boosts.presentation.BoostsViewModel
import org.telegram.messenger.feature.social.contacts.domain.repository.ContactsRepository
import org.telegram.messenger.feature.social.contacts.domain.usecase.AddContactUseCase
import org.telegram.messenger.feature.social.contacts.domain.usecase.DeleteContactUseCase
import org.telegram.messenger.feature.social.contacts.domain.usecase.GetContactUseCase
import org.telegram.messenger.feature.social.contacts.domain.usecase.GetContactsUseCase
import org.telegram.messenger.feature.social.contacts.domain.usecase.ObserveContactsUseCase
import org.telegram.messenger.feature.social.contacts.domain.usecase.SearchContactsUseCase
import org.telegram.messenger.feature.social.contacts.presentation.ContactsViewModel
import org.telegram.messenger.feature.social.di.SocialContainer
import org.telegram.messenger.feature.social.joinrequests.domain.repository.JoinRequestsRepository
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.ApproveAllJoinRequestsUseCase
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.ApproveJoinRequestUseCase
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.DismissAllJoinRequestsUseCase
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.DismissJoinRequestUseCase
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.GetCachedJoinRequestsUseCase
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.GetPendingRequestsCountUseCase
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.LoadJoinRequestsUseCase
import org.telegram.messenger.feature.social.joinrequests.domain.usecase.ObservePendingRequestsUseCase
import org.telegram.messenger.feature.social.joinrequests.presentation.JoinRequestsViewModel
import org.telegram.messenger.feature.social.location.domain.repository.LocationRepository
import org.telegram.messenger.feature.social.location.domain.usecase.GetActiveSharingsUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.GetLastKnownLocationUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.GetSharingInfoUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.IsSharingLocationUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.LoadPeerLiveLocationsUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.MarkLiveLocationsAsReadUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.ObserveActiveSharingsUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.ObserveLastKnownLocationUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.ObservePeerLocationsUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.SendLiveLocationUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.SendStaticLocationUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.SetProximityAlertUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.StopAllLocationSharingsUseCase
import org.telegram.messenger.feature.social.location.domain.usecase.StopLocationSharingUseCase
import org.telegram.messenger.feature.social.location.presentation.LocationViewModel
import org.telegram.messenger.feature.social.profile.domain.repository.ProfileRepository
import org.telegram.messenger.feature.social.profile.domain.usecase.BlockPeerUseCase
import org.telegram.messenger.feature.social.profile.domain.usecase.GetProfileUseCase
import org.telegram.messenger.feature.social.profile.domain.usecase.LoadFullProfileUseCase
import org.telegram.messenger.feature.social.profile.domain.usecase.ObserveProfileUseCase
import org.telegram.messenger.feature.social.profile.domain.usecase.UnblockPeerUseCase
import org.telegram.messenger.feature.social.profile.presentation.ProfileViewModel
import org.telegram.messenger.feature.system.adjustpan.domain.repository.AdjustPanRepository
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.CalculatePanTransitionPlanUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.ComputePanProgressUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.GetAdjustPanStateUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.ObserveAdjustPanStateUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.ResetAdjustPanUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.SetAdjustPanEnabledUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.StartAdjustPanTransitionUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.StopAdjustPanTransitionUseCase
import org.telegram.messenger.feature.system.adjustpan.domain.usecase.UpdateAdjustPanTransitionUseCase
import org.telegram.messenger.feature.system.adjustpan.presentation.AdjustPanViewModel
import org.telegram.messenger.feature.system.appconfig.domain.repository.AppConfigRepository
import org.telegram.messenger.feature.system.appconfig.domain.usecase.GetAiComposeConfigUseCase
import org.telegram.messenger.feature.system.appconfig.domain.usecase.GetAppConfigUseCase
import org.telegram.messenger.feature.system.appconfig.domain.usecase.GetAppLimitsUseCase
import org.telegram.messenger.feature.system.appconfig.domain.usecase.GetMessageLimitsUseCase
import org.telegram.messenger.feature.system.appconfig.domain.usecase.GetPollsConfigUseCase
import org.telegram.messenger.feature.system.appconfig.domain.usecase.GetRichMessageLimitsUseCase
import org.telegram.messenger.feature.system.appconfig.domain.usecase.GetStarsPricingConfigUseCase
import org.telegram.messenger.feature.system.appconfig.domain.usecase.GetTonPricingConfigUseCase
import org.telegram.messenger.feature.system.appconfig.domain.usecase.ObserveAppConfigUseCase
import org.telegram.messenger.feature.system.appconfig.domain.usecase.ReloadAppConfigUseCase
import org.telegram.messenger.feature.system.appconfig.domain.usecase.UpdateAppConfigValueUseCase
import org.telegram.messenger.feature.system.appconfig.presentation.AppConfigViewModel
import org.telegram.messenger.feature.system.browser.domain.repository.BrowserRepository
import org.telegram.messenger.feature.system.browser.domain.usecase.CheckUrlSafetyUseCase
import org.telegram.messenger.feature.system.browser.domain.usecase.ClassifyUrlTargetUseCase
import org.telegram.messenger.feature.system.browser.domain.usecase.ExtractUsernameFromUrlUseCase
import org.telegram.messenger.feature.system.browser.domain.usecase.GetBrowserStateUseCase
import org.telegram.messenger.feature.system.browser.domain.usecase.ManageBrowserHistoryUseCase
import org.telegram.messenger.feature.system.browser.domain.usecase.ObserveBrowserStateUseCase
import org.telegram.messenger.feature.system.browser.domain.usecase.OpenBrowserUrlUseCase
import org.telegram.messenger.feature.system.browser.domain.usecase.UpdateBrowserSettingsUseCase
import org.telegram.messenger.feature.system.browser.presentation.BrowserViewModel
import org.telegram.messenger.feature.system.countdowntimer.domain.repository.CountdownTimerRepository
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.ClearAllCountdownTimersUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.DecomposeCountdownTimeUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.FormatCountdownTimeUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.GetCountdownTimerUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.IsCountdownTimerRunningUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.ObserveCountdownStateUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.ObserveCountdownTimerUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.PauseCountdownTimerUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.ResumeCountdownTimerUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.StartCountdownTimerUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.StopCountdownTimerUseCase
import org.telegram.messenger.feature.system.countdowntimer.domain.usecase.TickCountdownTimerUseCase
import org.telegram.messenger.feature.system.countdowntimer.presentation.CountdownTimerViewModel
import org.telegram.messenger.feature.system.datastorage.domain.repository.DataStorageRepository
import org.telegram.messenger.feature.system.datastorage.domain.usecase.ClearCacheUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.ClearDatabaseUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.GetAutoDownloadPresetUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.GetKeepMediaSettingsUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.GetNetworkUsageUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.GetStorageUsageUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.ObserveAutoDownloadPresetUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.ObserveKeepMediaSettingsUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.ObserveNetworkUsageUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.ObserveStorageUsageUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.RefreshStorageUsageUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.ResetNetworkUsageUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.UpdateAutoDownloadPresetUseCase
import org.telegram.messenger.feature.system.datastorage.domain.usecase.UpdateKeepMediaUseCase
import org.telegram.messenger.feature.system.datastorage.presentation.DataStorageViewModel
import org.telegram.messenger.feature.system.di.SystemContainer
import org.telegram.messenger.feature.system.floatingdebug.domain.repository.FloatingDebugRepository
import org.telegram.messenger.feature.system.floatingdebug.domain.usecase.ClearFloatingDebugItemsUseCase
import org.telegram.messenger.feature.system.floatingdebug.domain.usecase.GetFloatingDebugItemsUseCase
import org.telegram.messenger.feature.system.floatingdebug.domain.usecase.GetFloatingDebugStateUseCase
import org.telegram.messenger.feature.system.floatingdebug.domain.usecase.IsFloatingDebugActiveUseCase
import org.telegram.messenger.feature.system.floatingdebug.domain.usecase.ObserveFloatingDebugStateUseCase
import org.telegram.messenger.feature.system.floatingdebug.domain.usecase.RegisterFloatingDebugItemsUseCase
import org.telegram.messenger.feature.system.floatingdebug.domain.usecase.SetFloatingDebugActiveUseCase
import org.telegram.messenger.feature.system.floatingdebug.domain.usecase.ToggleFloatingDebugActiveUseCase
import org.telegram.messenger.feature.system.floatingdebug.presentation.FloatingDebugViewModel
import org.telegram.messenger.feature.system.hints.domain.repository.HintsRepository
import org.telegram.messenger.feature.system.hints.domain.usecase.DoNotShowAgainHintUseCase
import org.telegram.messenger.feature.system.hints.domain.usecase.GetHintUseCase
import org.telegram.messenger.feature.system.hints.domain.usecase.GetHintsStateUseCase
import org.telegram.messenger.feature.system.hints.domain.usecase.IncrementHintUseCase
import org.telegram.messenger.feature.system.hints.domain.usecase.ObserveHintsUseCase
import org.telegram.messenger.feature.system.hints.domain.usecase.ResetAllHintsUseCase
import org.telegram.messenger.feature.system.hints.domain.usecase.ResetHintUseCase
import org.telegram.messenger.feature.system.hints.domain.usecase.ShouldShowHintUseCase
import org.telegram.messenger.feature.system.hints.presentation.HintsViewModel
import org.telegram.messenger.feature.system.keyboardhide.domain.repository.KeyboardHideRepository
import org.telegram.messenger.feature.system.keyboardhide.domain.usecase.CalculateKeyboardHideProgressUseCase
import org.telegram.messenger.feature.system.keyboardhide.domain.usecase.EndKeyboardHideMovingUseCase
import org.telegram.messenger.feature.system.keyboardhide.domain.usecase.EvaluateKeyboardDismissDecisionUseCase
import org.telegram.messenger.feature.system.keyboardhide.domain.usecase.FinishKeyboardHideDismissUseCase
import org.telegram.messenger.feature.system.keyboardhide.domain.usecase.GetKeyboardHideStateUseCase
import org.telegram.messenger.feature.system.keyboardhide.domain.usecase.ObserveKeyboardHideStateUseCase
import org.telegram.messenger.feature.system.keyboardhide.domain.usecase.ResetKeyboardHideUseCase
import org.telegram.messenger.feature.system.keyboardhide.domain.usecase.SetKeyboardHideEnabledUseCase
import org.telegram.messenger.feature.system.keyboardhide.domain.usecase.StartKeyboardHideMovingUseCase
import org.telegram.messenger.feature.system.keyboardhide.domain.usecase.UpdateKeyboardHideMovingUseCase
import org.telegram.messenger.feature.system.keyboardhide.presentation.KeyboardHideViewModel
import org.telegram.messenger.feature.system.keyboardinsets.domain.repository.KeyboardInsetsRepository
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.GetKeyboardInsetsUseCase
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.ObserveKeyboardInsetsUseCase
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.RequestInAppKeyboardHeightUseCase
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.RequestInAppKeyboardHeightWithNavbarUseCase
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.ResetInAppKeyboardHeightUseCase
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.UpdateSystemInsetsUseCase
import org.telegram.messenger.feature.system.keyboardinsets.presentation.KeyboardInsetsViewModel
import org.telegram.messenger.feature.system.launchericon.domain.repository.LauncherIconRepository
import org.telegram.messenger.feature.system.launchericon.domain.usecase.FixLauncherIconIfNeededUseCase
import org.telegram.messenger.feature.system.launchericon.domain.usecase.GetActiveLauncherIconUseCase
import org.telegram.messenger.feature.system.launchericon.domain.usecase.GetLauncherIconsUseCase
import org.telegram.messenger.feature.system.launchericon.domain.usecase.IsLauncherIconEnabledUseCase
import org.telegram.messenger.feature.system.launchericon.domain.usecase.ObserveLauncherIconsUseCase
import org.telegram.messenger.feature.system.launchericon.domain.usecase.SetLauncherIconUseCase
import org.telegram.messenger.feature.system.launchericon.presentation.LauncherIconViewModel
import org.telegram.messenger.feature.system.litemode.domain.repository.LiteModeRepository
import org.telegram.messenger.feature.system.litemode.domain.usecase.CalculateEffectiveFlagsUseCase
import org.telegram.messenger.feature.system.litemode.domain.usecase.CheckLiteModeFlagUseCase
import org.telegram.messenger.feature.system.litemode.domain.usecase.GetLiteModeStateUseCase
import org.telegram.messenger.feature.system.litemode.domain.usecase.ObserveLiteModeStateUseCase
import org.telegram.messenger.feature.system.litemode.domain.usecase.ResolvePresetUseCase
import org.telegram.messenger.feature.system.litemode.domain.usecase.SetLiteModePresetUseCase
import org.telegram.messenger.feature.system.litemode.domain.usecase.ToggleLiteModeFlagUseCase
import org.telegram.messenger.feature.system.litemode.domain.usecase.UpdatePowerSaverThresholdUseCase
import org.telegram.messenger.feature.system.litemode.presentation.LiteModeViewModel
import org.telegram.messenger.feature.system.localization.domain.repository.LocalizationRepository
import org.telegram.messenger.feature.system.localization.domain.usecase.ApplyLocaleUseCase
import org.telegram.messenger.feature.system.localization.domain.usecase.DetectRtlLanguageUseCase
import org.telegram.messenger.feature.system.localization.domain.usecase.FormatFullNameUseCase
import org.telegram.messenger.feature.system.localization.domain.usecase.FormatNumberWithSuffixUseCase
import org.telegram.messenger.feature.system.localization.domain.usecase.FormatRelativeTimestampUseCase
import org.telegram.messenger.feature.system.localization.domain.usecase.GetLocalizationStateUseCase
import org.telegram.messenger.feature.system.localization.domain.usecase.ObserveLocalizationStateUseCase
import org.telegram.messenger.feature.system.localization.domain.usecase.ResolvePluralQuantityUseCase
import org.telegram.messenger.feature.system.localization.domain.usecase.SetNameDisplayOrderUseCase
import org.telegram.messenger.feature.system.localization.domain.usecase.Toggle24HourFormatUseCase
import org.telegram.messenger.feature.system.localization.presentation.LocalizationViewModel
import org.telegram.messenger.feature.system.maintabs.domain.repository.MainTabsRepository
import org.telegram.messenger.feature.system.maintabs.domain.usecase.GetMainTabsConfigUseCase
import org.telegram.messenger.feature.system.maintabs.domain.usecase.ObserveMainTabsConfigUseCase
import org.telegram.messenger.feature.system.maintabs.domain.usecase.SelectMainTabUseCase
import org.telegram.messenger.feature.system.maintabs.domain.usecase.SetContactsPermissionWarningUseCase
import org.telegram.messenger.feature.system.maintabs.domain.usecase.SetMainTabsVisibleUseCase
import org.telegram.messenger.feature.system.maintabs.domain.usecase.SetShowCallsTabUseCase
import org.telegram.messenger.feature.system.maintabs.domain.usecase.UpdateChatsUnreadCountUseCase
import org.telegram.messenger.feature.system.maintabs.presentation.MainTabsViewModel
import org.telegram.messenger.feature.system.notifications.domain.repository.NotificationsRepository
import org.telegram.messenger.feature.system.notifications.domain.usecase.GetBadgeSettingsUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.GetBadgeUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.GetNotificationSettingsUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.IsDialogMutedUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.MuteDialogUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.ObserveBadgeSettingsUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.ObserveBadgeUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.ObserveNotificationSettingsUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.RefreshBadgeUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.ToggleContactJoinedNotificationsUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.ToggleInAppPreviewUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.ToggleInAppSoundsUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.ToggleInAppVibrateUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.ToggleInChatSoundUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.TogglePeerNotificationsUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.TogglePinnedMessagesNotificationsUseCase
import org.telegram.messenger.feature.system.notifications.domain.usecase.UpdateBadgeSettingsUseCase
import org.telegram.messenger.feature.system.notifications.presentation.NotificationsViewModel
import org.telegram.messenger.feature.system.pinchtozoom.domain.repository.PinchToZoomRepository
import org.telegram.messenger.feature.system.pinchtozoom.domain.usecase.CalculatePinchImageBoundsUseCase
import org.telegram.messenger.feature.system.pinchtozoom.domain.usecase.CalculatePinchScaleUseCase
import org.telegram.messenger.feature.system.pinchtozoom.domain.usecase.CalculatePinchTransformUseCase
import org.telegram.messenger.feature.system.pinchtozoom.domain.usecase.CalculatePinchTranslationUseCase
import org.telegram.messenger.feature.system.pinchtozoom.domain.usecase.EvaluatePinchGestureUseCase
import org.telegram.messenger.feature.system.pinchtozoom.domain.usecase.FinishPinchZoomUseCase
import org.telegram.messenger.feature.system.pinchtozoom.domain.usecase.GetPinchZoomStateUseCase
import org.telegram.messenger.feature.system.pinchtozoom.domain.usecase.ObservePinchZoomStateUseCase
import org.telegram.messenger.feature.system.pinchtozoom.domain.usecase.ResetPinchZoomUseCase
import org.telegram.messenger.feature.system.pinchtozoom.domain.usecase.StartPinchZoomUseCase
import org.telegram.messenger.feature.system.pinchtozoom.domain.usecase.UpdatePinchZoomUseCase
import org.telegram.messenger.feature.system.pinchtozoom.presentation.PinchToZoomViewModel
import org.telegram.messenger.feature.system.recyclerscroll.domain.repository.RecyclerScrollRepository
import org.telegram.messenger.feature.system.recyclerscroll.domain.usecase.CalculateScrollAnimationPlanUseCase
import org.telegram.messenger.feature.system.recyclerscroll.domain.usecase.CalculateScrollLengthUseCase
import org.telegram.messenger.feature.system.recyclerscroll.domain.usecase.CancelRecyclerScrollUseCase
import org.telegram.messenger.feature.system.recyclerscroll.domain.usecase.ComputeScrollViewTranslationsUseCase
import org.telegram.messenger.feature.system.recyclerscroll.domain.usecase.EvaluateScrollEligibilityUseCase
import org.telegram.messenger.feature.system.recyclerscroll.domain.usecase.FinishRecyclerScrollUseCase
import org.telegram.messenger.feature.system.recyclerscroll.domain.usecase.GetRecyclerScrollStateUseCase
import org.telegram.messenger.feature.system.recyclerscroll.domain.usecase.ObserveRecyclerScrollStateUseCase
import org.telegram.messenger.feature.system.recyclerscroll.domain.usecase.ResetRecyclerScrollUseCase
import org.telegram.messenger.feature.system.recyclerscroll.domain.usecase.StartRecyclerScrollUseCase
import org.telegram.messenger.feature.system.recyclerscroll.domain.usecase.UpdateRecyclerScrollProgressUseCase
import org.telegram.messenger.feature.system.recyclerscroll.presentation.RecyclerScrollViewModel
import org.telegram.messenger.feature.system.refreshrate.domain.repository.RefreshRateRepository
import org.telegram.messenger.feature.system.refreshrate.domain.usecase.GetDisplayRefreshModesUseCase
import org.telegram.messenger.feature.system.refreshrate.domain.usecase.GetRefreshRateStateUseCase
import org.telegram.messenger.feature.system.refreshrate.domain.usecase.ObserveRefreshRateStateUseCase
import org.telegram.messenger.feature.system.refreshrate.domain.usecase.RecordFrameMetricUseCase
import org.telegram.messenger.feature.system.refreshrate.domain.usecase.ResetRefreshRateStatsUseCase
import org.telegram.messenger.feature.system.refreshrate.domain.usecase.SetPreferredRefreshRateModeUseCase
import org.telegram.messenger.feature.system.refreshrate.domain.usecase.StartRefreshRateTrackingUseCase
import org.telegram.messenger.feature.system.refreshrate.domain.usecase.StopRefreshRateTrackingUseCase
import org.telegram.messenger.feature.system.refreshrate.domain.usecase.ToggleAdaptiveRefreshRateUseCase
import org.telegram.messenger.feature.system.refreshrate.presentation.RefreshRateViewModel
import org.telegram.messenger.feature.system.ringtones.domain.repository.RingtoneRepository
import org.telegram.messenger.feature.system.ringtones.domain.usecase.AddRingtoneUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.CancelRingtoneUploadUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.GetRingtoneByIdUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.GetRingtoneSoundPathUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.GetRingtonesUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.ObserveRingtoneStateUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.ObserveRingtonesUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.RefreshRingtonesUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.RemoveRingtoneUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.SaveRingtoneFromDocumentUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.SelectRingtoneUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.UploadRingtoneUseCase
import org.telegram.messenger.feature.system.ringtones.domain.usecase.ValidateRingtoneEligibilityUseCase
import org.telegram.messenger.feature.system.ringtones.presentation.RingtoneViewModel
import org.telegram.messenger.feature.system.settings.domain.repository.SettingsRepository
import org.telegram.messenger.feature.system.settings.domain.usecase.GetSettingsUseCase
import org.telegram.messenger.feature.system.settings.domain.usecase.ObserveSettingsUseCase
import org.telegram.messenger.feature.system.settings.domain.usecase.UpdateBubbleRadiusUseCase
import org.telegram.messenger.feature.system.settings.domain.usecase.UpdateFontSizeUseCase
import org.telegram.messenger.feature.system.settings.domain.usecase.UpdateSaveToGalleryUseCase
import org.telegram.messenger.feature.system.settings.domain.usecase.UpdateStreamMediaUseCase
import org.telegram.messenger.feature.system.settings.domain.usecase.UpdateSyncContactsUseCase
import org.telegram.messenger.feature.system.settings.presentation.SettingsViewModel
import org.telegram.messenger.feature.system.themes.domain.repository.ThemeRepository
import org.telegram.messenger.feature.system.themes.domain.usecase.ApplyThemeUseCase
import org.telegram.messenger.feature.system.themes.domain.usecase.GetAppearanceSettingsUseCase
import org.telegram.messenger.feature.system.themes.domain.usecase.GetAvailableThemesUseCase
import org.telegram.messenger.feature.system.themes.domain.usecase.ObserveAppearanceSettingsUseCase
import org.telegram.messenger.feature.system.themes.domain.usecase.ObserveAvailableThemesUseCase
import org.telegram.messenger.feature.system.themes.domain.usecase.ObserveNightModeUseCase
import org.telegram.messenger.feature.system.themes.domain.usecase.ResetAppearanceSettingsUseCase
import org.telegram.messenger.feature.system.themes.domain.usecase.SetBubbleRadiusUseCase
import org.telegram.messenger.feature.system.themes.domain.usecase.SetNightModeSettingsUseCase
import org.telegram.messenger.feature.system.themes.domain.usecase.SetNightModeTypeUseCase
import org.telegram.messenger.feature.system.themes.domain.usecase.SetThemeAccentUseCase
import org.telegram.messenger.feature.system.themes.presentation.ThemeViewModel
import org.telegram.messenger.feature.system.windowvisibility.domain.repository.WindowVisibilityRepository
import org.telegram.messenger.feature.system.windowvisibility.domain.usecase.CheckIsWindowVisibleUseCase
import org.telegram.messenger.feature.system.windowvisibility.domain.usecase.CreateVisibilityControllerUseCase
import org.telegram.messenger.feature.system.windowvisibility.domain.usecase.GetActiveHideReasonsUseCase
import org.telegram.messenger.feature.system.windowvisibility.domain.usecase.GetWindowVisibilityStateUseCase
import org.telegram.messenger.feature.system.windowvisibility.domain.usecase.ObserveWindowVisibilityChangesUseCase
import org.telegram.messenger.feature.system.windowvisibility.domain.usecase.ObserveWindowVisibilityStateUseCase
import org.telegram.messenger.feature.system.windowvisibility.domain.usecase.ReleaseHideWindowUseCase
import org.telegram.messenger.feature.system.windowvisibility.domain.usecase.RequestHideWindowUseCase
import org.telegram.messenger.feature.system.windowvisibility.domain.usecase.ResetWindowVisibilityUseCase
import org.telegram.messenger.feature.system.windowvisibility.domain.usecase.ToggleWindowHideUseCase
import org.telegram.messenger.feature.system.windowvisibility.presentation.WindowVisibilityViewModel
import org.telegram.ui.Components.chat.ChatActivityBottomViewsVisibilityController
import org.telegram.ui.Components.chat.ChatActivityDraftMessageMeasureController
import org.telegram.ui.Components.inset.WindowInsetsInAppController
import org.telegram.ui.LaunchActivity
import org.telegram.ui.MainTabsActivityController

class AccountFeatureContainer private constructor(val account: Int) {

    val business: BusinessContainer by lazy { BusinessContainer(account) }
    val media: MediaContainer by lazy { MediaContainer(account) }
    val messaging: MessagingContainer by lazy { MessagingContainer(account) }
    val network: NetworkContainer by lazy { NetworkContainer(account) }
    val security: SecurityContainer by lazy { SecurityContainer(account) }
    val social: SocialContainer by lazy { SocialContainer(account) }
    val system: SystemContainer by lazy { SystemContainer(account) }

    // ==================== BUSINESS DOMAIN ====================

    var paymentsRepository: PaymentsRepository
        get() = business.paymentsRepository
        set(value) { business.paymentsRepository = value }

    val observeStarsBalanceUseCase: ObserveStarsBalanceUseCase
        get() = business.observeStarsBalanceUseCase

    val observeStarTransactionsUseCase: ObserveStarTransactionsUseCase
        get() = business.observeStarTransactionsUseCase

    val observeStarSubscriptionsUseCase: ObserveStarSubscriptionsUseCase
        get() = business.observeStarSubscriptionsUseCase

    val getStarsBalanceUseCase: GetStarsBalanceUseCase
        get() = business.getStarsBalanceUseCase

    val getStarTransactionsUseCase: GetStarTransactionsUseCase
        get() = business.getStarTransactionsUseCase

    val getStarSubscriptionsUseCase: GetStarSubscriptionsUseCase
        get() = business.getStarSubscriptionsUseCase

    val getStarTopupOptionsUseCase: GetStarTopupOptionsUseCase
        get() = business.getStarTopupOptionsUseCase

    val refreshStarsBalanceUseCase: RefreshStarsBalanceUseCase
        get() = business.refreshStarsBalanceUseCase

    val refreshStarTransactionsUseCase: RefreshStarTransactionsUseCase
        get() = business.refreshStarTransactionsUseCase

    val refreshStarSubscriptionsUseCase: RefreshStarSubscriptionsUseCase
        get() = business.refreshStarSubscriptionsUseCase

    val paymentsViewModel: PaymentsViewModel
        get() = business.paymentsViewModel

    fun createPaymentsViewModel(): PaymentsViewModel = business.createPaymentsViewModel()

    var quickRepliesRepository: QuickRepliesRepository
        get() = business.quickRepliesRepository
        set(value) { business.quickRepliesRepository = value }

    val observeQuickRepliesUseCase: ObserveQuickRepliesUseCase
        get() = business.observeQuickRepliesUseCase

    val getQuickRepliesUseCase: GetQuickRepliesUseCase
        get() = business.getQuickRepliesUseCase

    val loadQuickRepliesUseCase: LoadQuickRepliesUseCase
        get() = business.loadQuickRepliesUseCase

    val findQuickReplyUseCase: FindQuickReplyUseCase
        get() = business.findQuickReplyUseCase

    val checkQuickReplyNameBusyUseCase: CheckQuickReplyNameBusyUseCase
        get() = business.checkQuickReplyNameBusyUseCase

    val canAddNewQuickReplyUseCase: CanAddNewQuickReplyUseCase
        get() = business.canAddNewQuickReplyUseCase

    val renameQuickReplyUseCase: RenameQuickReplyUseCase
        get() = business.renameQuickReplyUseCase

    val reorderQuickRepliesUseCase: ReorderQuickRepliesUseCase
        get() = business.reorderQuickRepliesUseCase

    val deleteQuickRepliesUseCase: DeleteQuickRepliesUseCase
        get() = business.deleteQuickRepliesUseCase

    val sendQuickReplyUseCase: SendQuickReplyUseCase
        get() = business.sendQuickReplyUseCase

    val quickRepliesViewModel: QuickRepliesViewModel
        get() = business.quickRepliesViewModel

    fun createQuickRepliesViewModel(): QuickRepliesViewModel = business.createQuickRepliesViewModel()

    var starGiftsRepository: StarGiftsRepository
        get() = business.starGiftsRepository
        set(value) { business.starGiftsRepository = value }

    val observeStarGiftsCatalogUseCase: ObserveStarGiftsCatalogUseCase
        get() = business.observeStarGiftsCatalogUseCase

    val getStarGiftsCatalogUseCase: GetStarGiftsCatalogUseCase
        get() = business.getStarGiftsCatalogUseCase

    val getStarGiftByIdUseCase: GetStarGiftByIdUseCase
        get() = business.getStarGiftByIdUseCase

    val observeProfileGiftsUseCase: ObserveProfileGiftsUseCase
        get() = business.observeProfileGiftsUseCase

    val loadProfileGiftsUseCase: LoadProfileGiftsUseCase
        get() = business.loadProfileGiftsUseCase

    val togglePinProfileGiftUseCase: TogglePinProfileGiftUseCase
        get() = business.togglePinProfileGiftUseCase

    val toggleHideProfileGiftUseCase: ToggleHideProfileGiftUseCase
        get() = business.toggleHideProfileGiftUseCase

    val starGiftsViewModel: StarGiftsViewModel
        get() = business.starGiftsViewModel

    fun createStarGiftsViewModel(): StarGiftsViewModel = business.createStarGiftsViewModel()

    var giftAuctionsRepository: GiftAuctionsRepository
        get() = business.giftAuctionsRepository
        set(value) { business.giftAuctionsRepository = value }

    val observeActiveAuctionsUseCase: ObserveActiveAuctionsUseCase
        get() = business.observeActiveAuctionsUseCase

    val observeAuctionUseCase: ObserveAuctionUseCase
        get() = business.observeAuctionUseCase

    val getActiveAuctionsUseCase: GetActiveAuctionsUseCase
        get() = business.getActiveAuctionsUseCase

    val getAuctionByIdUseCase: GetAuctionByIdUseCase
        get() = business.getAuctionByIdUseCase

    val getAuctionBySlugUseCase: GetAuctionBySlugUseCase
        get() = business.getAuctionBySlugUseCase

    val sendAuctionBidUseCase: SendAuctionBidUseCase
        get() = business.sendAuctionBidUseCase

    val loadAuctionAcquiredGiftsUseCase: LoadAuctionAcquiredGiftsUseCase
        get() = business.loadAuctionAcquiredGiftsUseCase

    val refreshActiveAuctionsUseCase: RefreshActiveAuctionsUseCase
        get() = business.refreshActiveAuctionsUseCase

    val giftAuctionsViewModel: GiftAuctionsViewModel
        get() = business.giftAuctionsViewModel

    fun createGiftAuctionsViewModel(): GiftAuctionsViewModel = business.createGiftAuctionsViewModel()

    var businessLinksRepository: BusinessLinksRepository
        get() = business.businessLinksRepository
        set(value) { business.businessLinksRepository = value }

    val observeBusinessLinksUseCase: ObserveBusinessLinksUseCase
        get() = business.observeBusinessLinksUseCase

    val getBusinessLinksUseCase: GetBusinessLinksUseCase
        get() = business.getBusinessLinksUseCase

    val loadBusinessLinksUseCase: LoadBusinessLinksUseCase
        get() = business.loadBusinessLinksUseCase

    val createBusinessLinkUseCase: CreateBusinessLinkUseCase
        get() = business.createBusinessLinkUseCase

    val editBusinessLinkUseCase: EditBusinessLinkUseCase
        get() = business.editBusinessLinkUseCase

    val deleteBusinessLinkUseCase: DeleteBusinessLinkUseCase
        get() = business.deleteBusinessLinkUseCase

    val findBusinessLinkUseCase: FindBusinessLinkUseCase
        get() = business.findBusinessLinkUseCase

    val canAddNewBusinessLinkUseCase: CanAddNewBusinessLinkUseCase
        get() = business.canAddNewBusinessLinkUseCase

    val businessLinksViewModel: BusinessLinksViewModel
        get() = business.businessLinksViewModel

    fun createBusinessLinksViewModel(): BusinessLinksViewModel = business.createBusinessLinksViewModel()

    var businessBotsRepository: BusinessBotsRepository
        get() = business.businessBotsRepository
        set(value) { business.businessBotsRepository = value }

    val observeConnectedBotsUseCase: ObserveConnectedBotsUseCase
        get() = business.observeConnectedBotsUseCase

    val getConnectedBotsUseCase: GetConnectedBotsUseCase
        get() = business.getConnectedBotsUseCase

    val loadConnectedBotsUseCase: LoadConnectedBotsUseCase
        get() = business.loadConnectedBotsUseCase

    val updateConnectedBotUseCase: UpdateConnectedBotUseCase
        get() = business.updateConnectedBotUseCase

    val deleteConnectedBotUseCase: DeleteConnectedBotUseCase
        get() = business.deleteConnectedBotUseCase

    val findConnectedBotUseCase: FindConnectedBotUseCase
        get() = business.findConnectedBotUseCase

    val businessBotsViewModel: BusinessBotsViewModel
        get() = business.businessBotsViewModel

    fun createBusinessBotsViewModel(): BusinessBotsViewModel = business.createBusinessBotsViewModel()

    var timezonesRepository: TimezonesRepository
        get() = business.timezonesRepository
        set(value) { business.timezonesRepository = value }

    val observeTimezonesUseCase: ObserveTimezonesUseCase
        get() = business.observeTimezonesUseCase

    val getTimezonesUseCase: GetTimezonesUseCase
        get() = business.getTimezonesUseCase

    val loadTimezonesUseCase: LoadTimezonesUseCase
        get() = business.loadTimezonesUseCase

    val findTimezoneUseCase: FindTimezoneUseCase
        get() = business.findTimezoneUseCase

    val getSystemTimezoneIdUseCase: GetSystemTimezoneIdUseCase
        get() = business.getSystemTimezoneIdUseCase

    val getTimezoneNameUseCase: GetTimezoneNameUseCase
        get() = business.getTimezoneNameUseCase

    val timezonesViewModel: TimezonesViewModel
        get() = business.timezonesViewModel

    fun createTimezonesViewModel(): TimezonesViewModel = business.createTimezonesViewModel()

    var botStarsRepository: BotStarsRepository
        get() = business.botStarsRepository
        set(value) { business.botStarsRepository = value }

    val observeBotStarsStatsUseCase: ObserveBotStarsStatsUseCase
        get() = business.observeBotStarsStatsUseCase

    val getBotStarsStatsUseCase: GetBotStarsStatsUseCase
        get() = business.getBotStarsStatsUseCase

    val observeTonStatsUseCase: ObserveTonStatsUseCase
        get() = business.observeTonStatsUseCase

    val getTonStatsUseCase: GetTonStatsUseCase
        get() = business.getTonStatsUseCase

    val observeBotTransactionsUseCase: ObserveBotTransactionsUseCase
        get() = business.observeBotTransactionsUseCase

    val loadBotTransactionsUseCase: LoadBotTransactionsUseCase
        get() = business.loadBotTransactionsUseCase

    val observeConnectedStarBotsUseCase: ObserveConnectedStarBotsUseCase
        get() = business.observeConnectedStarBotsUseCase

    val loadConnectedStarBotsUseCase: LoadConnectedStarBotsUseCase
        get() = business.loadConnectedStarBotsUseCase

    val loadSuggestedStarBotsUseCase: LoadSuggestedStarBotsUseCase
        get() = business.loadSuggestedStarBotsUseCase

    val getAdminedBotsAndChannelsUseCase: GetAdminedBotsAndChannelsUseCase
        get() = business.getAdminedBotsAndChannelsUseCase

    val botStarsViewModel: BotStarsViewModel
        get() = business.botStarsViewModel

    fun createBotStarsViewModel(): BotStarsViewModel = business.createBotStarsViewModel()

    var billingRepository: BillingRepository
        get() = business.billingRepository
        set(value) { business.billingRepository = value }

    val observeBillingStateUseCase: ObserveBillingStateUseCase
        get() = business.observeBillingStateUseCase

    val getBillingStateUseCase: GetBillingStateUseCase
        get() = business.getBillingStateUseCase

    val startBillingConnectionUseCase: StartBillingConnectionUseCase
        get() = business.startBillingConnectionUseCase

    val getPremiumProductUseCase: GetPremiumProductUseCase
        get() = business.getPremiumProductUseCase

    val formatCurrencyUseCase: FormatCurrencyUseCase
        get() = business.formatCurrencyUseCase

    val getCurrencyExpUseCase: GetCurrencyExpUseCase
        get() = business.getCurrencyExpUseCase

    val queryBillingPurchasesUseCase: QueryBillingPurchasesUseCase
        get() = business.queryBillingPurchasesUseCase

    val manageSubscriptionUseCase: ManageSubscriptionUseCase
        get() = business.manageSubscriptionUseCase

    val billingViewModel: BillingViewModel
        get() = business.billingViewModel

    fun createBillingViewModel(): BillingViewModel = business.createBillingViewModel()

    fun createBusinessRecipientsRepository(): BusinessRecipientsRepository = business.createBusinessRecipientsRepository()

    val businessRecipientsRepository: BusinessRecipientsRepository
        get() = business.businessRecipientsRepository

    val observeBusinessRecipientsUseCase: ObserveBusinessRecipientsUseCase
        get() = business.observeBusinessRecipientsUseCase

    val getBusinessRecipientsUseCase: GetBusinessRecipientsUseCase
        get() = business.getBusinessRecipientsUseCase

    val setBusinessRecipientsUseCase: SetBusinessRecipientsUseCase
        get() = business.setBusinessRecipientsUseCase

    val toggleExcludeSelectedUseCase: ToggleExcludeSelectedUseCase
        get() = business.toggleExcludeSelectedUseCase

    val toggleRecipientFilterUseCase: ToggleRecipientFilterUseCase
        get() = business.toggleRecipientFilterUseCase

    val addSelectedUsersUseCase: AddSelectedUsersUseCase
        get() = business.addSelectedUsersUseCase

    val removeSelectedUserUseCase: RemoveSelectedUserUseCase
        get() = business.removeSelectedUserUseCase

    val addExcludedUsersUseCase: AddExcludedUsersUseCase
        get() = business.addExcludedUsersUseCase

    val removeExcludedUserUseCase: RemoveExcludedUserUseCase
        get() = business.removeExcludedUserUseCase

    val checkRecipientsChangesUseCase: CheckRecipientsChangesUseCase
        get() = business.checkRecipientsChangesUseCase

    val validateBusinessRecipientsUseCase: ValidateBusinessRecipientsUseCase
        get() = business.validateBusinessRecipientsUseCase

    val resetBusinessRecipientsUseCase: ResetBusinessRecipientsUseCase
        get() = business.resetBusinessRecipientsUseCase

    val businessRecipientsViewModel: BusinessRecipientsViewModel
        get() = business.businessRecipientsViewModel

    fun createBusinessRecipientsViewModel(): BusinessRecipientsViewModel = business.createBusinessRecipientsViewModel()

    // ==================== MEDIA DOMAIN ====================

    var mediaRepository: MediaRepository
        get() = media.mediaRepository
        set(value) { media.mediaRepository = value }

    val observeMediaAlbumsUseCase: ObserveMediaAlbumsUseCase
        get() = media.observeMediaAlbumsUseCase

    val getMediaAlbumsUseCase: GetMediaAlbumsUseCase
        get() = media.getMediaAlbumsUseCase

    val getAlbumMediaUseCase: GetAlbumMediaUseCase
        get() = media.getAlbumMediaUseCase

    val getAllMediaUseCase: GetAllMediaUseCase
        get() = media.getAllMediaUseCase

    val mediaViewModel: MediaViewModel
        get() = media.mediaViewModel

    fun createMediaViewModel(): MediaViewModel = media.createMediaViewModel()

    var voipRepository: VoIPRepository
        get() = media.voipRepository
        set(value) { media.voipRepository = value }

    val observeCurrentCallUseCase: ObserveCurrentCallUseCase
        get() = media.observeCurrentCallUseCase

    val getCurrentCallUseCase: GetCurrentCallUseCase
        get() = media.getCurrentCallUseCase

    val startCallUseCase: StartCallUseCase
        get() = media.startCallUseCase

    val acceptCallUseCase: AcceptCallUseCase
        get() = media.acceptCallUseCase

    val declineCallUseCase: DeclineCallUseCase
        get() = media.declineCallUseCase

    val hangUpCallUseCase: HangUpCallUseCase
        get() = media.hangUpCallUseCase

    val toggleMuteUseCase: ToggleMuteUseCase
        get() = media.toggleMuteUseCase

    val toggleSpeakerphoneUseCase: ToggleSpeakerphoneUseCase
        get() = media.toggleSpeakerphoneUseCase

    val callViewModel: CallViewModel
        get() = media.callViewModel

    fun createCallViewModel(): CallViewModel = media.createCallViewModel()

    var fileLoaderRepository: FileLoaderRepository
        get() = media.fileLoaderRepository
        set(value) { media.fileLoaderRepository = value }

    val observeTransfersUseCase: ObserveTransfersUseCase
        get() = media.observeTransfersUseCase

    val observeTransferUseCase: ObserveTransferUseCase
        get() = media.observeTransferUseCase

    val getActiveDownloadsUseCase: GetActiveDownloadsUseCase
        get() = media.getActiveDownloadsUseCase

    val getRecentDownloadsUseCase: GetRecentDownloadsUseCase
        get() = media.getRecentDownloadsUseCase

    val loadFileUseCase: LoadFileUseCase
        get() = media.loadFileUseCase

    val cancelLoadFileUseCase: CancelLoadFileUseCase
        get() = media.cancelLoadFileUseCase

    val cancelAllDownloadsUseCase: CancelAllDownloadsUseCase
        get() = media.cancelAllDownloadsUseCase

    val uploadFileUseCase: UploadFileUseCase
        get() = media.uploadFileUseCase

    val cancelFileUploadUseCase: CancelFileUploadUseCase
        get() = media.cancelFileUploadUseCase

    val fileLoaderViewModel: FileLoaderViewModel
        get() = media.fileLoaderViewModel

    fun createFileLoaderViewModel(): FileLoaderViewModel = media.createFileLoaderViewModel()

    var storiesRepository: StoriesRepository
        get() = media.storiesRepository
        set(value) { media.storiesRepository = value }

    val observeStoriesUseCase: ObserveStoriesUseCase
        get() = media.observeStoriesUseCase

    val observeHiddenStoriesUseCase: ObserveHiddenStoriesUseCase
        get() = media.observeHiddenStoriesUseCase

    val observeStealthModeUseCase: ObserveStealthModeUseCase
        get() = media.observeStealthModeUseCase

    val observeSelfStoriesUseCase: ObserveSelfStoriesUseCase
        get() = media.observeSelfStoriesUseCase

    val getPeerStoriesUseCase: GetPeerStoriesUseCase
        get() = media.getPeerStoriesUseCase

    val markStoryAsReadUseCase: MarkStoryAsReadUseCase
        get() = media.markStoryAsReadUseCase

    val deleteStoryUseCase: DeleteStoryUseCase
        get() = media.deleteStoryUseCase

    val toggleStoryPinUseCase: ToggleStoryPinUseCase
        get() = media.toggleStoryPinUseCase

    val toggleStoryHiddenUseCase: ToggleStoryHiddenUseCase
        get() = media.toggleStoryHiddenUseCase

    val activateStealthModeUseCase: ActivateStealthModeUseCase
        get() = media.activateStealthModeUseCase

    val getStoryLimitUseCase: GetStoryLimitUseCase
        get() = media.getStoryLimitUseCase

    val refreshStoriesUseCase: RefreshStoriesUseCase
        get() = media.refreshStoriesUseCase

    val storiesViewModel: StoriesViewModel
        get() = media.storiesViewModel

    fun createStoriesViewModel(): StoriesViewModel = media.createStoriesViewModel()

    var chromecastRepository: ChromecastRepository
        get() = media.chromecastRepository
        set(value) { media.chromecastRepository = value }

    val observeChromecastStateUseCase: ObserveChromecastStateUseCase
        get() = media.observeChromecastStateUseCase

    val getChromecastStateUseCase: GetChromecastStateUseCase
        get() = media.getChromecastStateUseCase

    val isCastingUseCase: IsCastingUseCase
        get() = media.isCastingUseCase

    val isMediaPlayingOnCastUseCase: IsMediaPlayingOnCastUseCase
        get() = media.isMediaPlayingOnCastUseCase

    val castMediaUseCase: CastMediaUseCase
        get() = media.castMediaUseCase

    val stopCastingUseCase: StopCastingUseCase
        get() = media.stopCastingUseCase

    val setCastCoverFileUseCase: SetCastCoverFileUseCase
        get() = media.setCastCoverFileUseCase

    val chromecastViewModel: ChromecastViewModel
        get() = media.chromecastViewModel

    fun createChromecastViewModel(): ChromecastViewModel = media.createChromecastViewModel()

    var gallerySaveRepository: GallerySaveRepository
        get() = media.gallerySaveRepository
        set(value) { media.gallerySaveRepository = value }

    val observeGallerySaveConfigUseCase: ObserveGallerySaveConfigUseCase
        get() = media.observeGallerySaveConfigUseCase

    val getGallerySaveConfigUseCase: GetGallerySaveConfigUseCase
        get() = media.getGallerySaveConfigUseCase

    val getGallerySaveSettingsUseCase: GetGallerySaveSettingsUseCase
        get() = media.getGallerySaveSettingsUseCase

    val updateGallerySaveSettingsUseCase: UpdateGallerySaveSettingsUseCase
        get() = media.updateGallerySaveSettingsUseCase

    val toggleGallerySavePeerTypeUseCase: ToggleGallerySavePeerTypeUseCase
        get() = media.toggleGallerySavePeerTypeUseCase

    val setGallerySaveVideoLimitUseCase: SetGallerySaveVideoLimitUseCase
        get() = media.setGallerySaveVideoLimitUseCase

    val getGallerySaveExceptionsUseCase: GetGallerySaveExceptionsUseCase
        get() = media.getGallerySaveExceptionsUseCase

    val setGallerySaveExceptionUseCase: SetGallerySaveExceptionUseCase
        get() = media.setGallerySaveExceptionUseCase

    val removeGallerySaveExceptionUseCase: RemoveGallerySaveExceptionUseCase
        get() = media.removeGallerySaveExceptionUseCase

    val removeAllGallerySaveExceptionsUseCase: RemoveAllGallerySaveExceptionsUseCase
        get() = media.removeAllGallerySaveExceptionsUseCase

    val gallerySaveViewModel: GallerySaveViewModel
        get() = media.gallerySaveViewModel

    fun createGallerySaveViewModel(): GallerySaveViewModel = media.createGallerySaveViewModel()

    var pipRepository: PipRepository
        get() = media.pipRepository
        set(value) { media.pipRepository = value }

    val observePipSessionUseCase: ObservePipSessionUseCase
        get() = media.observePipSessionUseCase

    val getPipSessionUseCase: GetPipSessionUseCase
        get() = media.getPipSessionUseCase

    val registerPipSourceUseCase: RegisterPipSourceUseCase
        get() = media.registerPipSourceUseCase

    val unregisterPipSourceUseCase: UnregisterPipSourceUseCase
        get() = media.unregisterPipSourceUseCase

    val updatePipSourceStateUseCase: UpdatePipSourceStateUseCase
        get() = media.updatePipSourceStateUseCase

    val dispatchPipStateUseCase: DispatchPipStateUseCase
        get() = media.dispatchPipStateUseCase

    val triggerPipActionUseCase: TriggerPipActionUseCase
        get() = media.triggerPipActionUseCase

    val evaluatePipEligibilityUseCase: EvaluatePipEligibilityUseCase
        get() = media.evaluatePipEligibilityUseCase

    val pipViewModel: PipViewModel
        get() = media.pipViewModel

    fun createPipViewModel(): PipViewModel = media.createPipViewModel()

    var fileRefRepository: FileRefRepository
        get() = media.fileRefRepository
        set(value) { media.fileRefRepository = value }

    val observeFileRefStatsUseCase: ObserveFileRefStatsUseCase
        get() = media.observeFileRefStatsUseCase

    val getFileRefStatsUseCase: GetFileRefStatsUseCase
        get() = media.getFileRefStatsUseCase

    val requestReferenceRenewalUseCase: RequestReferenceRenewalUseCase
        get() = media.requestReferenceRenewalUseCase

    val notifyReferenceRenewedUseCase: NotifyReferenceRenewedUseCase
        get() = media.notifyReferenceRenewedUseCase

    val cancelFileRefRequestUseCase: CancelFileRefRequestUseCase
        get() = media.cancelFileRefRequestUseCase

    val clearFileRefCacheUseCase: ClearFileRefCacheUseCase
        get() = media.clearFileRefCacheUseCase

    val fileRefViewModel: FileRefViewModel
        get() = media.fileRefViewModel

    fun createFileRefViewModel(): FileRefViewModel = media.createFileRefViewModel()

    var cameraRepository: CameraRepository
        get() = media.cameraRepository
        set(value) { media.cameraRepository = value }

    val observeCameraStateUseCase: ObserveCameraStateUseCase
        get() = media.observeCameraStateUseCase

    val getCameraStateUseCase: GetCameraStateUseCase
        get() = media.getCameraStateUseCase

    val initCamerasUseCase: InitCamerasUseCase
        get() = media.initCamerasUseCase

    val selectCameraUseCase: SelectCameraUseCase
        get() = media.selectCameraUseCase

    val switchCameraUseCase: SwitchCameraUseCase
        get() = media.switchCameraUseCase

    val setCameraFlashModeUseCase: SetCameraFlashModeUseCase
        get() = media.setCameraFlashModeUseCase

    val toggleMirrorFrontCameraUseCase: ToggleMirrorFrontCameraUseCase
        get() = media.toggleMirrorFrontCameraUseCase

    val chooseOptimalResolutionUseCase: ChooseOptimalResolutionUseCase
        get() = media.chooseOptimalResolutionUseCase

    val notifyCameraRecordingUseCase: NotifyCameraRecordingUseCase
        get() = media.notifyCameraRecordingUseCase

    val cameraViewModel: CameraViewModel
        get() = media.cameraViewModel

    fun createCameraViewModel(): CameraViewModel = media.createCameraViewModel()

    var cacheByChatsRepository: CacheByChatsRepository
        get() = media.cacheByChatsRepository
        set(value) { media.cacheByChatsRepository = value }

    val observeCacheByChatsConfigUseCase: ObserveCacheByChatsConfigUseCase
        get() = media.observeCacheByChatsConfigUseCase

    val getCacheByChatsConfigUseCase: GetCacheByChatsConfigUseCase
        get() = media.getCacheByChatsConfigUseCase

    val setKeepMediaDurationUseCase: SetKeepMediaDurationUseCase
        get() = media.setKeepMediaDurationUseCase

    val setKeepMediaExceptionUseCase: SetKeepMediaExceptionUseCase
        get() = media.setKeepMediaExceptionUseCase

    val removeKeepMediaExceptionUseCase: RemoveKeepMediaExceptionUseCase
        get() = media.removeKeepMediaExceptionUseCase

    val clearKeepMediaExceptionsUseCase: ClearKeepMediaExceptionsUseCase
        get() = media.clearKeepMediaExceptionsUseCase

    val cacheByChatsViewModel: CacheByChatsViewModel
        get() = media.cacheByChatsViewModel

    fun createCacheByChatsViewModel(): CacheByChatsViewModel = media.createCacheByChatsViewModel()

    fun createSharedMediaRepository(): SharedMediaRepository = media.createSharedMediaRepository()

    val sharedMediaRepository: SharedMediaRepository
        get() = media.sharedMediaRepository

    val resolveAvailableTabsUseCase: ResolveAvailableTabsUseCase
        get() = media.resolveAvailableTabsUseCase

    val filterSharedMediaUseCase: FilterSharedMediaUseCase
        get() = media.filterSharedMediaUseCase

    val groupMediaByMonthUseCase: GroupMediaByMonthUseCase
        get() = media.groupMediaByMonthUseCase

    val calculateMediaSelectionUseCase: CalculateMediaSelectionUseCase
        get() = media.calculateMediaSelectionUseCase

    val observeSharedMediaStateUseCase: ObserveSharedMediaStateUseCase
        get() = media.observeSharedMediaStateUseCase

    val getSharedMediaStateUseCase: GetSharedMediaStateUseCase
        get() = media.getSharedMediaStateUseCase

    val selectSharedMediaTabUseCase: SelectSharedMediaTabUseCase
        get() = media.selectSharedMediaTabUseCase

    val setSharedMediaFilterUseCase: SetSharedMediaFilterUseCase
        get() = media.setSharedMediaFilterUseCase

    val toggleMediaSelectionUseCase: ToggleMediaSelectionUseCase
        get() = media.toggleMediaSelectionUseCase

    val clearMediaSelectionUseCase: ClearMediaSelectionUseCase
        get() = media.clearMediaSelectionUseCase

    val sharedMediaViewModel: SharedMediaViewModel
        get() = media.sharedMediaViewModel

    fun createSharedMediaViewModel(): SharedMediaViewModel = media.createSharedMediaViewModel()

    fun createContentPreviewRepository(): ContentPreviewRepository = media.createContentPreviewRepository()

    val contentPreviewRepository: ContentPreviewRepository
        get() = media.contentPreviewRepository

    val evaluatePreviewEligibilityUseCase: EvaluatePreviewEligibilityUseCase
        get() = media.evaluatePreviewEligibilityUseCase

    val calculatePreviewDragUseCase: CalculatePreviewDragUseCase
        get() = media.calculatePreviewDragUseCase

    val resolvePreviewActionsUseCase: ResolvePreviewActionsUseCase
        get() = media.resolvePreviewActionsUseCase

    val observeContentPreviewStateUseCase: ObserveContentPreviewStateUseCase
        get() = media.observeContentPreviewStateUseCase

    val getContentPreviewStateUseCase: GetContentPreviewStateUseCase
        get() = media.getContentPreviewStateUseCase

    val openContentPreviewUseCase: OpenContentPreviewUseCase
        get() = media.openContentPreviewUseCase

    val updatePreviewDragUseCase: UpdatePreviewDragUseCase
        get() = media.updatePreviewDragUseCase

    val triggerPreviewActionUseCase: TriggerPreviewActionUseCase
        get() = media.triggerPreviewActionUseCase

    val dismissContentPreviewUseCase: DismissContentPreviewUseCase
        get() = media.dismissContentPreviewUseCase

    val clearContentPreviewUseCase: ClearContentPreviewUseCase
        get() = media.clearContentPreviewUseCase

    val contentPreviewViewModel: ContentPreviewViewModel
        get() = media.contentPreviewViewModel

    fun createContentPreviewViewModel(): ContentPreviewViewModel = media.createContentPreviewViewModel()

    var photoViewerRepository: PhotoViewerRepository
        get() = media.photoViewerRepository
        set(value) { media.photoViewerRepository = value }

    val calculateMediaPagingUseCase: CalculateMediaPagingUseCase
        get() = media.calculateMediaPagingUseCase

    val calculateZoomTransformUseCase: CalculateZoomTransformUseCase
        get() = media.calculateZoomTransformUseCase

    val validateViewerActionsUseCase: ValidateViewerActionsUseCase
        get() = media.validateViewerActionsUseCase

    val resolveMediaQualityUseCase: ResolveMediaQualityUseCase
        get() = media.resolveMediaQualityUseCase

    val observePhotoViewerStateUseCase: ObservePhotoViewerStateUseCase
        get() = media.observePhotoViewerStateUseCase

    val getPhotoViewerStateUseCase: GetPhotoViewerStateUseCase
        get() = media.getPhotoViewerStateUseCase

    val openPhotoViewerUseCase: OpenPhotoViewerUseCase
        get() = media.openPhotoViewerUseCase

    val navigatePhotoViewerUseCase: NavigatePhotoViewerUseCase
        get() = media.navigatePhotoViewerUseCase

    val updatePlaybackStateUseCase: UpdatePlaybackStateUseCase
        get() = media.updatePlaybackStateUseCase

    val closePhotoViewerUseCase: ClosePhotoViewerUseCase
        get() = media.closePhotoViewerUseCase

    val photoViewerViewModel: PhotoViewerViewModel
        get() = media.photoViewerViewModel

    fun createPhotoViewerViewModel(): PhotoViewerViewModel = media.createPhotoViewerViewModel()

    val audioPlayerRepository: AudioPlayerRepository
        get() = media.audioPlayerRepository

    val observeAudioPlaybackStateUseCase: ObservePlaybackStateUseCase
        get() = media.observeAudioPlaybackStateUseCase

    val getAudioPlaybackStateUseCase: GetPlaybackStateUseCase
        get() = media.getAudioPlaybackStateUseCase

    val playTrackUseCase: PlayTrackUseCase
        get() = media.playTrackUseCase

    val toggleAudioPlayPauseUseCase: TogglePlayPauseUseCase
        get() = media.toggleAudioPlayPauseUseCase

    val seekAudioUseCase: SeekAudioUseCase
        get() = media.seekAudioUseCase

    val navigatePlaylistUseCase: NavigatePlaylistUseCase
        get() = media.navigatePlaylistUseCase

    val cyclePlaybackSpeedUseCase: CyclePlaybackSpeedUseCase
        get() = media.cyclePlaybackSpeedUseCase

    val cycleRepeatModeUseCase: CycleRepeatModeUseCase
        get() = media.cycleRepeatModeUseCase

    val toggleShuffleUseCase: ToggleShuffleUseCase
        get() = media.toggleShuffleUseCase

    val handleProximitySensorUseCase: HandleProximitySensorUseCase
        get() = media.handleProximitySensorUseCase

    val configureEqualizerUseCase: ConfigureEqualizerUseCase
        get() = media.configureEqualizerUseCase

    val audioPlayerViewModel: AudioPlayerViewModel
        get() = media.audioPlayerViewModel

    fun createAudioPlayerViewModel(): AudioPlayerViewModel = media.createAudioPlayerViewModel()

    val imageLoaderRepository: ImageLoaderRepository
        get() = media.imageLoaderRepository

    val parseImageFilterUseCase: ParseImageFilterUseCase
        get() = media.parseImageFilterUseCase

    val formatImageFilterUseCase: FormatImageFilterUseCase
        get() = media.formatImageFilterUseCase

    val buildImageCacheKeyUseCase: BuildImageCacheKeyUseCase
        get() = media.buildImageCacheKeyUseCase

    val calculateImageDownscaleUseCase: CalculateImageDownscaleUseCase
        get() = media.calculateImageDownscaleUseCase

    val evaluateImageCacheEligibilityUseCase: EvaluateImageCacheEligibilityUseCase
        get() = media.evaluateImageCacheEligibilityUseCase

    val observeImageLoaderStateUseCase: ObserveImageLoaderStateUseCase
        get() = media.observeImageLoaderStateUseCase

    val getImageLoaderStateUseCase: GetImageLoaderStateUseCase
        get() = media.getImageLoaderStateUseCase

    val enqueueImageRequestUseCase: EnqueueImageRequestUseCase
        get() = media.enqueueImageRequestUseCase

    val cancelImageRequestUseCase: CancelImageRequestUseCase
        get() = media.cancelImageRequestUseCase

    val trimImageMemoryUseCase: TrimImageMemoryUseCase
        get() = media.trimImageMemoryUseCase

    val clearImageCacheUseCase: ClearImageCacheUseCase
        get() = media.clearImageCacheUseCase

    val imageLoaderViewModel: ImageLoaderViewModel
        get() = media.imageLoaderViewModel

    fun createImageLoaderViewModel(): ImageLoaderViewModel = media.createImageLoaderViewModel()

    val downloadManagerRepository: DownloadManagerRepository
        get() = media.downloadManagerRepository

    val evaluateAutoDownloadEligibilityUseCase: EvaluateAutoDownloadEligibilityUseCase
        get() = media.evaluateAutoDownloadEligibilityUseCase

    val observeDownloadManagerStateUseCase: ObserveDownloadManagerStateUseCase
        get() = media.observeDownloadManagerStateUseCase

    val getDownloadManagerStateUseCase: GetDownloadManagerStateUseCase
        get() = media.getDownloadManagerStateUseCase

    val enqueueDownloadUseCase: EnqueueDownloadUseCase
        get() = media.enqueueDownloadUseCase

    val pauseDownloadUseCase: PauseDownloadUseCase
        get() = media.pauseDownloadUseCase

    val resumeDownloadUseCase: ResumeDownloadUseCase
        get() = media.resumeDownloadUseCase

    val cancelDownloadUseCase: CancelDownloadUseCase
        get() = media.cancelDownloadUseCase

    val retryDownloadUseCase: RetryDownloadUseCase
        get() = media.retryDownloadUseCase

    val clearRecentDownloadsUseCase: ClearRecentDownloadsUseCase
        get() = media.clearRecentDownloadsUseCase

    val markDownloadsAsViewedUseCase: MarkDownloadsAsViewedUseCase
        get() = media.markDownloadsAsViewedUseCase

    val updateDownloadProgressUseCase: UpdateDownloadProgressUseCase
        get() = media.updateDownloadProgressUseCase

    val setDownloadNetworkTypeUseCase: SetDownloadNetworkTypeUseCase
        get() = media.setDownloadNetworkTypeUseCase

    val updateDownloadPresetUseCase: UpdateDownloadPresetUseCase
        get() = media.updateDownloadPresetUseCase

    val downloadManagerViewModel: DownloadManagerViewModel
        get() = media.downloadManagerViewModel

    fun createDownloadManagerViewModel(): DownloadManagerViewModel = media.createDownloadManagerViewModel()

    val autoDeleteMediaRepository: AutoDeleteMediaRepository
        get() = media.autoDeleteMediaRepository

    val checkShouldRunCleanupUseCase: CheckShouldRunCleanupUseCase
        get() = media.checkShouldRunCleanupUseCase

    val calculateEvictionCandidatesUseCase: CalculateEvictionCandidatesUseCase
        get() = media.calculateEvictionCandidatesUseCase

    val lockFileUseCase: LockFileUseCase
        get() = media.lockFileUseCase

    val unlockFileUseCase: UnlockFileUseCase
        get() = media.unlockFileUseCase

    val isFileLockedUseCase: IsFileLockedUseCase
        get() = media.isFileLockedUseCase

    val runAutoDeleteCleanupUseCase: RunAutoDeleteCleanupUseCase
        get() = media.runAutoDeleteCleanupUseCase

    val observeAutoDeleteStateUseCase: ObserveAutoDeleteStateUseCase
        get() = media.observeAutoDeleteStateUseCase

    val getAutoDeleteStateUseCase: GetAutoDeleteStateUseCase
        get() = media.getAutoDeleteStateUseCase

    val autoDeleteMediaViewModel: AutoDeleteMediaViewModel
        get() = media.autoDeleteMediaViewModel

    fun createAutoDeleteMediaViewModel(): AutoDeleteMediaViewModel = media.createAutoDeleteMediaViewModel()

    var storyCustomParamsRepository: StoryCustomParamsRepository
        get() = media.storyCustomParamsRepository
        set(value) { media.storyCustomParamsRepository = value }

    val checkStoryCustomParamsEmptyUseCase: CheckStoryCustomParamsEmptyUseCase
        get() = media.checkStoryCustomParamsEmptyUseCase

    val computeStoryCustomParamsFlagsUseCase: ComputeStoryCustomParamsFlagsUseCase
        get() = media.computeStoryCustomParamsFlagsUseCase

    val observeStoryCustomParamsStateUseCase: ObserveStoryCustomParamsStateUseCase
        get() = media.observeStoryCustomParamsStateUseCase

    val getStoryCustomParamsStateUseCase: GetStoryCustomParamsStateUseCase
        get() = media.getStoryCustomParamsStateUseCase

    val getStoryCustomParamsUseCase: GetStoryCustomParamsUseCase
        get() = media.getStoryCustomParamsUseCase

    val saveStoryCustomParamsUseCase: SaveStoryCustomParamsUseCase
        get() = media.saveStoryCustomParamsUseCase

    val updateStoryTranslationUseCase: UpdateStoryTranslationUseCase
        get() = media.updateStoryTranslationUseCase

    val copyStoryCustomParamsUseCase: CopyStoryCustomParamsUseCase
        get() = media.copyStoryCustomParamsUseCase

    val removeStoryCustomParamsUseCase: RemoveStoryCustomParamsUseCase
        get() = media.removeStoryCustomParamsUseCase

    val clearAllStoryCustomParamsUseCase: ClearAllStoryCustomParamsUseCase
        get() = media.clearAllStoryCustomParamsUseCase

    val storyCustomParamsViewModel: StoryCustomParamsViewModel
        get() = media.storyCustomParamsViewModel

    fun createStoryCustomParamsViewModel(): StoryCustomParamsViewModel = media.createStoryCustomParamsViewModel()

    // ==================== MESSAGING DOMAIN ====================

    var savedMessagesRepository: SavedMessagesRepository
        get() = messaging.savedMessagesRepository
        set(value) { messaging.savedMessagesRepository = value }

    fun createSavedMessagesRepository(): SavedMessagesRepository = messaging.createSavedMessagesRepository()

    val getSavedDialogsUseCase: GetSavedDialogsUseCase
        get() = messaging.getSavedDialogsUseCase

    val togglePinSavedDialogUseCase: TogglePinSavedDialogUseCase
        get() = messaging.togglePinSavedDialogUseCase

    val deleteSavedDialogUseCase: DeleteSavedDialogUseCase
        get() = messaging.deleteSavedDialogUseCase

    val getSavedTagsUseCase: GetSavedTagsUseCase
        get() = messaging.getSavedTagsUseCase

    val searchSavedDialogsUseCase: SearchSavedDialogsUseCase
        get() = messaging.searchSavedDialogsUseCase

    fun getSavedMessagesViewModel(): SavedMessagesViewModel = messaging.getSavedMessagesViewModel()

    fun createSavedMessagesViewModel(): SavedMessagesViewModel = messaging.createSavedMessagesViewModel()

    var dialogsRepository: DialogsRepository
        get() = messaging.dialogsRepository
        set(value) { messaging.dialogsRepository = value }

    val getDialogsUseCase: GetDialogsUseCase
        get() = messaging.getDialogsUseCase

    val loadMoreDialogsUseCase: LoadMoreDialogsUseCase
        get() = messaging.loadMoreDialogsUseCase

    val pinDialogUseCase: PinDialogUseCase
        get() = messaging.pinDialogUseCase

    val deleteDialogUseCase: DeleteDialogUseCase
        get() = messaging.deleteDialogUseCase

    val markDialogAsReadUseCase: MarkDialogAsReadUseCase
        get() = messaging.markDialogAsReadUseCase

    fun getDialogsViewModel(): DialogsViewModel = messaging.getDialogsViewModel()

    fun createDialogsViewModel(): DialogsViewModel = messaging.createDialogsViewModel()

    var chatRepository: ChatRepository
        get() = messaging.chatRepository
        set(value) { messaging.chatRepository = value }

    val observeMessagesUseCase: ObserveMessagesUseCase
        get() = messaging.observeMessagesUseCase

    val getMessagesUseCase: GetMessagesUseCase
        get() = messaging.getMessagesUseCase

    val loadHistoryUseCase: LoadHistoryUseCase
        get() = messaging.loadHistoryUseCase

    val sendMessageUseCase: SendMessageUseCase
        get() = messaging.sendMessageUseCase

    val deleteMessagesUseCase: DeleteMessagesUseCase
        get() = messaging.deleteMessagesUseCase

    fun getChatViewModel(dialogId: Long): ChatViewModel = messaging.getChatViewModel(dialogId)

    fun createChatViewModel(dialogId: Long): ChatViewModel = messaging.createChatViewModel(dialogId)

    var foldersRepository: FoldersRepository
        get() = messaging.foldersRepository
        set(value) { messaging.foldersRepository = value }

    fun createFoldersRepository(): FoldersRepository = messaging.createFoldersRepository()

    val observeFoldersUseCase: ObserveFoldersUseCase
        get() = messaging.observeFoldersUseCase

    val getFoldersUseCase: GetFoldersUseCase
        get() = messaging.getFoldersUseCase

    val getFolderUseCase: GetFolderUseCase
        get() = messaging.getFolderUseCase

    val createFolderUseCase: CreateFolderUseCase
        get() = messaging.createFolderUseCase

    val updateFolderUseCase: UpdateFolderUseCase
        get() = messaging.updateFolderUseCase

    val deleteFolderUseCase: DeleteFolderUseCase
        get() = messaging.deleteFolderUseCase

    val reorderFoldersUseCase: ReorderFoldersUseCase
        get() = messaging.reorderFoldersUseCase

    val getSuggestedFoldersUseCase: GetSuggestedFoldersUseCase
        get() = messaging.getSuggestedFoldersUseCase

    val foldersViewModel: FoldersViewModel
        get() = messaging.foldersViewModel

    fun createFoldersViewModel(): FoldersViewModel = messaging.createFoldersViewModel()

    var stickersRepository: StickersRepository
        get() = messaging.stickersRepository
        set(value) { messaging.stickersRepository = value }

    val observeStickerSetsUseCase: ObserveStickerSetsUseCase
        get() = messaging.observeStickerSetsUseCase

    val getStickerSetsUseCase: GetStickerSetsUseCase
        get() = messaging.getStickerSetsUseCase

    val getStickerSetUseCase: GetStickerSetUseCase
        get() = messaging.getStickerSetUseCase

    val getRecentStickersUseCase: GetRecentStickersUseCase
        get() = messaging.getRecentStickersUseCase

    val getStickersForEmojiUseCase: GetStickersForEmojiUseCase
        get() = messaging.getStickersForEmojiUseCase

    val toggleStickerSetInstalledUseCase: ToggleStickerSetInstalledUseCase
        get() = messaging.toggleStickerSetInstalledUseCase

    val toggleStickerSetArchivedUseCase: ToggleStickerSetArchivedUseCase
        get() = messaging.toggleStickerSetArchivedUseCase

    val stickersViewModel: StickersViewModel
        get() = messaging.stickersViewModel

    fun getStickersViewModel(type: Int = 0): StickersViewModel = messaging.getStickersViewModel(type)

    fun createStickersViewModel(type: Int = 0): StickersViewModel = messaging.createStickersViewModel(type)

    var searchRepository: SearchRepository
        get() = messaging.searchRepository
        set(value) { messaging.searchRepository = value }

    val searchGlobalUseCase: SearchGlobalUseCase
        get() = messaging.searchGlobalUseCase

    val searchLocalUseCase: SearchLocalUseCase
        get() = messaging.searchLocalUseCase

    val getRecentSearchesUseCase: GetRecentSearchesUseCase
        get() = messaging.getRecentSearchesUseCase

    val clearRecentSearchesUseCase: ClearRecentSearchesUseCase
        get() = messaging.clearRecentSearchesUseCase

    val removeRecentSearchUseCase: RemoveRecentSearchUseCase
        get() = messaging.removeRecentSearchUseCase

    val getRecentHashtagsUseCase: GetRecentHashtagsUseCase
        get() = messaging.getRecentHashtagsUseCase

    val putRecentHashtagUseCase: PutRecentHashtagUseCase
        get() = messaging.putRecentHashtagUseCase

    val clearRecentHashtagsUseCase: ClearRecentHashtagsUseCase
        get() = messaging.clearRecentHashtagsUseCase

    val searchViewModel: SearchViewModel
        get() = messaging.searchViewModel

    fun createSearchViewModel(): SearchViewModel = messaging.createSearchViewModel()

    var topicsRepository: TopicsRepository
        get() = messaging.topicsRepository
        set(value) { messaging.topicsRepository = value }

    val observeTopicsUseCase: ObserveTopicsUseCase
        get() = messaging.observeTopicsUseCase

    val observeForumUnreadCountUseCase: ObserveForumUnreadCountUseCase
        get() = messaging.observeForumUnreadCountUseCase

    val getTopicsUseCase: GetTopicsUseCase
        get() = messaging.getTopicsUseCase

    val getTopicUseCase: GetTopicUseCase
        get() = messaging.getTopicUseCase

    val loadTopicsUseCase: LoadTopicsUseCase
        get() = messaging.loadTopicsUseCase

    val reloadTopicsUseCase: ReloadTopicsUseCase
        get() = messaging.reloadTopicsUseCase

    val toggleCloseTopicUseCase: ToggleCloseTopicUseCase
        get() = messaging.toggleCloseTopicUseCase

    val togglePinTopicUseCase: TogglePinTopicUseCase
        get() = messaging.togglePinTopicUseCase

    val toggleShowTopicUseCase: ToggleShowTopicUseCase
        get() = messaging.toggleShowTopicUseCase

    val deleteTopicsUseCase: DeleteTopicsUseCase
        get() = messaging.deleteTopicsUseCase

    val reorderPinnedTopicsUseCase: ReorderPinnedTopicsUseCase
        get() = messaging.reorderPinnedTopicsUseCase

    val markTopicReactionsAsReadUseCase: MarkTopicReactionsAsReadUseCase
        get() = messaging.markTopicReactionsAsReadUseCase

    val getForumUnreadCountUseCase: GetForumUnreadCountUseCase
        get() = messaging.getForumUnreadCountUseCase

    val topicsViewModel: TopicsViewModel
        get() = messaging.topicsViewModel

    fun createTopicsViewModel(): TopicsViewModel = messaging.createTopicsViewModel()

    var translationRepository: TranslationRepository
        get() = messaging.translationRepository
        set(value) { messaging.translationRepository = value }

    val observeTranslateSettingsUseCase: ObserveTranslateSettingsUseCase
        get() = messaging.observeTranslateSettingsUseCase

    val getTranslateSettingsUseCase: GetTranslateSettingsUseCase
        get() = messaging.getTranslateSettingsUseCase

    val setChatTranslateEnabledUseCase: SetChatTranslateEnabledUseCase
        get() = messaging.setChatTranslateEnabledUseCase

    val setContextTranslateEnabledUseCase: SetContextTranslateEnabledUseCase
        get() = messaging.setContextTranslateEnabledUseCase

    val setDoNotTranslateLanguagesUseCase: SetDoNotTranslateLanguagesUseCase
        get() = messaging.setDoNotTranslateLanguagesUseCase

    val addDoNotTranslateLanguageUseCase: AddDoNotTranslateLanguageUseCase
        get() = messaging.addDoNotTranslateLanguageUseCase

    val removeDoNotTranslateLanguageUseCase: RemoveDoNotTranslateLanguageUseCase
        get() = messaging.removeDoNotTranslateLanguageUseCase

    val observeDialogTranslationStateUseCase: ObserveDialogTranslationStateUseCase
        get() = messaging.observeDialogTranslationStateUseCase

    val getDialogTranslationStateUseCase: GetDialogTranslationStateUseCase
        get() = messaging.getDialogTranslationStateUseCase

    val toggleDialogTranslatingUseCase: ToggleDialogTranslatingUseCase
        get() = messaging.toggleDialogTranslatingUseCase

    val setDialogTargetLanguageUseCase: SetDialogTargetLanguageUseCase
        get() = messaging.setDialogTargetLanguageUseCase

    val translateTextUseCase: TranslateTextUseCase
        get() = messaging.translateTextUseCase

    val getAvailableLanguagesUseCase: GetAvailableLanguagesUseCase
        get() = messaging.getAvailableLanguagesUseCase

    val applyAppLanguageUseCase: ApplyAppLanguageUseCase
        get() = messaging.applyAppLanguageUseCase

    val translateViewModel: TranslateViewModel
        get() = messaging.translateViewModel

    fun createTranslateViewModel(): TranslateViewModel = messaging.createTranslateViewModel()

    var reactionsRepository: ReactionsRepository
        get() = messaging.reactionsRepository
        set(value) { messaging.reactionsRepository = value }

    val observeAvailableReactionsUseCase: ObserveAvailableReactionsUseCase
        get() = messaging.observeAvailableReactionsUseCase

    val getAvailableReactionsUseCase: GetAvailableReactionsUseCase
        get() = messaging.getAvailableReactionsUseCase

    val loadAvailableReactionsUseCase: LoadAvailableReactionsUseCase
        get() = messaging.loadAvailableReactionsUseCase

    val observeRecentReactionsUseCase: ObserveRecentReactionsUseCase
        get() = messaging.observeRecentReactionsUseCase

    val getRecentReactionsUseCase: GetRecentReactionsUseCase
        get() = messaging.getRecentReactionsUseCase

    val getReactionsSettingsUseCase: GetReactionsSettingsUseCase
        get() = messaging.getReactionsSettingsUseCase

    val getDoubleTapReactionUseCase: GetDoubleTapReactionUseCase
        get() = messaging.getDoubleTapReactionUseCase

    val setDoubleTapReactionUseCase: SetDoubleTapReactionUseCase
        get() = messaging.setDoubleTapReactionUseCase

    val sendReactionUseCase: SendReactionUseCase
        get() = messaging.sendReactionUseCase

    val clearReactionsUseCase: ClearReactionsUseCase
        get() = messaging.clearReactionsUseCase

    val sendVoteUseCase: SendVoteUseCase
        get() = messaging.sendVoteUseCase

    val reactionsViewModel: ReactionsViewModel
        get() = messaging.reactionsViewModel

    fun createReactionsViewModel(): ReactionsViewModel = messaging.createReactionsViewModel()

    var factCheckRepository: FactCheckRepository
        get() = messaging.factCheckRepository
        set(value) { messaging.factCheckRepository = value }

    val observeFactCheckLoadedUseCase: ObserveFactCheckLoadedUseCase
        get() = messaging.observeFactCheckLoadedUseCase

    val getFactCheckUseCase: GetFactCheckUseCase
        get() = messaging.getFactCheckUseCase

    val loadFactCheckUseCase: LoadFactCheckUseCase
        get() = messaging.loadFactCheckUseCase

    val applyFactCheckUseCase: ApplyFactCheckUseCase
        get() = messaging.applyFactCheckUseCase

    val deleteFactCheckUseCase: DeleteFactCheckUseCase
        get() = messaging.deleteFactCheckUseCase

    val getFactCheckLimitUseCase: GetFactCheckLimitUseCase
        get() = messaging.getFactCheckLimitUseCase

    val factCheckViewModel: FactCheckViewModel
        get() = messaging.factCheckViewModel

    fun createFactCheckViewModel(): FactCheckViewModel = messaging.createFactCheckViewModel()

    var chatThemeRepository: ChatThemeRepository
        get() = messaging.chatThemeRepository
        set(value) { messaging.chatThemeRepository = value }

    val observeDialogThemeUseCase: ObserveDialogThemeUseCase
        get() = messaging.observeDialogThemeUseCase

    val getDialogThemeStateUseCase: GetDialogThemeStateUseCase
        get() = messaging.getDialogThemeStateUseCase

    val getAvailableChatThemesUseCase: GetAvailableChatThemesUseCase
        get() = messaging.getAvailableChatThemesUseCase

    val setDialogThemeUseCase: SetDialogThemeUseCase
        get() = messaging.setDialogThemeUseCase

    val resetDialogThemeUseCase: ResetDialogThemeUseCase
        get() = messaging.resetDialogThemeUseCase

    val saveChatWallpaperUseCase: SaveChatWallpaperUseCase
        get() = messaging.saveChatWallpaperUseCase

    val chatThemeViewModel: ChatThemeViewModel
        get() = messaging.chatThemeViewModel

    fun createChatThemeViewModel(): ChatThemeViewModel = messaging.createChatThemeViewModel()

    var autoDeleteRepository: AutoDeleteRepository
        get() = messaging.autoDeleteRepository
        set(value) { messaging.autoDeleteRepository = value }

    val observeGlobalAutoDeleteUseCase: ObserveGlobalAutoDeleteUseCase
        get() = messaging.observeGlobalAutoDeleteUseCase

    val getGlobalAutoDeleteUseCase: GetGlobalAutoDeleteUseCase
        get() = messaging.getGlobalAutoDeleteUseCase

    val setGlobalAutoDeleteUseCase: SetGlobalAutoDeleteUseCase
        get() = messaging.setGlobalAutoDeleteUseCase

    val getChatAutoDeleteUseCase: GetChatAutoDeleteUseCase
        get() = messaging.getChatAutoDeleteUseCase

    val setChatAutoDeleteUseCase: SetChatAutoDeleteUseCase
        get() = messaging.setChatAutoDeleteUseCase

    val setChatsAutoDeleteBatchUseCase: SetChatsAutoDeleteBatchUseCase
        get() = messaging.setChatsAutoDeleteBatchUseCase

    val autoDeleteViewModel: AutoDeleteViewModel
        get() = messaging.autoDeleteViewModel

    fun createAutoDeleteViewModel(): AutoDeleteViewModel = messaging.createAutoDeleteViewModel()

    var aiTonesRepository: AiTonesRepository
        get() = messaging.aiTonesRepository
        set(value) { messaging.aiTonesRepository = value }

    val observeAiTonesUseCase: ObserveAiTonesUseCase
        get() = messaging.observeAiTonesUseCase

    val getAiTonesStateUseCase: GetAiTonesStateUseCase
        get() = messaging.getAiTonesStateUseCase

    val loadAiTonesUseCase: LoadAiTonesUseCase
        get() = messaging.loadAiTonesUseCase

    val addAiToneUseCase: AddAiToneUseCase
        get() = messaging.addAiToneUseCase

    val removeAiToneUseCase: RemoveAiToneUseCase
        get() = messaging.removeAiToneUseCase

    val unsaveAiToneUseCase: UnsaveAiToneUseCase
        get() = messaging.unsaveAiToneUseCase

    val editAiToneUseCase: EditAiToneUseCase
        get() = messaging.editAiToneUseCase

    val aiTonesViewModel: AiTonesViewModel
        get() = messaging.aiTonesViewModel

    fun createAiTonesViewModel(): AiTonesViewModel = messaging.createAiTonesViewModel()

    var hashtagSearchRepository: HashtagSearchRepository
        get() = messaging.hashtagSearchRepository
        set(value) { messaging.hashtagSearchRepository = value }

    val observeHashtagHistoryUseCase: ObserveHashtagHistoryUseCase
        get() = messaging.observeHashtagHistoryUseCase

    val getHashtagHistoryUseCase: GetHashtagHistoryUseCase
        get() = messaging.getHashtagHistoryUseCase

    val addHashtagToHistoryUseCase: AddHashtagToHistoryUseCase
        get() = messaging.addHashtagToHistoryUseCase

    val removeHashtagFromHistoryUseCase: RemoveHashtagFromHistoryUseCase
        get() = messaging.removeHashtagFromHistoryUseCase

    val clearHashtagHistoryUseCase: ClearHashtagHistoryUseCase
        get() = messaging.clearHashtagHistoryUseCase

    val observeHashtagSearchResultUseCase: ObserveHashtagSearchResultUseCase
        get() = messaging.observeHashtagSearchResultUseCase

    val searchHashtagUseCase: SearchHashtagUseCase
        get() = messaging.searchHashtagUseCase

    val jumpToHashtagMessageUseCase: JumpToHashtagMessageUseCase
        get() = messaging.jumpToHashtagMessageUseCase

    val clearHashtagSearchResultsUseCase: ClearHashtagSearchResultsUseCase
        get() = messaging.clearHashtagSearchResultsUseCase

    val hashtagSearchViewModel: HashtagSearchViewModel
        get() = messaging.hashtagSearchViewModel

    fun createHashtagSearchViewModel(): HashtagSearchViewModel = messaging.createHashtagSearchViewModel()

    var groupCallMessagesRepository: GroupCallMessagesRepository
        get() = messaging.groupCallMessagesRepository
        set(value) { messaging.groupCallMessagesRepository = value }

    val observeGroupCallMessagesUseCase: ObserveGroupCallMessagesUseCase
        get() = messaging.observeGroupCallMessagesUseCase

    val getGroupCallMessagesUseCase: GetGroupCallMessagesUseCase
        get() = messaging.getGroupCallMessagesUseCase

    val sendGroupCallMessageUseCase: SendGroupCallMessageUseCase
        get() = messaging.sendGroupCallMessageUseCase

    val popGroupCallMessageUseCase: PopGroupCallMessageUseCase
        get() = messaging.popGroupCallMessageUseCase

    val clearGroupCallMessagesUseCase: ClearGroupCallMessagesUseCase
        get() = messaging.clearGroupCallMessagesUseCase

    fun getGroupCallMessagesViewModel(callId: Long = 0L): GroupCallMessagesViewModel = messaging.getGroupCallMessagesViewModel(callId)

    fun createGroupCallMessagesViewModel(callId: Long = 0L): GroupCallMessagesViewModel = messaging.createGroupCallMessagesViewModel(callId)

    var chatMessagesMetadataRepository: ChatMessagesMetadataRepository
        get() = messaging.chatMessagesMetadataRepository
        set(value) { messaging.chatMessagesMetadataRepository = value }

    val observeChatMetadataStatsUseCase: ObserveChatMetadataStatsUseCase
        get() = messaging.observeChatMetadataStatsUseCase

    val getChatMetadataStatsUseCase: GetChatMetadataStatsUseCase
        get() = messaging.getChatMetadataStatsUseCase

    val checkMessagesMetadataUseCase: CheckMessagesMetadataUseCase
        get() = messaging.checkMessagesMetadataUseCase

    val loadMessagesReactionsUseCase: LoadMessagesReactionsUseCase
        get() = messaging.loadMessagesReactionsUseCase

    val loadMessagesExtendedMediaUseCase: LoadMessagesExtendedMediaUseCase
        get() = messaging.loadMessagesExtendedMediaUseCase

    val cancelPendingMetadataRequestsUseCase: CancelPendingMetadataRequestsUseCase
        get() = messaging.cancelPendingMetadataRequestsUseCase

    val chatMetadataViewModel: ChatMetadataViewModel
        get() = messaging.chatMetadataViewModel

    fun createChatMetadataViewModel(): ChatMetadataViewModel = messaging.createChatMetadataViewModel()

    var draftsRepository: DraftsRepository
        get() = messaging.draftsRepository
        set(value) { messaging.draftsRepository = value }

    val observeDraftsStateUseCase: ObserveDraftsStateUseCase
        get() = messaging.observeDraftsStateUseCase

    val getDraftsStateUseCase: GetDraftsStateUseCase
        get() = messaging.getDraftsStateUseCase

    val loadDraftsUseCase: LoadDraftsUseCase
        get() = messaging.loadDraftsUseCase

    val saveDraftUseCase: SaveDraftUseCase
        get() = messaging.saveDraftUseCase

    val deleteDraftUseCase: DeleteDraftUseCase
        get() = messaging.deleteDraftUseCase

    val deleteForEditUseCase: DeleteForEditUseCase
        get() = messaging.deleteForEditUseCase

    val getDraftForEditUseCase: GetDraftForEditUseCase
        get() = messaging.getDraftForEditUseCase

    val cleanupExpiredDraftsUseCase: CleanupExpiredDraftsUseCase
        get() = messaging.cleanupExpiredDraftsUseCase

    val draftsViewModel: DraftsViewModel
        get() = messaging.draftsViewModel

    fun createDraftsViewModel(): DraftsViewModel = messaging.createDraftsViewModel()

    fun createDraftMeasureRepository(legacyController: ChatActivityDraftMessageMeasureController? = null): DraftMeasureRepository = messaging.createDraftMeasureRepository(legacyController)

    val draftMeasureRepository: DraftMeasureRepository
        get() = messaging.draftMeasureRepository

    val calculateDraftMeasureOverrideUseCase: CalculateDraftMeasureOverrideUseCase
        get() = messaging.calculateDraftMeasureOverrideUseCase

    val setDraftMeasureTargetUseCase: SetDraftMeasureTargetUseCase
        get() = messaging.setDraftMeasureTargetUseCase

    val onDraftMessageIdChangedUseCase: OnDraftMessageIdChangedUseCase
        get() = messaging.onDraftMessageIdChangedUseCase

    val setPreviousMessageHeightUseCase: SetPreviousMessageHeightUseCase
        get() = messaging.setPreviousMessageHeightUseCase

    val resetDraftMeasureTargetUseCase: ResetDraftMeasureTargetUseCase
        get() = messaging.resetDraftMeasureTargetUseCase

    val observeDraftMeasureConfigUseCase: ObserveDraftMeasureConfigUseCase
        get() = messaging.observeDraftMeasureConfigUseCase

    val getDraftMeasureConfigUseCase: GetDraftMeasureConfigUseCase
        get() = messaging.getDraftMeasureConfigUseCase

    val draftMeasureViewModel: DraftMeasureViewModel
        get() = messaging.draftMeasureViewModel

    fun createDraftMeasureViewModel(legacyController: ChatActivityDraftMessageMeasureController? = null): DraftMeasureViewModel = messaging.createDraftMeasureViewModel(legacyController)

    fun createBottomViewsRepository(legacyController: ChatActivityBottomViewsVisibilityController? = null): BottomViewsVisibilityRepository = messaging.createBottomViewsRepository(legacyController)

    val bottomViewsRepository: BottomViewsVisibilityRepository
        get() = messaging.bottomViewsRepository

    val getBottomViewVisibilityUseCase: GetBottomViewVisibilityUseCase
        get() = messaging.getBottomViewVisibilityUseCase

    val setBottomViewVisibleUseCase: SetBottomViewVisibleUseCase
        get() = messaging.setBottomViewVisibleUseCase

    val getPriorityBottomContainerUseCase: GetPriorityBottomContainerUseCase
        get() = messaging.getPriorityBottomContainerUseCase

    val getBottomViewsStateUseCase: GetBottomViewsStateUseCase
        get() = messaging.getBottomViewsStateUseCase

    val observeBottomViewsVisibilityUseCase: ObserveBottomViewsVisibilityUseCase
        get() = messaging.observeBottomViewsVisibilityUseCase

    val bottomViewsViewModel: BottomViewsViewModel
        get() = messaging.bottomViewsViewModel

    fun createBottomViewsViewModel(legacyController: ChatActivityBottomViewsVisibilityController? = null): BottomViewsViewModel = messaging.createBottomViewsViewModel(legacyController)

    fun createRichCaptionRepository(): RichCaptionRepository = messaging.createRichCaptionRepository()

    val richCaptionRepository: RichCaptionRepository
        get() = messaging.richCaptionRepository

    val observeRichCaptionUseCase: ObserveRichCaptionUseCase
        get() = messaging.observeRichCaptionUseCase

    val getRichCaptionUseCase: GetRichCaptionUseCase
        get() = messaging.getRichCaptionUseCase

    val setRichCaptionTextUseCase: SetRichCaptionTextUseCase
        get() = messaging.setRichCaptionTextUseCase

    val setRichCaptionCreditUseCase: SetRichCaptionCreditUseCase
        get() = messaging.setRichCaptionCreditUseCase

    val setRichCaptionLockedUseCase: SetRichCaptionLockedUseCase
        get() = messaging.setRichCaptionLockedUseCase

    val calculateCaptionMeasureWidthUseCase: CalculateCaptionMeasureWidthUseCase
        get() = messaging.calculateCaptionMeasureWidthUseCase

    val checkCaptionPressHitUseCase: CheckCaptionPressHitUseCase
        get() = messaging.checkCaptionPressHitUseCase

    val clearRichCaptionUseCase: ClearRichCaptionUseCase
        get() = messaging.clearRichCaptionUseCase

    val richCaptionViewModel: RichCaptionViewModel
        get() = messaging.richCaptionViewModel

    fun createRichCaptionViewModel(): RichCaptionViewModel = messaging.createRichCaptionViewModel()

    fun createEmojiEffectsRepository(): EmojiEffectsRepository = messaging.createEmojiEffectsRepository()

    val emojiEffectsRepository: EmojiEffectsRepository
        get() = messaging.emojiEffectsRepository

    val normalizeEmojiUseCase: NormalizeEmojiUseCase
        get() = messaging.normalizeEmojiUseCase

    val evaluateEmojiSupportUseCase: EvaluateEmojiSupportUseCase
        get() = messaging.evaluateEmojiSupportUseCase

    val recordEmojiTapUseCase: RecordEmojiTapUseCase
        get() = messaging.recordEmojiTapUseCase

    val encodeEmojiInteractionsJsonUseCase: EncodeEmojiInteractionsJsonUseCase
        get() = messaging.encodeEmojiInteractionsJsonUseCase

    val decodeEmojiInteractionsJsonUseCase: DecodeEmojiInteractionsJsonUseCase
        get() = messaging.decodeEmojiInteractionsJsonUseCase

    val calculateEmojiBoundsUseCase: CalculateEmojiBoundsUseCase
        get() = messaging.calculateEmojiBoundsUseCase

    val calculateEmojiOverlayPositionUseCase: CalculateEmojiOverlayPositionUseCase
        get() = messaging.calculateEmojiOverlayPositionUseCase

    val evaluateAnimationQuotaUseCase: EvaluateAnimationQuotaUseCase
        get() = messaging.evaluateAnimationQuotaUseCase

    val observeEmojiEffectsStateUseCase: ObserveEmojiEffectsStateUseCase
        get() = messaging.observeEmojiEffectsStateUseCase

    val getEmojiEffectsStateUseCase: GetEmojiEffectsStateUseCase
        get() = messaging.getEmojiEffectsStateUseCase

    val startEmojiEffectUseCase: StartEmojiEffectUseCase
        get() = messaging.startEmojiEffectUseCase

    val updateEmojiEffectProgressUseCase: UpdateEmojiEffectProgressUseCase
        get() = messaging.updateEmojiEffectProgressUseCase

    val dismissEmojiEffectUseCase: DismissEmojiEffectUseCase
        get() = messaging.dismissEmojiEffectUseCase

    val clearEmojiEffectsUseCase: ClearEmojiEffectsUseCase
        get() = messaging.clearEmojiEffectsUseCase

    val emojiEffectsViewModel: EmojiEffectsViewModel
        get() = messaging.emojiEffectsViewModel

    fun createEmojiEffectsViewModel(): EmojiEffectsViewModel = messaging.createEmojiEffectsViewModel()

    fun createMentionsRepository(): MentionsRepository = messaging.createMentionsRepository()

    val mentionsRepository: MentionsRepository
        get() = messaging.mentionsRepository

    val validateUsernameUseCase: ValidateUsernameUseCase
        get() = messaging.validateUsernameUseCase

    val parseMentionQueryUseCase: ParseMentionQueryUseCase
        get() = messaging.parseMentionQueryUseCase

    val filterMentionsUseCase: FilterMentionsUseCase
        get() = messaging.filterMentionsUseCase

    val formatMentionReplacementUseCase: FormatMentionReplacementUseCase
        get() = messaging.formatMentionReplacementUseCase

    val observeMentionsStateUseCase: ObserveMentionsStateUseCase
        get() = messaging.observeMentionsStateUseCase

    val getMentionsStateUseCase: GetMentionsStateUseCase
        get() = messaging.getMentionsStateUseCase

    val updateMentionQueryUseCase: UpdateMentionQueryUseCase
        get() = messaging.updateMentionQueryUseCase

    val setMentionCandidatesUseCase: SetMentionCandidatesUseCase
        get() = messaging.setMentionCandidatesUseCase

    val dismissMentionsUseCase: DismissMentionsUseCase
        get() = messaging.dismissMentionsUseCase

    val clearMentionsUseCase: ClearMentionsUseCase
        get() = messaging.clearMentionsUseCase

    val mentionsViewModel: MentionsViewModel
        get() = messaging.mentionsViewModel

    fun createMentionsViewModel(): MentionsViewModel = messaging.createMentionsViewModel()

    fun createEmojiPickerRepository(): EmojiPickerRepository = messaging.createEmojiPickerRepository()

    val emojiPickerRepository: EmojiPickerRepository
        get() = messaging.emojiPickerRepository

    val resolveAvailablePickerTabsUseCase: ResolveAvailablePickerTabsUseCase
        get() = messaging.resolveAvailablePickerTabsUseCase

    val filterEmojiItemsUseCase: FilterEmojiItemsUseCase
        get() = messaging.filterEmojiItemsUseCase

    val filterStickersUseCase: FilterStickersUseCase
        get() = messaging.filterStickersUseCase

    val filterGifsUseCase: FilterGifsUseCase
        get() = messaging.filterGifsUseCase

    val observeEmojiPickerStateUseCase: ObserveEmojiPickerStateUseCase
        get() = messaging.observeEmojiPickerStateUseCase

    val getEmojiPickerStateUseCase: GetEmojiPickerStateUseCase
        get() = messaging.getEmojiPickerStateUseCase

    val selectPickerTabUseCase: SelectPickerTabUseCase
        get() = messaging.selectPickerTabUseCase

    val updatePickerSearchQueryUseCase: UpdatePickerSearchQueryUseCase
        get() = messaging.updatePickerSearchQueryUseCase

    val toggleStickerFavoriteUseCase: ToggleStickerFavoriteUseCase
        get() = messaging.toggleStickerFavoriteUseCase

    val clearRecentPickerItemsUseCase: ClearRecentPickerItemsUseCase
        get() = messaging.clearRecentPickerItemsUseCase

    val emojiPickerViewModel: EmojiPickerViewModel
        get() = messaging.emojiPickerViewModel

    fun createEmojiPickerViewModel(): EmojiPickerViewModel = messaging.createEmojiPickerViewModel()

    fun createChatAttachRepository(): ChatAttachRepository = messaging.createChatAttachRepository()

    val chatAttachRepository: ChatAttachRepository
        get() = messaging.chatAttachRepository

    val resolveAvailableAttachLayoutsUseCase: ResolveAvailableAttachLayoutsUseCase
        get() = messaging.resolveAvailableAttachLayoutsUseCase

    val calculateAttachCaptionLimitUseCase: CalculateAttachCaptionLimitUseCase
        get() = messaging.calculateAttachCaptionLimitUseCase

    val toggleAttachItemSelectionUseCase: ToggleAttachItemSelectionUseCase
        get() = messaging.toggleAttachItemSelectionUseCase

    val validateSendOptionsUseCase: ValidateSendOptionsUseCase
        get() = messaging.validateSendOptionsUseCase

    val observeChatAttachStateUseCase: ObserveChatAttachStateUseCase
        get() = messaging.observeChatAttachStateUseCase

    val getChatAttachStateUseCase: GetChatAttachStateUseCase
        get() = messaging.getChatAttachStateUseCase

    val selectAttachLayoutUseCase: SelectAttachLayoutUseCase
        get() = messaging.selectAttachLayoutUseCase

    val updateAttachSendOptionsUseCase: UpdateAttachSendOptionsUseCase
        get() = messaging.updateAttachSendOptionsUseCase

    val clearAttachSelectionUseCase: ClearAttachSelectionUseCase
        get() = messaging.clearAttachSelectionUseCase

    val openChatAttachAlertUseCase: OpenChatAttachAlertUseCase
        get() = messaging.openChatAttachAlertUseCase

    val chatAttachViewModel: ChatAttachViewModel
        get() = messaging.chatAttachViewModel

    fun createChatAttachViewModel(): ChatAttachViewModel = messaging.createChatAttachViewModel()

    var chatInputRepository: ChatInputRepository
        get() = messaging.chatInputRepository
        set(value) { messaging.chatInputRepository = value }

    val calculateSendButtonStateUseCase: CalculateSendButtonStateUseCase
        get() = messaging.calculateSendButtonStateUseCase

    val formatTextSelectionUseCase: FormatTextSelectionUseCase
        get() = messaging.formatTextSelectionUseCase

    val validateVoiceRecordActionUseCase: ValidateVoiceRecordActionUseCase
        get() = messaging.validateVoiceRecordActionUseCase

    val resolvePanelVisibilityUseCase: ResolvePanelVisibilityUseCase
        get() = messaging.resolvePanelVisibilityUseCase

    val observeChatInputStateUseCase: ObserveChatInputStateUseCase
        get() = messaging.observeChatInputStateUseCase

    val getChatInputStateUseCase: GetChatInputStateUseCase
        get() = messaging.getChatInputStateUseCase

    val setChatInputTextUseCase: SetChatInputTextUseCase
        get() = messaging.setChatInputTextUseCase

    val setChatInputPanelModeUseCase: SetChatInputPanelModeUseCase
        get() = messaging.setChatInputPanelModeUseCase

    val setChatInputReplyUseCase: SetChatInputReplyUseCase
        get() = messaging.setChatInputReplyUseCase

    val clearChatInputReplyUseCase: ClearChatInputReplyUseCase
        get() = messaging.clearChatInputReplyUseCase

    val chatInputViewModel: ChatInputViewModel
        get() = messaging.chatInputViewModel

    fun createChatInputViewModel(): ChatInputViewModel = messaging.createChatInputViewModel()

    val sendMessagesRepository: SendMessagesRepository
        get() = messaging.sendMessagesRepository

    val sendTextMessageUseCase: SendTextMessageUseCase
        get() = messaging.sendTextMessageUseCase

    val sendMediaMessageUseCase: SendMediaMessageUseCase
        get() = messaging.sendMediaMessageUseCase

    val sendMediaAlbumUseCase: SendMediaAlbumUseCase
        get() = messaging.sendMediaAlbumUseCase

    val forwardMessagesUseCase: ForwardMessagesUseCase
        get() = messaging.forwardMessagesUseCase

    val retrySendMessageUseCase: RetrySendMessageUseCase
        get() = messaging.retrySendMessageUseCase

    val cancelSendMessageUseCase: CancelSendMessageUseCase
        get() = messaging.cancelSendMessageUseCase

    val observePendingSendsUseCase: ObservePendingSendsUseCase
        get() = messaging.observePendingSendsUseCase

    val sendMessagesViewModel: SendMessagesViewModel
        get() = messaging.sendMessagesViewModel

    fun createSendMessagesViewModel(): SendMessagesViewModel = messaging.createSendMessagesViewModel()

    val messageCustomParamsRepository: MessageCustomParamsRepository
        get() = messaging.messageCustomParamsRepository

    val checkMessageCustomParamsEmptyUseCase: CheckMessageCustomParamsEmptyUseCase
        get() = messaging.checkMessageCustomParamsEmptyUseCase

    val mergeMessageCustomParamsUseCase: MergeMessageCustomParamsUseCase
        get() = messaging.mergeMessageCustomParamsUseCase

    val observeMessageCustomParamsStateUseCase: ObserveMessageCustomParamsStateUseCase
        get() = messaging.observeMessageCustomParamsStateUseCase

    val getMessageCustomParamsStateUseCase: GetMessageCustomParamsStateUseCase
        get() = messaging.getMessageCustomParamsStateUseCase

    val getMessageCustomParamsUseCase: GetMessageCustomParamsUseCase
        get() = messaging.getMessageCustomParamsUseCase

    val setMessageCustomParamsUseCase: SetMessageCustomParamsUseCase
        get() = messaging.setMessageCustomParamsUseCase

    val updateVoiceTranscriptionUseCase: UpdateVoiceTranscriptionUseCase
        get() = messaging.updateVoiceTranscriptionUseCase

    val updateMessageTranslationUseCase: UpdateMessageTranslationUseCase
        get() = messaging.updateMessageTranslationUseCase

    val updateMessageSummaryUseCase: UpdateMessageSummaryUseCase
        get() = messaging.updateMessageSummaryUseCase

    val copyMessageCustomParamsUseCase: CopyMessageCustomParamsUseCase
        get() = messaging.copyMessageCustomParamsUseCase

    val removeMessageCustomParamsUseCase: RemoveMessageCustomParamsUseCase
        get() = messaging.removeMessageCustomParamsUseCase

    val clearAllMessageCustomParamsUseCase: ClearAllMessageCustomParamsUseCase
        get() = messaging.clearAllMessageCustomParamsUseCase

    val messageCustomParamsViewModel: MessageCustomParamsViewModel
        get() = messaging.messageCustomParamsViewModel

    fun createMessageCustomParamsViewModel(): MessageCustomParamsViewModel = messaging.createMessageCustomParamsViewModel()

    var botForumRepository: BotForumRepository
        get() = messaging.botForumRepository
        set(value) { messaging.botForumRepository = value }

    val deriveTopicNameFromMessageUseCase: DeriveTopicNameFromMessageUseCase
        get() = messaging.deriveTopicNameFromMessageUseCase

    val resolveStreamingButtonStateUseCase: ResolveStreamingButtonStateUseCase
        get() = messaging.resolveStreamingButtonStateUseCase

    val observeBotForumStateUseCase: ObserveBotForumStateUseCase
        get() = messaging.observeBotForumStateUseCase

    val getBotForumStateUseCase: GetBotForumStateUseCase
        get() = messaging.getBotForumStateUseCase

    val getStreamingSendButtonStateUseCase: GetStreamingSendButtonStateUseCase
        get() = messaging.getStreamingSendButtonStateUseCase

    val checkIsStreamingTopicUseCase: CheckIsStreamingTopicUseCase
        get() = messaging.checkIsStreamingTopicUseCase

    val saveIsStreamingTopicUseCase: SaveIsStreamingTopicUseCase
        get() = messaging.saveIsStreamingTopicUseCase

    val checkHasBotForumDraftsUseCase: CheckHasBotForumDraftsUseCase
        get() = messaging.checkHasBotForumDraftsUseCase

    val stopStreamingDraftUseCase: StopStreamingDraftUseCase
        get() = messaging.stopStreamingDraftUseCase

    val updateBotForumDraftUseCase: UpdateBotForumDraftUseCase
        get() = messaging.updateBotForumDraftUseCase

    val removeMarkedRemovedDraftsUseCase: RemoveMarkedRemovedDraftsUseCase
        get() = messaging.removeMarkedRemovedDraftsUseCase

    val checkNewMessageDraftReplacementUseCase: CheckNewMessageDraftReplacementUseCase
        get() = messaging.checkNewMessageDraftReplacementUseCase

    val checkIsBotForumUseCase: CheckIsBotForumUseCase
        get() = messaging.checkIsBotForumUseCase

    val botForumViewModel: BotForumViewModel
        get() = messaging.botForumViewModel

    fun createBotForumViewModel(): BotForumViewModel = messaging.createBotForumViewModel()

    var ephemeralMessagesRepository: EphemeralMessagesRepository
        get() = messaging.ephemeralMessagesRepository
        set(value) { messaging.ephemeralMessagesRepository = value }

    val parseBotCommandUseCase: ParseBotCommandUseCase
        get() = messaging.parseBotCommandUseCase

    val getEphemeralCommandBotIdUseCase: GetEphemeralCommandBotIdUseCase
        get() = messaging.getEphemeralCommandBotIdUseCase

    val isEphemeralCommandUseCase: IsEphemeralCommandUseCase
        get() = messaging.isEphemeralCommandUseCase

    val packEphemeralMessageIdUseCase: PackEphemeralMessageIdUseCase
        get() = messaging.packEphemeralMessageIdUseCase

    val unpackEphemeralMessageIdUseCase: UnpackEphemeralMessageIdUseCase
        get() = messaging.unpackEphemeralMessageIdUseCase

    val isEphemeralMessageIdUseCase: IsEphemeralMessageIdUseCase
        get() = messaging.isEphemeralMessageIdUseCase

    val putWelcomeAnchorBindingUseCase: PutWelcomeAnchorBindingUseCase
        get() = messaging.putWelcomeAnchorBindingUseCase

    val removeWelcomeAnchorBindingUseCase: RemoveWelcomeAnchorBindingUseCase
        get() = messaging.removeWelcomeAnchorBindingUseCase

    val getWelcomeAnchorBindingsUseCase: GetWelcomeAnchorBindingsUseCase
        get() = messaging.getWelcomeAnchorBindingsUseCase

    val clearAllWelcomeAnchorBindingsUseCase: ClearAllWelcomeAnchorBindingsUseCase
        get() = messaging.clearAllWelcomeAnchorBindingsUseCase

    val observeEphemeralMessagesStateUseCase: ObserveEphemeralMessagesStateUseCase
        get() = messaging.observeEphemeralMessagesStateUseCase

    val getEphemeralMessagesStateUseCase: GetEphemeralMessagesStateUseCase
        get() = messaging.getEphemeralMessagesStateUseCase

    val ephemeralMessagesViewModel: EphemeralMessagesViewModel
        get() = messaging.ephemeralMessagesViewModel

    fun createEphemeralMessagesViewModel(): EphemeralMessagesViewModel = messaging.createEphemeralMessagesViewModel()

    var botKeyboardRepository: BotKeyboardRepository
        get() = messaging.botKeyboardRepository
        set(value) { messaging.botKeyboardRepository = value }

    val buildBotKeyboardLayoutUseCase: BuildBotKeyboardLayoutUseCase
        get() = messaging.buildBotKeyboardLayoutUseCase

    val checkIsForceReplyUseCase: CheckIsForceReplyUseCase
        get() = messaging.checkIsForceReplyUseCase

    val checkIsButtonWebViewUseCase: CheckIsButtonWebViewUseCase
        get() = messaging.checkIsButtonWebViewUseCase

    val resolveCustomButtonTypeUseCase: ResolveCustomButtonTypeUseCase
        get() = messaging.resolveCustomButtonTypeUseCase

    val getKeyboardForMessageUseCase: GetKeyboardForMessageUseCase
        get() = messaging.getKeyboardForMessageUseCase

    val setKeyboardForMessageUseCase: SetKeyboardForMessageUseCase
        get() = messaging.setKeyboardForMessageUseCase

    val removeKeyboardForMessageUseCase: RemoveKeyboardForMessageUseCase
        get() = messaging.removeKeyboardForMessageUseCase

    val clearAllKeyboardsUseCase: ClearAllKeyboardsUseCase
        get() = messaging.clearAllKeyboardsUseCase

    val recordButtonPressedUseCase: RecordButtonPressedUseCase
        get() = messaging.recordButtonPressedUseCase

    val observeBotKeyboardStateUseCase: ObserveBotKeyboardStateUseCase
        get() = messaging.observeBotKeyboardStateUseCase

    val getBotKeyboardStateUseCase: GetBotKeyboardStateUseCase
        get() = messaging.getBotKeyboardStateUseCase

    val botKeyboardViewModel: BotKeyboardViewModel
        get() = messaging.botKeyboardViewModel

    fun createBotKeyboardViewModel(): BotKeyboardViewModel = messaging.createBotKeyboardViewModel()

    var textHtmlRepository: org.telegram.messenger.feature.messaging.texthtml.domain.repository.TextHtmlRepository
        get() = messaging.textHtmlRepository
        set(value) { messaging.textHtmlRepository = value }

    val convertToHtmlUseCase: org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ConvertToHtmlUseCase
        get() = messaging.convertToHtmlUseCase

    val parseFromHtmlUseCase: org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ParseFromHtmlUseCase
        get() = messaging.parseFromHtmlUseCase

    val escapeHtmlUseCase: org.telegram.messenger.feature.messaging.texthtml.domain.usecase.EscapeHtmlUseCase
        get() = messaging.escapeHtmlUseCase

    val unescapeHtmlUseCase: org.telegram.messenger.feature.messaging.texthtml.domain.usecase.UnescapeHtmlUseCase
        get() = messaging.unescapeHtmlUseCase

    val stripHtmlFormattingUseCase: org.telegram.messenger.feature.messaging.texthtml.domain.usecase.StripHtmlFormattingUseCase
        get() = messaging.stripHtmlFormattingUseCase

    val extractHtmlSpansUseCase: org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ExtractHtmlSpansUseCase
        get() = messaging.extractHtmlSpansUseCase

    val hasRichFormattingUseCase: org.telegram.messenger.feature.messaging.texthtml.domain.usecase.HasRichFormattingUseCase
        get() = messaging.hasRichFormattingUseCase

    val observeTextHtmlStateUseCase: org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ObserveTextHtmlStateUseCase
        get() = messaging.observeTextHtmlStateUseCase

    val clearTextHtmlStateUseCase: org.telegram.messenger.feature.messaging.texthtml.domain.usecase.ClearTextHtmlStateUseCase
        get() = messaging.clearTextHtmlStateUseCase

    val textHtmlViewModel: org.telegram.messenger.feature.messaging.texthtml.presentation.TextHtmlViewModel
        get() = messaging.textHtmlViewModel

    fun createTextHtmlViewModel(): org.telegram.messenger.feature.messaging.texthtml.presentation.TextHtmlViewModel = messaging.createTextHtmlViewModel()

    // ==================== NETWORK DOMAIN ====================

    var proxyRepository: ProxyRepository
        get() = network.proxyRepository
        set(value) { network.proxyRepository = value }

    val observeProxySettingsUseCase: ObserveProxySettingsUseCase
        get() = network.observeProxySettingsUseCase

    val getProxySettingsUseCase: GetProxySettingsUseCase
        get() = network.getProxySettingsUseCase

    val addProxyUseCase: AddProxyUseCase
        get() = network.addProxyUseCase

    val deleteProxyUseCase: DeleteProxyUseCase
        get() = network.deleteProxyUseCase

    val enableProxyUseCase: EnableProxyUseCase
        get() = network.enableProxyUseCase

    val disableProxyUseCase: DisableProxyUseCase
        get() = network.disableProxyUseCase

    val toggleProxyRotationUseCase: ToggleProxyRotationUseCase
        get() = network.toggleProxyRotationUseCase

    val checkProxyPingUseCase: CheckProxyPingUseCase
        get() = network.checkProxyPingUseCase

    val proxyViewModel: ProxyViewModel
        get() = network.proxyViewModel

    fun createProxyViewModel(): ProxyViewModel = network.createProxyViewModel()

    var pushRepository: PushRepository
        get() = network.pushRepository
        set(value) { network.pushRepository = value }

    val observePushStatusUseCase: ObservePushStatusUseCase
        get() = network.observePushStatusUseCase

    val getPushStatusUseCase: GetPushStatusUseCase
        get() = network.getPushStatusUseCase

    val isPushAvailableUseCase: IsPushAvailableUseCase
        get() = network.isPushAvailableUseCase

    val requestPushTokenUseCase: RequestPushTokenUseCase
        get() = network.requestPushTokenUseCase

    val registerPushTokenUseCase: RegisterPushTokenUseCase
        get() = network.registerPushTokenUseCase

    val resetPushTokenUseCase: ResetPushTokenUseCase
        get() = network.resetPushTokenUseCase

    val pushViewModel: PushViewModel
        get() = network.pushViewModel

    fun createPushViewModel(): PushViewModel = network.createPushViewModel()

    var networkStatsRepository: NetworkStatsRepository
        get() = network.networkStatsRepository
        set(value) { network.networkStatsRepository = value }

    val observeNetworkStatsUseCase: ObserveNetworkStatsUseCase
        get() = network.observeNetworkStatsUseCase

    val observeAllNetworkStatsUseCase: ObserveAllNetworkStatsUseCase
        get() = network.observeAllNetworkStatsUseCase

    val getNetworkStatsUseCase: GetNetworkStatsUseCase
        get() = network.getNetworkStatsUseCase

    val getAllNetworkStatsUseCase: GetAllNetworkStatsUseCase
        get() = network.getAllNetworkStatsUseCase

    val incrementTrafficBytesUseCase: IncrementTrafficBytesUseCase
        get() = network.incrementTrafficBytesUseCase

    val incrementTrafficItemsUseCase: IncrementTrafficItemsUseCase
        get() = network.incrementTrafficItemsUseCase

    val incrementCallsTimeUseCase: IncrementCallsTimeUseCase
        get() = network.incrementCallsTimeUseCase

    val resetNetworkStatsUseCase: ResetNetworkStatsUseCase
        get() = network.resetNetworkStatsUseCase

    val refreshNetworkStatsUseCase: RefreshNetworkStatsUseCase
        get() = network.refreshNetworkStatsUseCase

    val calculateMessagesTrafficUseCase: CalculateMessagesTrafficUseCase
        get() = network.calculateMessagesTrafficUseCase

    val formatTrafficBytesUseCase: FormatTrafficBytesUseCase
        get() = network.formatTrafficBytesUseCase

    val formatCallsDurationUseCase: FormatCallsDurationUseCase
        get() = network.formatCallsDurationUseCase

    val networkStatsViewModel: NetworkStatsViewModel
        get() = network.networkStatsViewModel

    fun createNetworkStatsViewModel(): NetworkStatsViewModel = network.createNetworkStatsViewModel()

    var pushListenerRepository: PushListenerRepository
        get() = network.pushListenerRepository
        set(value) { network.pushListenerRepository = value }

    val observePushListenerStateUseCase: ObservePushListenerStateUseCase
        get() = network.observePushListenerStateUseCase

    val observeIncomingPushesUseCase: ObserveIncomingPushesUseCase
        get() = network.observeIncomingPushesUseCase

    val getPushListenerStateUseCase: GetPushListenerStateUseCase
        get() = network.getPushListenerStateUseCase

    val processIncomingPushUseCase: ProcessIncomingPushUseCase
        get() = network.processIncomingPushUseCase

    val registerPushListenerTokenUseCase: RegisterPushListenerTokenUseCase
        get() = network.registerPushListenerTokenUseCase

    val togglePushListeningUseCase: TogglePushListeningUseCase
        get() = network.togglePushListeningUseCase

    val determinePushActionTypeUseCase: DeterminePushActionTypeUseCase
        get() = network.determinePushActionTypeUseCase

    val parsePushJsonPayloadUseCase: ParsePushJsonPayloadUseCase
        get() = network.parsePushJsonPayloadUseCase

    val pushListenerViewModel: PushListenerViewModel
        get() = network.pushListenerViewModel

    fun createPushListenerViewModel(): PushListenerViewModel = network.createPushListenerViewModel()

    // ==================== SECURITY DOMAIN ====================

    var secretChatRepository: SecretChatRepository
        get() = security.secretChatRepository
        set(value) { security.secretChatRepository = value }

    val observeSecretChatUseCase: ObserveSecretChatUseCase
        get() = security.observeSecretChatUseCase

    val observeSecretChatsUseCase: ObserveSecretChatsUseCase
        get() = security.observeSecretChatsUseCase

    val getSecretChatUseCase: GetSecretChatUseCase
        get() = security.getSecretChatUseCase

    val startSecretChatUseCase: StartSecretChatUseCase
        get() = security.startSecretChatUseCase

    val acceptSecretChatUseCase: AcceptSecretChatUseCase
        get() = security.acceptSecretChatUseCase

    val declineSecretChatUseCase: DeclineSecretChatUseCase
        get() = security.declineSecretChatUseCase

    val setSecretChatTtlUseCase: SetSecretChatTtlUseCase
        get() = security.setSecretChatTtlUseCase

    val sendScreenshotNotificationUseCase: SendScreenshotNotificationUseCase
        get() = security.sendScreenshotNotificationUseCase

    fun getSecretChatViewModel(chatId: Int): SecretChatViewModel = security.getSecretChatViewModel(chatId)

    fun createSecretChatViewModel(chatId: Int): SecretChatViewModel = security.createSecretChatViewModel(chatId)

    val privacyRepository: PrivacyRepository
        get() = security.privacyRepository

    val observePrivacyRulesUseCase: ObservePrivacyRulesUseCase
        get() = security.observePrivacyRulesUseCase

    val getPrivacyRulesUseCase: GetPrivacyRulesUseCase
        get() = security.getPrivacyRulesUseCase

    val setPrivacyRuleUseCase: SetPrivacyRuleUseCase
        get() = security.setPrivacyRuleUseCase

    val loadPrivacyRulesUseCase: LoadPrivacyRulesUseCase
        get() = security.loadPrivacyRulesUseCase

    val observeBlockedPeersUseCase: ObserveBlockedPeersUseCase
        get() = security.observeBlockedPeersUseCase

    val getBlockedPeersUseCase: GetBlockedPeersUseCase
        get() = security.getBlockedPeersUseCase

    val blockPrivacyPeerUseCase: BlockPrivacyPeerUseCase
        get() = security.blockPrivacyPeerUseCase

    val unblockPrivacyPeerUseCase: UnblockPrivacyPeerUseCase
        get() = security.unblockPrivacyPeerUseCase

    val getPasscodeSettingsUseCase: GetPasscodeSettingsUseCase
        get() = security.getPasscodeSettingsUseCase

    val setPasscodeUseCase: SetPasscodeUseCase
        get() = security.setPasscodeUseCase

    val checkPasscodeUseCase: CheckPasscodeUseCase
        get() = security.checkPasscodeUseCase

    val clearPasscodeUseCase: ClearPasscodeUseCase
        get() = security.clearPasscodeUseCase

    val observeTwoStepVerificationUseCase: ObserveTwoStepVerificationUseCase
        get() = security.observeTwoStepVerificationUseCase

    val loadTwoStepVerificationUseCase: LoadTwoStepVerificationUseCase
        get() = security.loadTwoStepVerificationUseCase

    val privacyViewModel: PrivacyViewModel
        get() = security.privacyViewModel

    fun createPrivacyViewModel(): PrivacyViewModel = security.createPrivacyViewModel()

    var sessionsRepository: SessionsRepository
        get() = security.sessionsRepository
        set(value) { security.sessionsRepository = value }

    val observeSessionsUseCase: ObserveSessionsUseCase
        get() = security.observeSessionsUseCase

    val observeWebSessionsUseCase: ObserveWebSessionsUseCase
        get() = security.observeWebSessionsUseCase

    val getSessionsUseCase: GetSessionsUseCase
        get() = security.getSessionsUseCase

    val loadSessionsUseCase: LoadSessionsUseCase
        get() = security.loadSessionsUseCase

    val getWebSessionsUseCase: GetWebSessionsUseCase
        get() = security.getWebSessionsUseCase

    val loadWebSessionsUseCase: LoadWebSessionsUseCase
        get() = security.loadWebSessionsUseCase

    val terminateSessionUseCase: TerminateSessionUseCase
        get() = security.terminateSessionUseCase

    val terminateAllOtherSessionsUseCase: TerminateAllOtherSessionsUseCase
        get() = security.terminateAllOtherSessionsUseCase

    val terminateWebSessionUseCase: TerminateWebSessionUseCase
        get() = security.terminateWebSessionUseCase

    val terminateAllWebSessionsUseCase: TerminateAllWebSessionsUseCase
        get() = security.terminateAllWebSessionsUseCase

    val updateSessionSettingsUseCase: UpdateSessionSettingsUseCase
        get() = security.updateSessionSettingsUseCase

    val setSessionsTtlUseCase: SetSessionsTtlUseCase
        get() = security.setSessionsTtlUseCase

    val acceptQrLoginUseCase: AcceptQrLoginUseCase
        get() = security.acceptQrLoginUseCase

    val sessionsViewModel: SessionsViewModel
        get() = security.sessionsViewModel

    fun createSessionsViewModel(): SessionsViewModel = security.createSessionsViewModel()

    var passkeysRepository: PasskeysRepository
        get() = security.passkeysRepository
        set(value) { security.passkeysRepository = value }

    val observePasskeysUseCase: ObservePasskeysUseCase
        get() = security.observePasskeysUseCase

    val getPasskeysUseCase: GetPasskeysUseCase
        get() = security.getPasskeysUseCase

    val deletePasskeyUseCase: DeletePasskeyUseCase
        get() = security.deletePasskeyUseCase

    val checkCanAddPasskeyUseCase: CheckCanAddPasskeyUseCase
        get() = security.checkCanAddPasskeyUseCase

    val isPasskeysSupportedUseCase: IsPasskeysSupportedUseCase
        get() = security.isPasskeysSupportedUseCase

    val passkeysViewModel: PasskeysViewModel
        get() = security.passkeysViewModel

    fun createPasskeysViewModel(): PasskeysViewModel = security.createPasskeysViewModel()

    var unconfirmedAuthRepository: UnconfirmedAuthRepository
        get() = security.unconfirmedAuthRepository
        set(value) { security.unconfirmedAuthRepository = value }

    val observeUnconfirmedAuthsUseCase: ObserveUnconfirmedAuthsUseCase
        get() = security.observeUnconfirmedAuthsUseCase

    val getUnconfirmedAuthsUseCase: GetUnconfirmedAuthsUseCase
        get() = security.getUnconfirmedAuthsUseCase

    val confirmAuthUseCase: ConfirmAuthUseCase
        get() = security.confirmAuthUseCase

    val denyAuthUseCase: DenyAuthUseCase
        get() = security.denyAuthUseCase

    val confirmAllAuthsUseCase: ConfirmAllAuthsUseCase
        get() = security.confirmAllAuthsUseCase

    val denyAllAuthsUseCase: DenyAllAuthsUseCase
        get() = security.denyAllAuthsUseCase

    val clearUnconfirmedAuthsUseCase: ClearUnconfirmedAuthsUseCase
        get() = security.clearUnconfirmedAuthsUseCase

    val unconfirmedAuthViewModel: UnconfirmedAuthViewModel
        get() = security.unconfirmedAuthViewModel

    fun createUnconfirmedAuthViewModel(): UnconfirmedAuthViewModel = security.createUnconfirmedAuthViewModel()

    var captchaRepository: CaptchaRepository
        get() = security.captchaRepository
        set(value) { security.captchaRepository = value }

    val observeActiveCaptchaRequestsUseCase: ObserveActiveCaptchaRequestsUseCase
        get() = security.observeActiveCaptchaRequestsUseCase

    val getActiveCaptchaRequestsUseCase: GetActiveCaptchaRequestsUseCase
        get() = security.getActiveCaptchaRequestsUseCase

    val verifyCaptchaUseCase: VerifyCaptchaUseCase
        get() = security.verifyCaptchaUseCase

    val submitCaptchaResultUseCase: SubmitCaptchaResultUseCase
        get() = security.submitCaptchaResultUseCase

    val cancelCaptchaUseCase: CancelCaptchaUseCase
        get() = security.cancelCaptchaUseCase

    val captchaViewModel: CaptchaViewModel
        get() = security.captchaViewModel

    fun createCaptchaViewModel(): CaptchaViewModel = security.createCaptchaViewModel()

    var biometricsRepository: BiometricsRepository
        get() = security.biometricsRepository
        set(value) { security.biometricsRepository = value }

    val observeBiometricKeyStateUseCase: ObserveBiometricKeyStateUseCase
        get() = security.observeBiometricKeyStateUseCase

    val getBiometricKeyStateUseCase: GetBiometricKeyStateUseCase
        get() = security.getBiometricKeyStateUseCase

    val checkBiometricKeyReadyUseCase: CheckBiometricKeyReadyUseCase
        get() = security.checkBiometricKeyReadyUseCase

    val deleteInvalidBiometricKeyUseCase: DeleteInvalidBiometricKeyUseCase
        get() = security.deleteInvalidBiometricKeyUseCase

    val isBiometricKeyReadyUseCase: IsBiometricKeyReadyUseCase
        get() = security.isBiometricKeyReadyUseCase

    val hasDeviceBiometricsChangedUseCase: HasDeviceBiometricsChangedUseCase
        get() = security.hasDeviceBiometricsChangedUseCase

    val biometricsViewModel: BiometricsViewModel
        get() = security.biometricsViewModel

    fun createBiometricsViewModel(): BiometricsViewModel = security.createBiometricsViewModel()

    val authTokensRepository: AuthTokensRepository
        get() = security.authTokensRepository

    val pruneTokensListUseCase: PruneTokensListUseCase
        get() = security.pruneTokensListUseCase

    val validateAuthTokenFormatUseCase: ValidateAuthTokenFormatUseCase
        get() = security.validateAuthTokenFormatUseCase

    val observeAuthTokensStateUseCase: ObserveAuthTokensStateUseCase
        get() = security.observeAuthTokensStateUseCase

    val getAuthTokensStateUseCase: GetAuthTokensStateUseCase
        get() = security.getAuthTokensStateUseCase

    val getSavedLoginTokensUseCase: GetSavedLoginTokensUseCase
        get() = security.getSavedLoginTokensUseCase

    val saveLoginTokenUseCase: SaveLoginTokenUseCase
        get() = security.saveLoginTokenUseCase

    val getSavedLogoutTokensUseCase: GetSavedLogoutTokensUseCase
        get() = security.getSavedLogoutTokensUseCase

    val saveLogoutTokensUseCase: SaveLogoutTokensUseCase
        get() = security.saveLogoutTokensUseCase

    val addLogoutTokenUseCase: AddLogoutTokenUseCase
        get() = security.addLogoutTokenUseCase

    val removeTokenUseCase: RemoveTokenUseCase
        get() = security.removeTokenUseCase

    val clearAllTokensUseCase: ClearAllTokensUseCase
        get() = security.clearAllTokensUseCase

    val refreshAuthTokensUseCase: RefreshAuthTokensUseCase
        get() = security.refreshAuthTokensUseCase

    val authTokensViewModel: AuthTokensViewModel
        get() = security.authTokensViewModel

    fun createAuthTokensViewModel(): AuthTokensViewModel = security.createAuthTokensViewModel()

    var botGuardRepository: BotGuardRepository
        get() = security.botGuardRepository
        set(value) { security.botGuardRepository = value }

    val isGuardBotConfirmationNeededUseCase: IsGuardBotConfirmationNeededUseCase
        get() = security.isGuardBotConfirmationNeededUseCase

    val determineGuardBotLaunchFlowUseCase: DetermineGuardBotLaunchFlowUseCase
        get() = security.determineGuardBotLaunchFlowUseCase

    val registerGuardBotSessionUseCase: RegisterGuardBotSessionUseCase
        get() = security.registerGuardBotSessionUseCase

    val getGuardBotSessionUseCase: GetGuardBotSessionUseCase
        get() = security.getGuardBotSessionUseCase

    val getAllActiveGuardBotSessionsUseCase: GetAllActiveGuardBotSessionsUseCase
        get() = security.getAllActiveGuardBotSessionsUseCase

    val closeGuardBotSessionUseCase: CloseGuardBotSessionUseCase
        get() = security.closeGuardBotSessionUseCase

    val setGuardBotConfirmationShownUseCase: SetGuardBotConfirmationShownUseCase
        get() = security.setGuardBotConfirmationShownUseCase

    val clearAllGuardBotSessionsUseCase: ClearAllGuardBotSessionsUseCase
        get() = security.clearAllGuardBotSessionsUseCase

    val observeGuardBotDecisionsUseCase: ObserveGuardBotDecisionsUseCase
        get() = security.observeGuardBotDecisionsUseCase

    val observeGuardBotStateUseCase: ObserveGuardBotStateUseCase
        get() = security.observeGuardBotStateUseCase

    val mapJoinChatBotResultUseCase: MapJoinChatBotResultUseCase
        get() = security.mapJoinChatBotResultUseCase

    val formatGuardBotBulletinUseCase: FormatGuardBotBulletinUseCase
        get() = security.formatGuardBotBulletinUseCase

    val botGuardViewModel: BotGuardViewModel
        get() = security.botGuardViewModel

    fun createBotGuardViewModel(): BotGuardViewModel = security.createBotGuardViewModel()

    var flagSecureRepository: org.telegram.messenger.feature.security.flagsecure.domain.repository.FlagSecureRepository
        get() = security.flagSecureRepository
        set(value) { security.flagSecureRepository = value }

    val attachSecurityReasonUseCase: org.telegram.messenger.feature.security.flagsecure.domain.usecase.AttachSecurityReasonUseCase
        get() = security.attachSecurityReasonUseCase

    val detachSecurityReasonUseCase: org.telegram.messenger.feature.security.flagsecure.domain.usecase.DetachSecurityReasonUseCase
        get() = security.detachSecurityReasonUseCase

    val invalidateWindowSecurityUseCase: org.telegram.messenger.feature.security.flagsecure.domain.usecase.InvalidateWindowSecurityUseCase
        get() = security.invalidateWindowSecurityUseCase

    val isWindowSecuredUseCase: org.telegram.messenger.feature.security.flagsecure.domain.usecase.IsWindowSecuredUseCase
        get() = security.isWindowSecuredUseCase

    val getWindowSecurityStateUseCase: org.telegram.messenger.feature.security.flagsecure.domain.usecase.GetWindowSecurityStateUseCase
        get() = security.getWindowSecurityStateUseCase

    val getAllWindowStatesUseCase: org.telegram.messenger.feature.security.flagsecure.domain.usecase.GetAllWindowStatesUseCase
        get() = security.getAllWindowStatesUseCase

    val resetWindowSecurityUseCase: org.telegram.messenger.feature.security.flagsecure.domain.usecase.ResetWindowSecurityUseCase
        get() = security.resetWindowSecurityUseCase

    val observeWindowStateUseCase: org.telegram.messenger.feature.security.flagsecure.domain.usecase.ObserveWindowStateUseCase
        get() = security.observeWindowStateUseCase

    val observeAllWindowStatesUseCase: org.telegram.messenger.feature.security.flagsecure.domain.usecase.ObserveAllWindowStatesUseCase
        get() = security.observeAllWindowStatesUseCase

    val evaluateSecurityRuleUseCase: org.telegram.messenger.feature.security.flagsecure.domain.usecase.EvaluateSecurityRuleUseCase
        get() = security.evaluateSecurityRuleUseCase

    fun getFlagSecureViewModel(windowId: String = "main"): org.telegram.messenger.feature.security.flagsecure.presentation.FlagSecureViewModel = security.getFlagSecureViewModel(windowId)

    fun createFlagSecureViewModel(windowId: String = "main"): org.telegram.messenger.feature.security.flagsecure.presentation.FlagSecureViewModel = security.createFlagSecureViewModel(windowId)

    // ==================== SOCIAL DOMAIN ====================

    var profileRepository: ProfileRepository
        get() = social.profileRepository
        set(value) { social.profileRepository = value }

    val observeProfileUseCase: ObserveProfileUseCase
        get() = social.observeProfileUseCase

    val getProfileUseCase: GetProfileUseCase
        get() = social.getProfileUseCase

    val loadFullProfileUseCase: LoadFullProfileUseCase
        get() = social.loadFullProfileUseCase

    val blockPeerUseCase: BlockPeerUseCase
        get() = social.blockPeerUseCase

    val unblockPeerUseCase: UnblockPeerUseCase
        get() = social.unblockPeerUseCase

    fun getProfileViewModel(peerId: Long): ProfileViewModel = social.getProfileViewModel(peerId)

    fun createProfileViewModel(peerId: Long): ProfileViewModel = social.createProfileViewModel(peerId)

    var contactsRepository: ContactsRepository
        get() = social.contactsRepository
        set(value) { social.contactsRepository = value }

    fun createContactsRepository(): ContactsRepository = social.createContactsRepository()

    val observeContactsUseCase: ObserveContactsUseCase
        get() = social.observeContactsUseCase

    val getContactsUseCase: GetContactsUseCase
        get() = social.getContactsUseCase

    val getContactUseCase: GetContactUseCase
        get() = social.getContactUseCase

    val addContactUseCase: AddContactUseCase
        get() = social.addContactUseCase

    val deleteContactUseCase: DeleteContactUseCase
        get() = social.deleteContactUseCase

    val searchContactsUseCase: SearchContactsUseCase
        get() = social.searchContactsUseCase

    val contactsViewModel: ContactsViewModel
        get() = social.contactsViewModel

    fun createContactsViewModel(): ContactsViewModel = social.createContactsViewModel()

    var locationRepository: LocationRepository
        get() = social.locationRepository
        set(value) { social.locationRepository = value }

    val observeActiveSharingsUseCase: ObserveActiveSharingsUseCase
        get() = social.observeActiveSharingsUseCase

    val observePeerLocationsUseCase: ObservePeerLocationsUseCase
        get() = social.observePeerLocationsUseCase

    val observeLastKnownLocationUseCase: ObserveLastKnownLocationUseCase
        get() = social.observeLastKnownLocationUseCase

    val getActiveSharingsUseCase: GetActiveSharingsUseCase
        get() = social.getActiveSharingsUseCase

    val isSharingLocationUseCase: IsSharingLocationUseCase
        get() = social.isSharingLocationUseCase

    val getSharingInfoUseCase: GetSharingInfoUseCase
        get() = social.getSharingInfoUseCase

    val getLastKnownLocationUseCase: GetLastKnownLocationUseCase
        get() = social.getLastKnownLocationUseCase

    val loadPeerLiveLocationsUseCase: LoadPeerLiveLocationsUseCase
        get() = social.loadPeerLiveLocationsUseCase

    val stopLocationSharingUseCase: StopLocationSharingUseCase
        get() = social.stopLocationSharingUseCase

    val stopAllLocationSharingsUseCase: StopAllLocationSharingsUseCase
        get() = social.stopAllLocationSharingsUseCase

    val setProximityAlertUseCase: SetProximityAlertUseCase
        get() = social.setProximityAlertUseCase

    val sendStaticLocationUseCase: SendStaticLocationUseCase
        get() = social.sendStaticLocationUseCase

    val sendLiveLocationUseCase: SendLiveLocationUseCase
        get() = social.sendLiveLocationUseCase

    val markLiveLocationsAsReadUseCase: MarkLiveLocationsAsReadUseCase
        get() = social.markLiveLocationsAsReadUseCase

    val locationViewModel: LocationViewModel
        get() = social.locationViewModel

    fun createLocationViewModel(): LocationViewModel = social.createLocationViewModel()

    var boostsRepository: BoostsRepository
        get() = social.boostsRepository
        set(value) { social.boostsRepository = value }

    val getBoostsStatusUseCase: GetBoostsStatusUseCase
        get() = social.getBoostsStatusUseCase

    val getMyBoostsUseCase: GetMyBoostsUseCase
        get() = social.getMyBoostsUseCase

    val checkCanApplyBoostUseCase: CheckCanApplyBoostUseCase
        get() = social.checkCanApplyBoostUseCase

    val applyBoostUseCase: ApplyBoostUseCase
        get() = social.applyBoostUseCase

    val boostsViewModel: BoostsViewModel
        get() = social.boostsViewModel

    fun createBoostsViewModel(): BoostsViewModel = social.createBoostsViewModel()

    var joinRequestsRepository: JoinRequestsRepository
        get() = social.joinRequestsRepository
        set(value) { social.joinRequestsRepository = value }

    val observePendingRequestsUseCase: ObservePendingRequestsUseCase
        get() = social.observePendingRequestsUseCase

    val getPendingRequestsCountUseCase: GetPendingRequestsCountUseCase
        get() = social.getPendingRequestsCountUseCase

    val getCachedJoinRequestsUseCase: GetCachedJoinRequestsUseCase
        get() = social.getCachedJoinRequestsUseCase

    val loadJoinRequestsUseCase: LoadJoinRequestsUseCase
        get() = social.loadJoinRequestsUseCase

    val approveJoinRequestUseCase: ApproveJoinRequestUseCase
        get() = social.approveJoinRequestUseCase

    val dismissJoinRequestUseCase: DismissJoinRequestUseCase
        get() = social.dismissJoinRequestUseCase

    val approveAllJoinRequestsUseCase: ApproveAllJoinRequestsUseCase
        get() = social.approveAllJoinRequestsUseCase

    val dismissAllJoinRequestsUseCase: DismissAllJoinRequestsUseCase
        get() = social.dismissAllJoinRequestsUseCase

    val joinRequestsViewModel: JoinRequestsViewModel
        get() = social.joinRequestsViewModel

    fun createJoinRequestsViewModel(): JoinRequestsViewModel = social.createJoinRequestsViewModel()

    var birthdaysRepository: BirthdaysRepository
        get() = social.birthdaysRepository
        set(value) { social.birthdaysRepository = value }

    val observeBirthdaysUseCase: ObserveBirthdaysUseCase
        get() = social.observeBirthdaysUseCase

    val getBirthdaysStateUseCase: GetBirthdaysStateUseCase
        get() = social.getBirthdaysStateUseCase

    val checkBirthdaysUseCase: CheckBirthdaysUseCase
        get() = social.checkBirthdaysUseCase

    val hideTodayBirthdaysUseCase: HideTodayBirthdaysUseCase
        get() = social.hideTodayBirthdaysUseCase

    val isBirthdayTodayUseCase: IsBirthdayTodayUseCase
        get() = social.isBirthdayTodayUseCase

    val hasBirthdaysTodayUseCase: HasBirthdaysTodayUseCase
        get() = social.hasBirthdaysTodayUseCase

    val birthdaysViewModel: BirthdaysViewModel
        get() = social.birthdaysViewModel

    fun createBirthdaysViewModel(): BirthdaysViewModel = social.createBirthdaysViewModel()

    // ==================== SYSTEM DOMAIN ====================

    var settingsRepository: SettingsRepository
        get() = system.settingsRepository
        set(value) { system.settingsRepository = value }

    val observeSettingsUseCase: ObserveSettingsUseCase
        get() = system.observeSettingsUseCase

    val getSettingsUseCase: GetSettingsUseCase
        get() = system.getSettingsUseCase

    val updateFontSizeUseCase: UpdateFontSizeUseCase
        get() = system.updateFontSizeUseCase

    val updateBubbleRadiusUseCase: UpdateBubbleRadiusUseCase
        get() = system.updateBubbleRadiusUseCase

    val updateSaveToGalleryUseCase: UpdateSaveToGalleryUseCase
        get() = system.updateSaveToGalleryUseCase

    val updateStreamMediaUseCase: UpdateStreamMediaUseCase
        get() = system.updateStreamMediaUseCase

    val updateSyncContactsUseCase: UpdateSyncContactsUseCase
        get() = system.updateSyncContactsUseCase

    val settingsViewModel: SettingsViewModel
        get() = system.settingsViewModel

    fun createSettingsViewModel(): SettingsViewModel = system.createSettingsViewModel()

    var notificationsRepository: NotificationsRepository
        get() = system.notificationsRepository
        set(value) { system.notificationsRepository = value }

    val observeNotificationSettingsUseCase: ObserveNotificationSettingsUseCase
        get() = system.observeNotificationSettingsUseCase

    val getNotificationSettingsUseCase: GetNotificationSettingsUseCase
        get() = system.getNotificationSettingsUseCase

    val observeBadgeUseCase: ObserveBadgeUseCase
        get() = system.observeBadgeUseCase

    val getBadgeUseCase: GetBadgeUseCase
        get() = system.getBadgeUseCase

    val observeBadgeSettingsUseCase: ObserveBadgeSettingsUseCase
        get() = system.observeBadgeSettingsUseCase

    val getBadgeSettingsUseCase: GetBadgeSettingsUseCase
        get() = system.getBadgeSettingsUseCase

    val togglePeerNotificationsUseCase: TogglePeerNotificationsUseCase
        get() = system.togglePeerNotificationsUseCase

    val toggleInChatSoundUseCase: ToggleInChatSoundUseCase
        get() = system.toggleInChatSoundUseCase

    val toggleInAppSoundsUseCase: ToggleInAppSoundsUseCase
        get() = system.toggleInAppSoundsUseCase

    val toggleInAppVibrateUseCase: ToggleInAppVibrateUseCase
        get() = system.toggleInAppVibrateUseCase

    val toggleInAppPreviewUseCase: ToggleInAppPreviewUseCase
        get() = system.toggleInAppPreviewUseCase

    val toggleContactJoinedNotificationsUseCase: ToggleContactJoinedNotificationsUseCase
        get() = system.toggleContactJoinedNotificationsUseCase

    val togglePinnedMessagesNotificationsUseCase: TogglePinnedMessagesNotificationsUseCase
        get() = system.togglePinnedMessagesNotificationsUseCase

    val updateBadgeSettingsUseCase: UpdateBadgeSettingsUseCase
        get() = system.updateBadgeSettingsUseCase

    val muteDialogUseCase: MuteDialogUseCase
        get() = system.muteDialogUseCase

    val isDialogMutedUseCase: IsDialogMutedUseCase
        get() = system.isDialogMutedUseCase

    val refreshBadgeUseCase: RefreshBadgeUseCase
        get() = system.refreshBadgeUseCase

    val notificationsViewModel: NotificationsViewModel
        get() = system.notificationsViewModel

    fun createNotificationsViewModel(): NotificationsViewModel = system.createNotificationsViewModel()

    val themeRepository: ThemeRepository
        get() = system.themeRepository

    val observeAppearanceSettingsUseCase: ObserveAppearanceSettingsUseCase
        get() = system.observeAppearanceSettingsUseCase

    val getAppearanceSettingsUseCase: GetAppearanceSettingsUseCase
        get() = system.getAppearanceSettingsUseCase

    val observeAvailableThemesUseCase: ObserveAvailableThemesUseCase
        get() = system.observeAvailableThemesUseCase

    val getAvailableThemesUseCase: GetAvailableThemesUseCase
        get() = system.getAvailableThemesUseCase

    val applyThemeUseCase: ApplyThemeUseCase
        get() = system.applyThemeUseCase

    val observeNightModeUseCase: ObserveNightModeUseCase
        get() = system.observeNightModeUseCase

    val setNightModeTypeUseCase: SetNightModeTypeUseCase
        get() = system.setNightModeTypeUseCase

    val setNightModeSettingsUseCase: SetNightModeSettingsUseCase
        get() = system.setNightModeSettingsUseCase

    val setThemeAccentUseCase: SetThemeAccentUseCase
        get() = system.setThemeAccentUseCase

    val setBubbleRadiusUseCase: SetBubbleRadiusUseCase
        get() = system.setBubbleRadiusUseCase

    val resetAppearanceSettingsUseCase: ResetAppearanceSettingsUseCase
        get() = system.resetAppearanceSettingsUseCase

    val themeViewModel: ThemeViewModel
        get() = system.themeViewModel

    fun createThemeViewModel(): ThemeViewModel = system.createThemeViewModel()

    var dataStorageRepository: DataStorageRepository
        get() = system.dataStorageRepository
        set(value) { system.dataStorageRepository = value }

    val observeNetworkUsageUseCase: ObserveNetworkUsageUseCase
        get() = system.observeNetworkUsageUseCase

    val observeStorageUsageUseCase: ObserveStorageUsageUseCase
        get() = system.observeStorageUsageUseCase

    val observeAutoDownloadPresetUseCase: ObserveAutoDownloadPresetUseCase
        get() = system.observeAutoDownloadPresetUseCase

    val observeKeepMediaSettingsUseCase: ObserveKeepMediaSettingsUseCase
        get() = system.observeKeepMediaSettingsUseCase

    val getNetworkUsageUseCase: GetNetworkUsageUseCase
        get() = system.getNetworkUsageUseCase

    val resetNetworkUsageUseCase: ResetNetworkUsageUseCase
        get() = system.resetNetworkUsageUseCase

    val getStorageUsageUseCase: GetStorageUsageUseCase
        get() = system.getStorageUsageUseCase

    val clearCacheUseCase: ClearCacheUseCase
        get() = system.clearCacheUseCase

    val clearDatabaseUseCase: ClearDatabaseUseCase
        get() = system.clearDatabaseUseCase

    val getAutoDownloadPresetUseCase: GetAutoDownloadPresetUseCase
        get() = system.getAutoDownloadPresetUseCase

    val updateAutoDownloadPresetUseCase: UpdateAutoDownloadPresetUseCase
        get() = system.updateAutoDownloadPresetUseCase

    val getKeepMediaSettingsUseCase: GetKeepMediaSettingsUseCase
        get() = system.getKeepMediaSettingsUseCase

    val updateKeepMediaUseCase: UpdateKeepMediaUseCase
        get() = system.updateKeepMediaUseCase

    val refreshStorageUsageUseCase: RefreshStorageUsageUseCase
        get() = system.refreshStorageUsageUseCase

    val dataStorageViewModel: DataStorageViewModel
        get() = system.dataStorageViewModel

    fun createDataStorageViewModel(): DataStorageViewModel = system.createDataStorageViewModel()

    var launcherIconRepository: LauncherIconRepository
        get() = system.launcherIconRepository
        set(value) { system.launcherIconRepository = value }

    val observeLauncherIconsUseCase: ObserveLauncherIconsUseCase
        get() = system.observeLauncherIconsUseCase

    val getLauncherIconsUseCase: GetLauncherIconsUseCase
        get() = system.getLauncherIconsUseCase

    val getActiveLauncherIconUseCase: GetActiveLauncherIconUseCase
        get() = system.getActiveLauncherIconUseCase

    val isLauncherIconEnabledUseCase: IsLauncherIconEnabledUseCase
        get() = system.isLauncherIconEnabledUseCase

    val setLauncherIconUseCase: SetLauncherIconUseCase
        get() = system.setLauncherIconUseCase

    val fixLauncherIconIfNeededUseCase: FixLauncherIconIfNeededUseCase
        get() = system.fixLauncherIconIfNeededUseCase

    val launcherIconViewModel: LauncherIconViewModel
        get() = system.launcherIconViewModel

    fun createLauncherIconViewModel(): LauncherIconViewModel = system.createLauncherIconViewModel()

    var hintsRepository: HintsRepository
        get() = system.hintsRepository
        set(value) { system.hintsRepository = value }

    val observeHintsUseCase: ObserveHintsUseCase
        get() = system.observeHintsUseCase

    val getHintsStateUseCase: GetHintsStateUseCase
        get() = system.getHintsStateUseCase

    val getHintUseCase: GetHintUseCase
        get() = system.getHintUseCase

    val shouldShowHintUseCase: ShouldShowHintUseCase
        get() = system.shouldShowHintUseCase

    val incrementHintUseCase: IncrementHintUseCase
        get() = system.incrementHintUseCase

    val doNotShowAgainHintUseCase: DoNotShowAgainHintUseCase
        get() = system.doNotShowAgainHintUseCase

    val resetHintUseCase: ResetHintUseCase
        get() = system.resetHintUseCase

    val resetAllHintsUseCase: ResetAllHintsUseCase
        get() = system.resetAllHintsUseCase

    val hintsViewModel: HintsViewModel
        get() = system.hintsViewModel

    fun createHintsViewModel(): HintsViewModel = system.createHintsViewModel()

    var refreshRateRepository: RefreshRateRepository
        get() = system.refreshRateRepository
        set(value) { system.refreshRateRepository = value }

    val observeRefreshRateStateUseCase: ObserveRefreshRateStateUseCase
        get() = system.observeRefreshRateStateUseCase

    val getRefreshRateStateUseCase: GetRefreshRateStateUseCase
        get() = system.getRefreshRateStateUseCase

    val startRefreshRateTrackingUseCase: StartRefreshRateTrackingUseCase
        get() = system.startRefreshRateTrackingUseCase

    val stopRefreshRateTrackingUseCase: StopRefreshRateTrackingUseCase
        get() = system.stopRefreshRateTrackingUseCase

    val toggleAdaptiveRefreshRateUseCase: ToggleAdaptiveRefreshRateUseCase
        get() = system.toggleAdaptiveRefreshRateUseCase

    val setPreferredRefreshRateModeUseCase: SetPreferredRefreshRateModeUseCase
        get() = system.setPreferredRefreshRateModeUseCase

    val recordFrameMetricUseCase: RecordFrameMetricUseCase
        get() = system.recordFrameMetricUseCase

    val resetRefreshRateStatsUseCase: ResetRefreshRateStatsUseCase
        get() = system.resetRefreshRateStatsUseCase

    val getDisplayRefreshModesUseCase: GetDisplayRefreshModesUseCase
        get() = system.getDisplayRefreshModesUseCase

    val refreshRateViewModel: RefreshRateViewModel
        get() = system.refreshRateViewModel

    fun createRefreshRateViewModel(): RefreshRateViewModel = system.createRefreshRateViewModel()

    fun createFloatingDebugRepository(activityProvider: (() -> LaunchActivity?)? = null): FloatingDebugRepository = system.createFloatingDebugRepository(activityProvider)

    val floatingDebugRepository: FloatingDebugRepository
        get() = system.floatingDebugRepository

    val isFloatingDebugActiveUseCase: IsFloatingDebugActiveUseCase
        get() = system.isFloatingDebugActiveUseCase

    val setFloatingDebugActiveUseCase: SetFloatingDebugActiveUseCase
        get() = system.setFloatingDebugActiveUseCase

    val toggleFloatingDebugActiveUseCase: ToggleFloatingDebugActiveUseCase
        get() = system.toggleFloatingDebugActiveUseCase

    val getFloatingDebugItemsUseCase: GetFloatingDebugItemsUseCase
        get() = system.getFloatingDebugItemsUseCase

    val registerFloatingDebugItemsUseCase: RegisterFloatingDebugItemsUseCase
        get() = system.registerFloatingDebugItemsUseCase

    val clearFloatingDebugItemsUseCase: ClearFloatingDebugItemsUseCase
        get() = system.clearFloatingDebugItemsUseCase

    val observeFloatingDebugStateUseCase: ObserveFloatingDebugStateUseCase
        get() = system.observeFloatingDebugStateUseCase

    val getFloatingDebugStateUseCase: GetFloatingDebugStateUseCase
        get() = system.getFloatingDebugStateUseCase

    val floatingDebugViewModel: FloatingDebugViewModel
        get() = system.floatingDebugViewModel

    fun createFloatingDebugViewModel(activityProvider: (() -> LaunchActivity?)? = null): FloatingDebugViewModel = system.createFloatingDebugViewModel(activityProvider)

    fun createKeyboardInsetsRepository(inAppController: WindowInsetsInAppController? = null): KeyboardInsetsRepository = system.createKeyboardInsetsRepository(inAppController)

    val keyboardInsetsRepository: KeyboardInsetsRepository
        get() = system.keyboardInsetsRepository

    val requestInAppKeyboardHeightUseCase: RequestInAppKeyboardHeightUseCase
        get() = system.requestInAppKeyboardHeightUseCase

    val resetInAppKeyboardHeightUseCase: ResetInAppKeyboardHeightUseCase
        get() = system.resetInAppKeyboardHeightUseCase

    val requestInAppKeyboardHeightWithNavbarUseCase: RequestInAppKeyboardHeightWithNavbarUseCase
        get() = system.requestInAppKeyboardHeightWithNavbarUseCase

    val updateSystemInsetsUseCase: UpdateSystemInsetsUseCase
        get() = system.updateSystemInsetsUseCase

    val getKeyboardInsetsUseCase: GetKeyboardInsetsUseCase
        get() = system.getKeyboardInsetsUseCase

    val observeKeyboardInsetsUseCase: ObserveKeyboardInsetsUseCase
        get() = system.observeKeyboardInsetsUseCase

    val keyboardInsetsViewModel: KeyboardInsetsViewModel
        get() = system.keyboardInsetsViewModel

    fun createKeyboardInsetsViewModel(inAppController: WindowInsetsInAppController? = null): KeyboardInsetsViewModel = system.createKeyboardInsetsViewModel(inAppController)

    fun createMainTabsRepository(controller: MainTabsActivityController? = null): MainTabsRepository = system.createMainTabsRepository(controller)

    val mainTabsRepository: MainTabsRepository
        get() = system.mainTabsRepository

    val observeMainTabsConfigUseCase: ObserveMainTabsConfigUseCase
        get() = system.observeMainTabsConfigUseCase

    val getMainTabsConfigUseCase: GetMainTabsConfigUseCase
        get() = system.getMainTabsConfigUseCase

    val setMainTabsVisibleUseCase: SetMainTabsVisibleUseCase
        get() = system.setMainTabsVisibleUseCase

    val selectMainTabUseCase: SelectMainTabUseCase
        get() = system.selectMainTabUseCase

    val setShowCallsTabUseCase: SetShowCallsTabUseCase
        get() = system.setShowCallsTabUseCase

    val updateChatsUnreadCountUseCase: UpdateChatsUnreadCountUseCase
        get() = system.updateChatsUnreadCountUseCase

    val setContactsPermissionWarningUseCase: SetContactsPermissionWarningUseCase
        get() = system.setContactsPermissionWarningUseCase

    val mainTabsViewModel: MainTabsViewModel
        get() = system.mainTabsViewModel

    fun createMainTabsViewModel(controller: MainTabsActivityController? = null): MainTabsViewModel = system.createMainTabsViewModel(controller)

    fun createAdjustPanRepository(): AdjustPanRepository = system.createAdjustPanRepository()

    val adjustPanRepository: AdjustPanRepository
        get() = system.adjustPanRepository

    val calculatePanTransitionPlanUseCase: CalculatePanTransitionPlanUseCase
        get() = system.calculatePanTransitionPlanUseCase

    val computePanProgressUseCase: ComputePanProgressUseCase
        get() = system.computePanProgressUseCase

    val observeAdjustPanStateUseCase: ObserveAdjustPanStateUseCase
        get() = system.observeAdjustPanStateUseCase

    val getAdjustPanStateUseCase: GetAdjustPanStateUseCase
        get() = system.getAdjustPanStateUseCase

    val setAdjustPanEnabledUseCase: SetAdjustPanEnabledUseCase
        get() = system.setAdjustPanEnabledUseCase

    val startAdjustPanTransitionUseCase: StartAdjustPanTransitionUseCase
        get() = system.startAdjustPanTransitionUseCase

    val updateAdjustPanTransitionUseCase: UpdateAdjustPanTransitionUseCase
        get() = system.updateAdjustPanTransitionUseCase

    val stopAdjustPanTransitionUseCase: StopAdjustPanTransitionUseCase
        get() = system.stopAdjustPanTransitionUseCase

    val resetAdjustPanUseCase: ResetAdjustPanUseCase
        get() = system.resetAdjustPanUseCase

    val adjustPanViewModel: AdjustPanViewModel
        get() = system.adjustPanViewModel

    fun createAdjustPanViewModel(): AdjustPanViewModel = system.createAdjustPanViewModel()

    fun createKeyboardHideRepository(): KeyboardHideRepository = system.createKeyboardHideRepository()

    val keyboardHideRepository: KeyboardHideRepository
        get() = system.keyboardHideRepository

    val calculateKeyboardHideProgressUseCase: CalculateKeyboardHideProgressUseCase
        get() = system.calculateKeyboardHideProgressUseCase

    val evaluateKeyboardDismissDecisionUseCase: EvaluateKeyboardDismissDecisionUseCase
        get() = system.evaluateKeyboardDismissDecisionUseCase

    val observeKeyboardHideStateUseCase: ObserveKeyboardHideStateUseCase
        get() = system.observeKeyboardHideStateUseCase

    val getKeyboardHideStateUseCase: GetKeyboardHideStateUseCase
        get() = system.getKeyboardHideStateUseCase

    val setKeyboardHideEnabledUseCase: SetKeyboardHideEnabledUseCase
        get() = system.setKeyboardHideEnabledUseCase

    val startKeyboardHideMovingUseCase: StartKeyboardHideMovingUseCase
        get() = system.startKeyboardHideMovingUseCase

    val updateKeyboardHideMovingUseCase: UpdateKeyboardHideMovingUseCase
        get() = system.updateKeyboardHideMovingUseCase

    val endKeyboardHideMovingUseCase: EndKeyboardHideMovingUseCase
        get() = system.endKeyboardHideMovingUseCase

    val finishKeyboardHideDismissUseCase: FinishKeyboardHideDismissUseCase
        get() = system.finishKeyboardHideDismissUseCase

    val resetKeyboardHideUseCase: ResetKeyboardHideUseCase
        get() = system.resetKeyboardHideUseCase

    val keyboardHideViewModel: KeyboardHideViewModel
        get() = system.keyboardHideViewModel

    fun createKeyboardHideViewModel(): KeyboardHideViewModel = system.createKeyboardHideViewModel()

    fun createPinchToZoomRepository(): PinchToZoomRepository = system.createPinchToZoomRepository()

    val pinchToZoomRepository: PinchToZoomRepository
        get() = system.pinchToZoomRepository

    val observePinchZoomStateUseCase: ObservePinchZoomStateUseCase
        get() = system.observePinchZoomStateUseCase

    val getPinchZoomStateUseCase: GetPinchZoomStateUseCase
        get() = system.getPinchZoomStateUseCase

    val calculatePinchScaleUseCase: CalculatePinchScaleUseCase
        get() = system.calculatePinchScaleUseCase

    val calculatePinchTranslationUseCase: CalculatePinchTranslationUseCase
        get() = system.calculatePinchTranslationUseCase

    val calculatePinchTransformUseCase: CalculatePinchTransformUseCase
        get() = system.calculatePinchTransformUseCase

    val calculatePinchImageBoundsUseCase: CalculatePinchImageBoundsUseCase
        get() = system.calculatePinchImageBoundsUseCase

    val evaluatePinchGestureUseCase: EvaluatePinchGestureUseCase
        get() = system.evaluatePinchGestureUseCase

    val startPinchZoomUseCase: StartPinchZoomUseCase
        get() = system.startPinchZoomUseCase

    val updatePinchZoomUseCase: UpdatePinchZoomUseCase
        get() = system.updatePinchZoomUseCase

    val finishPinchZoomUseCase: FinishPinchZoomUseCase
        get() = system.finishPinchZoomUseCase

    val resetPinchZoomUseCase: ResetPinchZoomUseCase
        get() = system.resetPinchZoomUseCase

    val pinchToZoomViewModel: PinchToZoomViewModel
        get() = system.pinchToZoomViewModel

    fun createPinchToZoomViewModel(): PinchToZoomViewModel = system.createPinchToZoomViewModel()

    fun createRecyclerScrollRepository(): RecyclerScrollRepository = system.createRecyclerScrollRepository()

    val recyclerScrollRepository: RecyclerScrollRepository
        get() = system.recyclerScrollRepository

    val observeRecyclerScrollStateUseCase: ObserveRecyclerScrollStateUseCase
        get() = system.observeRecyclerScrollStateUseCase

    val getRecyclerScrollStateUseCase: GetRecyclerScrollStateUseCase
        get() = system.getRecyclerScrollStateUseCase

    val evaluateScrollEligibilityUseCase: EvaluateScrollEligibilityUseCase
        get() = system.evaluateScrollEligibilityUseCase

    val calculateScrollAnimationPlanUseCase: CalculateScrollAnimationPlanUseCase
        get() = system.calculateScrollAnimationPlanUseCase

    val calculateScrollLengthUseCase: CalculateScrollLengthUseCase
        get() = system.calculateScrollLengthUseCase

    val computeScrollViewTranslationsUseCase: ComputeScrollViewTranslationsUseCase
        get() = system.computeScrollViewTranslationsUseCase

    val startRecyclerScrollUseCase: StartRecyclerScrollUseCase
        get() = system.startRecyclerScrollUseCase

    val updateRecyclerScrollProgressUseCase: UpdateRecyclerScrollProgressUseCase
        get() = system.updateRecyclerScrollProgressUseCase

    val finishRecyclerScrollUseCase: FinishRecyclerScrollUseCase
        get() = system.finishRecyclerScrollUseCase

    val cancelRecyclerScrollUseCase: CancelRecyclerScrollUseCase
        get() = system.cancelRecyclerScrollUseCase

    val resetRecyclerScrollUseCase: ResetRecyclerScrollUseCase
        get() = system.resetRecyclerScrollUseCase

    val recyclerScrollViewModel: RecyclerScrollViewModel
        get() = system.recyclerScrollViewModel

    fun createRecyclerScrollViewModel(): RecyclerScrollViewModel = system.createRecyclerScrollViewModel()

    val localizationRepository: LocalizationRepository
        get() = system.localizationRepository

    val resolvePluralQuantityUseCase: ResolvePluralQuantityUseCase
        get() = system.resolvePluralQuantityUseCase

    val formatRelativeTimestampUseCase: FormatRelativeTimestampUseCase
        get() = system.formatRelativeTimestampUseCase

    val formatFullNameUseCase: FormatFullNameUseCase
        get() = system.formatFullNameUseCase

    val formatNumberWithSuffixUseCase: FormatNumberWithSuffixUseCase
        get() = system.formatNumberWithSuffixUseCase

    val detectRtlLanguageUseCase: DetectRtlLanguageUseCase
        get() = system.detectRtlLanguageUseCase

    val observeLocalizationStateUseCase: ObserveLocalizationStateUseCase
        get() = system.observeLocalizationStateUseCase

    val getLocalizationStateUseCase: GetLocalizationStateUseCase
        get() = system.getLocalizationStateUseCase

    val applyLocaleUseCase: ApplyLocaleUseCase
        get() = system.applyLocaleUseCase

    val toggle24HourFormatUseCase: Toggle24HourFormatUseCase
        get() = system.toggle24HourFormatUseCase

    val setNameDisplayOrderUseCase: SetNameDisplayOrderUseCase
        get() = system.setNameDisplayOrderUseCase

    val localizationViewModel: LocalizationViewModel
        get() = system.localizationViewModel

    fun createLocalizationViewModel(): LocalizationViewModel = system.createLocalizationViewModel()

    val ringtoneRepository: RingtoneRepository
        get() = system.ringtoneRepository

    val validateRingtoneEligibilityUseCase: ValidateRingtoneEligibilityUseCase
        get() = system.validateRingtoneEligibilityUseCase

    val observeRingtonesUseCase: ObserveRingtonesUseCase
        get() = system.observeRingtonesUseCase

    val observeRingtoneStateUseCase: ObserveRingtoneStateUseCase
        get() = system.observeRingtoneStateUseCase

    val getRingtonesUseCase: GetRingtonesUseCase
        get() = system.getRingtonesUseCase

    val getRingtoneByIdUseCase: GetRingtoneByIdUseCase
        get() = system.getRingtoneByIdUseCase

    val getRingtoneSoundPathUseCase: GetRingtoneSoundPathUseCase
        get() = system.getRingtoneSoundPathUseCase

    val addRingtoneUseCase: AddRingtoneUseCase
        get() = system.addRingtoneUseCase

    val removeRingtoneUseCase: RemoveRingtoneUseCase
        get() = system.removeRingtoneUseCase

    val saveRingtoneFromDocumentUseCase: SaveRingtoneFromDocumentUseCase
        get() = system.saveRingtoneFromDocumentUseCase

    val uploadRingtoneUseCase: UploadRingtoneUseCase
        get() = system.uploadRingtoneUseCase

    val cancelRingtoneUploadUseCase: CancelRingtoneUploadUseCase
        get() = system.cancelRingtoneUploadUseCase

    val refreshRingtonesUseCase: RefreshRingtonesUseCase
        get() = system.refreshRingtonesUseCase

    val selectRingtoneUseCase: SelectRingtoneUseCase
        get() = system.selectRingtoneUseCase

    val ringtoneViewModel: RingtoneViewModel
        get() = system.ringtoneViewModel

    fun createRingtoneViewModel(): RingtoneViewModel = system.createRingtoneViewModel()

    val browserRepository: BrowserRepository
        get() = system.browserRepository

    val classifyUrlTargetUseCase: ClassifyUrlTargetUseCase
        get() = system.classifyUrlTargetUseCase

    val extractUsernameFromUrlUseCase: ExtractUsernameFromUrlUseCase
        get() = system.extractUsernameFromUrlUseCase

    val checkUrlSafetyUseCase: CheckUrlSafetyUseCase
        get() = system.checkUrlSafetyUseCase

    val observeBrowserStateUseCase: ObserveBrowserStateUseCase
        get() = system.observeBrowserStateUseCase

    val getBrowserStateUseCase: GetBrowserStateUseCase
        get() = system.getBrowserStateUseCase

    val updateBrowserSettingsUseCase: UpdateBrowserSettingsUseCase
        get() = system.updateBrowserSettingsUseCase

    val openBrowserUrlUseCase: OpenBrowserUrlUseCase
        get() = system.openBrowserUrlUseCase

    val manageBrowserHistoryUseCase: ManageBrowserHistoryUseCase
        get() = system.manageBrowserHistoryUseCase

    val browserViewModel: BrowserViewModel
        get() = system.browserViewModel

    fun createBrowserViewModel(): BrowserViewModel = system.createBrowserViewModel()

    val liteModeRepository: LiteModeRepository
        get() = system.liteModeRepository

    val calculateEffectiveFlagsUseCase: CalculateEffectiveFlagsUseCase
        get() = system.calculateEffectiveFlagsUseCase

    val checkLiteModeFlagUseCase: CheckLiteModeFlagUseCase
        get() = system.checkLiteModeFlagUseCase

    val resolvePresetUseCase: ResolvePresetUseCase
        get() = system.resolvePresetUseCase

    val observeLiteModeStateUseCase: ObserveLiteModeStateUseCase
        get() = system.observeLiteModeStateUseCase

    val getLiteModeStateUseCase: GetLiteModeStateUseCase
        get() = system.getLiteModeStateUseCase

    val toggleLiteModeFlagUseCase: ToggleLiteModeFlagUseCase
        get() = system.toggleLiteModeFlagUseCase

    val setLiteModePresetUseCase: SetLiteModePresetUseCase
        get() = system.setLiteModePresetUseCase

    val updatePowerSaverThresholdUseCase: UpdatePowerSaverThresholdUseCase
        get() = system.updatePowerSaverThresholdUseCase

    val liteModeViewModel: LiteModeViewModel
        get() = system.liteModeViewModel

    fun createLiteModeViewModel(): LiteModeViewModel = system.createLiteModeViewModel()

    val appConfigRepository: AppConfigRepository
        get() = system.appConfigRepository

    val getAppConfigUseCase: GetAppConfigUseCase
        get() = system.getAppConfigUseCase

    val observeAppConfigUseCase: ObserveAppConfigUseCase
        get() = system.observeAppConfigUseCase

    val getMessageLimitsUseCase: GetMessageLimitsUseCase
        get() = system.getMessageLimitsUseCase

    val getStarsPricingConfigUseCase: GetStarsPricingConfigUseCase
        get() = system.getStarsPricingConfigUseCase

    val getTonPricingConfigUseCase: GetTonPricingConfigUseCase
        get() = system.getTonPricingConfigUseCase

    val getRichMessageLimitsUseCase: GetRichMessageLimitsUseCase
        get() = system.getRichMessageLimitsUseCase

    val getPollsConfigUseCase: GetPollsConfigUseCase
        get() = system.getPollsConfigUseCase

    val getAiComposeConfigUseCase: GetAiComposeConfigUseCase
        get() = system.getAiComposeConfigUseCase

    val getAppLimitsUseCase: GetAppLimitsUseCase
        get() = system.getAppLimitsUseCase

    val reloadAppConfigUseCase: ReloadAppConfigUseCase
        get() = system.reloadAppConfigUseCase

    val updateAppConfigValueUseCase: UpdateAppConfigValueUseCase
        get() = system.updateAppConfigValueUseCase

    val appConfigViewModel: AppConfigViewModel
        get() = system.appConfigViewModel

    fun createAppConfigViewModel(): AppConfigViewModel = system.createAppConfigViewModel()

    var windowVisibilityRepository: WindowVisibilityRepository
        get() = system.windowVisibilityRepository
        set(value) { system.windowVisibilityRepository = value }

    val requestHideWindowUseCase: RequestHideWindowUseCase
        get() = system.requestHideWindowUseCase

    val releaseHideWindowUseCase: ReleaseHideWindowUseCase
        get() = system.releaseHideWindowUseCase

    val toggleWindowHideUseCase: ToggleWindowHideUseCase
        get() = system.toggleWindowHideUseCase

    val checkIsWindowVisibleUseCase: CheckIsWindowVisibleUseCase
        get() = system.checkIsWindowVisibleUseCase

    val getWindowVisibilityStateUseCase: GetWindowVisibilityStateUseCase
        get() = system.getWindowVisibilityStateUseCase

    val getActiveHideReasonsUseCase: GetActiveHideReasonsUseCase
        get() = system.getActiveHideReasonsUseCase

    val resetWindowVisibilityUseCase: ResetWindowVisibilityUseCase
        get() = system.resetWindowVisibilityUseCase

    val observeWindowVisibilityStateUseCase: ObserveWindowVisibilityStateUseCase
        get() = system.observeWindowVisibilityStateUseCase

    val observeWindowVisibilityChangesUseCase: ObserveWindowVisibilityChangesUseCase
        get() = system.observeWindowVisibilityChangesUseCase

    val createVisibilityControllerUseCase: CreateVisibilityControllerUseCase
        get() = system.createVisibilityControllerUseCase

    val windowVisibilityViewModel: WindowVisibilityViewModel
        get() = system.windowVisibilityViewModel

    fun createWindowVisibilityViewModel(): WindowVisibilityViewModel = system.createWindowVisibilityViewModel()

    var countdownTimerRepository: CountdownTimerRepository
        get() = system.countdownTimerRepository
        set(value) { system.countdownTimerRepository = value }

    val startCountdownTimerUseCase: StartCountdownTimerUseCase
        get() = system.startCountdownTimerUseCase

    val stopCountdownTimerUseCase: StopCountdownTimerUseCase
        get() = system.stopCountdownTimerUseCase

    val pauseCountdownTimerUseCase: PauseCountdownTimerUseCase
        get() = system.pauseCountdownTimerUseCase

    val resumeCountdownTimerUseCase: ResumeCountdownTimerUseCase
        get() = system.resumeCountdownTimerUseCase

    val getCountdownTimerUseCase: GetCountdownTimerUseCase
        get() = system.getCountdownTimerUseCase

    val isCountdownTimerRunningUseCase: IsCountdownTimerRunningUseCase
        get() = system.isCountdownTimerRunningUseCase

    val tickCountdownTimerUseCase: TickCountdownTimerUseCase
        get() = system.tickCountdownTimerUseCase

    val clearAllCountdownTimersUseCase: ClearAllCountdownTimersUseCase
        get() = system.clearAllCountdownTimersUseCase

    val observeCountdownTimerUseCase: ObserveCountdownTimerUseCase
        get() = system.observeCountdownTimerUseCase

    val observeCountdownStateUseCase: ObserveCountdownStateUseCase
        get() = system.observeCountdownStateUseCase

    val decomposeCountdownTimeUseCase: DecomposeCountdownTimeUseCase
        get() = system.decomposeCountdownTimeUseCase

    val formatCountdownTimeUseCase: FormatCountdownTimeUseCase
        get() = system.formatCountdownTimeUseCase

    val countdownTimerViewModel: CountdownTimerViewModel
        get() = system.countdownTimerViewModel

    fun createCountdownTimerViewModel(): CountdownTimerViewModel = system.createCountdownTimerViewModel()

    var leakDetectorRepository: org.telegram.messenger.feature.system.leakdetector.domain.repository.LeakDetectorRepository
        get() = system.leakDetectorRepository
        set(value) { system.leakDetectorRepository = value }

    val startLeakDetectionUseCase: org.telegram.messenger.feature.system.leakdetector.domain.usecase.StartLeakDetectionUseCase
        get() = system.startLeakDetectionUseCase

    val stopLeakDetectionUseCase: org.telegram.messenger.feature.system.leakdetector.domain.usecase.StopLeakDetectionUseCase
        get() = system.stopLeakDetectionUseCase

    val trackInstanceUseCase: org.telegram.messenger.feature.system.leakdetector.domain.usecase.TrackInstanceUseCase
        get() = system.trackInstanceUseCase

    val triggerLeakCheckUseCase: org.telegram.messenger.feature.system.leakdetector.domain.usecase.TriggerLeakCheckUseCase
        get() = system.triggerLeakCheckUseCase

    val confirmLeakUseCase: org.telegram.messenger.feature.system.leakdetector.domain.usecase.ConfirmLeakUseCase
        get() = system.confirmLeakUseCase

    val getTrackedClassesStatsUseCase: org.telegram.messenger.feature.system.leakdetector.domain.usecase.GetTrackedClassesStatsUseCase
        get() = system.getTrackedClassesStatsUseCase

    val getConfirmedLeaksUseCase: org.telegram.messenger.feature.system.leakdetector.domain.usecase.GetConfirmedLeaksUseCase
        get() = system.getConfirmedLeaksUseCase

    val resetLeakDetectorUseCase: org.telegram.messenger.feature.system.leakdetector.domain.usecase.ResetLeakDetectorUseCase
        get() = system.resetLeakDetectorUseCase

    val observeLeakDetectorStateUseCase: org.telegram.messenger.feature.system.leakdetector.domain.usecase.ObserveLeakDetectorStateUseCase
        get() = system.observeLeakDetectorStateUseCase

    val observeConfirmedLeaksUseCase: org.telegram.messenger.feature.system.leakdetector.domain.usecase.ObserveConfirmedLeaksUseCase
        get() = system.observeConfirmedLeaksUseCase

    val leakDetectorViewModel: org.telegram.messenger.feature.system.leakdetector.presentation.LeakDetectorViewModel
        get() = system.leakDetectorViewModel

    fun createLeakDetectorViewModel(): org.telegram.messenger.feature.system.leakdetector.presentation.LeakDetectorViewModel = system.createLeakDetectorViewModel()

    var fpsContentRepository: org.telegram.messenger.feature.system.fpscontent.domain.repository.FpsContentRepository
        get() = system.fpsContentRepository
        set(value) { system.fpsContentRepository = value }

    val registerFrameCallbackUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.RegisterFrameCallbackUseCase
        get() = system.registerFrameCallbackUseCase

    val registerRunnableCallbackUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.RegisterRunnableCallbackUseCase
        get() = system.registerRunnableCallbackUseCase

    val unregisterCallbackUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.UnregisterCallbackUseCase
        get() = system.unregisterCallbackUseCase

    val requestViewInvalidationUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.RequestViewInvalidationUseCase
        get() = system.requestViewInvalidationUseCase

    val requestDrawableInvalidationUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.RequestDrawableInvalidationUseCase
        get() = system.requestDrawableInvalidationUseCase

    val dispatchVsyncTickUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.DispatchVsyncTickUseCase
        get() = system.dispatchVsyncTickUseCase

    val calculateFpsTimingUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.CalculateFpsTimingUseCase
        get() = system.calculateFpsTimingUseCase

    val getFpsContentStatsUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.GetFpsContentStatsUseCase
        get() = system.getFpsContentStatsUseCase

    val getFpsSubscriptionsUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.GetFpsSubscriptionsUseCase
        get() = system.getFpsSubscriptionsUseCase

    val observeFpsContentStatsUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.ObserveFpsContentStatsUseCase
        get() = system.observeFpsContentStatsUseCase

    val observeFpsTicksUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.ObserveFpsTicksUseCase
        get() = system.observeFpsTicksUseCase

    val resetFpsContentUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.ResetFpsContentUseCase
        get() = system.resetFpsContentUseCase

    val fpsContentViewModel: org.telegram.messenger.feature.system.fpscontent.presentation.FpsContentViewModel
        get() = system.fpsContentViewModel

    fun createFpsContentViewModel(): org.telegram.messenger.feature.system.fpscontent.presentation.FpsContentViewModel = system.createFpsContentViewModel()

    var anrWatchdogRepository: org.telegram.messenger.feature.system.anrwatchdog.domain.repository.AnrWatchdogRepository
        get() = system.anrWatchdogRepository
        set(value) { system.anrWatchdogRepository = value }

    val startAnrMonitoringUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.StartAnrMonitoringUseCase
        get() = system.startAnrMonitoringUseCase

    val stopAnrMonitoringUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.StopAnrMonitoringUseCase
        get() = system.stopAnrMonitoringUseCase

    val setAppForegroundStatusUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.SetAppForegroundStatusUseCase
        get() = system.setAppForegroundStatusUseCase

    val sendMainThreadPingUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.SendMainThreadPingUseCase
        get() = system.sendMainThreadPingUseCase

    val acknowledgePingUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.AcknowledgePingUseCase
        get() = system.acknowledgePingUseCase

    val checkMainThreadFreezeUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.CheckMainThreadFreezeUseCase
        get() = system.checkMainThreadFreezeUseCase

    val resolveIncidentUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.ResolveIncidentUseCase
        get() = system.resolveIncidentUseCase

    val getAnrWatchdogStateUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.GetAnrWatchdogStateUseCase
        get() = system.getAnrWatchdogStateUseCase

    val getAnrIncidentsUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.GetAnrIncidentsUseCase
        get() = system.getAnrIncidentsUseCase

    val clearAnrHistoryUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.ClearAnrHistoryUseCase
        get() = system.clearAnrHistoryUseCase

    val observeAnrWatchdogStateUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.ObserveAnrWatchdogStateUseCase
        get() = system.observeAnrWatchdogStateUseCase

    val observeAnrIncidentsUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.ObserveAnrIncidentsUseCase
        get() = system.observeAnrIncidentsUseCase

    val anrWatchdogViewModel: org.telegram.messenger.feature.system.anrwatchdog.presentation.AnrWatchdogViewModel
        get() = system.anrWatchdogViewModel

    fun createAnrWatchdogViewModel(): org.telegram.messenger.feature.system.anrwatchdog.presentation.AnrWatchdogViewModel = system.createAnrWatchdogViewModel()

    var emuDetectorRepository: org.telegram.messenger.feature.system.emudetector.domain.repository.EmuDetectorRepository
        get() = system.emuDetectorRepository
        set(value) { system.emuDetectorRepository = value }

    val detectEnvironmentUseCase: org.telegram.messenger.feature.system.emudetector.domain.usecase.DetectEnvironmentUseCase
        get() = system.detectEnvironmentUseCase

    val isEmulatorUseCase: org.telegram.messenger.feature.system.emudetector.domain.usecase.IsEmulatorUseCase
        get() = system.isEmulatorUseCase

    val getCachedDiagnosticsUseCase: org.telegram.messenger.feature.system.emudetector.domain.usecase.GetCachedDiagnosticsUseCase
        get() = system.getCachedDiagnosticsUseCase

    val observeDiagnosticsUseCase: org.telegram.messenger.feature.system.emudetector.domain.usecase.ObserveDiagnosticsUseCase
        get() = system.observeDiagnosticsUseCase

    val observeIsEmulatorUseCase: org.telegram.messenger.feature.system.emudetector.domain.usecase.ObserveIsEmulatorUseCase
        get() = system.observeIsEmulatorUseCase

    val getDetectorConfigUseCase: org.telegram.messenger.feature.system.emudetector.domain.usecase.GetDetectorConfigUseCase
        get() = system.getDetectorConfigUseCase

    val updateDetectorConfigUseCase: org.telegram.messenger.feature.system.emudetector.domain.usecase.UpdateDetectorConfigUseCase
        get() = system.updateDetectorConfigUseCase

    val addCustomPackageNameUseCase: org.telegram.messenger.feature.system.emudetector.domain.usecase.AddCustomPackageNameUseCase
        get() = system.addCustomPackageNameUseCase

    val clearDetectorCacheUseCase: org.telegram.messenger.feature.system.emudetector.domain.usecase.ClearDetectorCacheUseCase
        get() = system.clearDetectorCacheUseCase

    val emuDetectorViewModel: org.telegram.messenger.feature.system.emudetector.presentation.EmuDetectorViewModel
        get() = system.emuDetectorViewModel

    fun createEmuDetectorViewModel(): org.telegram.messenger.feature.system.emudetector.presentation.EmuDetectorViewModel = system.createEmuDetectorViewModel()

    var animationLockerRepository: org.telegram.messenger.feature.system.animationlocker.domain.repository.AnimationLockerRepository
        get() = system.animationLockerRepository
        set(value) { system.animationLockerRepository = value }

    val acquireAnimationLockUseCase: org.telegram.messenger.feature.system.animationlocker.domain.usecase.AcquireAnimationLockUseCase
        get() = system.acquireAnimationLockUseCase

    val releaseAnimationLockUseCase: org.telegram.messenger.feature.system.animationlocker.domain.usecase.ReleaseAnimationLockUseCase
        get() = system.releaseAnimationLockUseCase

    val releaseAllAnimationLocksUseCase: org.telegram.messenger.feature.system.animationlocker.domain.usecase.ReleaseAllAnimationLocksUseCase
        get() = system.releaseAllAnimationLocksUseCase

    val setAnimationLockerDisabledUseCase: org.telegram.messenger.feature.system.animationlocker.domain.usecase.SetAnimationLockerDisabledUseCase
        get() = system.setAnimationLockerDisabledUseCase

    val isAnimationLockedUseCase: org.telegram.messenger.feature.system.animationlocker.domain.usecase.IsAnimationLockedUseCase
        get() = system.isAnimationLockedUseCase

    val isNotificationAllowedUseCase: org.telegram.messenger.feature.system.animationlocker.domain.usecase.IsNotificationAllowedUseCase
        get() = system.isNotificationAllowedUseCase

    val getAnimationLockerStateUseCase: org.telegram.messenger.feature.system.animationlocker.domain.usecase.GetAnimationLockerStateUseCase
        get() = system.getAnimationLockerStateUseCase

    val getAnimationLockerConfigUseCase: org.telegram.messenger.feature.system.animationlocker.domain.usecase.GetAnimationLockerConfigUseCase
        get() = system.getAnimationLockerConfigUseCase

    val updateAnimationLockerConfigUseCase: org.telegram.messenger.feature.system.animationlocker.domain.usecase.UpdateAnimationLockerConfigUseCase
        get() = system.updateAnimationLockerConfigUseCase

    val observeAnimationLockerStateUseCase: org.telegram.messenger.feature.system.animationlocker.domain.usecase.ObserveAnimationLockerStateUseCase
        get() = system.observeAnimationLockerStateUseCase

    val observeIsAnimationLockedUseCase: org.telegram.messenger.feature.system.animationlocker.domain.usecase.ObserveIsAnimationLockedUseCase
        get() = system.observeIsAnimationLockedUseCase

    val animationLockerViewModel: org.telegram.messenger.feature.system.animationlocker.presentation.AnimationLockerViewModel
        get() = system.animationLockerViewModel

    fun createAnimationLockerViewModel(): org.telegram.messenger.feature.system.animationlocker.presentation.AnimationLockerViewModel = system.createAnimationLockerViewModel()

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
