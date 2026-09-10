package org.telegram.messenger.feature.system.appconfig.domain.model

/**
 * Clean domain models representing global configuration limits and parameters
 * synced from the Telegram backend (TL_help.appConfig).
 */

data class StarsConfigModel(
    val paidMessagesDefault: Int = 10,
    val suggestedPostCommissionPermille: Int = 850,
    val suggestedPostMin: Int = 5,
    val suggestedPostMax: Int = 100_000,
    val resaleCommissionPermille: Int = 800,
    val resaleMin: Int = 125,
    val resaleMax: Int = 35_000,
    val ratingLearnMoreUrl: String = "https://telegram.org/blog/telegram-stars",
    val topUpInvoiceDisabled: Boolean = false,
    val giftsEnabled: Boolean = true,
    val paidPostAmountMax: Long = 100_000L,
    val revenueWithdrawalMin: Long = 1_000L
)

data class TonConfigModel(
    val suggestedPostCommissionPermille: Int = 850,
    val suggestedPostAmountMinNano: Long = 10_000_000L, // 0.01 TON
    val suggestedPostAmountMaxNano: Long = 10_000_000_000_000L, // 10,000 TON
    val usdRate: Double = 3.0,
    val resaleCommissionPermille: Int = 800,
    val resaleAmountMinNano: Long = 10_000_000L,
    val resaleAmountMaxNano: Long = 10_000_000_000_000L,
    val blockchainExplorerUrl: String? = null,
    val proxyAddress: String? = null
)

data class PollsConfigModel(
    val answersMax: Int = 12,
    val countriesMax: Int = 12,
    val answerLengthMax: Int = 100,
    val questionLengthMax: Int = 255,
    val solutionLengthMax: Int = 200,
    val captionLengthMax: Int = 300,
    val closePeriodMaxSeconds: Long = 86400L * 30L,
    val answerDeletePeriodSeconds: Long = 300L
)

data class RichMessageConfigModel(
    val lengthLimit: Int = 32_768,
    val maxBlocks: Int = 500,
    val maxDepth: Int = 16,
    val maxMedia: Int = 50,
    val maxTableCols: Int = 20,
    val posting: String = "premium"
)

data class AiComposeConfigModel(
    val toneExamplesNum: Int = 3,
    val toneTitleLengthMax: Int = 12,
    val tonePromptLengthMax: Int = 1024,
    val toneSavedLimitDefault: Int = 5,
    val toneSavedLimitPremium: Int = 20
)

data class AppLimitsConfigModel(
    val messageLengthLimitDefault: Int = 4096,
    val messageLengthLimitPremium: Int = 8192,
    val botsCreateLimitDefault: Int = 20,
    val botsCreateLimitPremium: Int = 40,
    val channelsLimitDefault: Int = 500,
    val channelsLimitPremium: Int = 1000,
    val savedGifsLimitDefault: Int = 200,
    val savedGifsLimitPremium: Int = 400,
    val storiesAlbumsLimit: Int = 100,
    val storiesAlbumStoriesLimit: Int = 100,
    val stargiftsCollectionsLimit: Int = 100,
    val stargiftsCollectionGiftsLimit: Int = 100,
    val quickReplyMessagesLimit: Int = 20,
    val quoteLengthMax: Int = 1024,
    val todoItemsMax: Int = 100,
    val todoTitleLengthMax: Int = 128,
    val todoItemLengthMax: Int = 256,
    val contactNoteLengthLimit: Int = 128,
    val passkeysAccountPasskeysMax: Int = 5
)

data class AppGlobalConfigState(
    val stars: StarsConfigModel = StarsConfigModel(),
    val ton: TonConfigModel = TonConfigModel(),
    val polls: PollsConfigModel = PollsConfigModel(),
    val richMessage: RichMessageConfigModel = RichMessageConfigModel(),
    val aiCompose: AiComposeConfigModel = AiComposeConfigModel(),
    val limits: AppLimitsConfigModel = AppLimitsConfigModel(),
    val phoneCountryIso2: String = "en",
    val disableBlurInLightTheme: Boolean = false,
    val disableBlurInDarkTheme: Boolean = false,
    val settingsDisplayPasskeys: Boolean = false,
    val needAgeVideoVerification: Boolean = false,
    val messageTypingDraftTtlSeconds: Long = 30L,
    val groupCallMessageTtlSeconds: Long = 10L,
    val groupCallMessageLengthLimit: Int = 128,
    val customEntries: Map<String, Any> = emptyMap()
)
