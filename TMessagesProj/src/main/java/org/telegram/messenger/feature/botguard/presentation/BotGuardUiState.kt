package org.telegram.messenger.feature.botguard.presentation

import org.telegram.messenger.feature.botguard.domain.model.BotGuardBulletinInfo
import org.telegram.messenger.feature.botguard.domain.model.BotGuardDecisionResult
import org.telegram.messenger.feature.botguard.domain.model.BotGuardSession

data class BotGuardUiState(
    val activeSessions: List<BotGuardSession> = emptyList(),
    val lastDecision: BotGuardDecisionResult? = null,
    val promptConfirmationSession: BotGuardSession? = null,
    val webViewToLaunch: BotGuardSession? = null,
    val bulletinToShow: BotGuardBulletinInfo? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
