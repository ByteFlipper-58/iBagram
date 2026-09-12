package org.telegram.messenger.feature.security.di

import java.util.concurrent.ConcurrentHashMap
import org.telegram.messenger.feature.security.authtokens.data.repository.LegacyAuthTokensRepository
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
import org.telegram.messenger.feature.security.biometrics.data.repository.LegacyBiometricsRepository
import org.telegram.messenger.feature.security.biometrics.domain.repository.BiometricsRepository
import org.telegram.messenger.feature.security.biometrics.domain.usecase.CheckBiometricKeyReadyUseCase
import org.telegram.messenger.feature.security.biometrics.domain.usecase.DeleteInvalidBiometricKeyUseCase
import org.telegram.messenger.feature.security.biometrics.domain.usecase.GetBiometricKeyStateUseCase
import org.telegram.messenger.feature.security.biometrics.domain.usecase.HasDeviceBiometricsChangedUseCase
import org.telegram.messenger.feature.security.biometrics.domain.usecase.IsBiometricKeyReadyUseCase
import org.telegram.messenger.feature.security.biometrics.domain.usecase.ObserveBiometricKeyStateUseCase
import org.telegram.messenger.feature.security.biometrics.presentation.BiometricsViewModel
import org.telegram.messenger.feature.security.botguard.data.repository.LegacyBotGuardRepository
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
import org.telegram.messenger.feature.security.captcha.data.repository.LegacyCaptchaRepository
import org.telegram.messenger.feature.security.captcha.domain.repository.CaptchaRepository
import org.telegram.messenger.feature.security.captcha.domain.usecase.CancelCaptchaUseCase
import org.telegram.messenger.feature.security.captcha.domain.usecase.GetActiveCaptchaRequestsUseCase
import org.telegram.messenger.feature.security.captcha.domain.usecase.ObserveActiveCaptchaRequestsUseCase
import org.telegram.messenger.feature.security.captcha.domain.usecase.SubmitCaptchaResultUseCase
import org.telegram.messenger.feature.security.captcha.domain.usecase.VerifyCaptchaUseCase
import org.telegram.messenger.feature.security.captcha.presentation.CaptchaViewModel
import org.telegram.messenger.feature.security.passkeys.data.repository.LegacyPasskeysRepository
import org.telegram.messenger.feature.security.passkeys.domain.repository.PasskeysRepository
import org.telegram.messenger.feature.security.passkeys.domain.usecase.CheckCanAddPasskeyUseCase
import org.telegram.messenger.feature.security.passkeys.domain.usecase.DeletePasskeyUseCase
import org.telegram.messenger.feature.security.passkeys.domain.usecase.GetPasskeysUseCase
import org.telegram.messenger.feature.security.passkeys.domain.usecase.IsPasskeysSupportedUseCase
import org.telegram.messenger.feature.security.passkeys.domain.usecase.ObservePasskeysUseCase
import org.telegram.messenger.feature.security.passkeys.presentation.PasskeysViewModel
import org.telegram.messenger.feature.security.privacy.data.repository.LegacyPrivacyRepository
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
import org.telegram.messenger.feature.security.secretchat.data.datasource.SecretChatLocalDataSource
import org.telegram.messenger.feature.security.secretchat.data.datasource.SecretChatRemoteDataSource
import org.telegram.messenger.feature.security.secretchat.data.repository.LegacySecretChatRepository
import org.telegram.messenger.feature.security.secretchat.data.repository.SecretChatRepositoryImpl
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
import org.telegram.messenger.feature.security.sessions.data.repository.LegacySessionsRepository
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
import org.telegram.messenger.feature.security.unconfirmedauth.data.repository.LegacyUnconfirmedAuthRepository
import org.telegram.messenger.feature.security.unconfirmedauth.domain.repository.UnconfirmedAuthRepository
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.ClearUnconfirmedAuthsUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.ConfirmAllAuthsUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.ConfirmAuthUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.DenyAllAuthsUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.DenyAuthUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.GetUnconfirmedAuthsUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.domain.usecase.ObserveUnconfirmedAuthsUseCase
import org.telegram.messenger.feature.security.unconfirmedauth.presentation.UnconfirmedAuthViewModel

class SecurityContainer(val account: Int) {

    val secretChatRemoteDataSource: SecretChatRemoteDataSource by lazy {
        SecretChatRemoteDataSource(account)
    }

    val secretChatLocalDataSource: SecretChatLocalDataSource by lazy {
        SecretChatLocalDataSource(account)
    }

    fun createSecretChatRepository(): SecretChatRepository {
        return SecretChatRepositoryImpl(
            currentAccount = account,
            localDataSource = secretChatLocalDataSource,
            remoteDataSource = secretChatRemoteDataSource
        )
    }

    private var customSecretChatRepository: SecretChatRepository? = null

    var secretChatRepository: SecretChatRepository
        get() = customSecretChatRepository ?: createSecretChatRepository()
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

    val authTokensRepository: AuthTokensRepository by lazy {
        LegacyAuthTokensRepository(account)
    }

    val pruneTokensListUseCase: PruneTokensListUseCase
        get() = PruneTokensListUseCase()

    val validateAuthTokenFormatUseCase: ValidateAuthTokenFormatUseCase
        get() = ValidateAuthTokenFormatUseCase()

    val observeAuthTokensStateUseCase: ObserveAuthTokensStateUseCase
        get() = ObserveAuthTokensStateUseCase(authTokensRepository)

    val getAuthTokensStateUseCase: GetAuthTokensStateUseCase
        get() = GetAuthTokensStateUseCase(authTokensRepository)

    val getSavedLoginTokensUseCase: GetSavedLoginTokensUseCase
        get() = GetSavedLoginTokensUseCase(authTokensRepository)

    val saveLoginTokenUseCase: SaveLoginTokenUseCase
        get() = SaveLoginTokenUseCase(authTokensRepository, validateAuthTokenFormatUseCase)

    val getSavedLogoutTokensUseCase: GetSavedLogoutTokensUseCase
        get() = GetSavedLogoutTokensUseCase(authTokensRepository)

    val saveLogoutTokensUseCase: SaveLogoutTokensUseCase
        get() = SaveLogoutTokensUseCase(authTokensRepository, pruneTokensListUseCase)

    val addLogoutTokenUseCase: AddLogoutTokenUseCase
        get() = AddLogoutTokenUseCase(authTokensRepository, validateAuthTokenFormatUseCase)

    val removeTokenUseCase: RemoveTokenUseCase
        get() = RemoveTokenUseCase(authTokensRepository)

    val clearAllTokensUseCase: ClearAllTokensUseCase
        get() = ClearAllTokensUseCase(authTokensRepository)

    val refreshAuthTokensUseCase: RefreshAuthTokensUseCase
        get() = RefreshAuthTokensUseCase(authTokensRepository)

    private var cachedAuthTokensViewModel: AuthTokensViewModel? = null

    val authTokensViewModel: AuthTokensViewModel
        get() {
            var vm = cachedAuthTokensViewModel
            if (vm == null) {
                vm = createAuthTokensViewModel()
                cachedAuthTokensViewModel = vm
            }
            return vm
        }

    fun createAuthTokensViewModel(): AuthTokensViewModel {
        return AuthTokensViewModel(
            observeAuthTokensState = observeAuthTokensStateUseCase,
            saveLoginToken = saveLoginTokenUseCase,
            addLogoutToken = addLogoutTokenUseCase,
            removeToken = removeTokenUseCase,
            clearAllTokens = clearAllTokensUseCase,
            refreshAuthTokens = refreshAuthTokensUseCase,
            repository = authTokensRepository
        )
    }

    private var customBotGuardRepository: BotGuardRepository? = null

    var botGuardRepository: BotGuardRepository
        get() = customBotGuardRepository ?: LegacyBotGuardRepository(account)
        set(value) {
            customBotGuardRepository = value
        }

    val isGuardBotConfirmationNeededUseCase: IsGuardBotConfirmationNeededUseCase
        get() = IsGuardBotConfirmationNeededUseCase(botGuardRepository)

    val determineGuardBotLaunchFlowUseCase: DetermineGuardBotLaunchFlowUseCase
        get() = DetermineGuardBotLaunchFlowUseCase(isGuardBotConfirmationNeededUseCase)

    val registerGuardBotSessionUseCase: RegisterGuardBotSessionUseCase
        get() = RegisterGuardBotSessionUseCase(botGuardRepository)

    val getGuardBotSessionUseCase: GetGuardBotSessionUseCase
        get() = GetGuardBotSessionUseCase(botGuardRepository)

    val getAllActiveGuardBotSessionsUseCase: GetAllActiveGuardBotSessionsUseCase
        get() = GetAllActiveGuardBotSessionsUseCase(botGuardRepository)

    val closeGuardBotSessionUseCase: CloseGuardBotSessionUseCase
        get() = CloseGuardBotSessionUseCase(botGuardRepository)

    val setGuardBotConfirmationShownUseCase: SetGuardBotConfirmationShownUseCase
        get() = SetGuardBotConfirmationShownUseCase(botGuardRepository)

    val clearAllGuardBotSessionsUseCase: ClearAllGuardBotSessionsUseCase
        get() = ClearAllGuardBotSessionsUseCase(botGuardRepository)

    val observeGuardBotDecisionsUseCase: ObserveGuardBotDecisionsUseCase
        get() = ObserveGuardBotDecisionsUseCase(botGuardRepository)

    val observeGuardBotStateUseCase: ObserveGuardBotStateUseCase
        get() = ObserveGuardBotStateUseCase(botGuardRepository)

    val mapJoinChatBotResultUseCase: MapJoinChatBotResultUseCase
        get() = MapJoinChatBotResultUseCase()

    val formatGuardBotBulletinUseCase: FormatGuardBotBulletinUseCase
        get() = FormatGuardBotBulletinUseCase()

    private var cachedBotGuardViewModel: BotGuardViewModel? = null

    val botGuardViewModel: BotGuardViewModel
        get() {
            var vm = cachedBotGuardViewModel
            if (vm == null) {
                vm = createBotGuardViewModel()
                cachedBotGuardViewModel = vm
            }
            return vm
        }

    fun createBotGuardViewModel(): BotGuardViewModel {
        return BotGuardViewModel(
            determineLaunchFlowUseCase = determineGuardBotLaunchFlowUseCase,
            registerSessionUseCase = registerGuardBotSessionUseCase,
            closeSessionUseCase = closeGuardBotSessionUseCase,
            setConfirmationShownUseCase = setGuardBotConfirmationShownUseCase,
            observeDecisionsUseCase = observeGuardBotDecisionsUseCase,
            observeStateUseCase = observeGuardBotStateUseCase,
            formatBulletinUseCase = formatGuardBotBulletinUseCase
        )
    }

    private var customFlagSecureRepository: org.telegram.messenger.feature.security.flagsecure.domain.repository.FlagSecureRepository? = null

    var flagSecureRepository: org.telegram.messenger.feature.security.flagsecure.domain.repository.FlagSecureRepository
        get() = customFlagSecureRepository ?: org.telegram.messenger.feature.security.flagsecure.data.repository.LegacyFlagSecureRepository()
        set(value) {
            customFlagSecureRepository = value
        }

    val attachSecurityReasonUseCase: org.telegram.messenger.feature.security.flagsecure.domain.usecase.AttachSecurityReasonUseCase
        get() = org.telegram.messenger.feature.security.flagsecure.domain.usecase.AttachSecurityReasonUseCase(flagSecureRepository)

    val detachSecurityReasonUseCase: org.telegram.messenger.feature.security.flagsecure.domain.usecase.DetachSecurityReasonUseCase
        get() = org.telegram.messenger.feature.security.flagsecure.domain.usecase.DetachSecurityReasonUseCase(flagSecureRepository)

    val invalidateWindowSecurityUseCase: org.telegram.messenger.feature.security.flagsecure.domain.usecase.InvalidateWindowSecurityUseCase
        get() = org.telegram.messenger.feature.security.flagsecure.domain.usecase.InvalidateWindowSecurityUseCase(flagSecureRepository)

    val isWindowSecuredUseCase: org.telegram.messenger.feature.security.flagsecure.domain.usecase.IsWindowSecuredUseCase
        get() = org.telegram.messenger.feature.security.flagsecure.domain.usecase.IsWindowSecuredUseCase(flagSecureRepository)

    val getWindowSecurityStateUseCase: org.telegram.messenger.feature.security.flagsecure.domain.usecase.GetWindowSecurityStateUseCase
        get() = org.telegram.messenger.feature.security.flagsecure.domain.usecase.GetWindowSecurityStateUseCase(flagSecureRepository)

    val getAllWindowStatesUseCase: org.telegram.messenger.feature.security.flagsecure.domain.usecase.GetAllWindowStatesUseCase
        get() = org.telegram.messenger.feature.security.flagsecure.domain.usecase.GetAllWindowStatesUseCase(flagSecureRepository)

    val resetWindowSecurityUseCase: org.telegram.messenger.feature.security.flagsecure.domain.usecase.ResetWindowSecurityUseCase
        get() = org.telegram.messenger.feature.security.flagsecure.domain.usecase.ResetWindowSecurityUseCase(flagSecureRepository)

    val observeWindowStateUseCase: org.telegram.messenger.feature.security.flagsecure.domain.usecase.ObserveWindowStateUseCase
        get() = org.telegram.messenger.feature.security.flagsecure.domain.usecase.ObserveWindowStateUseCase(flagSecureRepository)

    val observeAllWindowStatesUseCase: org.telegram.messenger.feature.security.flagsecure.domain.usecase.ObserveAllWindowStatesUseCase
        get() = org.telegram.messenger.feature.security.flagsecure.domain.usecase.ObserveAllWindowStatesUseCase(flagSecureRepository)

    val evaluateSecurityRuleUseCase: org.telegram.messenger.feature.security.flagsecure.domain.usecase.EvaluateSecurityRuleUseCase
        get() = org.telegram.messenger.feature.security.flagsecure.domain.usecase.EvaluateSecurityRuleUseCase()

    private val cachedFlagSecureViewModels = ConcurrentHashMap<String, org.telegram.messenger.feature.security.flagsecure.presentation.FlagSecureViewModel>()

    fun getFlagSecureViewModel(windowId: String = "main"): org.telegram.messenger.feature.security.flagsecure.presentation.FlagSecureViewModel {
        return cachedFlagSecureViewModels.computeIfAbsent(windowId) { createFlagSecureViewModel(it) }
    }

    fun createFlagSecureViewModel(windowId: String = "main"): org.telegram.messenger.feature.security.flagsecure.presentation.FlagSecureViewModel {
        return org.telegram.messenger.feature.security.flagsecure.presentation.FlagSecureViewModel(
            initialWindowId = windowId,
            attachSecurityReasonUseCase = attachSecurityReasonUseCase,
            detachSecurityReasonUseCase = detachSecurityReasonUseCase,
            invalidateWindowSecurityUseCase = invalidateWindowSecurityUseCase,
            getWindowSecurityStateUseCase = getWindowSecurityStateUseCase,
            resetWindowSecurityUseCase = resetWindowSecurityUseCase,
            observeWindowStateUseCase = observeWindowStateUseCase,
            observeAllWindowStatesUseCase = observeAllWindowStatesUseCase
        )
    }

    // --- AnimationLocker ---
}
