package org.telegram.messenger.feature.sessions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.sessions.data.mapper.SessionMapper
import org.telegram.messenger.feature.sessions.domain.model.SessionModel
import org.telegram.messenger.feature.sessions.domain.model.SessionsListModel
import org.telegram.messenger.feature.sessions.domain.model.WebSessionModel
import org.telegram.messenger.feature.sessions.domain.repository.SessionsRepository
import org.telegram.messenger.feature.sessions.domain.usecase.AcceptQrLoginUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.GetSessionsUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.GetWebSessionsUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.LoadSessionsUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.LoadWebSessionsUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.ObserveSessionsUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.ObserveWebSessionsUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.SetSessionsTtlUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.TerminateAllOtherSessionsUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.TerminateAllWebSessionsUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.TerminateSessionUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.TerminateWebSessionUseCase
import org.telegram.messenger.feature.sessions.domain.usecase.UpdateSessionSettingsUseCase
import org.telegram.messenger.feature.sessions.presentation.SessionsEvent
import org.telegram.messenger.feature.sessions.presentation.SessionsViewModel
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account
import java.util.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class SessionsDomainTest {

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
    fun testSessionModelsAndAggregates() {
        val session = SessionModel(
            hash = 1001L,
            deviceModel = "Pixel 8 Pro",
            platform = "Android",
            systemVersion = "14.0",
            appName = "Telegram Android",
            appVersion = "10.14.0",
            dateCreated = 1700000000,
            dateActive = 1700050000,
            ip = "192.168.1.1",
            country = "Germany",
            region = "Berlin",
            isCurrent = true,
            isOfficialApp = true,
            isPasswordPending = false,
            acceptSecretChats = true,
            acceptCalls = true,
            canAcceptSecretChats = true,
            canAcceptCalls = true,
            isUnconfirmed = false
        )

        assertEquals(1001L, session.hash)
        assertEquals("Pixel 8 Pro", session.deviceModel)
        assertTrue(session.isCurrent)
        assertTrue(session.acceptSecretChats)
        assertTrue(session.canAcceptCalls)

        val webSession = WebSessionModel(
            hash = 2002L,
            botId = 9999L,
            domain = "web.telegram.org",
            browser = "Chrome",
            platform = "macOS",
            dateCreated = 1700001000,
            dateActive = 1700051000,
            ip = "10.0.0.1",
            region = "Munich"
        )

        assertEquals(2002L, webSession.hash)
        assertEquals(9999L, webSession.botId)
        assertEquals("web.telegram.org", webSession.domain)

        val listAggregate = SessionsListModel(
            currentSession = session,
            otherSessions = listOf(session.copy(hash = 1002L, isCurrent = false)),
            passwordPendingSessions = emptyList(),
            ttlDays = 183
        )

        assertEquals(1001L, listAggregate.currentSession?.hash)
        assertEquals(1, listAggregate.otherSessions.size)
        assertEquals(183, listAggregate.ttlDays)
    }

    @Test
    fun testSessionMapper() {
        val auth = TLRPC.TL_authorization().apply {
            hash = 5555L
            flags = 1 // is current
            official_app = true
            password_pending = false
            encrypted_requests_disabled = false
            call_requests_disabled = true
            unconfirmed = false
            device_model = "Samsung Galaxy S24"
            platform = "Android"
            system_version = "14"
            api_id = 2040 // official app without secret chats
            app_name = "Telegram"
            app_version = "10.9"
            date_created = 1690000000
            date_active = 1690010000
            ip = "1.2.3.4"
            country = "United States"
            region = "California"
        }

        val mapped = SessionMapper.mapSession(auth)
        assertEquals(5555L, mapped.hash)
        assertTrue(mapped.isCurrent)
        assertTrue(mapped.isOfficialApp)
        assertFalse(mapped.isPasswordPending)
        assertTrue(mapped.acceptSecretChats)
        assertFalse(mapped.acceptCalls)
        assertFalse(mapped.canAcceptSecretChats) // api_id == 2040
        assertTrue(mapped.canAcceptCalls)
        assertEquals("Samsung Galaxy S24", mapped.deviceModel)
        assertEquals("California", mapped.region)

        val webAuth = TLRPC.TL_webAuthorization().apply {
            hash = 7777L
            bot_id = 12345L
            domain = "fragment.com"
            browser = "Safari"
            platform = "iOS"
            date_created = 1690002000
            date_active = 1690022000
            ip = "4.3.2.1"
            region = "Frankfurt"
        }

        val mappedWeb = SessionMapper.mapWebSession(webAuth)
        assertEquals(7777L, mappedWeb.hash)
        assertEquals(12345L, mappedWeb.botId)
        assertEquals("fragment.com", mappedWeb.domain)
        assertEquals("Safari", mappedWeb.browser)

        val res = TL_account.authorizations().apply {
            authorization_ttl_days = 90
            authorizations.add(auth)
            authorizations.add(TLRPC.TL_authorization().apply {
                hash = 8888L
                flags = 0
                password_pending = true
                device_model = "Desktop"
            })
            authorizations.add(TLRPC.TL_authorization().apply {
                hash = 9999L
                flags = 0
                password_pending = false
                device_model = "Laptop"
            })
        }

        val listModel = SessionMapper.mapAuthorizations(res)
        assertEquals(90, listModel.ttlDays)
        assertNotNull(listModel.currentSession)
        assertEquals(5555L, listModel.currentSession?.hash)
        assertEquals(1, listModel.passwordPendingSessions.size)
        assertEquals(8888L, listModel.passwordPendingSessions[0].hash)
        assertEquals(1, listModel.otherSessions.size)
        assertEquals(9999L, listModel.otherSessions[0].hash)
    }

    @Test
    fun testUseCasesWithFakeRepository() = runBlocking {
        val fakeRepo = FakeSessionsRepository()

        val getUseCase = GetSessionsUseCase(fakeRepo)
        val terminateUseCase = TerminateSessionUseCase(fakeRepo)
        val ttlUseCase = SetSessionsTtlUseCase(fakeRepo)
        val updateSettingsUseCase = UpdateSessionSettingsUseCase(fakeRepo)
        val qrLoginUseCase = AcceptQrLoginUseCase(fakeRepo)

        val sessionsResult = getUseCase()
        assertTrue(sessionsResult is Result.Success)
        val list = (sessionsResult as Result.Success).data
        assertEquals(100L, list.currentSession?.hash)
        assertEquals(2, list.otherSessions.size)

        // Terminate other session
        val termResult = terminateUseCase(200L)
        assertTrue(termResult is Result.Success)
        val updatedList = (getUseCase() as Result.Success).data
        assertEquals(1, updatedList.otherSessions.size)
        assertNull(updatedList.otherSessions.find { it.hash == 200L })

        // Update TTL
        val ttlResult = ttlUseCase(365)
        assertTrue(ttlResult is Result.Success)
        assertEquals(365, (getUseCase() as Result.Success).data.ttlDays)

        // Update settings
        val settingsResult = updateSettingsUseCase(300L, acceptSecretChats = false, acceptCalls = false)
        assertTrue(settingsResult is Result.Success)
        val session300 = (getUseCase() as Result.Success).data.otherSessions.first { it.hash == 300L }
        assertFalse(session300.acceptSecretChats)
        assertFalse(session300.acceptCalls)

        // Accept QR login
        val qrResult = qrLoginUseCase.byToken("test_token".toByteArray())
        assertTrue(qrResult is Result.Success)
    }

    @Test
    fun testSessionsViewModelFlowAndEvents() = runBlocking {
        val fakeRepo = FakeSessionsRepository()

        val viewModel = SessionsViewModel(
            observeSessionsUseCase = ObserveSessionsUseCase(fakeRepo),
            observeWebSessionsUseCase = ObserveWebSessionsUseCase(fakeRepo),
            loadSessionsUseCase = LoadSessionsUseCase(fakeRepo),
            loadWebSessionsUseCase = LoadWebSessionsUseCase(fakeRepo),
            terminateSessionUseCase = TerminateSessionUseCase(fakeRepo),
            terminateAllOtherSessionsUseCase = TerminateAllOtherSessionsUseCase(fakeRepo),
            terminateWebSessionUseCase = TerminateWebSessionUseCase(fakeRepo),
            terminateAllWebSessionsUseCase = TerminateAllWebSessionsUseCase(fakeRepo),
            updateSessionSettingsUseCase = UpdateSessionSettingsUseCase(fakeRepo),
            setSessionsTtlUseCase = SetSessionsTtlUseCase(fakeRepo),
            acceptQrLoginUseCase = AcceptQrLoginUseCase(fakeRepo)
        )

        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(100L, state.currentSession?.hash)
        assertEquals(2, state.otherSessions.size)
        assertEquals(3, state.totalActiveSessionsCount)
        assertTrue(state.hasOtherSessions)
        assertEquals(1, state.webSessions.size)

        // Terminate session event
        viewModel.onEvent(SessionsEvent.TerminateSession(200L))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals(1, state.otherSessions.size)
        assertEquals("Session terminated", state.actionSuccessMessage)

        // Terminate all other sessions event
        viewModel.onEvent(SessionsEvent.TerminateAllOtherSessions)
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertTrue(state.otherSessions.isEmpty())
        assertFalse(state.hasOtherSessions)
        assertEquals(1, state.totalActiveSessionsCount) // only current

        // Set TTL event
        viewModel.onEvent(SessionsEvent.SetSessionsTtl(90))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals(90, state.ttlDays)

        // Accept QR link event
        val rawToken = Base64.getUrlEncoder().encodeToString("qr_sample_key".toByteArray())
        val qrLink = "tg://login?token=$rawToken"
        viewModel.onEvent(SessionsEvent.AcceptQrLoginByLink(qrLink))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals("QR login accepted", state.actionSuccessMessage)

        // Clear messages
        viewModel.onEvent(SessionsEvent.ClearMessages)
        state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertNull(state.actionSuccessMessage)
    }

    private class FakeSessionsRepository : SessionsRepository {
        private var listModel = SessionsListModel(
            currentSession = SessionModel(
                hash = 100L,
                deviceModel = "My Phone",
                platform = "Android",
                systemVersion = "14",
                appName = "Telegram",
                appVersion = "10.0",
                dateCreated = 1700000000,
                dateActive = 1700010000,
                ip = "127.0.0.1",
                country = "Local",
                region = "Local",
                isCurrent = true,
                isOfficialApp = true,
                isPasswordPending = false,
                acceptSecretChats = true,
                acceptCalls = true,
                canAcceptSecretChats = true,
                canAcceptCalls = true,
                isUnconfirmed = false
            ),
            otherSessions = listOf(
                SessionModel(
                    hash = 200L,
                    deviceModel = "Tablet",
                    platform = "Android",
                    systemVersion = "13",
                    appName = "Telegram",
                    appVersion = "10.0",
                    dateCreated = 1690000000,
                    dateActive = 1690010000,
                    ip = "127.0.0.2",
                    country = "Local",
                    region = "Local",
                    isCurrent = false,
                    isOfficialApp = true,
                    isPasswordPending = false,
                    acceptSecretChats = true,
                    acceptCalls = true,
                    canAcceptSecretChats = true,
                    canAcceptCalls = true,
                    isUnconfirmed = false
                ),
                SessionModel(
                    hash = 300L,
                    deviceModel = "Desktop",
                    platform = "Windows",
                    systemVersion = "11",
                    appName = "Telegram Desktop",
                    appVersion = "4.15",
                    dateCreated = 1680000000,
                    dateActive = 1680010000,
                    ip = "127.0.0.3",
                    country = "Local",
                    region = "Local",
                    isCurrent = false,
                    isOfficialApp = true,
                    isPasswordPending = false,
                    acceptSecretChats = true,
                    acceptCalls = true,
                    canAcceptSecretChats = true,
                    canAcceptCalls = true,
                    isUnconfirmed = false
                )
            ),
            passwordPendingSessions = emptyList(),
            ttlDays = 183
        )

        private var webList = listOf(
            WebSessionModel(
                hash = 500L,
                botId = 111L,
                domain = "web.telegram.org",
                browser = "Chrome",
                platform = "Windows",
                dateCreated = 1700000000,
                dateActive = 1700010000,
                ip = "127.0.0.1",
                region = "Local"
            )
        )

        private val sessionsFlow = MutableStateFlow(listModel)
        private val webSessionsFlow = MutableStateFlow(webList)

        override fun observeSessions(): Flow<SessionsListModel> = sessionsFlow.asStateFlow()

        override fun observeWebSessions(): Flow<List<WebSessionModel>> = webSessionsFlow.asStateFlow()

        override suspend fun getSessions(): Result<SessionsListModel> = Result.Success(listModel)

        override suspend fun loadSessions(): Result<SessionsListModel> {
            sessionsFlow.value = listModel
            return Result.Success(listModel)
        }

        override suspend fun getWebSessions(): Result<List<WebSessionModel>> = Result.Success(webList)

        override suspend fun loadWebSessions(): Result<List<WebSessionModel>> {
            webSessionsFlow.value = webList
            return Result.Success(webList)
        }

        override suspend fun terminateSession(hash: Long): Result<Unit> {
            listModel = listModel.copy(
                otherSessions = listModel.otherSessions.filter { it.hash != hash },
                passwordPendingSessions = listModel.passwordPendingSessions.filter { it.hash != hash }
            )
            sessionsFlow.value = listModel
            return Result.Success(Unit)
        }

        override suspend fun terminateAllOtherSessions(): Result<Unit> {
            listModel = listModel.copy(
                otherSessions = emptyList(),
                passwordPendingSessions = emptyList()
            )
            sessionsFlow.value = listModel
            return Result.Success(Unit)
        }

        override suspend fun terminateWebSession(hash: Long): Result<Unit> {
            webList = webList.filter { it.hash != hash }
            webSessionsFlow.value = webList
            return Result.Success(Unit)
        }

        override suspend fun terminateAllWebSessions(): Result<Unit> {
            webList = emptyList()
            webSessionsFlow.value = webList
            return Result.Success(Unit)
        }

        override suspend fun updateSessionSettings(
            hash: Long,
            acceptSecretChats: Boolean,
            acceptCalls: Boolean
        ): Result<Unit> {
            val updateItem: (SessionModel) -> SessionModel = { s ->
                if (s.hash == hash) {
                    s.copy(acceptSecretChats = acceptSecretChats, acceptCalls = acceptCalls)
                } else s
            }
            listModel = listModel.copy(
                currentSession = listModel.currentSession?.let(updateItem),
                otherSessions = listModel.otherSessions.map(updateItem),
                passwordPendingSessions = listModel.passwordPendingSessions.map(updateItem)
            )
            sessionsFlow.value = listModel
            return Result.Success(Unit)
        }

        override suspend fun setSessionsTtl(ttlDays: Int): Result<Unit> {
            listModel = listModel.copy(ttlDays = ttlDays)
            sessionsFlow.value = listModel
            return Result.Success(Unit)
        }

        override suspend fun acceptQrLogin(token: ByteArray): Result<Unit> {
            return Result.Success(Unit)
        }

        override suspend fun acceptQrLoginByLink(link: String): Result<Unit> {
            return Result.Success(Unit)
        }
    }
}
