package org.telegram.messenger.feature.security.botguard.presentation

import org.telegram.messenger.feature.security.botguard.domain.model.BotGuardBulletinInfo
import org.telegram.messenger.feature.security.botguard.domain.model.BotGuardDecisionResult
import org.telegram.messenger.feature.security.botguard.domain.model.BotGuardSession

data class BotGuardUiState(
    val activeSessions: List<BotGuardSession> = emptyList(),
    val lastDecision: BotGuardDecisionResult? = null,
    val promptConfirmationSession: BotGuardSession? = null,
    val webViewToLaunch: BotGuardSession? = null,
    val bulletinToShow: BotGuardBulletinInfo? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
