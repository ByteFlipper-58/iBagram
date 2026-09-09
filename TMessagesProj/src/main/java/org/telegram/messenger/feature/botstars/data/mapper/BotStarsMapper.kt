package org.telegram.messenger.feature.botstars.data.mapper

import org.telegram.messenger.DialogObject
import org.telegram.messenger.feature.botstars.domain.model.BotStarsRevenueStatsModel
import org.telegram.messenger.feature.botstars.domain.model.BotStarsRevenueStatusModel
import org.telegram.messenger.feature.botstars.domain.model.BotStarsTransactionModel
import org.telegram.messenger.feature.botstars.domain.model.ConnectedBotStarRefModel
import org.telegram.messenger.feature.botstars.domain.model.StarRefProgramModel
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_payments
import org.telegram.tgnet.tl.TL_stars

object BotStarsMapper {

    fun toRevenueStatus(status: TLRPC.TL_starsRevenueStatus?): BotStarsRevenueStatusModel? {
        if (status == null) return null
        val current = status.current_balance?.amount ?: 0L
        val available = status.available_balance?.amount ?: 0L
        val overall = status.overall_revenue?.amount ?: 0L
        val isTon = status.current_balance is TL_stars.TL_starsTonAmount
        return BotStarsRevenueStatusModel(
            currentBalance = current,
            availableBalance = available,
            overallRevenue = overall,
            withdrawalEnabled = status.withdrawal_enabled,
            nextWithdrawalAt = status.next_withdrawal_at.toLong(),
            isTon = isTon
        )
    }

    fun toRevenueStats(dialogId: Long, tl: TLRPC.TL_payments_starsRevenueStats?): BotStarsRevenueStatsModel? {
        if (tl == null) return null
        return BotStarsRevenueStatsModel(
            dialogId = dialogId,
            status = toRevenueStatus(tl.status),
            usdRate = tl.usd_rate
        )
    }

    fun toTransaction(tl: TL_stars.StarsTransaction?): BotStarsTransactionModel? {
        if (tl == null) return null
        val peerId = if (tl.peer != null) DialogObject.getPeerDialogId(tl.peer.peer) else 0L
        val stars = tl.amount?.amount ?: 0L
        val isOutgoing = (tl.flags and 8) != 0 || stars < 0
        return BotStarsTransactionModel(
            id = tl.id ?: "",
            stars = stars,
            date = tl.date.toLong(),
            peerId = peerId,
            title = tl.title,
            description = tl.description,
            isOutgoing = isOutgoing,
            isRefund = (tl.flags and 4) != 0,
            isPending = (tl.flags and 16) != 0,
            floodskip = (tl.flags and 32) != 0,
            starRefCommissionPermille = tl.starref_commission_permille
        )
    }

    fun toTransactionList(list: List<TL_stars.StarsTransaction>?): List<BotStarsTransactionModel> {
        if (list == null) return emptyList()
        return list.mapNotNull { toTransaction(it) }
    }

    fun toConnectedBot(tl: TL_payments.connectedBotStarRef?): ConnectedBotStarRefModel? {
        if (tl == null) return null
        val rev = tl.revoked || (tl.flags and 2) != 0
        return ConnectedBotStarRefModel(
            botId = tl.bot_id,
            date = tl.date,
            url = tl.url,
            commissionPermille = tl.commission_permille,
            durationMonths = tl.duration_months,
            revoked = rev,
            participants = tl.participants,
            revenue = tl.revenue
        )
    }

    fun toConnectedBotList(list: List<TL_payments.connectedBotStarRef>?): List<ConnectedBotStarRefModel> {
        if (list == null) return emptyList()
        return list.mapNotNull { toConnectedBot(it) }
    }

    fun toSuggestedBot(tl: TL_payments.starRefProgram?): StarRefProgramModel? {
        if (tl == null) return null
        return StarRefProgramModel(
            botId = tl.bot_id,
            commissionPermille = tl.commission_permille,
            durationMonths = tl.duration_months,
            endDate = tl.end_date,
            dailyRevenuePerUser = tl.daily_revenue_per_user?.amount ?: 0L
        )
    }

    fun toSuggestedBotList(list: List<TL_payments.starRefProgram>?): List<StarRefProgramModel> {
        if (list == null) return emptyList()
        return list.mapNotNull { toSuggestedBot(it) }
    }
}
