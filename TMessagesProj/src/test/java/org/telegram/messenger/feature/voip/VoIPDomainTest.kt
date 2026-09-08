package org.telegram.messenger.feature.voip

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.voip.data.mapper.CallMapper
import org.telegram.messenger.feature.voip.domain.model.CallModel
import org.telegram.messenger.feature.voip.domain.model.CallState
import org.telegram.messenger.feature.voip.domain.repository.VoIPRepository
import org.telegram.messenger.feature.voip.domain.usecase.AcceptCallUseCase
import org.telegram.messenger.feature.voip.domain.usecase.DeclineCallUseCase
import org.telegram.messenger.feature.voip.domain.usecase.GetCurrentCallUseCase
import org.telegram.messenger.feature.voip.domain.usecase.HangUpCallUseCase
import org.telegram.messenger.feature.voip.domain.usecase.ObserveCurrentCallUseCase
import org.telegram.messenger.feature.voip.domain.usecase.StartCallUseCase
import org.telegram.messenger.feature.voip.domain.usecase.ToggleMuteUseCase
import org.telegram.messenger.feature.voip.domain.usecase.ToggleSpeakerphoneUseCase
import org.telegram.messenger.feature.voip.presentation.CallEvent
import org.telegram.messenger.feature.voip.presentation.CallUiState
import org.telegram.messenger.feature.voip.presentation.CallViewModel
import org.telegram.messenger.voip.VoIPService
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class VoIPDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeVoIPRepository : VoIPRepository {
        val callFlow = MutableSharedFlow<CallModel?>(replay = 1)
        var currentCall: CallModel? = null
            set(value) {
                field = value
                callFlow.tryEmit(value)
            }
        var shouldSucceed = true

        init {
            callFlow.tryEmit(currentCall)
        }

        override fun observeCurrentCall(): Flow<CallModel?> = callFlow.asSharedFlow()

        override suspend fun getCurrentCall(): CallModel? = currentCall

        override suspend fun startCall(userId: Long, isVideo: Boolean): Result<Unit> {
            return if (shouldSucceed) {
                currentCall = CallModel(
                    userId = userId,
                    userName = "User $userId",
                    isOutgoing = true,
                    isVideo = isVideo,
                    state = CallState.REQUESTING
                )
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Failed to start call"))
            }
        }

        override suspend fun acceptCall(): Result<Unit> {
            return if (shouldSucceed) {
                currentCall = currentCall?.copy(state = CallState.ACTIVE)
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Failed to accept call"))
            }
        }

        override suspend fun declineCall(): Result<Unit> {
            return if (shouldSucceed) {
                currentCall = currentCall?.copy(state = CallState.ENDED)
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Failed to decline call"))
            }
        }

        override suspend fun hangUp(): Result<Unit> {
            return if (shouldSucceed) {
                currentCall = currentCall?.copy(state = CallState.ENDED)
                Result.success(Unit)
            } else {
                Result.failure(AppError.Generic("Failed to hang up"))
            }
        }

        override suspend fun toggleMute(): Result<Boolean> {
            return if (shouldSucceed) {
                val newMute = !(currentCall?.isMuted ?: false)
                currentCall = currentCall?.copy(isMuted = newMute)
                Result.success(newMute)
            } else {
                Result.failure(AppError.Generic("Failed to toggle mute"))
            }
        }

        override suspend fun toggleSpeakerphone(): Result<Boolean> {
            return if (shouldSucceed) {
                val newSpeaker = !(currentCall?.isSpeakerphoneOn ?: false)
                currentCall = currentCall?.copy(isSpeakerphoneOn = newSpeaker)
                Result.success(newSpeaker)
            } else {
                Result.failure(AppError.Generic("Failed to toggle speakerphone"))
            }
        }
    }

    @Test
    fun testCallMapperStateMappings() {
        assertEquals(CallState.REQUESTING, CallMapper.mapState(VoIPService.STATE_REQUESTING))
        assertEquals(CallState.WAITING_INCOMING, CallMapper.mapState(VoIPService.STATE_WAITING_INCOMING))
        assertEquals(CallState.RINGING, CallMapper.mapState(VoIPService.STATE_RINGING))
        assertEquals(CallState.CONNECTING, CallMapper.mapState(VoIPService.STATE_WAITING))
        assertEquals(CallState.CONNECTING, CallMapper.mapState(VoIPService.STATE_WAIT_INIT))
        assertEquals(CallState.CONNECTING, CallMapper.mapState(VoIPService.STATE_WAIT_INIT_ACK))
        assertEquals(CallState.CONNECTING, CallMapper.mapState(VoIPService.STATE_CREATING))
        assertEquals(CallState.EXCHANGING_KEYS, CallMapper.mapState(VoIPService.STATE_EXCHANGING_KEYS))
        assertEquals(CallState.ACTIVE, CallMapper.mapState(VoIPService.STATE_ESTABLISHED))
        assertEquals(CallState.RECONNECTING, CallMapper.mapState(VoIPService.STATE_RECONNECTING))
        assertEquals(CallState.BUSY, CallMapper.mapState(VoIPService.STATE_BUSY))
        assertEquals(CallState.ENDED, CallMapper.mapState(VoIPService.STATE_HANGING_UP))
        assertEquals(CallState.ENDED, CallMapper.mapState(VoIPService.STATE_ENDED))
        assertEquals(CallState.FAILED, CallMapper.mapState(VoIPService.STATE_FAILED))
        assertEquals(CallState.IDLE, CallMapper.mapState(999))
    }

    @Test
    fun testCallMapperToDomain() {
        val user = TLRPC.TL_user().apply {
            id = 12345L
            first_name = "John"
            last_name = "Doe"
        }
        val model = CallMapper.toDomain(
            user = user,
            isOutgoing = true,
            isVideo = false,
            callStateInt = VoIPService.STATE_ESTABLISHED,
            durationSeconds = 42L,
            isMuted = false,
            isSpeakerphoneOn = true
        )
        assertEquals(12345L, model.userId)
        assertEquals("John Doe", model.userName)
        assertTrue(model.isOutgoing)
        assertFalse(model.isVideo)
        assertEquals(CallState.ACTIVE, model.state)
        assertEquals(42L, model.durationSeconds)
        assertFalse(model.isMuted)
        assertTrue(model.isSpeakerphoneOn)
    }

    @Test
    fun testCallMapperNullService() {
        assertNull(CallMapper.toDomain(null))
    }

    @Test
    fun testStartCallUseCaseSuccess() = runTest {
        val repo = FakeVoIPRepository()
        val useCase = StartCallUseCase(repo)

        val result = useCase(userId = 777L, isVideo = true)
        assertTrue(result is Result.Success)
        val currentCall = repo.getCurrentCall()
        assertNotNull(currentCall)
        assertEquals(777L, currentCall?.userId)
        assertTrue(currentCall?.isVideo == true)
        assertEquals(CallState.REQUESTING, currentCall?.state)
    }

    @Test
    fun testStartCallUseCaseFailure() = runTest {
        val repo = FakeVoIPRepository().apply { shouldSucceed = false }
        val useCase = StartCallUseCase(repo)

        val result = useCase(userId = 777L, isVideo = false)
        assertTrue(result is Result.Failure)
        assertEquals("Failed to start call", (result as Result.Failure).error.message)
    }

    @Test
    fun testAcceptCallUseCase() = runTest {
        val repo = FakeVoIPRepository()
        repo.currentCall = CallModel(userId = 123L, state = CallState.WAITING_INCOMING)
        val useCase = AcceptCallUseCase(repo)

        val result = useCase()
        assertTrue(result is Result.Success)
        assertEquals(CallState.ACTIVE, repo.getCurrentCall()?.state)
    }

    @Test
    fun testDeclineCallUseCase() = runTest {
        val repo = FakeVoIPRepository()
        repo.currentCall = CallModel(userId = 123L, state = CallState.WAITING_INCOMING)
        val useCase = DeclineCallUseCase(repo)

        val result = useCase()
        assertTrue(result is Result.Success)
        assertEquals(CallState.ENDED, repo.getCurrentCall()?.state)
    }

    @Test
    fun testHangUpCallUseCase() = runTest {
        val repo = FakeVoIPRepository()
        repo.currentCall = CallModel(userId = 123L, state = CallState.ACTIVE)
        val useCase = HangUpCallUseCase(repo)

        val result = useCase()
        assertTrue(result is Result.Success)
        assertEquals(CallState.ENDED, repo.getCurrentCall()?.state)
    }

    @Test
    fun testToggleMuteUseCase() = runTest {
        val repo = FakeVoIPRepository()
        repo.currentCall = CallModel(userId = 123L, isMuted = false)
        val useCase = ToggleMuteUseCase(repo)

        val result1 = useCase()
        assertTrue(result1 is Result.Success)
        assertTrue((result1 as Result.Success).data)
        assertTrue(repo.getCurrentCall()?.isMuted == true)

        val result2 = useCase()
        assertTrue(result2 is Result.Success)
        assertFalse((result2 as Result.Success).data)
        assertFalse(repo.getCurrentCall()?.isMuted == true)
    }

    @Test
    fun testToggleSpeakerphoneUseCase() = runTest {
        val repo = FakeVoIPRepository()
        repo.currentCall = CallModel(userId = 123L, isSpeakerphoneOn = false)
        val useCase = ToggleSpeakerphoneUseCase(repo)

        val result1 = useCase()
        assertTrue(result1 is Result.Success)
        assertTrue((result1 as Result.Success).data)
        assertTrue(repo.getCurrentCall()?.isSpeakerphoneOn == true)

        val result2 = useCase()
        assertTrue(result2 is Result.Success)
        assertFalse((result2 as Result.Success).data)
        assertFalse(repo.getCurrentCall()?.isSpeakerphoneOn == true)
    }

    @Test
    fun testObserveCurrentCallUseCase() = runTest {
        val repo = FakeVoIPRepository()
        val useCase = ObserveCurrentCallUseCase(repo)

        repo.currentCall = CallModel(userId = 100L, state = CallState.ACTIVE)
        val observed = useCase().first()
        assertEquals(100L, observed?.userId)
        assertEquals(CallState.ACTIVE, observed?.state)
    }

    @Test
    fun testCallViewModelLifecycleAndEvents() = runTest {
        val repo = FakeVoIPRepository()
        val viewModel = CallViewModel(
            observeCurrentCallUseCase = ObserveCurrentCallUseCase(repo),
            getCurrentCallUseCase = GetCurrentCallUseCase(repo),
            startCallUseCase = StartCallUseCase(repo),
            acceptCallUseCase = AcceptCallUseCase(repo),
            declineCallUseCase = DeclineCallUseCase(repo),
            hangUpCallUseCase = HangUpCallUseCase(repo),
            toggleMuteUseCase = ToggleMuteUseCase(repo),
            toggleSpeakerphoneUseCase = ToggleSpeakerphoneUseCase(repo)
        )

        val events = mutableListOf<CallEvent>()
        val eventsJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is CallUiState.Idle)

        // Start call
        viewModel.startCall(userId = 555L, isVideo = false)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is CallUiState.Active)
        assertEquals(555L, (viewModel.uiState.value as CallUiState.Active).call.userId)

        // Accept call event
        viewModel.acceptCall()
        advanceUntilIdle()
        assertTrue(events.contains(CallEvent.CallAccepted))

        // Toggle mute event
        viewModel.toggleMute()
        advanceUntilIdle()
        assertTrue(events.any { it is CallEvent.MuteToggled && it.isMuted })

        // Toggle speakerphone event
        viewModel.toggleSpeakerphone()
        advanceUntilIdle()
        assertTrue(events.any { it is CallEvent.SpeakerphoneToggled && it.isOn })

        // Hang up call event
        viewModel.hangUp()
        advanceUntilIdle()
        assertTrue(events.contains(CallEvent.CallEnded))
        assertTrue(viewModel.uiState.value is CallUiState.Ended)

        eventsJob.cancel()
    }

    @Test
    fun testCallViewModelErrorEvents() = runTest {
        val repo = FakeVoIPRepository().apply { shouldSucceed = false }
        val viewModel = CallViewModel(
            observeCurrentCallUseCase = ObserveCurrentCallUseCase(repo),
            getCurrentCallUseCase = GetCurrentCallUseCase(repo),
            startCallUseCase = StartCallUseCase(repo),
            acceptCallUseCase = AcceptCallUseCase(repo),
            declineCallUseCase = DeclineCallUseCase(repo),
            hangUpCallUseCase = HangUpCallUseCase(repo),
            toggleMuteUseCase = ToggleMuteUseCase(repo),
            toggleSpeakerphoneUseCase = ToggleSpeakerphoneUseCase(repo)
        )

        val events = mutableListOf<CallEvent>()
        val eventsJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }

        advanceUntilIdle()

        viewModel.startCall(userId = 123L, isVideo = false)
        advanceUntilIdle()
        assertTrue(events.any { it is CallEvent.ShowError && it.message == "Failed to start call" })

        viewModel.acceptCall()
        advanceUntilIdle()
        assertTrue(events.any { it is CallEvent.ShowError && it.message == "Failed to accept call" })

        viewModel.hangUp()
        advanceUntilIdle()
        assertTrue(events.any { it is CallEvent.ShowError && it.message == "Failed to hang up" })

        eventsJob.cancel()
    }

    @Test
    fun testAccountFeatureContainerWiring() {
        val container = AccountFeatureContainer.get(0)
        val customRepo = FakeVoIPRepository()
        container.voipRepository = customRepo

        assertEquals(customRepo, container.voipRepository)
        assertNotNull(container.observeCurrentCallUseCase)
        assertNotNull(container.getCurrentCallUseCase)
        assertNotNull(container.startCallUseCase)
        assertNotNull(container.acceptCallUseCase)
        assertNotNull(container.declineCallUseCase)
        assertNotNull(container.hangUpCallUseCase)
        assertNotNull(container.toggleMuteUseCase)
        assertNotNull(container.toggleSpeakerphoneUseCase)
        assertNotNull(container.callViewModel)

        AccountFeatureContainer.reset(0)
    }
}
