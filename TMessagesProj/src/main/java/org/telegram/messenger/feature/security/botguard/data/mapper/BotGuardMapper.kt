package org.telegram.messenger.feature.security.botguard.data.mapper

import org.telegram.messenger.BotGuardHelper
import org.telegram.messenger.feature.security.botguard.domain.model.BotGuardDecisionResult
import org.telegram.messenger.feature.security.botguard.domain.model.BotGuardDecisionStatus
import org.telegram.tgnet.TLRPC

object BotGuardMapper {

    fun mapJoinChatBotResult(result: TLRPC.JoinChatBotResult?): BotGuardDecisionStatus {
        if (result == null) {
            return BotGuardDecisionStatus.Unknown(0)
        }
        return when (result) {
            is TLRPC.TL_joinChatBotResultApproved -> BotGuardDecisionStatus.Approved
            is TLRPC.TL_joinChatBotResultDeclined -> BotGuardDecisionStatus.Declined
            is TLRPC.TL_joinChatBotResultQueued -> BotGuardDecisionStatus.Queued
            is TLRPC.TL_joinChatBotResultWebView -> BotGuardDecisionStatus.WebView(result.url ?: "")
            else -> BotGuardDecisionStatus.Unknown(0)
        }
    }

    fun toNotification(
        decision: BotGuardDecisionResult,
        rawResult: TLRPC.JoinChatBotResult? = null
    ): BotGuardHelper.GuardBotDecisionResultNotification {
        val resolvedResult: TLRPC.JoinChatBotResult = rawResult ?: when (decision.status) {
            is BotGuardDecisionStatus.Approved -> TLRPC.TL_joinChatBotResultApproved()
            is BotGuardDecisionStatus.Declined -> TLRPC.TL_joinChatBotResultDeclined()
            is BotGuardDecisionStatus.Queued -> TLRPC.TL_joinChatBotResultQueued()
            is BotGuardDecisionStatus.WebView -> {
                val res = TLRPC.TL_joinChatBotResultWebView()
                res.url = decision.status.url
                res
            }
            else -> TLRPC.TL_joinChatBotResultDeclined()
        }

        return BotGuardHelper.GuardBotDecisionResultNotification(
            decision.dialogId,
            decision.guardBotId,
            decision.queryId,
            resolvedResult
        )
    }

    fun fromNotification(
        notification: BotGuardHelper.GuardBotDecisionResultNotification
    ): BotGuardDecisionResult {
        return BotGuardDecisionResult(
            dialogId = notification.dialogId,
            guardBotId = notification.guardBotId,
            queryId = notification.queryId,
            status = mapJoinChatBotResult(notification.result)
        )
    }
}
