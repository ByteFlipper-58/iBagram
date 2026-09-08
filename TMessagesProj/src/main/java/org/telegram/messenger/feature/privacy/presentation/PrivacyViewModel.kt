package org.telegram.messenger.feature.privacy.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.telegram.messenger.feature.privacy.domain.model.PrivacyRuleType
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

class PrivacyViewModel(
    private val privacyRepository: PrivacyRepository,
    private val observePrivacyRulesUseCase: ObservePrivacyRulesUseCase,
    private val getPrivacyRulesUseCase: GetPrivacyRulesUseCase,
    private val setPrivacyRuleUseCase: SetPrivacyRuleUseCase,
    private val loadPrivacyRulesUseCase: LoadPrivacyRulesUseCase,
    private val observeBlockedPeersUseCase: ObserveBlockedPeersUseCase,
    private val getBlockedPeersUseCase: GetBlockedPeersUseCase,
    private val blockPrivacyPeerUseCase: BlockPrivacyPeerUseCase,
    private val unblockPrivacyPeerUseCase: UnblockPrivacyPeerUseCase,
    private val getPasscodeSettingsUseCase: GetPasscodeSettingsUseCase,
    private val setPasscodeUseCase: SetPasscodeUseCase,
    private val checkPasscodeUseCase: CheckPasscodeUseCase,
    private val clearPasscodeUseCase: ClearPasscodeUseCase,
    private val observeTwoStepVerificationUseCase: ObserveTwoStepVerificationUseCase,
    private val loadTwoStepVerificationUseCase: LoadTwoStepVerificationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivacyUiState())
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        refreshPasscode()
        loadInitialData()
        observeData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            loadPrivacyRulesUseCase()
            val blocked = getBlockedPeersUseCase()
            val passcode = getPasscodeSettingsUseCase()
            val twoStep = loadTwoStepVerificationUseCase().getOrNull()

            _uiState.update { state ->
                state.copy(
                    blockedPeers = blocked,
                    blockedCount = blocked.size,
                    passcodeSettings = passcode,
                    twoStepVerification = twoStep ?: state.twoStepVerification,
                    isLoading = false
                )
            }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            observeBlockedPeersUseCase().collect { peers ->
                _uiState.update { it.copy(blockedPeers = peers, blockedCount = peers.size) }
            }
        }
        viewModelScope.launch {
            observeTwoStepVerificationUseCase().collect { twoStep ->
                _uiState.update { it.copy(twoStepVerification = twoStep) }
            }
        }
        for (type in PrivacyRuleType.entries) {
            viewModelScope.launch {
                observePrivacyRulesUseCase(type).collect { rule ->
                    if (rule != null) {
                        _uiState.update { state ->
                            state.copy(privacyRules = state.privacyRules + (type to rule))
                        }
                    }
                }
            }
        }
    }

    fun refreshPasscode() {
        val passcode = getPasscodeSettingsUseCase()
        _uiState.update { it.copy(passcodeSettings = passcode) }
    }

    fun onEvent(event: PrivacyEvent) {
        viewModelScope.launch {
            when (event) {
                is PrivacyEvent.SetRule -> {
                    val result = setPrivacyRuleUseCase(event.type, event.rule)
                    result.onSuccess {
                        _uiState.update { state ->
                            state.copy(privacyRules = state.privacyRules + (event.type to event.rule))
                        }
                    }.onFailure { error ->
                        _uiState.update { it.copy(errorMessage = error.message) }
                    }
                }
                is PrivacyEvent.ReloadRules -> {
                    _uiState.update { it.copy(isLoading = true) }
                    loadPrivacyRulesUseCase()
                    _uiState.update { it.copy(isLoading = false) }
                }
                is PrivacyEvent.BlockPeer -> {
                    blockPrivacyPeerUseCase(event.peerId).onFailure { error ->
                        _uiState.update { it.copy(errorMessage = error.message) }
                    }
                }
                is PrivacyEvent.UnblockPeer -> {
                    unblockPrivacyPeerUseCase(event.peerId).onFailure { error ->
                        _uiState.update { it.copy(errorMessage = error.message) }
                    }
                }
                is PrivacyEvent.SetPasscode -> {
                    setPasscodeUseCase(event.passcode, event.type).onSuccess {
                        refreshPasscode()
                        _events.emit("Passcode set successfully")
                    }.onFailure { error ->
                        _uiState.update { it.copy(errorMessage = error.message) }
                    }
                }
                is PrivacyEvent.ClearPasscode -> {
                    clearPasscodeUseCase().onSuccess {
                        refreshPasscode()
                        _events.emit("Passcode cleared")
                    }.onFailure { error ->
                        _uiState.update { it.copy(errorMessage = error.message) }
                    }
                }
                is PrivacyEvent.ToggleFingerprint -> {
                    privacyRepository.setUseFingerprint(event.enabled)
                    refreshPasscode()
                }
                is PrivacyEvent.ToggleScreenCapture -> {
                    privacyRepository.setAllowScreenCapture(event.allow)
                    refreshPasscode()
                }
                is PrivacyEvent.SetAutoLock -> {
                    privacyRepository.setAutoLockIn(event.seconds)
                    refreshPasscode()
                }
                is PrivacyEvent.ReloadTwoStepVerification -> {
                    loadTwoStepVerificationUseCase().onSuccess { twoStep ->
                        _uiState.update { it.copy(twoStepVerification = twoStep) }
                    }
                }
                is PrivacyEvent.DismissError -> {
                    _uiState.update { it.copy(errorMessage = null) }
                }
            }
        }
    }
}
