package org.telegram.messenger.feature.system.appconfig.data.mapper

import org.telegram.messenger.AppGlobalConfig
import org.telegram.messenger.MessagesController
import org.telegram.messenger.feature.system.appconfig.domain.model.AiComposeConfigModel
import org.telegram.messenger.feature.system.appconfig.domain.model.AppGlobalConfigState
import org.telegram.messenger.feature.system.appconfig.domain.model.AppLimitsConfigModel
import org.telegram.messenger.feature.system.appconfig.domain.model.PollsConfigModel
import org.telegram.messenger.feature.system.appconfig.domain.model.RichMessageConfigModel
import org.telegram.messenger.feature.system.appconfig.domain.model.StarsConfigModel
import org.telegram.messenger.feature.system.appconfig.domain.model.TonConfigModel
import java.util.concurrent.TimeUnit

object AppConfigMapper {

    fun fromAppGlobalConfig(
        config: AppGlobalConfig?,
        controller: MessagesController? = null
    ): AppGlobalConfigState {
        if (config == null) {
            return AppGlobalConfigState()
        }

        val stars = StarsConfigModel(
            paidMessagesDefault = runCatching { config.starsPaidMessagesChannelAmountDefault.get() }.getOrDefault(10),
            suggestedPostCommissionPermille = runCatching { config.starsSuggestedPostCommissionPermille.get() }.getOrDefault(850),
            suggestedPostMin = runCatching { config.starsSuggestedPostAmountMin.get() }.getOrDefault(5),
            suggestedPostMax = runCatching { config.starsSuggestedPostAmountMax.get() }.getOrDefault(100_000),
            resaleCommissionPermille = runCatching { config.starsStarGiftResaleCommissionPermille.get() }.getOrDefault(800),
            resaleMin = runCatching { config.starsStarGiftResaleAmountMin.get() }.getOrDefault(125),
            resaleMax = runCatching { config.starsStarGiftResaleAmountMax.get() }.getOrDefault(35_000),
            ratingLearnMoreUrl = runCatching { config.starsRatingLearnMoreUrl.get() }.getOrNull() ?: "https://telegram.org/blog/telegram-stars",
            topUpInvoiceDisabled = runCatching { config.starsSpendTopUpInvoiceDisabled.get() }.getOrDefault(false),
            giftsEnabled = runCatching { controller?.starsGiftsEnabled ?: true }.getOrDefault(true),
            paidPostAmountMax = runCatching { controller?.starsPaidPostAmountMax ?: 100_000L }.getOrDefault(100_000L),
            revenueWithdrawalMin = runCatching { controller?.starsRevenueWithdrawalMin ?: 1_000L }.getOrDefault(1_000L)
        )

        val ton = TonConfigModel(
            suggestedPostCommissionPermille = runCatching { config.tonSuggestedPostCommissionPermille.get() }.getOrDefault(850),
            suggestedPostAmountMinNano = runCatching { config.tonSuggestedPostAmountMin.get() }.getOrDefault(10_000_000L),
            suggestedPostAmountMaxNano = runCatching { config.tonSuggestedPostAmountMax.get() }.getOrDefault(10_000_000_000_000L),
            usdRate = runCatching { config.tonUsdRate.get() }.getOrDefault(3.0),
            resaleCommissionPermille = runCatching { config.tonStarGiftResaleCommissionPermille.get() }.getOrDefault(800),
            resaleAmountMinNano = runCatching { config.tonStarGiftResaleAmountMin.get() }.getOrDefault(10_000_000L),
            resaleAmountMaxNano = runCatching { config.tonStarGiftResaleAmountMax.get() }.getOrDefault(10_000_000_000_000L),
            blockchainExplorerUrl = runCatching { controller?.tonBlockchainExplorerUrl }.getOrNull(),
            proxyAddress = runCatching { controller?.tonProxyAddress }.getOrNull()
        )

        val polls = PollsConfigModel(
            answersMax = runCatching { config.pollAnswersMax.get() }.getOrDefault(12),
            countriesMax = runCatching { config.pollCountriesMax.get() }.getOrDefault(12),
            answerLengthMax = runCatching { config.pollAnswerLengthMax.get() }.getOrDefault(100),
            questionLengthMax = runCatching { config.pollQuestionLengthMax.get() }.getOrDefault(255),
            solutionLengthMax = runCatching { config.pollSolutionLengthMax.get() }.getOrDefault(200),
            captionLengthMax = runCatching { config.pollCaptionLengthMax.get() }.getOrDefault(300),
            closePeriodMaxSeconds = runCatching { config.pollClosePeriodMax.get(TimeUnit.SECONDS) }.getOrDefault(86400L * 30L),
            answerDeletePeriodSeconds = runCatching { config.pollAnswerDeletePeriod.get(TimeUnit.SECONDS) }.getOrDefault(300L)
        )

        val richMessage = RichMessageConfigModel(
            lengthLimit = runCatching { config.richMessageLengthLimit.get() }.getOrDefault(32_768),
            maxBlocks = runCatching { config.richMessageMaxBlocks.get() }.getOrDefault(500),
            maxDepth = runCatching { config.richMessageMaxDepth.get() }.getOrDefault(16),
            maxMedia = runCatching { config.richMessageMaxMedia.get() }.getOrDefault(50),
            maxTableCols = runCatching { config.richMessageMaxTableCols.get() }.getOrDefault(20),
            posting = runCatching { config.richMessagePosting.get() }.getOrNull() ?: "premium"
        )

        val aiCompose = AiComposeConfigModel(
            toneExamplesNum = runCatching { config.aicomposeToneExamplesNum.get() }.getOrDefault(3),
            toneTitleLengthMax = runCatching { config.aicomposeToneTitleLengthMax.get() }.getOrDefault(12),
            tonePromptLengthMax = runCatching { config.aicomposeTonePromptLengthMax.get() }.getOrDefault(1024),
            toneSavedLimitDefault = runCatching { config.aicomposeToneSavedLimitDefault.get() }.getOrDefault(5),
            toneSavedLimitPremium = runCatching { config.aicomposeToneSavedLimitPremium.get() }.getOrDefault(20)
        )

        val limits = AppLimitsConfigModel(
            messageLengthLimitDefault = runCatching { config.messageLengthLimitDefault.get() }.getOrDefault(4096),
            messageLengthLimitPremium = runCatching { config.messageLengthLimitPremium.get() }.getOrDefault(8192),
            botsCreateLimitDefault = runCatching { config.botsCreateLimitDefault.get() }.getOrDefault(20),
            botsCreateLimitPremium = runCatching { config.botsCreateLimitPremium.get() }.getOrDefault(40),
            channelsLimitDefault = runCatching { controller?.channelsLimitDefault ?: 500 }.getOrDefault(500),
            channelsLimitPremium = runCatching { controller?.channelsLimitPremium ?: 1000 }.getOrDefault(1000),
            savedGifsLimitDefault = runCatching { controller?.savedGifsLimitDefault ?: 200 }.getOrDefault(200),
            savedGifsLimitPremium = runCatching { controller?.savedGifsLimitPremium ?: 400 }.getOrDefault(400),
            storiesAlbumsLimit = runCatching { config.storiesAlbumsLimit.get() }.getOrDefault(100),
            storiesAlbumStoriesLimit = runCatching { config.storiesAlbumStoriesLimit.get() }.getOrDefault(100),
            stargiftsCollectionsLimit = runCatching { config.stargiftsCollectionsLimit.get() }.getOrDefault(100),
            stargiftsCollectionGiftsLimit = runCatching { config.stargiftsCollectionGiftsLimit.get() }.getOrDefault(100),
            quickReplyMessagesLimit = runCatching { config.quickReplyMessagesLimit.get() }.getOrDefault(20),
            quoteLengthMax = runCatching { controller?.quoteLengthMax ?: 1024 }.getOrDefault(1024),
            todoItemsMax = runCatching { controller?.todoItemsMax ?: 100 }.getOrDefault(100),
            todoTitleLengthMax = runCatching { controller?.todoTitleLengthMax ?: 128 }.getOrDefault(128),
            todoItemLengthMax = runCatching { controller?.todoItemLengthMax ?: 256 }.getOrDefault(256),
            contactNoteLengthLimit = runCatching { config.contactNoteLengthLimit.get() }.getOrDefault(128),
            passkeysAccountPasskeysMax = runCatching { config.passkeysAccountPasskeysMax.get() }.getOrDefault(5)
        )

        return AppGlobalConfigState(
            stars = stars,
            ton = ton,
            polls = polls,
            richMessage = richMessage,
            aiCompose = aiCompose,
            limits = limits,
            phoneCountryIso2 = runCatching { config.phoneCountryIso2.get() }.getOrNull() ?: "en",
            disableBlurInLightTheme = runCatching { config.disableBlurInLightTheme.get() }.getOrDefault(false),
            disableBlurInDarkTheme = runCatching { config.disableBlurInDarkTheme.get() }.getOrDefault(false),
            settingsDisplayPasskeys = runCatching { config.settingsDisplayPasskeys.get() }.getOrDefault(false),
            needAgeVideoVerification = runCatching { config.needAgeVideoVerification.get() }.getOrDefault(false),
            messageTypingDraftTtlSeconds = runCatching { config.messageTypingDraftTtl.get(TimeUnit.SECONDS) }.getOrDefault(30L),
            groupCallMessageTtlSeconds = runCatching { config.groupCallMessageTtl.get(TimeUnit.SECONDS) }.getOrDefault(10L),
            groupCallMessageLengthLimit = runCatching { config.groupCallMessageLengthLimit.get() }.getOrDefault(128)
        )
    }
}
