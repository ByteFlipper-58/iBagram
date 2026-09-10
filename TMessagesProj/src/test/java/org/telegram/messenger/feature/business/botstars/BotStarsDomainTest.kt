package org.telegram.messenger.feature.business.botstars

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.di.AccountFeatureContainer
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.botstars.data.mapper.BotStarsMapper
import org.telegram.messenger.feature.business.botstars.domain.model.BotStarsRevenueStatsModel
import org.telegram.messenger.feature.business.botstars.domain.model.BotStarsRevenueStatusModel
import org.telegram.messenger.feature.business.botstars.domain.model.BotStarsStateModel
import org.telegram.messenger.feature.business.botstars.domain.model.BotStarsTransactionModel
import org.telegram.messenger.feature.business.botstars.domain.model.BotStarsTransactionType
import org.telegram.messenger.feature.business.botstars.domain.model.ConnectedBotStarRefModel
import org.telegram.messenger.feature.business.botstars.domain.model.StarRefProgramModel
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
import org.telegram.messenger.feature.business.botstars.presentation.BotStarsEvent
import org.telegram.messenger.feature.business.botstars.presentation.BotStarsViewModel
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_payments
import org.telegram.tgnet.tl.TL_stars

@OptIn(ExperimentalCoroutinesApi::class)
class BotStarsDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeBotStarsRepository : BotStarsRepository {
        var starsStats: BotStarsRevenueStatsModel? = null
        var tonStats: BotStarsRevenueStatsModel? = null
        val transactions = mutableListOf<BotStarsTransactionModel>()
        val connectedBots = mutableListOf<ConnectedBotStarRefModel>()
        val suggestedBots = mutableListOf<StarRefProgramModel>()
        val adminedBots = mutableListOf<Long>()
        val adminedChannels = mutableListOf<Long>()

        var shouldFail = false
        var lastLoadedTransactionsForce = false

        private val starsFlow = MutableStateFlow<BotStarsRevenueStatsModel?>(null)
        private val tonFlow = MutableStateFlow<BotStarsRevenueStatsModel?>(null)
        private val txFlow = MutableStateFlow<List<BotStarsTransactionModel>>(emptyList())
        private val botsFlow = MutableStateFlow<List<ConnectedBotStarRefModel>>(emptyList())

        fun emitStars(stats: BotStarsRevenueStatsModel?) {
            starsStats = stats
            starsFlow.value = stats
        }

        fun emitTon(stats: BotStarsRevenueStatsModel?) {
            tonStats = stats
            tonFlow.value = stats
        }

        fun emitTransactions() {
            txFlow.value = transactions.toList()
        }

        fun emitConnectedBots() {
            botsFlow.value = connectedBots.toList()
        }

        override fun observeBotStarsStats(dialogId: Long): Flow<BotStarsRevenueStatsModel?> = starsFlow

        override suspend fun getBotStarsStats(dialogId: Long, force: Boolean): Result<BotStarsRevenueStatsModel?> {
            return if (shouldFail) {
                Result.failure("Failed to load stars stats")
            } else {
                emitStars(starsStats)
                Result.Success(starsStats)
            }
        }

        override fun observeTonStats(dialogId: Long): Flow<BotStarsRevenueStatsModel?> = tonFlow

        override suspend fun getTonStats(dialogId: Long, force: Boolean): Result<BotStarsRevenueStatsModel?> {
            return if (shouldFail) {
                Result.failure("Failed to load ton stats")
            } else {
                emitTon(tonStats)
                Result.Success(tonStats)
            }
        }

        override fun observeTransactions(dialogId: Long, type: BotStarsTransactionType): Flow<List<BotStarsTransactionModel>> = txFlow

        override suspend fun loadTransactions(
            dialogId: Long,
            type: BotStarsTransactionType,
            reload: Boolean
        ): Result<List<BotStarsTransactionModel>> {
            lastLoadedTransactionsForce = reload
            return if (shouldFail) {
                Result.failure("Failed to load transactions")
            } else {
                emitTransactions()
                Result.Success(transactions.toList())
            }
        }

        override fun observeConnectedBots(dialogId: Long): Flow<List<ConnectedBotStarRefModel>> = botsFlow

        override suspend fun loadConnectedBots(dialogId: Long, reload: Boolean): Result<List<ConnectedBotStarRefModel>> {
            return if (shouldFail) {
                Result.failure("Failed to load connected bots")
            } else {
                emitConnectedBots()
                Result.Success(connectedBots.toList())
            }
        }

        override suspend fun loadSuggestedBots(dialogId: Long, sort: Int): Result<List<StarRefProgramModel>> {
            return if (shouldFail) {
                Result.failure("Failed to load suggested bots")
            } else {
                Result.Success(suggestedBots.toList())
            }
        }

        override suspend fun loadAdminedBots(): Result<List<Long>> {
            return Result.Success(adminedBots.toList())
        }

        override suspend fun loadAdminedChannels(): Result<List<Long>> {
            return Result.Success(adminedChannels.toList())
        }
    }

    @Test
    fun testRevenueModels() {
        val status = BotStarsRevenueStatusModel(
            currentBalance = 1000L,
            availableBalance = 500L,
            overallRevenue = 2000L,
            withdrawalEnabled = true,
            nextWithdrawalAt = 1700000000L,
            isTon = false
        )
        assertTrue(status.hasBalance)
        assertTrue(status.withdrawalEnabled)
        assertFalse(status.isTon)

        val stats = BotStarsRevenueStatsModel(
            dialogId = 12345L,
            status = status,
            usdRate = 0.02
        )
        assertEquals(12345L, stats.dialogId)
        assertEquals(1000L, stats.currentBalance)
        assertEquals(500L, stats.availableBalance)
        assertEquals(2000L, stats.overallRevenue)
        assertTrue(stats.isWithdrawalEnabled)
        assertTrue(stats.hasStars)
        assertEquals(0.02, stats.usdRate, 0.0001)

        val emptyStats = BotStarsRevenueStatsModel(dialogId = 999L, status = null)
        assertEquals(0L, emptyStats.currentBalance)
        assertFalse(emptyStats.hasStars)
    }

    @Test
    fun testTransactionAndBotModels() {
        val txIncoming = BotStarsTransactionModel(
            id = "tx1",
            stars = 500L,
            date = 1690000000L,
            peerId = 1001L,
            title = "Star Gift Purchase",
            isOutgoing = false
        )
        assertTrue(txIncoming.isIncoming)
        assertFalse(txIncoming.isOutgoing)

        val txOutgoing = BotStarsTransactionModel(
            id = "tx2",
            stars = -200L,
            date = 1690000100L,
            peerId = 1002L,
            title = "Withdrawal",
            isOutgoing = true,
            isRefund = false
        )
        assertFalse(txOutgoing.isIncoming)
        assertTrue(txOutgoing.isOutgoing)

        assertEquals(BotStarsTransactionType.ALL, BotStarsTransactionType.fromLegacyId(0))
        assertEquals(BotStarsTransactionType.INCOMING, BotStarsTransactionType.fromLegacyId(1))
        assertEquals(BotStarsTransactionType.OUTGOING, BotStarsTransactionType.fromLegacyId(2))

        val connectedBot = ConnectedBotStarRefModel(
            botId = 444L,
            revoked = false,
            participants = 10,
            revenue = 1500L
        )
        assertTrue(connectedBot.isActive)

        val revokedBot = connectedBot.copy(revoked = true)
        assertFalse(revokedBot.isActive)

        val state = BotStarsStateModel(
            dialogId = 12345L,
            transactions = listOf(txIncoming, txOutgoing),
            connectedBots = listOf(connectedBot, revokedBot)
        )
        assertEquals(2, state.totalTransactions)
        assertEquals(1, state.connectedBotsCount)
    }

    @Test
    fun testBotStarsMapper() {
        val tlStatus = TLRPC.TL_starsRevenueStatus().apply {
            current_balance = TL_stars.StarsAmount.ofStars(1000)
            available_balance = TL_stars.StarsAmount.ofStars(800)
            overall_revenue = TL_stars.StarsAmount.ofStars(5000)
            withdrawal_enabled = true
            next_withdrawal_at = 1720000000
        }

        val tlStats = TLRPC.TL_payments_starsRevenueStats().apply {
            status = tlStatus
            usd_rate = 0.02
        }

        val domainStats = BotStarsMapper.toRevenueStats(777L, tlStats)
        assertNotNull(domainStats)
        assertEquals(777L, domainStats?.dialogId)
        assertEquals(1000L, domainStats?.currentBalance)
        assertEquals(800L, domainStats?.availableBalance)
        assertEquals(5000L, domainStats?.overallRevenue)
        assertTrue(domainStats?.isWithdrawalEnabled == true)

        assertNull(BotStarsMapper.toRevenueStats(777L, null))
        assertNull(BotStarsMapper.toRevenueStatus(null))

        val tlTx = TL_stars.TL_starsTransaction().apply {
            id = "star_tx_99"
            amount = TL_stars.StarsAmount.ofStars(250)
            date = 1705000000
            title = "Test TX"
            flags = 0
        }
        val domainTx = BotStarsMapper.toTransaction(tlTx)
        assertNotNull(domainTx)
        assertEquals("star_tx_99", domainTx?.id)
        assertEquals(250L, domainTx?.stars)
        assertTrue(domainTx?.isIncoming == true)

        val tlConnectedBot = TL_payments.connectedBotStarRef().apply {
            bot_id = 12345L
            date = 1700000
            url = "https://t.me/testbot"
            commission_permille = 150
            duration_months = 6
            revoked = false
            participants = 5L
            revenue = 990L
        }
        val domainBot = BotStarsMapper.toConnectedBot(tlConnectedBot)
        assertNotNull(domainBot)
        assertEquals(12345L, domainBot?.botId)
        assertEquals(150, domainBot?.commissionPermille)
        assertEquals(5L, domainBot?.participants)
        assertEquals(990L, domainBot?.revenue)
        assertTrue(domainBot?.isActive == true)

        val tlSuggestedBot = TL_payments.starRefProgram().apply {
            bot_id = 9999L
            commission_permille = 200
            duration_months = 12
            end_date = 1750000000
            daily_revenue_per_user = TL_stars.StarsAmount.ofStars(500)
        }
        val domainSuggested = BotStarsMapper.toSuggestedBot(tlSuggestedBot)
        assertNotNull(domainSuggested)
        assertEquals(9999L, domainSuggested?.botId)
        assertEquals(200, domainSuggested?.commissionPermille)
        assertEquals(1750000000, domainSuggested?.endDate)
        assertEquals(500L, domainSuggested?.dailyRevenuePerUser)
    }

    @Test
    fun testUseCases() = runTest {
        val fakeRepo = FakeBotStarsRepository().apply {
            starsStats = BotStarsRevenueStatsModel(
                dialogId = 100L,
                status = BotStarsRevenueStatusModel(currentBalance = 300L, availableBalance = 300L)
            )
            tonStats = BotStarsRevenueStatsModel(
                dialogId = 100L,
                status = BotStarsRevenueStatusModel(currentBalance = 50L, isTon = true)
            )
            transactions.add(BotStarsTransactionModel("tx1", 300L, 1700000L, 100L))
            connectedBots.add(ConnectedBotStarRefModel(botId = 888L, revenue = 1200L))
            suggestedBots.add(StarRefProgramModel(botId = 999L, commissionPermille = 100))
            adminedBots.add(888L)
            adminedChannels.add(777L)
        }

        val observeStars = ObserveBotStarsStatsUseCase(fakeRepo)
        val getStars = GetBotStarsStatsUseCase(fakeRepo)
        val observeTon = ObserveTonStatsUseCase(fakeRepo)
        val getTon = GetTonStatsUseCase(fakeRepo)
        val observeTxs = ObserveBotTransactionsUseCase(fakeRepo)
        val loadTxs = LoadBotTransactionsUseCase(fakeRepo)
        val observeBots = ObserveConnectedStarBotsUseCase(fakeRepo)
        val loadBots = LoadConnectedStarBotsUseCase(fakeRepo)
        val loadSuggested = LoadSuggestedStarBotsUseCase(fakeRepo)
        val admined = GetAdminedBotsAndChannelsUseCase(fakeRepo)

        val starsResult = getStars(100L)
        assertTrue(starsResult is Result.Success)
        assertEquals(300L, (starsResult as Result.Success).data?.currentBalance)

        val tonResult = getTon(100L)
        assertTrue(tonResult is Result.Success)
        assertEquals(50L, (tonResult as Result.Success).data?.currentBalance)

        val txsResult = loadTxs(100L, reload = true)
        assertTrue(txsResult is Result.Success)
        assertEquals(1, (txsResult as Result.Success).data.size)
        assertTrue(fakeRepo.lastLoadedTransactionsForce)

        val botsResult = loadBots(100L)
        assertTrue(botsResult is Result.Success)
        assertEquals(1, (botsResult as Result.Success).data.size)

        val suggestedResult = loadSuggested(100L)
        assertTrue(suggestedResult is Result.Success)
        assertEquals(1, (suggestedResult as Result.Success).data.size)

        val adminedBotsRes = admined.getAdminedBots()
        assertTrue(adminedBotsRes is Result.Success)
        assertEquals(listOf(888L), (adminedBotsRes as Result.Success).data)

        val adminedChannelsRes = admined.getAdminedChannels()
        assertTrue(adminedChannelsRes is Result.Success)
        assertEquals(listOf(777L), (adminedChannelsRes as Result.Success).data)

        fakeRepo.shouldFail = true
        val failResult = getStars(100L)
        assertTrue(failResult is Result.Failure)
    }

    @Test
    fun testViewModelFlowAndEvents() = runTest {
        val fakeRepo = FakeBotStarsRepository().apply {
            starsStats = BotStarsRevenueStatsModel(
                dialogId = 200L,
                status = BotStarsRevenueStatusModel(currentBalance = 1500L, availableBalance = 1000L)
            )
            transactions.add(BotStarsTransactionModel("txA", 500L, 1700000L, 200L))
            connectedBots.add(ConnectedBotStarRefModel(botId = 555L, revenue = 800L))
        }

        val viewModel = BotStarsViewModel(
            observeBotStarsStatsUseCase = ObserveBotStarsStatsUseCase(fakeRepo),
            getBotStarsStatsUseCase = GetBotStarsStatsUseCase(fakeRepo),
            observeTonStatsUseCase = ObserveTonStatsUseCase(fakeRepo),
            getTonStatsUseCase = GetTonStatsUseCase(fakeRepo),
            observeBotTransactionsUseCase = ObserveBotTransactionsUseCase(fakeRepo),
            loadBotTransactionsUseCase = LoadBotTransactionsUseCase(fakeRepo),
            observeConnectedStarBotsUseCase = ObserveConnectedStarBotsUseCase(fakeRepo),
            loadConnectedStarBotsUseCase = LoadConnectedStarBotsUseCase(fakeRepo),
            loadSuggestedStarBotsUseCase = LoadSuggestedStarBotsUseCase(fakeRepo),
            getAdminedBotsAndChannelsUseCase = GetAdminedBotsAndChannelsUseCase(fakeRepo)
        )

        viewModel.onEvent(BotStarsEvent.SetDialogId(200L))
        fakeRepo.emitStars(fakeRepo.starsStats)
        fakeRepo.emitTransactions()
        fakeRepo.emitConnectedBots()
        advanceUntilIdle()

        assertEquals(200L, viewModel.uiState.value.dialogId)
        assertEquals(1500L, viewModel.uiState.value.currentBalance)
        assertEquals(1000L, viewModel.uiState.value.availableBalance)
        assertEquals(1, viewModel.uiState.value.transactions.size)
        assertTrue(viewModel.uiState.value.hasTransactions)
        assertEquals(1, viewModel.uiState.value.connectedBotsCount)

        // Select transaction type
        viewModel.onEvent(BotStarsEvent.SelectTransactionType(BotStarsTransactionType.INCOMING))
        assertEquals(BotStarsTransactionType.INCOMING, viewModel.uiState.value.selectedTransactionType)

        // Refresh stats
        viewModel.onEvent(BotStarsEvent.RefreshStats(force = true))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)

        // Error handling
        fakeRepo.shouldFail = true
        viewModel.onEvent(BotStarsEvent.RefreshStats(force = true))
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.onEvent(BotStarsEvent.ClearError)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testAccountFeatureContainerIntegration() = runTest {
        val container = AccountFeatureContainer.get(0)
        val fakeRepo = FakeBotStarsRepository().apply {
            starsStats = BotStarsRevenueStatsModel(
                dialogId = 300L,
                status = BotStarsRevenueStatusModel(currentBalance = 777L)
            )
        }
        container.botStarsRepository = fakeRepo

        assertEquals(fakeRepo, container.botStarsRepository)
        val res = container.getBotStarsStatsUseCase(300L)
        assertTrue(res is Result.Success)
        assertEquals(777L, (res as Result.Success).data?.currentBalance)

        val vm = container.createBotStarsViewModel()
        assertNotNull(vm)
    }
}
