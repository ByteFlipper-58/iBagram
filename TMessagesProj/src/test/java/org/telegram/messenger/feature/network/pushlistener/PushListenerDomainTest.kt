package org.telegram.messenger.feature.network.pushlistener

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.network.pushlistener.data.repository.LegacyPushListenerRepository
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushActionType
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushDecryptStatus
import org.telegram.messenger.feature.network.pushlistener.domain.model.PushType
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.DeterminePushActionTypeUseCase
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.GetPushListenerStateUseCase
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.ObserveIncomingPushesUseCase
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.ObservePushListenerStateUseCase
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.ParsePushJsonPayloadUseCase
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.ProcessIncomingPushUseCase
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.RegisterPushListenerTokenUseCase
import org.telegram.messenger.feature.network.pushlistener.domain.usecase.TogglePushListeningUseCase
import org.telegram.messenger.feature.network.pushlistener.presentation.PushListenerEvent
import org.telegram.messenger.feature.network.pushlistener.presentation.PushListenerViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class PushListenerDomainTest {

    private lateinit var repository: LegacyPushListenerRepository
    private lateinit var observeStateUseCase: ObservePushListenerStateUseCase
    private lateinit var observeIncomingPushesUseCase: ObserveIncomingPushesUseCase
    private lateinit var getStateUseCase: GetPushListenerStateUseCase
    private lateinit var processPushUseCase: ProcessIncomingPushUseCase
    private lateinit var registerTokenUseCase: RegisterPushListenerTokenUseCase
    private lateinit var toggleListeningUseCase: TogglePushListeningUseCase
    private lateinit var determineActionTypeUseCase: DeterminePushActionTypeUseCase
    private lateinit var parsePayloadUseCase: ParsePushJsonPayloadUseCase

    @Before
    fun setUp() {
        determineActionTypeUseCase = DeterminePushActionTypeUseCase()
        parsePayloadUseCase = ParsePushJsonPayloadUseCase(determineActionTypeUseCase)
        repository = LegacyPushListenerRepository(
            currentAccount = 0,
            ioDispatcher = Dispatchers.Unconfined,
            parsePayload = parsePayloadUseCase
        )
        observeStateUseCase = ObservePushListenerStateUseCase(repository)
        observeIncomingPushesUseCase = ObserveIncomingPushesUseCase(repository)
        getStateUseCase = GetPushListenerStateUseCase(repository)
        processPushUseCase = ProcessIncomingPushUseCase(repository)
        registerTokenUseCase = RegisterPushListenerTokenUseCase(repository)
        toggleListeningUseCase = TogglePushListeningUseCase(repository)
    }

    @Test
    fun testDeterminePushActionTypeUseCase() {
        assertEquals(PushActionType.DC_UPDATE, determineActionTypeUseCase("DC_UPDATE"))
        assertEquals(PushActionType.MESSAGE_ANNOUNCEMENT, determineActionTypeUseCase("MESSAGE_ANNOUNCEMENT"))
        assertEquals(PushActionType.SESSION_REVOKE, determineActionTypeUseCase("SESSION_REVOKE"))
        assertEquals(PushActionType.GEO_LIVE_PENDING, determineActionTypeUseCase("GEO_LIVE_PENDING"))
        assertEquals(PushActionType.OAUTH_REQUEST, determineActionTypeUseCase("OAUTH_REQUEST"))
        assertEquals(PushActionType.VOIP_CALL, determineActionTypeUseCase("CONF_CALL_REQUEST"))
        assertEquals(PushActionType.VOIP_CALL, determineActionTypeUseCase("CONF_VIDEOCALL_REQUEST"))
        assertEquals(PushActionType.READ_HISTORY, determineActionTypeUseCase("READ_HISTORY"))
        assertEquals(PushActionType.READ_STORIES, determineActionTypeUseCase("READ_STORIES"))
        assertEquals(PushActionType.STORY_DELETED, determineActionTypeUseCase("STORY_DELETED"))
        assertEquals(PushActionType.MESSAGE_DELETED, determineActionTypeUseCase("MESSAGE_DELETED"))
        assertEquals(PushActionType.READ_REACTION, determineActionTypeUseCase("READ_REACTION"))
        assertEquals(PushActionType.NEW_MESSAGE, determineActionTypeUseCase("CHAT_MESSAGE"))
        assertEquals(PushActionType.NEW_MESSAGE, determineActionTypeUseCase("ENCRYPTED_MESSAGE"))
        assertEquals(PushActionType.UNKNOWN, determineActionTypeUseCase("NON_EXISTENT_KEY"))
    }

    @Test
    fun testParsePushJsonPayloadUseCase() {
        val rawJson = """
            {
                "loc_key": "CHAT_MESSAGE",
                "custom": {
                    "channel_id": 123456789,
                    "msg_id": 42,
                    "silent": 1,
                    "schedule": 0,
                    "topic_id": 7
                }
            }
        """.trimIndent()

        val payload = parsePayloadUseCase(
            pushType = PushType.FIREBASE,
            jsonString = rawJson,
            timestamp = 1700000000000L
        )

        assertEquals(PushType.FIREBASE, payload.pushType)
        assertEquals("CHAT_MESSAGE", payload.locKey)
        assertEquals(PushActionType.NEW_MESSAGE, payload.actionType)
        assertEquals(-123456789L, payload.dialogId)
        assertEquals(123456789L, payload.channelId)
        assertEquals(42, payload.messageId)
        assertEquals(7, payload.topicId)
        assertTrue(payload.isSilent)
        assertFalse(payload.isScheduled)
        assertEquals(1700000000000L, payload.receiveTimeMs)
    }

    @Test
    fun testRegisterPushTokenAndState() = runTest {
        registerTokenUseCase(PushType.FIREBASE, "fcm_test_token_123")
        registerTokenUseCase(PushType.HUAWEI, "hcm_test_token_456")

        val state = getStateUseCase()
        assertEquals("fcm_test_token_123", state.registeredTokens[PushType.FIREBASE])
        assertEquals("hcm_test_token_456", state.registeredTokens[PushType.HUAWEI])
    }

    @Test
    fun testProcessPushAndObserveFlow() = runTest {
        val pushJson = """
            {
                "loc_key": "CONF_CALL_REQUEST",
                "custom": {
                    "from_id": 999888,
                    "call_id": 555
                }
            }
        """.trimIndent()

        val result = processPushUseCase(PushType.FIREBASE, pushJson, 1700000005000L)
        assertTrue(result.isHandled)
        assertEquals(PushDecryptStatus.SUCCESS, result.status)
        assertNotNull(result.payload)
        assertEquals(PushActionType.VOIP_CALL, result.payload?.actionType)
        assertEquals(999888L, result.payload?.dialogId)

        val state = getStateUseCase()
        assertEquals(1, state.totalReceivedPushes)
        assertEquals(result.payload, state.lastProcessedPush)
    }

    @Test
    fun testPushListenerViewModelMviFlow() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val viewModel = PushListenerViewModel(
            observeStateUseCase = observeStateUseCase,
            processPushUseCase = processPushUseCase,
            registerTokenUseCase = registerTokenUseCase,
            toggleListeningUseCase = toggleListeningUseCase,
            scope = testScope
        )

        testScope.advanceUntilIdle()

        // Initial state
        assertTrue(viewModel.uiState.value.isListening)
        assertEquals(0, viewModel.uiState.value.totalReceived)

        // Register token
        viewModel.onEvent(PushListenerEvent.RegisterToken(PushType.FIREBASE, "fcm_token_xyz"))
        testScope.advanceUntilIdle()

        assertEquals("Token registered for FIREBASE", viewModel.uiState.value.infoMessage)
        assertEquals("fcm_token_xyz", viewModel.uiState.value.registeredTokens[PushType.FIREBASE])

        // Process push
        val samplePush = """
            {
                "loc_key": "DC_UPDATE",
                "custom": {
                    "dc": 2
                }
            }
        """.trimIndent()
        viewModel.onEvent(PushListenerEvent.ProcessPush(PushType.FIREBASE, samplePush))
        testScope.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.totalReceived)
        assertNotNull(viewModel.uiState.value.lastPush)
        assertEquals("DC_UPDATE", viewModel.uiState.value.lastPush?.actionType)
        assertEquals("Push processed: DC_UPDATE", viewModel.uiState.value.infoMessage)

        // Toggle listening
        viewModel.onEvent(PushListenerEvent.ToggleListening(false))
        testScope.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isListening)

        // Process push when disabled
        viewModel.onEvent(PushListenerEvent.ProcessPush(PushType.FIREBASE, samplePush))
        testScope.advanceUntilIdle()
        assertEquals("Push listener is disabled", viewModel.uiState.value.error)

        // Clear history
        viewModel.onEvent(PushListenerEvent.ClearHistory)
        testScope.advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.totalReceived)
        assertEquals(null, viewModel.uiState.value.lastPush)

        viewModel.onCleared()
    }
}
