package org.telegram.messenger.feature.business.di

import org.telegram.messenger.feature.business.billing.data.repository.LegacyBillingRepository
import org.telegram.messenger.feature.business.billing.domain.repository.BillingRepository
import org.telegram.messenger.feature.business.billing.domain.usecase.FormatCurrencyUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.GetBillingStateUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.GetCurrencyExpUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.GetPremiumProductUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.ManageSubscriptionUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.ObserveBillingStateUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.QueryBillingPurchasesUseCase
import org.telegram.messenger.feature.business.billing.domain.usecase.StartBillingConnectionUseCase
import org.telegram.messenger.feature.business.billing.presentation.BillingViewModel
import org.telegram.messenger.feature.business.botstars.data.repository.LegacyBotStarsRepository
import org.telegram.messenger.feature.business.botstars.domain.repository.BotStarsRepository
import org.telegram.messenger.feature.business.botstars.domain.usecase.GetAdminedBotsAndChannelsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.GetBotStarsStatsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.GetTonStatsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.LoadBotTransactionsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.LoadConnectedStarBotsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.LoadSuggestedStarBotsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.ObserveBotStarsStatsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.ObserveBotTransactionsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.ObserveConnectedStarBotsUseCase
import org.telegram.messenger.feature.business.botstars.domain.usecase.ObserveTonStatsUseCase
import org.telegram.messenger.feature.business.botstars.presentation.BotStarsViewModel
import org.telegram.messenger.feature.business.businessbots.data.repository.LegacyBusinessBotsRepository
import org.telegram.messenger.feature.business.businessbots.domain.repository.BusinessBotsRepository
import org.telegram.messenger.feature.business.businessbots.domain.usecase.DeleteConnectedBotUseCase
import org.telegram.messenger.feature.business.businessbots.domain.usecase.FindConnectedBotUseCase
import org.telegram.messenger.feature.business.businessbots.domain.usecase.GetConnectedBotsUseCase
import org.telegram.messenger.feature.business.businessbots.domain.usecase.LoadConnectedBotsUseCase
import org.telegram.messenger.feature.business.businessbots.domain.usecase.ObserveConnectedBotsUseCase
import org.telegram.messenger.feature.business.businessbots.domain.usecase.UpdateConnectedBotUseCase
import org.telegram.messenger.feature.business.businessbots.presentation.BusinessBotsViewModel
import org.telegram.messenger.feature.business.businesslinks.data.repository.LegacyBusinessLinksRepository
import org.telegram.messenger.feature.business.businesslinks.domain.repository.BusinessLinksRepository
import org.telegram.messenger.feature.business.businesslinks.domain.usecase.CanAddNewBusinessLinkUseCase
import org.telegram.messenger.feature.business.businesslinks.domain.usecase.CreateBusinessLinkUseCase
import org.telegram.messenger.feature.business.businesslinks.domain.usecase.DeleteBusinessLinkUseCase
import org.telegram.messenger.feature.business.businesslinks.domain.usecase.EditBusinessLinkUseCase
import org.telegram.messenger.feature.business.businesslinks.domain.usecase.FindBusinessLinkUseCase
import org.telegram.messenger.feature.business.businesslinks.domain.usecase.GetBusinessLinksUseCase
import org.telegram.messenger.feature.business.businesslinks.domain.usecase.LoadBusinessLinksUseCase
import org.telegram.messenger.feature.business.businesslinks.domain.usecase.ObserveBusinessLinksUseCase
import org.telegram.messenger.feature.business.businesslinks.presentation.BusinessLinksViewModel
import org.telegram.messenger.feature.business.businessrecipients.data.repository.LegacyBusinessRecipientsRepository
import org.telegram.messenger.feature.business.businessrecipients.domain.repository.BusinessRecipientsRepository
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.AddExcludedUsersUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.AddSelectedUsersUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.CheckRecipientsChangesUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.GetBusinessRecipientsUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.ObserveBusinessRecipientsUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.RemoveExcludedUserUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.RemoveSelectedUserUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.ResetBusinessRecipientsUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.SetBusinessRecipientsUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.ToggleExcludeSelectedUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.ToggleRecipientFilterUseCase
import org.telegram.messenger.feature.business.businessrecipients.domain.usecase.ValidateBusinessRecipientsUseCase
import org.telegram.messenger.feature.business.businessrecipients.presentation.BusinessRecipientsViewModel
import org.telegram.messenger.feature.business.giftauctions.data.repository.LegacyGiftAuctionsRepository
import org.telegram.messenger.feature.business.giftauctions.domain.repository.GiftAuctionsRepository
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.GetActiveAuctionsUseCase
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.GetAuctionByIdUseCase
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.GetAuctionBySlugUseCase
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.LoadAuctionAcquiredGiftsUseCase
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.ObserveActiveAuctionsUseCase
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.ObserveAuctionUseCase
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.RefreshActiveAuctionsUseCase
import org.telegram.messenger.feature.business.giftauctions.domain.usecase.SendAuctionBidUseCase
import org.telegram.messenger.feature.business.giftauctions.presentation.GiftAuctionsViewModel
import org.telegram.messenger.feature.business.payments.data.repository.LegacyPaymentsRepository
import org.telegram.messenger.feature.business.payments.domain.repository.PaymentsRepository
import org.telegram.messenger.feature.business.payments.domain.usecase.GetStarSubscriptionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.GetStarTopupOptionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.GetStarTransactionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.GetStarsBalanceUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.ObserveStarSubscriptionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.ObserveStarTransactionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.ObserveStarsBalanceUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.RefreshStarSubscriptionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.RefreshStarTransactionsUseCase
import org.telegram.messenger.feature.business.payments.domain.usecase.RefreshStarsBalanceUseCase
import org.telegram.messenger.feature.business.payments.presentation.PaymentsViewModel
import org.telegram.messenger.feature.business.quickreplies.data.repository.LegacyQuickRepliesRepository
import org.telegram.messenger.feature.business.quickreplies.domain.repository.QuickRepliesRepository
import org.telegram.messenger.feature.business.quickreplies.domain.usecase.CanAddNewQuickReplyUseCase
import org.telegram.messenger.feature.business.quickreplies.domain.usecase.CheckQuickReplyNameBusyUseCase
import org.telegram.messenger.feature.business.quickreplies.domain.usecase.DeleteQuickRepliesUseCase
import org.telegram.messenger.feature.business.quickreplies.domain.usecase.FindQuickReplyUseCase
import org.telegram.messenger.feature.business.quickreplies.domain.usecase.GetQuickRepliesUseCase
import org.telegram.messenger.feature.business.quickreplies.domain.usecase.LoadQuickRepliesUseCase
import org.telegram.messenger.feature.business.quickreplies.domain.usecase.ObserveQuickRepliesUseCase
import org.telegram.messenger.feature.business.quickreplies.domain.usecase.RenameQuickReplyUseCase
import org.telegram.messenger.feature.business.quickreplies.domain.usecase.ReorderQuickRepliesUseCase
import org.telegram.messenger.feature.business.quickreplies.domain.usecase.SendQuickReplyUseCase
import org.telegram.messenger.feature.business.quickreplies.presentation.QuickRepliesViewModel
import org.telegram.messenger.feature.business.stargifts.data.repository.LegacyStarGiftsRepository
import org.telegram.messenger.feature.business.stargifts.domain.repository.StarGiftsRepository
import org.telegram.messenger.feature.business.stargifts.domain.usecase.GetStarGiftByIdUseCase
import org.telegram.messenger.feature.business.stargifts.domain.usecase.GetStarGiftsCatalogUseCase
import org.telegram.messenger.feature.business.stargifts.domain.usecase.LoadProfileGiftsUseCase
import org.telegram.messenger.feature.business.stargifts.domain.usecase.ObserveProfileGiftsUseCase
import org.telegram.messenger.feature.business.stargifts.domain.usecase.ObserveStarGiftsCatalogUseCase
import org.telegram.messenger.feature.business.stargifts.domain.usecase.ToggleHideProfileGiftUseCase
import org.telegram.messenger.feature.business.stargifts.domain.usecase.TogglePinProfileGiftUseCase
import org.telegram.messenger.feature.business.stargifts.presentation.StarGiftsViewModel
import org.telegram.messenger.feature.business.timezones.data.repository.LegacyTimezonesRepository
import org.telegram.messenger.feature.business.timezones.domain.repository.TimezonesRepository
import org.telegram.messenger.feature.business.timezones.domain.usecase.FindTimezoneUseCase
import org.telegram.messenger.feature.business.timezones.domain.usecase.GetSystemTimezoneIdUseCase
import org.telegram.messenger.feature.business.timezones.domain.usecase.GetTimezoneNameUseCase
import org.telegram.messenger.feature.business.timezones.domain.usecase.GetTimezonesUseCase
import org.telegram.messenger.feature.business.timezones.domain.usecase.LoadTimezonesUseCase
import org.telegram.messenger.feature.business.timezones.domain.usecase.ObserveTimezonesUseCase
import org.telegram.messenger.feature.business.timezones.presentation.TimezonesViewModel

class BusinessContainer(val account: Int) {

    private var customPaymentsRepository: PaymentsRepository? = null

    var paymentsRepository: PaymentsRepository
        get() = customPaymentsRepository ?: LegacyPaymentsRepository(account)
        set(value) {
            customPaymentsRepository = value
        }

    val observeStarsBalanceUseCase: ObserveStarsBalanceUseCase
        get() = ObserveStarsBalanceUseCase(paymentsRepository)

    val observeStarTransactionsUseCase: ObserveStarTransactionsUseCase
        get() = ObserveStarTransactionsUseCase(paymentsRepository)

    val observeStarSubscriptionsUseCase: ObserveStarSubscriptionsUseCase
        get() = ObserveStarSubscriptionsUseCase(paymentsRepository)

    val getStarsBalanceUseCase: GetStarsBalanceUseCase
        get() = GetStarsBalanceUseCase(paymentsRepository)

    val getStarTransactionsUseCase: GetStarTransactionsUseCase
        get() = GetStarTransactionsUseCase(paymentsRepository)

    val getStarSubscriptionsUseCase: GetStarSubscriptionsUseCase
        get() = GetStarSubscriptionsUseCase(paymentsRepository)

    val getStarTopupOptionsUseCase: GetStarTopupOptionsUseCase
        get() = GetStarTopupOptionsUseCase(paymentsRepository)

    val refreshStarsBalanceUseCase: RefreshStarsBalanceUseCase
        get() = RefreshStarsBalanceUseCase(paymentsRepository)

    val refreshStarTransactionsUseCase: RefreshStarTransactionsUseCase
        get() = RefreshStarTransactionsUseCase(paymentsRepository)

    val refreshStarSubscriptionsUseCase: RefreshStarSubscriptionsUseCase
        get() = RefreshStarSubscriptionsUseCase(paymentsRepository)

    private var cachedPaymentsViewModel: PaymentsViewModel? = null

    val paymentsViewModel: PaymentsViewModel
        get() {
            var vm = cachedPaymentsViewModel
            if (vm == null) {
                vm = createPaymentsViewModel()
                cachedPaymentsViewModel = vm
            }
            return vm
        }

    fun createPaymentsViewModel(): PaymentsViewModel {
        return PaymentsViewModel(
            observeStarsBalanceUseCase = observeStarsBalanceUseCase,
            observeStarTransactionsUseCase = observeStarTransactionsUseCase,
            observeStarSubscriptionsUseCase = observeStarSubscriptionsUseCase,
            getStarTopupOptionsUseCase = getStarTopupOptionsUseCase,
            refreshStarsBalanceUseCase = refreshStarsBalanceUseCase,
            refreshStarTransactionsUseCase = refreshStarTransactionsUseCase,
            refreshStarSubscriptionsUseCase = refreshStarSubscriptionsUseCase
        )
    }

    private var customQuickRepliesRepository: QuickRepliesRepository? = null

    var quickRepliesRepository: QuickRepliesRepository
        get() = customQuickRepliesRepository ?: LegacyQuickRepliesRepository(account)
        set(value) {
            customQuickRepliesRepository = value
        }

    val observeQuickRepliesUseCase: ObserveQuickRepliesUseCase
        get() = ObserveQuickRepliesUseCase(quickRepliesRepository)

    val getQuickRepliesUseCase: GetQuickRepliesUseCase
        get() = GetQuickRepliesUseCase(quickRepliesRepository)

    val loadQuickRepliesUseCase: LoadQuickRepliesUseCase
        get() = LoadQuickRepliesUseCase(quickRepliesRepository)

    val findQuickReplyUseCase: FindQuickReplyUseCase
        get() = FindQuickReplyUseCase(quickRepliesRepository)

    val checkQuickReplyNameBusyUseCase: CheckQuickReplyNameBusyUseCase
        get() = CheckQuickReplyNameBusyUseCase(quickRepliesRepository)

    val canAddNewQuickReplyUseCase: CanAddNewQuickReplyUseCase
        get() = CanAddNewQuickReplyUseCase(quickRepliesRepository)

    val renameQuickReplyUseCase: RenameQuickReplyUseCase
        get() = RenameQuickReplyUseCase(quickRepliesRepository)

    val reorderQuickRepliesUseCase: ReorderQuickRepliesUseCase
        get() = ReorderQuickRepliesUseCase(quickRepliesRepository)

    val deleteQuickRepliesUseCase: DeleteQuickRepliesUseCase
        get() = DeleteQuickRepliesUseCase(quickRepliesRepository)

    val sendQuickReplyUseCase: SendQuickReplyUseCase
        get() = SendQuickReplyUseCase(quickRepliesRepository)

    private var cachedQuickRepliesViewModel: QuickRepliesViewModel? = null

    val quickRepliesViewModel: QuickRepliesViewModel
        get() {
            var vm = cachedQuickRepliesViewModel
            if (vm == null) {
                vm = createQuickRepliesViewModel()
                cachedQuickRepliesViewModel = vm
            }
            return vm
        }

    fun createQuickRepliesViewModel(): QuickRepliesViewModel {
        return QuickRepliesViewModel(
            observeQuickRepliesUseCase = observeQuickRepliesUseCase,
            loadQuickRepliesUseCase = loadQuickRepliesUseCase,
            canAddNewQuickReplyUseCase = canAddNewQuickReplyUseCase,
            renameQuickReplyUseCase = renameQuickReplyUseCase,
            reorderQuickRepliesUseCase = reorderQuickRepliesUseCase,
            deleteQuickRepliesUseCase = deleteQuickRepliesUseCase,
            sendQuickReplyUseCase = sendQuickReplyUseCase
        )
    }

    private var customStarGiftsRepository: StarGiftsRepository? = null

    var starGiftsRepository: StarGiftsRepository
        get() = customStarGiftsRepository ?: LegacyStarGiftsRepository(account)
        set(value) {
            customStarGiftsRepository = value
        }

    val observeStarGiftsCatalogUseCase: ObserveStarGiftsCatalogUseCase
        get() = ObserveStarGiftsCatalogUseCase(starGiftsRepository)

    val getStarGiftsCatalogUseCase: GetStarGiftsCatalogUseCase
        get() = GetStarGiftsCatalogUseCase(starGiftsRepository)

    val getStarGiftByIdUseCase: GetStarGiftByIdUseCase
        get() = GetStarGiftByIdUseCase(starGiftsRepository)

    val observeProfileGiftsUseCase: ObserveProfileGiftsUseCase
        get() = ObserveProfileGiftsUseCase(starGiftsRepository)

    val loadProfileGiftsUseCase: LoadProfileGiftsUseCase
        get() = LoadProfileGiftsUseCase(starGiftsRepository)

    val togglePinProfileGiftUseCase: TogglePinProfileGiftUseCase
        get() = TogglePinProfileGiftUseCase(starGiftsRepository)

    val toggleHideProfileGiftUseCase: ToggleHideProfileGiftUseCase
        get() = ToggleHideProfileGiftUseCase(starGiftsRepository)

    private var cachedStarGiftsViewModel: StarGiftsViewModel? = null

    val starGiftsViewModel: StarGiftsViewModel
        get() {
            var vm = cachedStarGiftsViewModel
            if (vm == null) {
                vm = createStarGiftsViewModel()
                cachedStarGiftsViewModel = vm
            }
            return vm
        }

    fun createStarGiftsViewModel(): StarGiftsViewModel {
        return StarGiftsViewModel(
            observeStarGiftsCatalogUseCase = observeStarGiftsCatalogUseCase,
            getStarGiftsCatalogUseCase = getStarGiftsCatalogUseCase,
            getStarGiftByIdUseCase = getStarGiftByIdUseCase,
            observeProfileGiftsUseCase = observeProfileGiftsUseCase,
            loadProfileGiftsUseCase = loadProfileGiftsUseCase,
            togglePinProfileGiftUseCase = togglePinProfileGiftUseCase,
            toggleHideProfileGiftUseCase = toggleHideProfileGiftUseCase
        )
    }

    private var customGiftAuctionsRepository: GiftAuctionsRepository? = null

    var giftAuctionsRepository: GiftAuctionsRepository
        get() = customGiftAuctionsRepository ?: LegacyGiftAuctionsRepository(account)
        set(value) {
            customGiftAuctionsRepository = value
        }

    val observeActiveAuctionsUseCase: ObserveActiveAuctionsUseCase
        get() = ObserveActiveAuctionsUseCase(giftAuctionsRepository)

    val observeAuctionUseCase: ObserveAuctionUseCase
        get() = ObserveAuctionUseCase(giftAuctionsRepository)

    val getActiveAuctionsUseCase: GetActiveAuctionsUseCase
        get() = GetActiveAuctionsUseCase(giftAuctionsRepository)

    val getAuctionByIdUseCase: GetAuctionByIdUseCase
        get() = GetAuctionByIdUseCase(giftAuctionsRepository)

    val getAuctionBySlugUseCase: GetAuctionBySlugUseCase
        get() = GetAuctionBySlugUseCase(giftAuctionsRepository)

    val sendAuctionBidUseCase: SendAuctionBidUseCase
        get() = SendAuctionBidUseCase(giftAuctionsRepository)

    val loadAuctionAcquiredGiftsUseCase: LoadAuctionAcquiredGiftsUseCase
        get() = LoadAuctionAcquiredGiftsUseCase(giftAuctionsRepository)

    val refreshActiveAuctionsUseCase: RefreshActiveAuctionsUseCase
        get() = RefreshActiveAuctionsUseCase(giftAuctionsRepository)

    private var cachedGiftAuctionsViewModel: GiftAuctionsViewModel? = null

    val giftAuctionsViewModel: GiftAuctionsViewModel
        get() {
            var vm = cachedGiftAuctionsViewModel
            if (vm == null) {
                vm = createGiftAuctionsViewModel()
                cachedGiftAuctionsViewModel = vm
            }
            return vm
        }

    fun createGiftAuctionsViewModel(): GiftAuctionsViewModel {
        return GiftAuctionsViewModel(
            observeActiveAuctionsUseCase = observeActiveAuctionsUseCase,
            observeAuctionUseCase = observeAuctionUseCase,
            getActiveAuctionsUseCase = getActiveAuctionsUseCase,
            getAuctionByIdUseCase = getAuctionByIdUseCase,
            getAuctionBySlugUseCase = getAuctionBySlugUseCase,
            sendAuctionBidUseCase = sendAuctionBidUseCase,
            loadAuctionAcquiredGiftsUseCase = loadAuctionAcquiredGiftsUseCase,
            refreshActiveAuctionsUseCase = refreshActiveAuctionsUseCase
        )
    }

    private var customBusinessLinksRepository: BusinessLinksRepository? = null

    var businessLinksRepository: BusinessLinksRepository
        get() = customBusinessLinksRepository ?: LegacyBusinessLinksRepository(account)
        set(value) {
            customBusinessLinksRepository = value
        }

    val observeBusinessLinksUseCase: ObserveBusinessLinksUseCase
        get() = ObserveBusinessLinksUseCase(businessLinksRepository)

    val getBusinessLinksUseCase: GetBusinessLinksUseCase
        get() = GetBusinessLinksUseCase(businessLinksRepository)

    val loadBusinessLinksUseCase: LoadBusinessLinksUseCase
        get() = LoadBusinessLinksUseCase(businessLinksRepository)

    val createBusinessLinkUseCase: CreateBusinessLinkUseCase
        get() = CreateBusinessLinkUseCase(businessLinksRepository)

    val editBusinessLinkUseCase: EditBusinessLinkUseCase
        get() = EditBusinessLinkUseCase(businessLinksRepository)

    val deleteBusinessLinkUseCase: DeleteBusinessLinkUseCase
        get() = DeleteBusinessLinkUseCase(businessLinksRepository)

    val findBusinessLinkUseCase: FindBusinessLinkUseCase
        get() = FindBusinessLinkUseCase(businessLinksRepository)

    val canAddNewBusinessLinkUseCase: CanAddNewBusinessLinkUseCase
        get() = CanAddNewBusinessLinkUseCase(businessLinksRepository)

    private var cachedBusinessLinksViewModel: BusinessLinksViewModel? = null

    val businessLinksViewModel: BusinessLinksViewModel
        get() {
            var vm = cachedBusinessLinksViewModel
            if (vm == null) {
                vm = createBusinessLinksViewModel()
                cachedBusinessLinksViewModel = vm
            }
            return vm
        }

    fun createBusinessLinksViewModel(): BusinessLinksViewModel {
        return BusinessLinksViewModel(
            observeBusinessLinksUseCase = observeBusinessLinksUseCase,
            loadBusinessLinksUseCase = loadBusinessLinksUseCase,
            createBusinessLinkUseCase = createBusinessLinkUseCase,
            editBusinessLinkUseCase = editBusinessLinkUseCase,
            deleteBusinessLinkUseCase = deleteBusinessLinkUseCase,
            canAddNewBusinessLinkUseCase = canAddNewBusinessLinkUseCase
        )
    }

    private var customBusinessBotsRepository: BusinessBotsRepository? = null

    var businessBotsRepository: BusinessBotsRepository
        get() = customBusinessBotsRepository ?: LegacyBusinessBotsRepository(account)
        set(value) {
            customBusinessBotsRepository = value
        }

    val observeConnectedBotsUseCase: ObserveConnectedBotsUseCase
        get() = ObserveConnectedBotsUseCase(businessBotsRepository)

    val getConnectedBotsUseCase: GetConnectedBotsUseCase
        get() = GetConnectedBotsUseCase(businessBotsRepository)

    val loadConnectedBotsUseCase: LoadConnectedBotsUseCase
        get() = LoadConnectedBotsUseCase(businessBotsRepository)

    val updateConnectedBotUseCase: UpdateConnectedBotUseCase
        get() = UpdateConnectedBotUseCase(businessBotsRepository)

    val deleteConnectedBotUseCase: DeleteConnectedBotUseCase
        get() = DeleteConnectedBotUseCase(businessBotsRepository)

    val findConnectedBotUseCase: FindConnectedBotUseCase
        get() = FindConnectedBotUseCase(businessBotsRepository)

    private var cachedBusinessBotsViewModel: BusinessBotsViewModel? = null

    val businessBotsViewModel: BusinessBotsViewModel
        get() {
            var vm = cachedBusinessBotsViewModel
            if (vm == null) {
                vm = createBusinessBotsViewModel()
                cachedBusinessBotsViewModel = vm
            }
            return vm
        }

    fun createBusinessBotsViewModel(): BusinessBotsViewModel {
        return BusinessBotsViewModel(
            observeConnectedBotsUseCase = observeConnectedBotsUseCase,
            loadConnectedBotsUseCase = loadConnectedBotsUseCase,
            updateConnectedBotUseCase = updateConnectedBotUseCase,
            deleteConnectedBotUseCase = deleteConnectedBotUseCase
        )
    }

    // --- Timezones ---
    private var customTimezonesRepository: TimezonesRepository? = null

    var timezonesRepository: TimezonesRepository
        get() = customTimezonesRepository ?: LegacyTimezonesRepository(account)
        set(value) {
            customTimezonesRepository = value
        }

    val observeTimezonesUseCase: ObserveTimezonesUseCase
        get() = ObserveTimezonesUseCase(timezonesRepository)

    val getTimezonesUseCase: GetTimezonesUseCase
        get() = GetTimezonesUseCase(timezonesRepository)

    val loadTimezonesUseCase: LoadTimezonesUseCase
        get() = LoadTimezonesUseCase(timezonesRepository)

    val findTimezoneUseCase: FindTimezoneUseCase
        get() = FindTimezoneUseCase(timezonesRepository)

    val getSystemTimezoneIdUseCase: GetSystemTimezoneIdUseCase
        get() = GetSystemTimezoneIdUseCase(timezonesRepository)

    val getTimezoneNameUseCase: GetTimezoneNameUseCase
        get() = GetTimezoneNameUseCase(timezonesRepository)

    private var cachedTimezonesViewModel: TimezonesViewModel? = null

    val timezonesViewModel: TimezonesViewModel
        get() {
            var vm = cachedTimezonesViewModel
            if (vm == null) {
                vm = createTimezonesViewModel()
                cachedTimezonesViewModel = vm
            }
            return vm
        }

    fun createTimezonesViewModel(): TimezonesViewModel {
        return TimezonesViewModel(
            observeTimezonesUseCase = observeTimezonesUseCase,
            getTimezonesUseCase = getTimezonesUseCase,
            loadTimezonesUseCase = loadTimezonesUseCase,
            findTimezoneUseCase = findTimezoneUseCase,
            getSystemTimezoneIdUseCase = getSystemTimezoneIdUseCase,
            getTimezoneNameUseCase = getTimezoneNameUseCase
        )
    }

    // --- Bot Stars ---
    private var customBotStarsRepository: BotStarsRepository? = null

    var botStarsRepository: BotStarsRepository
        get() = customBotStarsRepository ?: LegacyBotStarsRepository(account)
        set(value) {
            customBotStarsRepository = value
        }

    val observeBotStarsStatsUseCase: ObserveBotStarsStatsUseCase
        get() = ObserveBotStarsStatsUseCase(botStarsRepository)

    val getBotStarsStatsUseCase: GetBotStarsStatsUseCase
        get() = GetBotStarsStatsUseCase(botStarsRepository)

    val observeTonStatsUseCase: ObserveTonStatsUseCase
        get() = ObserveTonStatsUseCase(botStarsRepository)

    val getTonStatsUseCase: GetTonStatsUseCase
        get() = GetTonStatsUseCase(botStarsRepository)

    val observeBotTransactionsUseCase: ObserveBotTransactionsUseCase
        get() = ObserveBotTransactionsUseCase(botStarsRepository)

    val loadBotTransactionsUseCase: LoadBotTransactionsUseCase
        get() = LoadBotTransactionsUseCase(botStarsRepository)

    val observeConnectedStarBotsUseCase: ObserveConnectedStarBotsUseCase
        get() = ObserveConnectedStarBotsUseCase(botStarsRepository)

    val loadConnectedStarBotsUseCase: LoadConnectedStarBotsUseCase
        get() = LoadConnectedStarBotsUseCase(botStarsRepository)

    val loadSuggestedStarBotsUseCase: LoadSuggestedStarBotsUseCase
        get() = LoadSuggestedStarBotsUseCase(botStarsRepository)

    val getAdminedBotsAndChannelsUseCase: GetAdminedBotsAndChannelsUseCase
        get() = GetAdminedBotsAndChannelsUseCase(botStarsRepository)

    private var cachedBotStarsViewModel: BotStarsViewModel? = null

    val botStarsViewModel: BotStarsViewModel
        get() {
            var vm = cachedBotStarsViewModel
            if (vm == null) {
                vm = createBotStarsViewModel()
                cachedBotStarsViewModel = vm
            }
            return vm
        }

    fun createBotStarsViewModel(): BotStarsViewModel {
        return BotStarsViewModel(
            observeBotStarsStatsUseCase = observeBotStarsStatsUseCase,
            getBotStarsStatsUseCase = getBotStarsStatsUseCase,
            observeTonStatsUseCase = observeTonStatsUseCase,
            getTonStatsUseCase = getTonStatsUseCase,
            observeBotTransactionsUseCase = observeBotTransactionsUseCase,
            loadBotTransactionsUseCase = loadBotTransactionsUseCase,
            observeConnectedStarBotsUseCase = observeConnectedStarBotsUseCase,
            loadConnectedStarBotsUseCase = loadConnectedStarBotsUseCase,
            loadSuggestedStarBotsUseCase = loadSuggestedStarBotsUseCase,
            getAdminedBotsAndChannelsUseCase = getAdminedBotsAndChannelsUseCase
        )
    }

    private var customBillingRepository: BillingRepository? = null

    var billingRepository: BillingRepository
        get() = customBillingRepository ?: LegacyBillingRepository()
        set(value) {
            customBillingRepository = value
        }

    val observeBillingStateUseCase: ObserveBillingStateUseCase
        get() = ObserveBillingStateUseCase(billingRepository)

    val getBillingStateUseCase: GetBillingStateUseCase
        get() = GetBillingStateUseCase(billingRepository)

    val startBillingConnectionUseCase: StartBillingConnectionUseCase
        get() = StartBillingConnectionUseCase(billingRepository)

    val getPremiumProductUseCase: GetPremiumProductUseCase
        get() = GetPremiumProductUseCase(billingRepository)

    val formatCurrencyUseCase: FormatCurrencyUseCase
        get() = FormatCurrencyUseCase(billingRepository)

    val getCurrencyExpUseCase: GetCurrencyExpUseCase
        get() = GetCurrencyExpUseCase(billingRepository)

    val queryBillingPurchasesUseCase: QueryBillingPurchasesUseCase
        get() = QueryBillingPurchasesUseCase(billingRepository)

    val manageSubscriptionUseCase: ManageSubscriptionUseCase
        get() = ManageSubscriptionUseCase(billingRepository)

    private var cachedBillingViewModel: BillingViewModel? = null

    val billingViewModel: BillingViewModel
        get() {
            var vm = cachedBillingViewModel
            if (vm == null) {
                vm = createBillingViewModel()
                cachedBillingViewModel = vm
            }
            return vm
        }

    fun createBillingViewModel(): BillingViewModel {
        return BillingViewModel(
            observeBillingStateUseCase = observeBillingStateUseCase,
            getBillingStateUseCase = getBillingStateUseCase,
            startBillingConnectionUseCase = startBillingConnectionUseCase,
            queryBillingPurchasesUseCase = queryBillingPurchasesUseCase,
            manageSubscriptionUseCase = manageSubscriptionUseCase
        )
    }

    fun createBusinessRecipientsRepository(): BusinessRecipientsRepository {
        return LegacyBusinessRecipientsRepository()
    }

    val businessRecipientsRepository: BusinessRecipientsRepository by lazy {
        LegacyBusinessRecipientsRepository()
    }

    val observeBusinessRecipientsUseCase: ObserveBusinessRecipientsUseCase
        get() = ObserveBusinessRecipientsUseCase(businessRecipientsRepository)

    val getBusinessRecipientsUseCase: GetBusinessRecipientsUseCase
        get() = GetBusinessRecipientsUseCase(businessRecipientsRepository)

    val setBusinessRecipientsUseCase: SetBusinessRecipientsUseCase
        get() = SetBusinessRecipientsUseCase(businessRecipientsRepository)

    val toggleExcludeSelectedUseCase: ToggleExcludeSelectedUseCase
        get() = ToggleExcludeSelectedUseCase(businessRecipientsRepository)

    val toggleRecipientFilterUseCase: ToggleRecipientFilterUseCase
        get() = ToggleRecipientFilterUseCase(businessRecipientsRepository)

    val addSelectedUsersUseCase: AddSelectedUsersUseCase
        get() = AddSelectedUsersUseCase(businessRecipientsRepository)

    val removeSelectedUserUseCase: RemoveSelectedUserUseCase
        get() = RemoveSelectedUserUseCase(businessRecipientsRepository)

    val addExcludedUsersUseCase: AddExcludedUsersUseCase
        get() = AddExcludedUsersUseCase(businessRecipientsRepository)

    val removeExcludedUserUseCase: RemoveExcludedUserUseCase
        get() = RemoveExcludedUserUseCase(businessRecipientsRepository)

    val checkRecipientsChangesUseCase: CheckRecipientsChangesUseCase
        get() = CheckRecipientsChangesUseCase(businessRecipientsRepository)

    val validateBusinessRecipientsUseCase: ValidateBusinessRecipientsUseCase
        get() = ValidateBusinessRecipientsUseCase(businessRecipientsRepository)

    val resetBusinessRecipientsUseCase: ResetBusinessRecipientsUseCase
        get() = ResetBusinessRecipientsUseCase(businessRecipientsRepository)

    private var cachedBusinessRecipientsViewModel: BusinessRecipientsViewModel? = null

    val businessRecipientsViewModel: BusinessRecipientsViewModel
        get() {
            var vm = cachedBusinessRecipientsViewModel
            if (vm == null) {
                vm = createBusinessRecipientsViewModel()
                cachedBusinessRecipientsViewModel = vm
            }
            return vm
        }

    fun createBusinessRecipientsViewModel(): BusinessRecipientsViewModel {
        return BusinessRecipientsViewModel(
            observeRecipientsUseCase = observeBusinessRecipientsUseCase,
            getRecipientsUseCase = getBusinessRecipientsUseCase,
            setRecipientsUseCase = setBusinessRecipientsUseCase,
            toggleExcludeSelectedUseCase = toggleExcludeSelectedUseCase,
            toggleRecipientFilterUseCase = toggleRecipientFilterUseCase,
            addSelectedUsersUseCase = addSelectedUsersUseCase,
            removeSelectedUserUseCase = removeSelectedUserUseCase,
            addExcludedUsersUseCase = addExcludedUsersUseCase,
            removeExcludedUserUseCase = removeExcludedUserUseCase,
            checkChangesUseCase = checkRecipientsChangesUseCase,
            validateUseCase = validateBusinessRecipientsUseCase,
            resetUseCase = resetBusinessRecipientsUseCase
        )
    }

}
