package org.telegram.messenger.feature.system.di

import org.telegram.messenger.feature.system.adjustpan.data.repository.LegacyAdjustPanRepository
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
import org.telegram.messenger.feature.system.appconfig.data.repository.LegacyAppConfigRepository
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
import org.telegram.messenger.feature.system.browser.data.repository.LegacyBrowserRepository
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
import org.telegram.messenger.feature.system.countdowntimer.data.repository.LegacyCountdownTimerRepository
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
import org.telegram.messenger.feature.system.datastorage.data.repository.LegacyDataStorageRepository
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
import org.telegram.messenger.feature.system.floatingdebug.data.repository.LegacyFloatingDebugRepository
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
import org.telegram.messenger.feature.system.hints.data.repository.LegacyHintsRepository
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
import org.telegram.messenger.feature.system.keyboardhide.data.repository.LegacyKeyboardHideRepository
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
import org.telegram.messenger.feature.system.keyboardinsets.data.repository.LegacyKeyboardInsetsRepository
import org.telegram.messenger.feature.system.keyboardinsets.domain.repository.KeyboardInsetsRepository
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.GetKeyboardInsetsUseCase
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.ObserveKeyboardInsetsUseCase
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.RequestInAppKeyboardHeightUseCase
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.RequestInAppKeyboardHeightWithNavbarUseCase
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.ResetInAppKeyboardHeightUseCase
import org.telegram.messenger.feature.system.keyboardinsets.domain.usecase.UpdateSystemInsetsUseCase
import org.telegram.messenger.feature.system.keyboardinsets.presentation.KeyboardInsetsViewModel
import org.telegram.messenger.feature.system.launchericon.data.repository.LegacyLauncherIconRepository
import org.telegram.messenger.feature.system.launchericon.domain.repository.LauncherIconRepository
import org.telegram.messenger.feature.system.launchericon.domain.usecase.FixLauncherIconIfNeededUseCase
import org.telegram.messenger.feature.system.launchericon.domain.usecase.GetActiveLauncherIconUseCase
import org.telegram.messenger.feature.system.launchericon.domain.usecase.GetLauncherIconsUseCase
import org.telegram.messenger.feature.system.launchericon.domain.usecase.IsLauncherIconEnabledUseCase
import org.telegram.messenger.feature.system.launchericon.domain.usecase.ObserveLauncherIconsUseCase
import org.telegram.messenger.feature.system.launchericon.domain.usecase.SetLauncherIconUseCase
import org.telegram.messenger.feature.system.launchericon.presentation.LauncherIconViewModel
import org.telegram.messenger.feature.system.litemode.data.repository.LegacyLiteModeRepository
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
import org.telegram.messenger.feature.system.localization.data.repository.LegacyLocalizationRepository
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
import org.telegram.messenger.feature.system.maintabs.data.repository.LegacyMainTabsRepository
import org.telegram.messenger.feature.system.maintabs.domain.repository.MainTabsRepository
import org.telegram.messenger.feature.system.maintabs.domain.usecase.GetMainTabsConfigUseCase
import org.telegram.messenger.feature.system.maintabs.domain.usecase.ObserveMainTabsConfigUseCase
import org.telegram.messenger.feature.system.maintabs.domain.usecase.SelectMainTabUseCase
import org.telegram.messenger.feature.system.maintabs.domain.usecase.SetContactsPermissionWarningUseCase
import org.telegram.messenger.feature.system.maintabs.domain.usecase.SetMainTabsVisibleUseCase
import org.telegram.messenger.feature.system.maintabs.domain.usecase.SetShowCallsTabUseCase
import org.telegram.messenger.feature.system.maintabs.domain.usecase.UpdateChatsUnreadCountUseCase
import org.telegram.messenger.feature.system.maintabs.presentation.MainTabsViewModel
import org.telegram.messenger.feature.system.notifications.data.datasource.NotificationsLocalDataSource
import org.telegram.messenger.feature.system.notifications.data.datasource.NotificationsRemoteDataSource
import org.telegram.messenger.feature.system.notifications.data.repository.LegacyNotificationsRepository
import org.telegram.messenger.feature.system.notifications.data.repository.NotificationsRepositoryImpl
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
import org.telegram.messenger.feature.system.pinchtozoom.data.repository.LegacyPinchToZoomRepository
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
import org.telegram.messenger.feature.system.recyclerscroll.data.repository.LegacyRecyclerScrollRepository
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
import org.telegram.messenger.feature.system.refreshrate.data.repository.LegacyRefreshRateRepository
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
import org.telegram.messenger.feature.system.ringtones.data.repository.LegacyRingtoneRepository
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
import org.telegram.messenger.feature.system.settings.data.repository.LegacySettingsRepository
import org.telegram.messenger.feature.system.settings.domain.repository.SettingsRepository
import org.telegram.messenger.feature.system.settings.domain.usecase.GetSettingsUseCase
import org.telegram.messenger.feature.system.settings.domain.usecase.ObserveSettingsUseCase
import org.telegram.messenger.feature.system.settings.domain.usecase.UpdateBubbleRadiusUseCase
import org.telegram.messenger.feature.system.settings.domain.usecase.UpdateFontSizeUseCase
import org.telegram.messenger.feature.system.settings.domain.usecase.UpdateSaveToGalleryUseCase
import org.telegram.messenger.feature.system.settings.domain.usecase.UpdateStreamMediaUseCase
import org.telegram.messenger.feature.system.settings.domain.usecase.UpdateSyncContactsUseCase
import org.telegram.messenger.feature.system.settings.presentation.SettingsViewModel
import org.telegram.messenger.feature.system.themes.data.repository.LegacyThemeRepository
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
import org.telegram.messenger.feature.system.windowvisibility.data.repository.LegacyWindowVisibilityRepository
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
import org.telegram.ui.Components.inset.WindowInsetsInAppController
import org.telegram.ui.LaunchActivity
import org.telegram.ui.MainTabsActivityController

class SystemContainer(val account: Int) {

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

    val notificationsRemoteDataSource: NotificationsRemoteDataSource by lazy {
        NotificationsRemoteDataSource(account)
    }

    val notificationsLocalDataSource: NotificationsLocalDataSource by lazy {
        NotificationsLocalDataSource(account)
    }

    private var customNotificationsRepository: NotificationsRepository? = null

    var notificationsRepository: NotificationsRepository
        get() = customNotificationsRepository ?: createNotificationsRepository()
        set(value) {
            customNotificationsRepository = value
        }

    fun createNotificationsRepository(): NotificationsRepository {
        return NotificationsRepositoryImpl(
            currentAccount = account,
            localDataSource = notificationsLocalDataSource,
            remoteDataSource = notificationsRemoteDataSource
        )
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

    val browserRepository: BrowserRepository by lazy {
        LegacyBrowserRepository(account)
    }

    val classifyUrlTargetUseCase: ClassifyUrlTargetUseCase
        get() = ClassifyUrlTargetUseCase()

    val extractUsernameFromUrlUseCase: ExtractUsernameFromUrlUseCase
        get() = ExtractUsernameFromUrlUseCase()

    val checkUrlSafetyUseCase: CheckUrlSafetyUseCase
        get() = CheckUrlSafetyUseCase(classifyUrlTargetUseCase, extractUsernameFromUrlUseCase)

    val observeBrowserStateUseCase: ObserveBrowserStateUseCase
        get() = ObserveBrowserStateUseCase(browserRepository)

    val getBrowserStateUseCase: GetBrowserStateUseCase
        get() = GetBrowserStateUseCase(browserRepository)

    val updateBrowserSettingsUseCase: UpdateBrowserSettingsUseCase
        get() = UpdateBrowserSettingsUseCase(browserRepository)

    val openBrowserUrlUseCase: OpenBrowserUrlUseCase
        get() = OpenBrowserUrlUseCase(browserRepository, checkUrlSafetyUseCase)

    val manageBrowserHistoryUseCase: ManageBrowserHistoryUseCase
        get() = ManageBrowserHistoryUseCase(browserRepository)

    private var cachedBrowserViewModel: BrowserViewModel? = null

    val browserViewModel: BrowserViewModel
        get() {
            var vm = cachedBrowserViewModel
            if (vm == null) {
                vm = createBrowserViewModel()
                cachedBrowserViewModel = vm
            }
            return vm
        }

    fun createBrowserViewModel(): BrowserViewModel {
        return BrowserViewModel(
            observeBrowserState = observeBrowserStateUseCase,
            updateBrowserSettings = updateBrowserSettingsUseCase,
            checkUrlSafety = checkUrlSafetyUseCase,
            openBrowserUrl = openBrowserUrlUseCase,
            manageBrowserHistory = manageBrowserHistoryUseCase
        )
    }

    // --- Feature: LiteMode (Slice #87) ---
    val liteModeRepository: LiteModeRepository by lazy {
        LegacyLiteModeRepository(account)
    }

    val calculateEffectiveFlagsUseCase: CalculateEffectiveFlagsUseCase
        get() = CalculateEffectiveFlagsUseCase()

    val checkLiteModeFlagUseCase: CheckLiteModeFlagUseCase
        get() = CheckLiteModeFlagUseCase(calculateEffectiveFlagsUseCase)

    val resolvePresetUseCase: ResolvePresetUseCase
        get() = ResolvePresetUseCase()

    val observeLiteModeStateUseCase: ObserveLiteModeStateUseCase
        get() = ObserveLiteModeStateUseCase(liteModeRepository)

    val getLiteModeStateUseCase: GetLiteModeStateUseCase
        get() = GetLiteModeStateUseCase(liteModeRepository)

    val toggleLiteModeFlagUseCase: ToggleLiteModeFlagUseCase
        get() = ToggleLiteModeFlagUseCase(liteModeRepository)

    val setLiteModePresetUseCase: SetLiteModePresetUseCase
        get() = SetLiteModePresetUseCase(liteModeRepository)

    val updatePowerSaverThresholdUseCase: UpdatePowerSaverThresholdUseCase
        get() = UpdatePowerSaverThresholdUseCase(liteModeRepository)

    private var cachedLiteModeViewModel: LiteModeViewModel? = null

    val liteModeViewModel: LiteModeViewModel
        get() {
            var vm = cachedLiteModeViewModel
            if (vm == null) {
                vm = createLiteModeViewModel()
                cachedLiteModeViewModel = vm
            }
            return vm
        }

    fun createLiteModeViewModel(): LiteModeViewModel {
        return LiteModeViewModel(
            observeLiteModeState = observeLiteModeStateUseCase,
            toggleLiteModeFlag = toggleLiteModeFlagUseCase,
            setLiteModePreset = setLiteModePresetUseCase,
            updatePowerSaverThreshold = updatePowerSaverThresholdUseCase,
            repository = liteModeRepository
        )
    }

    val appConfigRepository: AppConfigRepository by lazy {
        LegacyAppConfigRepository(account)
    }

    val getAppConfigUseCase: GetAppConfigUseCase
        get() = GetAppConfigUseCase(appConfigRepository)

    val observeAppConfigUseCase: ObserveAppConfigUseCase
        get() = ObserveAppConfigUseCase(appConfigRepository)

    val getMessageLimitsUseCase: GetMessageLimitsUseCase
        get() = GetMessageLimitsUseCase(appConfigRepository)

    val getStarsPricingConfigUseCase: GetStarsPricingConfigUseCase
        get() = GetStarsPricingConfigUseCase(appConfigRepository)

    val getTonPricingConfigUseCase: GetTonPricingConfigUseCase
        get() = GetTonPricingConfigUseCase(appConfigRepository)

    val getRichMessageLimitsUseCase: GetRichMessageLimitsUseCase
        get() = GetRichMessageLimitsUseCase(appConfigRepository)

    val getPollsConfigUseCase: GetPollsConfigUseCase
        get() = GetPollsConfigUseCase(appConfigRepository)

    val getAiComposeConfigUseCase: GetAiComposeConfigUseCase
        get() = GetAiComposeConfigUseCase(appConfigRepository)

    val getAppLimitsUseCase: GetAppLimitsUseCase
        get() = GetAppLimitsUseCase(appConfigRepository)

    val reloadAppConfigUseCase: ReloadAppConfigUseCase
        get() = ReloadAppConfigUseCase(appConfigRepository)

    val updateAppConfigValueUseCase: UpdateAppConfigValueUseCase
        get() = UpdateAppConfigValueUseCase(appConfigRepository)

    private var cachedAppConfigViewModel: AppConfigViewModel? = null

    val appConfigViewModel: AppConfigViewModel
        get() {
            var vm = cachedAppConfigViewModel
            if (vm == null) {
                vm = createAppConfigViewModel()
                cachedAppConfigViewModel = vm
            }
            return vm
        }

    fun createAppConfigViewModel(): AppConfigViewModel {
        return AppConfigViewModel(
            observeAppConfig = observeAppConfigUseCase,
            reloadAppConfig = reloadAppConfigUseCase,
            updateAppConfigValue = updateAppConfigValueUseCase,
            repository = appConfigRepository
        )
    }

    private var customWindowVisibilityRepository: WindowVisibilityRepository? = null

    var windowVisibilityRepository: WindowVisibilityRepository
        get() = customWindowVisibilityRepository ?: LegacyWindowVisibilityRepository()
        set(value) {
            customWindowVisibilityRepository = value
        }

    val requestHideWindowUseCase: RequestHideWindowUseCase
        get() = RequestHideWindowUseCase(windowVisibilityRepository)

    val releaseHideWindowUseCase: ReleaseHideWindowUseCase
        get() = ReleaseHideWindowUseCase(windowVisibilityRepository)

    val toggleWindowHideUseCase: ToggleWindowHideUseCase
        get() = ToggleWindowHideUseCase(windowVisibilityRepository)

    val checkIsWindowVisibleUseCase: CheckIsWindowVisibleUseCase
        get() = CheckIsWindowVisibleUseCase(windowVisibilityRepository)

    val getWindowVisibilityStateUseCase: GetWindowVisibilityStateUseCase
        get() = GetWindowVisibilityStateUseCase(windowVisibilityRepository)

    val getActiveHideReasonsUseCase: GetActiveHideReasonsUseCase
        get() = GetActiveHideReasonsUseCase(windowVisibilityRepository)

    val resetWindowVisibilityUseCase: ResetWindowVisibilityUseCase
        get() = ResetWindowVisibilityUseCase(windowVisibilityRepository)

    val observeWindowVisibilityStateUseCase: ObserveWindowVisibilityStateUseCase
        get() = ObserveWindowVisibilityStateUseCase(windowVisibilityRepository)

    val observeWindowVisibilityChangesUseCase: ObserveWindowVisibilityChangesUseCase
        get() = ObserveWindowVisibilityChangesUseCase(windowVisibilityRepository)

    val createVisibilityControllerUseCase: CreateVisibilityControllerUseCase
        get() = CreateVisibilityControllerUseCase(windowVisibilityRepository)

    private var cachedWindowVisibilityViewModel: WindowVisibilityViewModel? = null

    val windowVisibilityViewModel: WindowVisibilityViewModel
        get() {
            var vm = cachedWindowVisibilityViewModel
            if (vm == null) {
                vm = createWindowVisibilityViewModel()
                cachedWindowVisibilityViewModel = vm
            }
            return vm
        }

    fun createWindowVisibilityViewModel(): WindowVisibilityViewModel {
        return WindowVisibilityViewModel(
            requestHideWindowUseCase = requestHideWindowUseCase,
            releaseHideWindowUseCase = releaseHideWindowUseCase,
            toggleWindowHideUseCase = toggleWindowHideUseCase,
            checkIsWindowVisibleUseCase = checkIsWindowVisibleUseCase,
            getWindowVisibilityStateUseCase = getWindowVisibilityStateUseCase,
            getActiveHideReasonsUseCase = getActiveHideReasonsUseCase,
            resetWindowVisibilityUseCase = resetWindowVisibilityUseCase,
            observeWindowVisibilityStateUseCase = observeWindowVisibilityStateUseCase
        )
    }

    private var customCountdownTimerRepository: CountdownTimerRepository? = null

    var countdownTimerRepository: CountdownTimerRepository
        get() = customCountdownTimerRepository ?: LegacyCountdownTimerRepository()
        set(value) {
            customCountdownTimerRepository = value
        }

    val startCountdownTimerUseCase: StartCountdownTimerUseCase
        get() = StartCountdownTimerUseCase(countdownTimerRepository)

    val stopCountdownTimerUseCase: StopCountdownTimerUseCase
        get() = StopCountdownTimerUseCase(countdownTimerRepository)

    val pauseCountdownTimerUseCase: PauseCountdownTimerUseCase
        get() = PauseCountdownTimerUseCase(countdownTimerRepository)

    val resumeCountdownTimerUseCase: ResumeCountdownTimerUseCase
        get() = ResumeCountdownTimerUseCase(countdownTimerRepository)

    val getCountdownTimerUseCase: GetCountdownTimerUseCase
        get() = GetCountdownTimerUseCase(countdownTimerRepository)

    val isCountdownTimerRunningUseCase: IsCountdownTimerRunningUseCase
        get() = IsCountdownTimerRunningUseCase(countdownTimerRepository)

    val tickCountdownTimerUseCase: TickCountdownTimerUseCase
        get() = TickCountdownTimerUseCase(countdownTimerRepository)

    val clearAllCountdownTimersUseCase: ClearAllCountdownTimersUseCase
        get() = ClearAllCountdownTimersUseCase(countdownTimerRepository)

    val observeCountdownTimerUseCase: ObserveCountdownTimerUseCase
        get() = ObserveCountdownTimerUseCase(countdownTimerRepository)

    val observeCountdownStateUseCase: ObserveCountdownStateUseCase
        get() = ObserveCountdownStateUseCase(countdownTimerRepository)

    val decomposeCountdownTimeUseCase: DecomposeCountdownTimeUseCase
        get() = DecomposeCountdownTimeUseCase()

    val formatCountdownTimeUseCase: FormatCountdownTimeUseCase
        get() = FormatCountdownTimeUseCase(decomposeCountdownTimeUseCase)

    private var cachedCountdownTimerViewModel: CountdownTimerViewModel? = null

    val countdownTimerViewModel: CountdownTimerViewModel
        get() {
            var vm = cachedCountdownTimerViewModel
            if (vm == null) {
                vm = createCountdownTimerViewModel()
                cachedCountdownTimerViewModel = vm
            }
            return vm
        }

    fun createCountdownTimerViewModel(): CountdownTimerViewModel {
        return CountdownTimerViewModel(
            startCountdownTimerUseCase = startCountdownTimerUseCase,
            stopCountdownTimerUseCase = stopCountdownTimerUseCase,
            pauseCountdownTimerUseCase = pauseCountdownTimerUseCase,
            resumeCountdownTimerUseCase = resumeCountdownTimerUseCase,
            getCountdownTimerUseCase = getCountdownTimerUseCase,
            isCountdownTimerRunningUseCase = isCountdownTimerRunningUseCase,
            tickCountdownTimerUseCase = tickCountdownTimerUseCase,
            clearAllCountdownTimersUseCase = clearAllCountdownTimersUseCase,
            observeCountdownStateUseCase = observeCountdownStateUseCase,
            formatCountdownTimeUseCase = formatCountdownTimeUseCase
        )
    }

    private var customLeakDetectorRepository: org.telegram.messenger.feature.system.leakdetector.domain.repository.LeakDetectorRepository? = null

    var leakDetectorRepository: org.telegram.messenger.feature.system.leakdetector.domain.repository.LeakDetectorRepository
        get() = customLeakDetectorRepository ?: org.telegram.messenger.feature.system.leakdetector.data.repository.LegacyLeakDetectorRepository()
        set(value) {
            customLeakDetectorRepository = value
        }

    val startLeakDetectionUseCase: org.telegram.messenger.feature.system.leakdetector.domain.usecase.StartLeakDetectionUseCase
        get() = org.telegram.messenger.feature.system.leakdetector.domain.usecase.StartLeakDetectionUseCase(leakDetectorRepository)

    val stopLeakDetectionUseCase: org.telegram.messenger.feature.system.leakdetector.domain.usecase.StopLeakDetectionUseCase
        get() = org.telegram.messenger.feature.system.leakdetector.domain.usecase.StopLeakDetectionUseCase(leakDetectorRepository)

    val trackInstanceUseCase: org.telegram.messenger.feature.system.leakdetector.domain.usecase.TrackInstanceUseCase
        get() = org.telegram.messenger.feature.system.leakdetector.domain.usecase.TrackInstanceUseCase(leakDetectorRepository)

    val triggerLeakCheckUseCase: org.telegram.messenger.feature.system.leakdetector.domain.usecase.TriggerLeakCheckUseCase
        get() = org.telegram.messenger.feature.system.leakdetector.domain.usecase.TriggerLeakCheckUseCase(leakDetectorRepository)

    val confirmLeakUseCase: org.telegram.messenger.feature.system.leakdetector.domain.usecase.ConfirmLeakUseCase
        get() = org.telegram.messenger.feature.system.leakdetector.domain.usecase.ConfirmLeakUseCase(leakDetectorRepository)

    val getTrackedClassesStatsUseCase: org.telegram.messenger.feature.system.leakdetector.domain.usecase.GetTrackedClassesStatsUseCase
        get() = org.telegram.messenger.feature.system.leakdetector.domain.usecase.GetTrackedClassesStatsUseCase(leakDetectorRepository)

    val getConfirmedLeaksUseCase: org.telegram.messenger.feature.system.leakdetector.domain.usecase.GetConfirmedLeaksUseCase
        get() = org.telegram.messenger.feature.system.leakdetector.domain.usecase.GetConfirmedLeaksUseCase(leakDetectorRepository)

    val resetLeakDetectorUseCase: org.telegram.messenger.feature.system.leakdetector.domain.usecase.ResetLeakDetectorUseCase
        get() = org.telegram.messenger.feature.system.leakdetector.domain.usecase.ResetLeakDetectorUseCase(leakDetectorRepository)

    val observeLeakDetectorStateUseCase: org.telegram.messenger.feature.system.leakdetector.domain.usecase.ObserveLeakDetectorStateUseCase
        get() = org.telegram.messenger.feature.system.leakdetector.domain.usecase.ObserveLeakDetectorStateUseCase(leakDetectorRepository)

    val observeConfirmedLeaksUseCase: org.telegram.messenger.feature.system.leakdetector.domain.usecase.ObserveConfirmedLeaksUseCase
        get() = org.telegram.messenger.feature.system.leakdetector.domain.usecase.ObserveConfirmedLeaksUseCase(leakDetectorRepository)

    private var cachedLeakDetectorViewModel: org.telegram.messenger.feature.system.leakdetector.presentation.LeakDetectorViewModel? = null

    val leakDetectorViewModel: org.telegram.messenger.feature.system.leakdetector.presentation.LeakDetectorViewModel
        get() {
            var vm = cachedLeakDetectorViewModel
            if (vm == null) {
                vm = createLeakDetectorViewModel()
                cachedLeakDetectorViewModel = vm
            }
            return vm
        }

    fun createLeakDetectorViewModel(): org.telegram.messenger.feature.system.leakdetector.presentation.LeakDetectorViewModel {
        return org.telegram.messenger.feature.system.leakdetector.presentation.LeakDetectorViewModel(
            startLeakDetectionUseCase = startLeakDetectionUseCase,
            stopLeakDetectionUseCase = stopLeakDetectionUseCase,
            trackInstanceUseCase = trackInstanceUseCase,
            triggerLeakCheckUseCase = triggerLeakCheckUseCase,
            confirmLeakUseCase = confirmLeakUseCase,
            getTrackedClassesStatsUseCase = getTrackedClassesStatsUseCase,
            getConfirmedLeaksUseCase = getConfirmedLeaksUseCase,
            resetLeakDetectorUseCase = resetLeakDetectorUseCase,
            observeLeakDetectorStateUseCase = observeLeakDetectorStateUseCase
        )
    }

    // --- 60 FPS Frame Rate & V-Sync Content Arbitration (feature.fpscontent) ---
    private var customFpsContentRepository: org.telegram.messenger.feature.system.fpscontent.domain.repository.FpsContentRepository? = null

    var fpsContentRepository: org.telegram.messenger.feature.system.fpscontent.domain.repository.FpsContentRepository
        get() = customFpsContentRepository ?: org.telegram.messenger.feature.system.fpscontent.data.repository.LegacyFpsContentRepository()
        set(value) {
            customFpsContentRepository = value
        }

    val registerFrameCallbackUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.RegisterFrameCallbackUseCase
        get() = org.telegram.messenger.feature.system.fpscontent.domain.usecase.RegisterFrameCallbackUseCase(fpsContentRepository)

    val registerRunnableCallbackUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.RegisterRunnableCallbackUseCase
        get() = org.telegram.messenger.feature.system.fpscontent.domain.usecase.RegisterRunnableCallbackUseCase(fpsContentRepository)

    val unregisterCallbackUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.UnregisterCallbackUseCase
        get() = org.telegram.messenger.feature.system.fpscontent.domain.usecase.UnregisterCallbackUseCase(fpsContentRepository)

    val requestViewInvalidationUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.RequestViewInvalidationUseCase
        get() = org.telegram.messenger.feature.system.fpscontent.domain.usecase.RequestViewInvalidationUseCase(fpsContentRepository)

    val requestDrawableInvalidationUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.RequestDrawableInvalidationUseCase
        get() = org.telegram.messenger.feature.system.fpscontent.domain.usecase.RequestDrawableInvalidationUseCase(fpsContentRepository)

    val dispatchVsyncTickUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.DispatchVsyncTickUseCase
        get() = org.telegram.messenger.feature.system.fpscontent.domain.usecase.DispatchVsyncTickUseCase(fpsContentRepository)

    val calculateFpsTimingUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.CalculateFpsTimingUseCase
        get() = org.telegram.messenger.feature.system.fpscontent.domain.usecase.CalculateFpsTimingUseCase()

    val getFpsContentStatsUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.GetFpsContentStatsUseCase
        get() = org.telegram.messenger.feature.system.fpscontent.domain.usecase.GetFpsContentStatsUseCase(fpsContentRepository)

    val getFpsSubscriptionsUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.GetFpsSubscriptionsUseCase
        get() = org.telegram.messenger.feature.system.fpscontent.domain.usecase.GetFpsSubscriptionsUseCase(fpsContentRepository)

    val observeFpsContentStatsUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.ObserveFpsContentStatsUseCase
        get() = org.telegram.messenger.feature.system.fpscontent.domain.usecase.ObserveFpsContentStatsUseCase(fpsContentRepository)

    val observeFpsTicksUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.ObserveFpsTicksUseCase
        get() = org.telegram.messenger.feature.system.fpscontent.domain.usecase.ObserveFpsTicksUseCase(fpsContentRepository)

    val resetFpsContentUseCase: org.telegram.messenger.feature.system.fpscontent.domain.usecase.ResetFpsContentUseCase
        get() = org.telegram.messenger.feature.system.fpscontent.domain.usecase.ResetFpsContentUseCase(fpsContentRepository)

    private var cachedFpsContentViewModel: org.telegram.messenger.feature.system.fpscontent.presentation.FpsContentViewModel? = null

    val fpsContentViewModel: org.telegram.messenger.feature.system.fpscontent.presentation.FpsContentViewModel
        get() {
            var vm = cachedFpsContentViewModel
            if (vm == null) {
                vm = createFpsContentViewModel()
                cachedFpsContentViewModel = vm
            }
            return vm
        }

    fun createFpsContentViewModel(): org.telegram.messenger.feature.system.fpscontent.presentation.FpsContentViewModel {
        return org.telegram.messenger.feature.system.fpscontent.presentation.FpsContentViewModel(
            registerFrameCallbackUseCase = registerFrameCallbackUseCase,
            registerRunnableCallbackUseCase = registerRunnableCallbackUseCase,
            unregisterCallbackUseCase = unregisterCallbackUseCase,
            requestViewInvalidationUseCase = requestViewInvalidationUseCase,
            requestDrawableInvalidationUseCase = requestDrawableInvalidationUseCase,
            dispatchVsyncTickUseCase = dispatchVsyncTickUseCase,
            getFpsContentStatsUseCase = getFpsContentStatsUseCase,
            getFpsSubscriptionsUseCase = getFpsSubscriptionsUseCase,
            observeFpsContentStatsUseCase = observeFpsContentStatsUseCase,
            observeFpsTicksUseCase = observeFpsTicksUseCase,
            resetFpsContentUseCase = resetFpsContentUseCase
        )
    }

    // --- Main Thread ANR Watchdog & UI Freeze Diagnostics (feature.anrwatchdog) ---
    private var customAnrWatchdogRepository: org.telegram.messenger.feature.system.anrwatchdog.domain.repository.AnrWatchdogRepository? = null

    var anrWatchdogRepository: org.telegram.messenger.feature.system.anrwatchdog.domain.repository.AnrWatchdogRepository
        get() = customAnrWatchdogRepository ?: org.telegram.messenger.feature.system.anrwatchdog.data.repository.LegacyAnrWatchdogRepository()
        set(value) {
            customAnrWatchdogRepository = value
        }

    val startAnrMonitoringUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.StartAnrMonitoringUseCase
        get() = org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.StartAnrMonitoringUseCase(anrWatchdogRepository)

    val stopAnrMonitoringUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.StopAnrMonitoringUseCase
        get() = org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.StopAnrMonitoringUseCase(anrWatchdogRepository)

    val setAppForegroundStatusUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.SetAppForegroundStatusUseCase
        get() = org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.SetAppForegroundStatusUseCase(anrWatchdogRepository)

    val sendMainThreadPingUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.SendMainThreadPingUseCase
        get() = org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.SendMainThreadPingUseCase(anrWatchdogRepository)

    val acknowledgePingUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.AcknowledgePingUseCase
        get() = org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.AcknowledgePingUseCase(anrWatchdogRepository)

    val checkMainThreadFreezeUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.CheckMainThreadFreezeUseCase
        get() = org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.CheckMainThreadFreezeUseCase(anrWatchdogRepository)

    val resolveIncidentUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.ResolveIncidentUseCase
        get() = org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.ResolveIncidentUseCase(anrWatchdogRepository)

    val getAnrWatchdogStateUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.GetAnrWatchdogStateUseCase
        get() = org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.GetAnrWatchdogStateUseCase(anrWatchdogRepository)

    val getAnrIncidentsUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.GetAnrIncidentsUseCase
        get() = org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.GetAnrIncidentsUseCase(anrWatchdogRepository)

    val clearAnrHistoryUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.ClearAnrHistoryUseCase
        get() = org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.ClearAnrHistoryUseCase(anrWatchdogRepository)

    val observeAnrWatchdogStateUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.ObserveAnrWatchdogStateUseCase
        get() = org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.ObserveAnrWatchdogStateUseCase(anrWatchdogRepository)

    val observeAnrIncidentsUseCase: org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.ObserveAnrIncidentsUseCase
        get() = org.telegram.messenger.feature.system.anrwatchdog.domain.usecase.ObserveAnrIncidentsUseCase(anrWatchdogRepository)

    private var cachedAnrWatchdogViewModel: org.telegram.messenger.feature.system.anrwatchdog.presentation.AnrWatchdogViewModel? = null

    val anrWatchdogViewModel: org.telegram.messenger.feature.system.anrwatchdog.presentation.AnrWatchdogViewModel
        get() {
            var vm = cachedAnrWatchdogViewModel
            if (vm == null) {
                vm = createAnrWatchdogViewModel()
                cachedAnrWatchdogViewModel = vm
            }
            return vm
        }

    fun createAnrWatchdogViewModel(): org.telegram.messenger.feature.system.anrwatchdog.presentation.AnrWatchdogViewModel {
        return org.telegram.messenger.feature.system.anrwatchdog.presentation.AnrWatchdogViewModel(
            startAnrMonitoringUseCase = startAnrMonitoringUseCase,
            stopAnrMonitoringUseCase = stopAnrMonitoringUseCase,
            setAppForegroundStatusUseCase = setAppForegroundStatusUseCase,
            sendMainThreadPingUseCase = sendMainThreadPingUseCase,
            acknowledgePingUseCase = acknowledgePingUseCase,
            checkMainThreadFreezeUseCase = checkMainThreadFreezeUseCase,
            resolveIncidentUseCase = resolveIncidentUseCase,
            getAnrWatchdogStateUseCase = getAnrWatchdogStateUseCase,
            getAnrIncidentsUseCase = getAnrIncidentsUseCase,
            clearAnrHistoryUseCase = clearAnrHistoryUseCase,
            observeAnrWatchdogStateUseCase = observeAnrWatchdogStateUseCase,
            observeAnrIncidentsUseCase = observeAnrIncidentsUseCase
        )
    }

    // --- EmuDetector ---
    private var customEmuDetectorRepository: org.telegram.messenger.feature.system.emudetector.domain.repository.EmuDetectorRepository? = null

    var emuDetectorRepository: org.telegram.messenger.feature.system.emudetector.domain.repository.EmuDetectorRepository
        get() = customEmuDetectorRepository ?: org.telegram.messenger.feature.system.emudetector.data.repository.LegacyEmuDetectorRepository()
        set(value) {
            customEmuDetectorRepository = value
        }

    val detectEnvironmentUseCase: org.telegram.messenger.feature.system.emudetector.domain.usecase.DetectEnvironmentUseCase
        get() = org.telegram.messenger.feature.system.emudetector.domain.usecase.DetectEnvironmentUseCase(emuDetectorRepository)

    val isEmulatorUseCase: org.telegram.messenger.feature.system.emudetector.domain.usecase.IsEmulatorUseCase
        get() = org.telegram.messenger.feature.system.emudetector.domain.usecase.IsEmulatorUseCase(emuDetectorRepository)

    val getCachedDiagnosticsUseCase: org.telegram.messenger.feature.system.emudetector.domain.usecase.GetCachedDiagnosticsUseCase
        get() = org.telegram.messenger.feature.system.emudetector.domain.usecase.GetCachedDiagnosticsUseCase(emuDetectorRepository)

    val observeDiagnosticsUseCase: org.telegram.messenger.feature.system.emudetector.domain.usecase.ObserveDiagnosticsUseCase
        get() = org.telegram.messenger.feature.system.emudetector.domain.usecase.ObserveDiagnosticsUseCase(emuDetectorRepository)

    val observeIsEmulatorUseCase: org.telegram.messenger.feature.system.emudetector.domain.usecase.ObserveIsEmulatorUseCase
        get() = org.telegram.messenger.feature.system.emudetector.domain.usecase.ObserveIsEmulatorUseCase(emuDetectorRepository)

    val getDetectorConfigUseCase: org.telegram.messenger.feature.system.emudetector.domain.usecase.GetDetectorConfigUseCase
        get() = org.telegram.messenger.feature.system.emudetector.domain.usecase.GetDetectorConfigUseCase(emuDetectorRepository)

    val updateDetectorConfigUseCase: org.telegram.messenger.feature.system.emudetector.domain.usecase.UpdateDetectorConfigUseCase
        get() = org.telegram.messenger.feature.system.emudetector.domain.usecase.UpdateDetectorConfigUseCase(emuDetectorRepository)

    val addCustomPackageNameUseCase: org.telegram.messenger.feature.system.emudetector.domain.usecase.AddCustomPackageNameUseCase
        get() = org.telegram.messenger.feature.system.emudetector.domain.usecase.AddCustomPackageNameUseCase(emuDetectorRepository)

    val clearDetectorCacheUseCase: org.telegram.messenger.feature.system.emudetector.domain.usecase.ClearDetectorCacheUseCase
        get() = org.telegram.messenger.feature.system.emudetector.domain.usecase.ClearDetectorCacheUseCase(emuDetectorRepository)

    private var cachedEmuDetectorViewModel: org.telegram.messenger.feature.system.emudetector.presentation.EmuDetectorViewModel? = null

    val emuDetectorViewModel: org.telegram.messenger.feature.system.emudetector.presentation.EmuDetectorViewModel
        get() {
            var vm = cachedEmuDetectorViewModel
            if (vm == null) {
                vm = createEmuDetectorViewModel()
                cachedEmuDetectorViewModel = vm
            }
            return vm
        }

    fun createEmuDetectorViewModel(): org.telegram.messenger.feature.system.emudetector.presentation.EmuDetectorViewModel {
        return org.telegram.messenger.feature.system.emudetector.presentation.EmuDetectorViewModel(
            detectEnvironmentUseCase = detectEnvironmentUseCase,
            observeDiagnosticsUseCase = observeDiagnosticsUseCase,
            getDetectorConfigUseCase = getDetectorConfigUseCase,
            updateDetectorConfigUseCase = updateDetectorConfigUseCase,
            addCustomPackageNameUseCase = addCustomPackageNameUseCase,
            clearDetectorCacheUseCase = clearDetectorCacheUseCase
        )
    }

    // --- FlagSecure ---
    private var customAnimationLockerRepository: org.telegram.messenger.feature.system.animationlocker.domain.repository.AnimationLockerRepository? = null

    var animationLockerRepository: org.telegram.messenger.feature.system.animationlocker.domain.repository.AnimationLockerRepository
        get() = customAnimationLockerRepository ?: org.telegram.messenger.feature.system.animationlocker.data.repository.LegacyAnimationLockerRepository(account)
        set(value) {
            customAnimationLockerRepository = value
        }

    val acquireAnimationLockUseCase: org.telegram.messenger.feature.system.animationlocker.domain.usecase.AcquireAnimationLockUseCase
        get() = org.telegram.messenger.feature.system.animationlocker.domain.usecase.AcquireAnimationLockUseCase(animationLockerRepository)

    val releaseAnimationLockUseCase: org.telegram.messenger.feature.system.animationlocker.domain.usecase.ReleaseAnimationLockUseCase
        get() = org.telegram.messenger.feature.system.animationlocker.domain.usecase.ReleaseAnimationLockUseCase(animationLockerRepository)

    val releaseAllAnimationLocksUseCase: org.telegram.messenger.feature.system.animationlocker.domain.usecase.ReleaseAllAnimationLocksUseCase
        get() = org.telegram.messenger.feature.system.animationlocker.domain.usecase.ReleaseAllAnimationLocksUseCase(animationLockerRepository)

    val setAnimationLockerDisabledUseCase: org.telegram.messenger.feature.system.animationlocker.domain.usecase.SetAnimationLockerDisabledUseCase
        get() = org.telegram.messenger.feature.system.animationlocker.domain.usecase.SetAnimationLockerDisabledUseCase(animationLockerRepository)

    val isAnimationLockedUseCase: org.telegram.messenger.feature.system.animationlocker.domain.usecase.IsAnimationLockedUseCase
        get() = org.telegram.messenger.feature.system.animationlocker.domain.usecase.IsAnimationLockedUseCase(animationLockerRepository)

    val isNotificationAllowedUseCase: org.telegram.messenger.feature.system.animationlocker.domain.usecase.IsNotificationAllowedUseCase
        get() = org.telegram.messenger.feature.system.animationlocker.domain.usecase.IsNotificationAllowedUseCase(animationLockerRepository)

    val getAnimationLockerStateUseCase: org.telegram.messenger.feature.system.animationlocker.domain.usecase.GetAnimationLockerStateUseCase
        get() = org.telegram.messenger.feature.system.animationlocker.domain.usecase.GetAnimationLockerStateUseCase(animationLockerRepository)

    val getAnimationLockerConfigUseCase: org.telegram.messenger.feature.system.animationlocker.domain.usecase.GetAnimationLockerConfigUseCase
        get() = org.telegram.messenger.feature.system.animationlocker.domain.usecase.GetAnimationLockerConfigUseCase(animationLockerRepository)

    val updateAnimationLockerConfigUseCase: org.telegram.messenger.feature.system.animationlocker.domain.usecase.UpdateAnimationLockerConfigUseCase
        get() = org.telegram.messenger.feature.system.animationlocker.domain.usecase.UpdateAnimationLockerConfigUseCase(animationLockerRepository)

    val observeAnimationLockerStateUseCase: org.telegram.messenger.feature.system.animationlocker.domain.usecase.ObserveAnimationLockerStateUseCase
        get() = org.telegram.messenger.feature.system.animationlocker.domain.usecase.ObserveAnimationLockerStateUseCase(animationLockerRepository)

    val observeIsAnimationLockedUseCase: org.telegram.messenger.feature.system.animationlocker.domain.usecase.ObserveIsAnimationLockedUseCase
        get() = org.telegram.messenger.feature.system.animationlocker.domain.usecase.ObserveIsAnimationLockedUseCase(animationLockerRepository)

    private var cachedAnimationLockerViewModel: org.telegram.messenger.feature.system.animationlocker.presentation.AnimationLockerViewModel? = null

    val animationLockerViewModel: org.telegram.messenger.feature.system.animationlocker.presentation.AnimationLockerViewModel
        get() {
            var vm = cachedAnimationLockerViewModel
            if (vm == null) {
                vm = createAnimationLockerViewModel()
                cachedAnimationLockerViewModel = vm
            }
            return vm
        }

    fun createAnimationLockerViewModel(): org.telegram.messenger.feature.system.animationlocker.presentation.AnimationLockerViewModel {
        return org.telegram.messenger.feature.system.animationlocker.presentation.AnimationLockerViewModel(
            acquireAnimationLockUseCase = acquireAnimationLockUseCase,
            releaseAnimationLockUseCase = releaseAnimationLockUseCase,
            releaseAllAnimationLocksUseCase = releaseAllAnimationLocksUseCase,
            setAnimationLockerDisabledUseCase = setAnimationLockerDisabledUseCase,
            getAnimationLockerStateUseCase = getAnimationLockerStateUseCase,
            getAnimationLockerConfigUseCase = getAnimationLockerConfigUseCase,
            updateAnimationLockerConfigUseCase = updateAnimationLockerConfigUseCase,
            observeAnimationLockerStateUseCase = observeAnimationLockerStateUseCase
        )
    }

}
