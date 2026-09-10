package org.telegram.messenger.feature.security.botguard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.telegram.messenger.feature.security.botguard.domain.model.BotGuardLaunchDecision
import org.telegram.messenger.feature.security.botguard.domain.model.BotGuardSession
import org.telegram.messenger.feature.security.botguard.domain.usecase.CloseGuardBotSessionUseCase
import org.telegram.messenger.feature.security.botguard.domain.usecase.DetermineGuardBotLaunchFlowUseCase
import org.telegram.messenger.feature.security.botguard.domain.usecase.FormatGuardBotBulletinUseCase
import org.telegram.messenger.feature.security.botguard.domain.usecase.ObserveGuardBotDecisionsUseCase
import org.telegram.messenger.feature.security.botguard.domain.usecase.ObserveGuardBotStateUseCase
import org.telegram.messenger.feature.security.botguard.domain.usecase.RegisterGuardBotSessionUseCase
import org.telegram.messenger.feature.security.botguard.domain.usecase.SetGuardBotConfirmationShownUseCase

class BotGuardViewModel(
    private val determineLaunchFlowUseCase: DetermineGuardBotLaunchFlowUseCase,
    private val registerSessionUseCase: RegisterGuardBotSessionUseCase,
    private val closeSessionUseCase: CloseGuardBotSessionUseCase,
    private val setConfirmationShownUseCase: SetGuardBotConfirmationShownUseCase,
    private val observeDecisionsUseCase: ObserveGuardBotDecisionsUseCase,
    private val observeStateUseCase: ObserveGuardBotStateUseCase,
    private val formatBulletinUseCase: FormatGuardBotBulletinUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BotGuardUiState())
    val uiState: StateFlow<BotGuardUiState> = _uiState.asStateFlow()

    init {
        observeStateUseCase()
            .onEach { state ->
                _uiState.update { current ->
                    current.copy(
                        activeSessions = state.activeSessions.values.toList(),
                        lastDecision = state.lastDecision
                    )
                }
            }
            .launchIn(viewModelScope)

        observeDecisionsUseCase()
            .onEach { decision ->
                _uiState.update { current ->
                    current.copy(
                        lastDecision = decision,
                        activeSessions = current.activeSessions.filter { it.queryId != decision.queryId }
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: BotGuardEvent) {
        when (event) {
            is BotGuardEvent.RequestOpenGuardBot -> {
                val launchDecision = determineLaunchFlowUseCase(
                    dialogId = event.dialogId,
                    guardBotId = event.guardBotId,
                    queryId = event.queryId,
                    alreadyConfirmed = false
                )

                when (launchDecision) {
                    is BotGuardLaunchDecision.PromptConfirmation -> {
                        val session = BotGuardSession(
                            dialogId = event.dialogId,
                            guardBotId = event.guardBotId,
                            queryId = event.queryId,
                            isConfirmed = false
                        )
                        _uiState.update { current ->
                            current.copy(
                                promptConfirmationSession = session,
                                webViewToLaunch = null
                            )
                        }
                    }
                    is BotGuardLaunchDecision.LaunchDirectly -> {
                        val session = registerSessionUseCase(
                            dialogId = event.dialogId,
                            guardBotId = event.guardBotId,
                            queryId = event.queryId,
                            isConfirmed = true
                        )
                        _uiState.update { current ->
                            current.copy(
                                promptConfirmationSession = null,
                                webViewToLaunch = session
                            )
                        }
                    }
                }
            }

            is BotGuardEvent.ConfirmLaunch -> {
                setConfirmationShownUseCase(event.guardBotId, true)
                val session = registerSessionUseCase(
                    dialogId = event.dialogId,
                    guardBotId = event.guardBotId,
                    queryId = event.queryId,
                    isConfirmed = true
                )
                _uiState.update { current ->
                    current.copy(
                        promptConfirmationSession = null,
                        webViewToLaunch = session
                    )
                }
            }

            is BotGuardEvent.DismissConfirmation -> {
                _uiState.update { current ->
                    current.copy(promptConfirmationSession = null)
                }
            }

            is BotGuardEvent.DecisionReceived -> {
                val decision = closeSessionUseCase(
                    dialogId = event.dialogId,
                    queryId = event.queryId,
                    status = event.status
                )
                val bulletin = formatBulletinUseCase(decision, event.chatName)
                _uiState.update { current ->
                    current.copy(
                        lastDecision = decision,
                        bulletinToShow = bulletin,
                        activeSessions = current.activeSessions.filter { it.queryId != event.queryId }
                    )
                }
            }

            is BotGuardEvent.CloseSession -> {
                closeSessionUseCase(
                    dialogId = 0L,
                    queryId = event.queryId,
                    status = org.telegram.messenger.feature.security.botguard.domain.model.BotGuardDecisionStatus.Dismissed
                )
            }

            is BotGuardEvent.ClearPendingActions -> {
                _uiState.update { current ->
                    current.copy(
                        promptConfirmationSession = null,
                        webViewToLaunch = null,
                        bulletinToShow = null,
                        errorMessage = null
                    )
                }
            }
        }
    }
}
