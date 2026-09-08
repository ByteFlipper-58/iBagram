package org.telegram.messenger.feature.payments.data.mapper

import org.telegram.messenger.DialogObject
import org.telegram.messenger.feature.payments.domain.model.StarSubscriptionModel
import org.telegram.messenger.feature.payments.domain.model.StarTopupOptionModel
import org.telegram.messenger.feature.payments.domain.model.StarTransactionModel
import org.telegram.messenger.feature.payments.domain.model.StarsBalanceModel
import org.telegram.tgnet.tl.TL_stars

object PaymentMapper {

    fun mapBalance(balance: Long, isAvailable: Boolean): StarsBalanceModel {
        return StarsBalanceModel(
            balance = balance,
            currency = "XTR",
            isAvailable = isAvailable
        )
    }

    fun mapTransaction(tx: TL_stars.StarsTransaction): StarTransactionModel {
        val peerId = if (tx.peer is TL_stars.TL_starsTransactionPeer) {
            DialogObject.getPeerDialogId((tx.peer as TL_stars.TL_starsTransactionPeer).peer)
        } else {
            0L
        }

        val amount = tx.amount?.amount ?: 0L

        return StarTransactionModel(
            id = tx.id ?: "",
            amount = amount,
            date = tx.date.toLong(),
            title = tx.title,
            description = tx.description,
            isRefund = tx.refund,
            isPending = tx.pending,
            isFailed = tx.failed,
            peerId = peerId
        )
    }

    fun mapSubscription(sub: TL_stars.StarsSubscription): StarSubscriptionModel {
        val peerId = if (sub.peer != null) {
            DialogObject.getPeerDialogId(sub.peer)
        } else {
            0L
        }

        val pricingAmount = sub.pricing?.amount ?: 0L

        return StarSubscriptionModel(
            id = sub.id ?: "",
            peerId = peerId,
            untilDate = sub.until_date.toLong(),
            pricingAmount = pricingAmount,
            isCanceled = sub.canceled,
            inviteHash = sub.chat_invite_hash
        )
    }

    fun mapTopupOption(opt: TL_stars.TL_starsTopupOption): StarTopupOptionModel {
        return StarTopupOptionModel(
            stars = opt.stars,
            amount = opt.amount,
            currency = opt.currency ?: "USD"
        )
    }
}
