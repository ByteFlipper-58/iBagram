package org.telegram.messenger.feature.botguard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
import org.telegram.messenger.feature.botguard.data.mapper.BotGuardMapper
import org.telegram.messenger.feature.botguard.data.repository.LegacyBotGuardRepository
import org.telegram.messenger.feature.botguard.domain.model.BotGuardBulletinType
import org.telegram.messenger.feature.botguard.domain.model.BotGuardDecisionResult
import org.telegram.messenger.feature.botguard.domain.model.BotGuardDecisionStatus
import org.telegram.messenger.feature.botguard.domain.model.BotGuardLaunchDecision
import org.telegram.messenger.feature.botguard.domain.usecase.ClearAllGuardBotSessionsUseCase
import org.telegram.messenger.feature.botguard.domain.usecase.CloseGuardBotSessionUseCase
import org.telegram.messenger.feature.botguard.domain.usecase.DetermineGuardBotLaunchFlowUseCase
import org.telegram.messenger.feature.botguard.domain.usecase.FormatGuardBotBulletinUseCase
import org.telegram.messenger.feature.botguard.domain.usecase.GetAllActiveGuardBotSessionsUseCase
import org.telegram.messenger.feature.botguard.domain.usecase.GetGuardBotSessionUseCase
import org.telegram.messenger.feature.botguard.domain.usecase.IsGuardBotConfirmationNeededUseCase
import org.telegram.messenger.feature.botguard.domain.usecase.MapJoinChatBotResultUseCase
import org.telegram.messenger.feature.botguard.domain.usecase.ObserveGuardBotDecisionsUseCase
import org.telegram.messenger.feature.botguard.domain.usecase.ObserveGuardBotStateUseCase
import org.telegram.messenger.feature.botguard.domain.usecase.RegisterGuardBotSessionUseCase
import org.telegram.messenger.feature.botguard.domain.usecase.SetGuardBotConfirmationShownUseCase
import org.telegram.messenger.feature.botguard.presentation.BotGuardEvent
import org.telegram.messenger.feature.botguard.presentation.BotGuardViewModel
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class BotGuardDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testIsGuardBotConfirmationNeededUseCase() {
        val repository = LegacyBotGuardRepository(0)
        val isConfirmationNeeded = IsGuardBotConfirmationNeededUseCase(repository)

        val botId = 123456L

        // Initially not confirmed and not whitelisted -> confirmation needed
        assertTrue(isConfirmationNeeded(botId))

        // Set confirmed -> not needed
        repository.setConfirmationShown(botId, true)
        assertFalse(isConfirmationNeeded(botId))

        // Reset confirmed, set whitelisted -> not needed
        repository.setConfirmationShown(botId, false)
        repository.setBotWhitelistedInMemory(botId, true)
        assertFalse(isConfirmationNeeded(botId))
    }

    @Test
    fun testDetermineGuardBotLaunchFlowUseCase() {
        val repository = LegacyBotGuardRepository(0)
        val isConfirmationNeeded = IsGuardBotConfirmationNeededUseCase(repository)
        val determineLaunchFlow = DetermineGuardBotLaunchFlowUseCase(isConfirmationNeeded)

        val dialogId = -100123456L
        val botId = 9999L
        val queryId = 8888L

        // When alreadyConfirmed is true -> LaunchDirectly
        val directResult = determineLaunchFlow(dialogId, botId, queryId, alreadyConfirmed = true)
        assertTrue(directResult is BotGuardLaunchDecision.LaunchDirectly)

        // When not confirmed and not whitelisted -> PromptConfirmation
        val promptResult = determineLaunchFlow(dialogId, botId, queryId, alreadyConfirmed = false)
        assertTrue(promptResult is BotGuardLaunchDecision.PromptConfirmation)
        val prompt = promptResult as BotGuardLaunchDecision.PromptConfirmation
        assertEquals(dialogId, prompt.dialogId)
        assertEquals(botId, prompt.guardBotId)
        assertEquals(queryId, prompt.queryId)

        // When confirmed -> LaunchDirectly
        repository.setConfirmationShown(botId, true)
        val afterConfirmResult = determineLaunchFlow(dialogId, botId, queryId, alreadyConfirmed = false)
        assertTrue(afterConfirmResult is BotGuardLaunchDecision.LaunchDirectly)
    }

    @Test
    fun testSessionRegistrationAndRetrieval() {
        val repository = LegacyBotGuardRepository(0)
        val registerSession = RegisterGuardBotSessionUseCase(repository)
        val getSession = GetGuardBotSessionUseCase(repository)
        val getAllActive = GetAllActiveGuardBotSessionsUseCase(repository)
        val closeSession = CloseGuardBotSessionUseCase(repository)
        val clearAll = ClearAllGuardBotSessionsUseCase(repository)

        val dialogId = -100987654L
        val botId = 5555L
        val queryId = 7777L

        val session = registerSession(dialogId, botId, queryId, isConfirmed = true)
        assertEquals(dialogId, session.dialogId)
        assertEquals(botId, session.guardBotId)
        assertEquals(queryId, session.queryId)
        assertTrue(session.isConfirmed)

        val fetched = getSession(queryId)
        assertNotNull(fetched)
        assertEquals(botId, fetched?.guardBotId)

        val all = getAllActive()
        assertEquals(1, all.size)

        // Close session with Approved status
        val decision = closeSession(dialogId, queryId, BotGuardDecisionStatus.Approved)
        assertEquals(dialogId, decision.dialogId)
        assertEquals(botId, decision.guardBotId)
        assertEquals(queryId, decision.queryId)
        assertEquals(BotGuardDecisionStatus.Approved, decision.status)

        assertNull(getSession(queryId))
        assertEquals(0, getAllActive().size)

        // Register two sessions and clear
        registerSession(dialogId, botId, 1001L, true)
        registerSession(dialogId, botId, 1002L, true)
        assertEquals(2, getAllActive().size)

        clearAll()
        assertEquals(0, getAllActive().size)
    }

    @Test
    fun testMapJoinChatBotResult() {
        val mapUseCase = MapJoinChatBotResultUseCase()

        val approved = mapUseCase(0xAE152A69.toInt())
        assertTrue(approved is BotGuardDecisionStatus.Approved)

        val declined = mapUseCase(0x0EFA0194.toInt())
        assertTrue(declined is BotGuardDecisionStatus.Declined)

        val queued = mapUseCase(0x98A3A840.toInt())
        assertTrue(queued is BotGuardDecisionStatus.Queued)

        val webView = mapUseCase(0xD6E3B813.toInt(), "https://t.me/guardbot")
        assertTrue(webView is BotGuardDecisionStatus.WebView)
        assertEquals("https://t.me/guardbot", (webView as BotGuardDecisionStatus.WebView).url)

        val unknown = mapUseCase(0x12345678)
        assertTrue(unknown is BotGuardDecisionStatus.Unknown)

        // Test mapper with TLRPC objects
        val tlApproved = TLRPC.TL_joinChatBotResultApproved()
        assertEquals(BotGuardDecisionStatus.Approved, BotGuardMapper.mapJoinChatBotResult(tlApproved))

        val tlDeclined = TLRPC.TL_joinChatBotResultDeclined()
        assertEquals(BotGuardDecisionStatus.Declined, BotGuardMapper.mapJoinChatBotResult(tlDeclined))

        val tlQueued = TLRPC.TL_joinChatBotResultQueued()
        assertEquals(BotGuardDecisionStatus.Queued, BotGuardMapper.mapJoinChatBotResult(tlQueued))

        val tlWebView = TLRPC.TL_joinChatBotResultWebView()
        tlWebView.url = "https://example.com/verify"
        val mappedWebView = BotGuardMapper.mapJoinChatBotResult(tlWebView)
        assertTrue(mappedWebView is BotGuardDecisionStatus.WebView)
        assertEquals("https://example.com/verify", (mappedWebView as BotGuardDecisionStatus.WebView).url)
    }

    @Test
    fun testFormatGuardBotBulletin() {
        val formatUseCase = FormatGuardBotBulletinUseCase()
        val chatName = "Secret Channel"

        val approvedDecision = BotGuardDecisionResult(
            dialogId = 100L,
            guardBotId = 200L,
            queryId = 300L,
            status = BotGuardDecisionStatus.Approved
        )
        val approvedInfo = formatUseCase(approvedDecision, chatName)
        assertEquals(BotGuardBulletinType.APPROVED, approvedInfo.type)
        assertEquals("GuardBotJoinRequestApproved", approvedInfo.messageKey)
        assertEquals(chatName, approvedInfo.chatName)

        val declinedDecision = BotGuardDecisionResult(
            dialogId = 100L,
            guardBotId = 200L,
            queryId = 300L,
            status = BotGuardDecisionStatus.Declined
        )
        val declinedInfo = formatUseCase(declinedDecision, chatName)
        assertEquals(BotGuardBulletinType.DECLINED, declinedInfo.type)
        assertEquals("GuardBotJoinRequestDeclined", declinedInfo.messageKey)

        val queuedDecision = BotGuardDecisionResult(
            dialogId = 100L,
            guardBotId = 200L,
            queryId = 300L,
            status = BotGuardDecisionStatus.Queued
        )
        val queuedInfo = formatUseCase(queuedDecision, chatName)
        assertEquals(BotGuardBulletinType.QUEUED, queuedInfo.type)
        assertEquals("GuardBotJoinRequestQueued", queuedInfo.messageKey)

        val dismissedDecision = BotGuardDecisionResult(
            dialogId = 100L,
            guardBotId = 200L,
            queryId = 300L,
            status = BotGuardDecisionStatus.Dismissed
        )
        val dismissedInfo = formatUseCase(dismissedDecision, chatName)
        assertEquals(BotGuardBulletinType.NONE, dismissedInfo.type)
        assertEquals("", dismissedInfo.messageKey)
    }

    @Test
    fun testBotGuardViewModelFlow() = runTest(testDispatcher) {
        val repository = LegacyBotGuardRepository(0)
        val isConfirmationNeeded = IsGuardBotConfirmationNeededUseCase(repository)
        val determineLaunchFlow = DetermineGuardBotLaunchFlowUseCase(isConfirmationNeeded)
        val registerSession = RegisterGuardBotSessionUseCase(repository)
        val closeSession = CloseGuardBotSessionUseCase(repository)
        val setConfirmationShown = SetGuardBotConfirmationShownUseCase(repository)
        val observeDecisions = ObserveGuardBotDecisionsUseCase(repository)
        val observeState = ObserveGuardBotStateUseCase(repository)
        val formatBulletin = FormatGuardBotBulletinUseCase()

        val viewModel = BotGuardViewModel(
            determineLaunchFlowUseCase = determineLaunchFlow,
            registerSessionUseCase = registerSession,
            closeSessionUseCase = closeSession,
            setConfirmationShownUseCase = setConfirmationShown,
            observeDecisionsUseCase = observeDecisions,
            observeStateUseCase = observeState,
            formatBulletinUseCase = formatBulletin
        )

        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.activeSessions.size)
        assertNull(viewModel.uiState.value.promptConfirmationSession)

        val dialogId = -100444L
        val botId = 1111L
        val queryId = 2222L

        // 1. Request open for unconfirmed bot -> prompt confirmation
        viewModel.onEvent(BotGuardEvent.RequestOpenGuardBot(dialogId, botId, queryId))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.promptConfirmationSession)
        assertEquals(botId, viewModel.uiState.value.promptConfirmationSession?.guardBotId)
        assertNull(viewModel.uiState.value.webViewToLaunch)

        // 2. Confirm launch -> sets confirmation shown, launches webview
        viewModel.onEvent(BotGuardEvent.ConfirmLaunch(dialogId, botId, queryId))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.promptConfirmationSession)
        assertNotNull(viewModel.uiState.value.webViewToLaunch)
        assertEquals(queryId, viewModel.uiState.value.webViewToLaunch?.queryId)
        assertTrue(repository.isConfirmationShown(botId))

        // 3. Decision received -> closes session and sets bulletin
        viewModel.onEvent(
            BotGuardEvent.DecisionReceived(
                dialogId = dialogId,
                guardBotId = botId,
                queryId = queryId,
                status = BotGuardDecisionStatus.Approved,
                chatName = "Test Chat"
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val bulletin = viewModel.uiState.value.bulletinToShow
        assertNotNull(bulletin)
        assertEquals(BotGuardBulletinType.APPROVED, bulletin?.type)
        assertEquals("Test Chat", bulletin?.chatName)
        assertEquals(0, viewModel.uiState.value.activeSessions.size)

        // 4. Clear pending actions
        viewModel.onEvent(BotGuardEvent.ClearPendingActions)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.bulletinToShow)
        assertNull(viewModel.uiState.value.webViewToLaunch)
    }
}
