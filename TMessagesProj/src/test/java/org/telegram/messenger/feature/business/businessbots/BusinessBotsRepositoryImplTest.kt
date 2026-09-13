package org.telegram.messenger.feature.business.businessbots

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.business.businessbots.data.datasource.BusinessBotsLocalDataSource
import org.telegram.messenger.feature.business.businessbots.data.datasource.BusinessBotsRemoteDataSource
import org.telegram.messenger.feature.business.businessbots.data.repository.BusinessBotsRepositoryImpl
import org.telegram.messenger.feature.business.businessbots.domain.model.BusinessBotRecipientsModel
import org.telegram.messenger.feature.business.businessbots.domain.model.BusinessBotRightsModel
import org.telegram.messenger.feature.business.businessbots.domain.model.ConnectedBotModel

@OptIn(ExperimentalCoroutinesApi::class)
class BusinessBotsRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val currentAccount = 0

    private lateinit var localDataSource: BusinessBotsLocalDataSource
    private lateinit var remoteDataSource: BusinessBotsRemoteDataSource
    private lateinit var repository: BusinessBotsRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        localDataSource = BusinessBotsLocalDataSource(currentAccount)
        remoteDataSource = BusinessBotsRemoteDataSource(currentAccount)
        repository = BusinessBotsRepositoryImpl(
            currentAccount = currentAccount,
            localDataSource = localDataSource,
            remoteDataSource = remoteDataSource,
            mainDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getConnectedBots returns cached bots`() = runTest(testDispatcher) {
        val sample = listOf(
            ConnectedBotModel(
                botId = 12345L,
                recipients = BusinessBotRecipientsModel(newChats = true),
                rights = BusinessBotRightsModel.makeDefault(),
                device = "Server",
                location = "US",
                date = 1700000000
            )
        )
        localDataSource.setTestBots(sample)

        val bots = repository.getConnectedBots()
        assertEquals(1, bots.size)
        assertEquals(12345L, bots[0].botId)
        assertEquals("Server", bots[0].device)
    }

    @Test
    fun `findConnectedBot finds existing bot by id`() = runTest(testDispatcher) {
        val sample = listOf(
            ConnectedBotModel(botId = 111L),
            ConnectedBotModel(botId = 222L)
        )
        localDataSource.setTestBots(sample)

        val found = repository.findConnectedBot(111L)
        assertNotNull(found)
        assertEquals(111L, found?.botId)

        val notFound = repository.findConnectedBot(999L)
        assertNull(notFound)
    }

    @Test
    fun `updateConnectedBot updates local data source`() = runTest(testDispatcher) {
        val rights = BusinessBotRightsModel(reply = true, readMessages = true)
        val recipients = BusinessBotRecipientsModel(newChats = true)

        localDataSource.updateBot(
            ConnectedBotModel(
                botId = 555L,
                recipients = recipients,
                rights = rights
            )
        )

        val bot = repository.findConnectedBot(555L)
        assertNotNull(bot)
        assertEquals(555L, bot?.botId)
        assertTrue(bot?.rights?.reply == true)
        assertTrue(bot?.recipients?.newChats == true)
    }

    @Test
    fun `deleteConnectedBot removes bot from local storage`() = runTest(testDispatcher) {
        val sample = listOf(
            ConnectedBotModel(botId = 100L),
            ConnectedBotModel(botId = 200L)
        )
        localDataSource.setTestBots(sample)

        localDataSource.deleteBot(100L)

        val bots = repository.getConnectedBots()
        assertEquals(1, bots.size)
        assertEquals(200L, bots[0].botId)
    }

    @Test
    fun `observeConnectedBots emits current list`() = runTest(testDispatcher) {
        val sample = listOf(
            ConnectedBotModel(botId = 777L)
        )
        localDataSource.setTestBots(sample)

        val emitted = repository.observeConnectedBots().first()
        assertEquals(1, emitted.size)
        assertEquals(777L, emitted[0].botId)
    }
}
