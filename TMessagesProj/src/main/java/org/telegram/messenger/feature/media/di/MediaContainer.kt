package org.telegram.messenger.feature.media.di

import org.telegram.messenger.feature.media.audioplayer.data.repository.LegacyAudioPlayerRepository
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
import org.telegram.messenger.feature.media.autodeletemedia.data.repository.LegacyAutoDeleteMediaRepository
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
import org.telegram.messenger.feature.media.cachebychats.data.repository.LegacyCacheByChatsRepository
import org.telegram.messenger.feature.media.cachebychats.domain.repository.CacheByChatsRepository
import org.telegram.messenger.feature.media.cachebychats.domain.usecase.ClearKeepMediaExceptionsUseCase
import org.telegram.messenger.feature.media.cachebychats.domain.usecase.GetCacheByChatsConfigUseCase
import org.telegram.messenger.feature.media.cachebychats.domain.usecase.ObserveCacheByChatsConfigUseCase
import org.telegram.messenger.feature.media.cachebychats.domain.usecase.RemoveKeepMediaExceptionUseCase
import org.telegram.messenger.feature.media.cachebychats.domain.usecase.SetKeepMediaDurationUseCase
import org.telegram.messenger.feature.media.cachebychats.domain.usecase.SetKeepMediaExceptionUseCase
import org.telegram.messenger.feature.media.cachebychats.presentation.CacheByChatsViewModel
import org.telegram.messenger.feature.media.camera.data.repository.LegacyCameraRepository
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
import org.telegram.messenger.feature.media.chromecast.data.repository.LegacyChromecastRepository
import org.telegram.messenger.feature.media.chromecast.domain.repository.ChromecastRepository
import org.telegram.messenger.feature.media.chromecast.domain.usecase.CastMediaUseCase
import org.telegram.messenger.feature.media.chromecast.domain.usecase.GetChromecastStateUseCase
import org.telegram.messenger.feature.media.chromecast.domain.usecase.IsCastingUseCase
import org.telegram.messenger.feature.media.chromecast.domain.usecase.IsMediaPlayingOnCastUseCase
import org.telegram.messenger.feature.media.chromecast.domain.usecase.ObserveChromecastStateUseCase
import org.telegram.messenger.feature.media.chromecast.domain.usecase.SetCastCoverFileUseCase
import org.telegram.messenger.feature.media.chromecast.domain.usecase.StopCastingUseCase
import org.telegram.messenger.feature.media.chromecast.presentation.ChromecastViewModel
import org.telegram.messenger.feature.media.contentpreview.data.repository.LegacyContentPreviewRepository
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
import org.telegram.messenger.feature.media.downloadmanager.data.datasource.DownloadManagerLocalDataSource
import org.telegram.messenger.feature.media.downloadmanager.data.datasource.DownloadManagerRemoteDataSource
import org.telegram.messenger.feature.media.downloadmanager.data.repository.DownloadManagerRepositoryImpl
import org.telegram.messenger.feature.media.downloadmanager.data.repository.LegacyDownloadManagerRepository
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
import org.telegram.messenger.feature.media.fileloader.data.repository.LegacyFileLoaderRepository
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
import org.telegram.messenger.feature.media.fileref.data.datasource.FileRefLocalDataSource
import org.telegram.messenger.feature.media.fileref.data.datasource.FileRefRemoteDataSource
import org.telegram.messenger.feature.media.fileref.data.repository.FileRefRepositoryImpl
import org.telegram.messenger.feature.media.fileref.data.repository.LegacyFileRefRepository
import org.telegram.messenger.feature.media.fileref.domain.repository.FileRefRepository
import org.telegram.messenger.feature.media.fileref.domain.usecase.CancelFileRefRequestUseCase
import org.telegram.messenger.feature.media.fileref.domain.usecase.ClearFileRefCacheUseCase
import org.telegram.messenger.feature.media.fileref.domain.usecase.GetFileRefStatsUseCase
import org.telegram.messenger.feature.media.fileref.domain.usecase.NotifyReferenceRenewedUseCase
import org.telegram.messenger.feature.media.fileref.domain.usecase.ObserveFileRefStatsUseCase
import org.telegram.messenger.feature.media.fileref.domain.usecase.RequestReferenceRenewalUseCase
import org.telegram.messenger.feature.media.fileref.presentation.FileRefViewModel
import org.telegram.messenger.feature.media.gallerysave.data.repository.LegacyGallerySaveRepository
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
import org.telegram.messenger.feature.media.imageloader.data.repository.LegacyImageLoaderRepository
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
import org.telegram.messenger.feature.media.mediadata.data.repository.LegacyMediaRepository
import org.telegram.messenger.feature.media.mediadata.domain.repository.MediaRepository
import org.telegram.messenger.feature.media.mediadata.domain.usecase.GetAlbumMediaUseCase
import org.telegram.messenger.feature.media.mediadata.domain.usecase.GetAllMediaUseCase
import org.telegram.messenger.feature.media.mediadata.domain.usecase.GetMediaAlbumsUseCase
import org.telegram.messenger.feature.media.mediadata.domain.usecase.ObserveMediaAlbumsUseCase
import org.telegram.messenger.feature.media.mediadata.presentation.MediaViewModel
import org.telegram.messenger.feature.media.photoviewer.data.repository.LegacyPhotoViewerRepository
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
import org.telegram.messenger.feature.media.pip.data.repository.LegacyPipRepository
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
import org.telegram.messenger.feature.media.sharedmedia.data.repository.LegacySharedMediaRepository
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
import org.telegram.messenger.feature.media.stories.data.repository.LegacyStoriesRepository
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
import org.telegram.messenger.feature.media.storycustomparams.data.repository.LegacyStoryCustomParamsRepository
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
import org.telegram.messenger.feature.media.voip.data.repository.LegacyVoIPRepository
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

class MediaContainer(val account: Int) {

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

    val fileRefRemoteDataSource: FileRefRemoteDataSource by lazy {
        FileRefRemoteDataSource(account)
    }

    val fileRefLocalDataSource: FileRefLocalDataSource by lazy {
        FileRefLocalDataSource(account)
    }

    fun createFileRefRepository(): FileRefRepository {
        return FileRefRepositoryImpl(
            account = account,
            localDataSource = fileRefLocalDataSource,
            remoteDataSource = fileRefRemoteDataSource
        )
    }

    private var customFileRefRepository: FileRefRepository? = null

    var fileRefRepository: FileRefRepository
        get() = customFileRefRepository ?: createFileRefRepository()
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

    val downloadManagerRemoteDataSource: DownloadManagerRemoteDataSource by lazy {
        DownloadManagerRemoteDataSource(account)
    }

    val downloadManagerLocalDataSource: DownloadManagerLocalDataSource by lazy {
        DownloadManagerLocalDataSource(account)
    }

    fun createDownloadManagerRepository(): DownloadManagerRepository {
        return DownloadManagerRepositoryImpl(
            currentAccount = account,
            localDataSource = downloadManagerLocalDataSource,
            remoteDataSource = downloadManagerRemoteDataSource
        )
    }

    private var customDownloadManagerRepository: DownloadManagerRepository? = null

    var downloadManagerRepository: DownloadManagerRepository
        get() = customDownloadManagerRepository ?: createDownloadManagerRepository()
        set(value) {
            customDownloadManagerRepository = value
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

    val autoDeleteMediaRepository: AutoDeleteMediaRepository by lazy {
        LegacyAutoDeleteMediaRepository(account)
    }

    val checkShouldRunCleanupUseCase: CheckShouldRunCleanupUseCase
        get() = CheckShouldRunCleanupUseCase()

    val calculateEvictionCandidatesUseCase: CalculateEvictionCandidatesUseCase
        get() = CalculateEvictionCandidatesUseCase()

    val lockFileUseCase: LockFileUseCase
        get() = LockFileUseCase(autoDeleteMediaRepository)

    val unlockFileUseCase: UnlockFileUseCase
        get() = UnlockFileUseCase(autoDeleteMediaRepository)

    val isFileLockedUseCase: IsFileLockedUseCase
        get() = IsFileLockedUseCase(autoDeleteMediaRepository)

    val runAutoDeleteCleanupUseCase: RunAutoDeleteCleanupUseCase
        get() = RunAutoDeleteCleanupUseCase(autoDeleteMediaRepository)

    val observeAutoDeleteStateUseCase: ObserveAutoDeleteStateUseCase
        get() = ObserveAutoDeleteStateUseCase(autoDeleteMediaRepository)

    val getAutoDeleteStateUseCase: GetAutoDeleteStateUseCase
        get() = GetAutoDeleteStateUseCase(autoDeleteMediaRepository)

    private var cachedAutoDeleteMediaViewModel: AutoDeleteMediaViewModel? = null

    val autoDeleteMediaViewModel: AutoDeleteMediaViewModel
        get() {
            var vm = cachedAutoDeleteMediaViewModel
            if (vm == null) {
                vm = createAutoDeleteMediaViewModel()
                cachedAutoDeleteMediaViewModel = vm
            }
            return vm
        }

    fun createAutoDeleteMediaViewModel(): AutoDeleteMediaViewModel {
        return AutoDeleteMediaViewModel(
            observeAutoDeleteState = observeAutoDeleteStateUseCase,
            runAutoDeleteCleanup = runAutoDeleteCleanupUseCase,
            lockFile = lockFileUseCase,
            unlockFile = unlockFileUseCase,
            repository = autoDeleteMediaRepository
        )
    }

    private var customStoryCustomParamsRepository: StoryCustomParamsRepository? = null

    var storyCustomParamsRepository: StoryCustomParamsRepository
        get() = customStoryCustomParamsRepository ?: LegacyStoryCustomParamsRepository(account)
        set(value) {
            customStoryCustomParamsRepository = value
        }

    val checkStoryCustomParamsEmptyUseCase: CheckStoryCustomParamsEmptyUseCase
        get() = CheckStoryCustomParamsEmptyUseCase()

    val computeStoryCustomParamsFlagsUseCase: ComputeStoryCustomParamsFlagsUseCase
        get() = ComputeStoryCustomParamsFlagsUseCase()

    val observeStoryCustomParamsStateUseCase: ObserveStoryCustomParamsStateUseCase
        get() = ObserveStoryCustomParamsStateUseCase(storyCustomParamsRepository)

    val getStoryCustomParamsStateUseCase: GetStoryCustomParamsStateUseCase
        get() = GetStoryCustomParamsStateUseCase(storyCustomParamsRepository)

    val getStoryCustomParamsUseCase: GetStoryCustomParamsUseCase
        get() = GetStoryCustomParamsUseCase(storyCustomParamsRepository)

    val saveStoryCustomParamsUseCase: SaveStoryCustomParamsUseCase
        get() = SaveStoryCustomParamsUseCase(storyCustomParamsRepository)

    val updateStoryTranslationUseCase: UpdateStoryTranslationUseCase
        get() = UpdateStoryTranslationUseCase(storyCustomParamsRepository)

    val copyStoryCustomParamsUseCase: CopyStoryCustomParamsUseCase
        get() = CopyStoryCustomParamsUseCase(storyCustomParamsRepository)

    val removeStoryCustomParamsUseCase: RemoveStoryCustomParamsUseCase
        get() = RemoveStoryCustomParamsUseCase(storyCustomParamsRepository)

    val clearAllStoryCustomParamsUseCase: ClearAllStoryCustomParamsUseCase
        get() = ClearAllStoryCustomParamsUseCase(storyCustomParamsRepository)

    private var cachedStoryCustomParamsViewModel: StoryCustomParamsViewModel? = null

    val storyCustomParamsViewModel: StoryCustomParamsViewModel
        get() {
            var vm = cachedStoryCustomParamsViewModel
            if (vm == null) {
                vm = createStoryCustomParamsViewModel()
                cachedStoryCustomParamsViewModel = vm
            }
            return vm
        }

    fun createStoryCustomParamsViewModel(): StoryCustomParamsViewModel {
        return StoryCustomParamsViewModel(
            observeStateUseCase = observeStoryCustomParamsStateUseCase,
            getParamsUseCase = getStoryCustomParamsUseCase,
            saveParamsUseCase = saveStoryCustomParamsUseCase,
            updateTranslationUseCase = updateStoryTranslationUseCase,
            copyParamsUseCase = copyStoryCustomParamsUseCase,
            removeParamsUseCase = removeStoryCustomParamsUseCase,
            clearAllUseCase = clearAllStoryCustomParamsUseCase,
            checkEmptyUseCase = checkStoryCustomParamsEmptyUseCase
        )
    }

}
