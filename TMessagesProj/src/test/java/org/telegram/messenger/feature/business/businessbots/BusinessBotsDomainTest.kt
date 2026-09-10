package org.telegram.messenger.feature.business.businessbots

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
import org.telegram.messenger.feature.business.businessbots.data.mapper.BusinessBotMapper
import org.telegram.messenger.feature.business.businessbots.domain.model.BusinessBotRecipientsModel
import org.telegram.messenger.feature.business.businessbots.domain.model.BusinessBotRightsModel
import org.telegram.messenger.feature.business.businessbots.domain.model.BusinessBotsStateModel
import org.telegram.messenger.feature.business.businessbots.domain.model.ConnectedBotModel
import org.telegram.messenger.feature.business.businessbots.domain.repository.BusinessBotsRepository
import org.telegram.messenger.feature.business.businessbots.domain.usecase.DeleteConnectedBotUseCase
import org.telegram.messenger.feature.business.businessbots.domain.usecase.FindConnectedBotUseCase
import org.telegram.messenger.feature.business.businessbots.domain.usecase.GetConnectedBotsUseCase
import org.telegram.messenger.feature.business.businessbots.domain.usecase.LoadConnectedBotsUseCase
import org.telegram.messenger.feature.business.businessbots.domain.usecase.ObserveConnectedBotsUseCase
import org.telegram.messenger.feature.business.businessbots.domain.usecase.UpdateConnectedBotUseCase
import org.telegram.messenger.feature.business.businessbots.presentation.BusinessBotsEvent
import org.telegram.messenger.feature.business.businessbots.presentation.BusinessBotsViewModel
import org.telegram.tgnet.tl.TL_account
import java.util.ArrayList

@OptIn(ExperimentalCoroutinesApi::class)
class BusinessBotsDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeBusinessBotsRepository : BusinessBotsRepository {
        val botsList = mutableListOf<ConnectedBotModel>()
        var shouldFail = false
        var lastLoadedForce = false

        private val botsFlow = MutableStateFlow<List<ConnectedBotModel>>(emptyList())

        fun emitBots() {
            botsFlow.value = botsList.toList()
        }

        override fun observeConnectedBots(): Flow<List<ConnectedBotModel>> = botsFlow

        override suspend fun getConnectedBots(): List<ConnectedBotModel> = botsList.toList()

        override suspend fun loadConnectedBots(forceReload: Boolean): Result<List<ConnectedBotModel>> {
            lastLoadedForce = forceReload
            return if (shouldFail) {
                Result.failure("Failed to load connected bots")
            } else {
                emitBots()
                Result.Success(botsList.toList())
            }
        }

        override suspend fun updateConnectedBot(
            botId: Long,
            rights: BusinessBotRightsModel,
            recipients: BusinessBotRecipientsModel
        ): Result<ConnectedBotModel> {
            if (shouldFail) return Result.failure("Failed to update connected bot")
            val index = botsList.indexOfFirst { it.botId == botId }
            val updated = if (index != -1) {
                botsList[index].copy(rights = rights, recipients = recipients)
            } else {
                ConnectedBotModel(botId = botId, rights = rights, recipients = recipients)
            }
            if (index != -1) {
                botsList[index] = updated
            } else {
                botsList.add(updated)
            }
            emitBots()
            return Result.Success(updated)
        }

        override suspend fun deleteConnectedBot(botId: Long): Result<Unit> {
            if (shouldFail) return Result.failure("Failed to delete connected bot")
            val removed = botsList.removeAll { it.botId == botId }
            if (!removed) return Result.failure("Connected bot not found")
            emitBots()
            return Result.Success(Unit)
        }

        override fun findConnectedBot(botId: Long): ConnectedBotModel? {
            return botsList.find { it.botId == botId }
        }
    }

    @Test
    fun testBusinessBotRightsModel() {
        val defaultRights = BusinessBotRightsModel.makeDefault()
        assertTrue(defaultRights.reply)
        assertTrue(defaultRights.readMessages)
        assertTrue(defaultRights.deleteSentMessages)
        assertTrue(defaultRights.deleteReceivedMessages)
        assertFalse(defaultRights.editName)
        assertFalse(defaultRights.editBio)
        assertFalse(defaultRights.editProfilePhoto)
        assertFalse(defaultRights.editUsername)
        assertFalse(defaultRights.viewGifts)
        assertFalse(defaultRights.sellGifts)
        assertFalse(defaultRights.changeGiftSettings)
        assertFalse(defaultRights.transferAndUpgradeGifts)
        assertFalse(defaultRights.transferStars)
        assertFalse(defaultRights.manageStories)

        val allRights = BusinessBotRightsModel.all()
        assertTrue(allRights.reply)
        assertTrue(allRights.editName)
        assertTrue(allRights.manageStories)
        assertTrue(allRights.transferStars)
    }

    @Test
    fun testBusinessBotRecipientsModel() {
        val defaultRecipients = BusinessBotRecipientsModel()
        assertTrue(defaultRecipients.excludeSelected)
        assertTrue(defaultRecipients.users.isEmpty())
        assertFalse(defaultRecipients.hasFilters)

        val filteredRecipients = BusinessBotRecipientsModel(
            excludeSelected = false,
            users = listOf(12345L, 67890L),
            existingChats = true
        )
        assertFalse(filteredRecipients.excludeSelected)
        assertEquals(2, filteredRecipients.users.size)
        assertTrue(filteredRecipients.hasFilters)
    }

    @Test
    fun testBusinessBotsStateModel() {
        val emptyState = BusinessBotsStateModel()
        assertFalse(emptyState.hasConnectedBot)
        assertNull(emptyState.firstBot)

        val bot = ConnectedBotModel(botId = 1001L, device = "Android", location = "Paris")
        val activeState = BusinessBotsStateModel(
            connectedBots = listOf(bot),
            isLoading = false
        )
        assertTrue(activeState.hasConnectedBot)
        assertEquals(1001L, activeState.firstBot?.botId)
        assertEquals("Android", activeState.firstBot?.device)
    }

    @Test
    fun testBusinessBotMapper() {
        val defaultModel = BusinessBotMapper.mapRights(null)
        assertEquals(BusinessBotRightsModel.makeDefault(), defaultModel)

        val tlRights = TL_account.TL_businessBotRights().apply {
            reply = true
            read_messages = true
            delete_sent_messages = false
            delete_received_messages = true
            edit_name = true
            edit_bio = false
            manage_stories = true
        }

        val mappedRights = BusinessBotMapper.mapRights(tlRights)
        assertTrue(mappedRights.reply)
        assertTrue(mappedRights.readMessages)
        assertFalse(mappedRights.deleteSentMessages)
        assertTrue(mappedRights.deleteReceivedMessages)
        assertTrue(mappedRights.editName)
        assertFalse(mappedRights.editBio)
        assertTrue(mappedRights.manageStories)

        val backToTlRights = BusinessBotMapper.toTlRights(mappedRights)
        assertEquals(tlRights.reply, backToTlRights.reply)
        assertEquals(tlRights.read_messages, backToTlRights.read_messages)
        assertEquals(tlRights.delete_sent_messages, backToTlRights.delete_sent_messages)
        assertEquals(tlRights.edit_name, backToTlRights.edit_name)
        assertEquals(tlRights.manage_stories, backToTlRights.manage_stories)

        val tlRecipients = TL_account.TL_businessBotRecipients().apply {
            exclude_selected = false
            existing_chats = true
            new_chats = false
            contacts = true
            non_contacts = false
            users = ArrayList(listOf(101L, 102L))
        }

        val mappedRecipients = BusinessBotMapper.mapRecipients(tlRecipients)
        assertFalse(mappedRecipients.excludeSelected)
        assertTrue(mappedRecipients.existingChats)
        assertFalse(mappedRecipients.newChats)
        assertTrue(mappedRecipients.contacts)
        assertEquals(listOf(101L, 102L), mappedRecipients.users)

        val backToTlRecipients = BusinessBotMapper.toTlRecipients(mappedRecipients)
        assertEquals(tlRecipients.exclude_selected, backToTlRecipients.exclude_selected)
        assertEquals(tlRecipients.existing_chats, backToTlRecipients.existing_chats)
        assertEquals(tlRecipients.contacts, backToTlRecipients.contacts)
        assertEquals(tlRecipients.users, backToTlRecipients.users)

        assertNull(BusinessBotMapper.mapConnectedBot(null))

        val tlBot = TL_account.TL_connectedBot().apply {
            bot_id = 9999L
            device = "iPhone 15"
            location = "London"
            date = 1715000000
            rights = tlRights
            recipients = tlRecipients
        }

        val mappedBot = BusinessBotMapper.mapConnectedBot(tlBot)
        assertNotNull(mappedBot)
        assertEquals(9999L, mappedBot!!.botId)
        assertEquals("iPhone 15", mappedBot.device)
        assertEquals("London", mappedBot.location)
        assertEquals(1715000000, mappedBot.date)
        assertEquals(mappedRights, mappedBot.rights)
        assertEquals(mappedRecipients, mappedBot.recipients)

        val emptyResponse = BusinessBotMapper.mapConnectedBots(null)
        assertTrue(emptyResponse.isEmpty())

        val tlResponse = TL_account.connectedBots().apply {
            connected_bots = ArrayList(listOf(tlBot))
        }
        val mappedList = BusinessBotMapper.mapConnectedBots(tlResponse)
        assertEquals(1, mappedList.size)
        assertEquals(9999L, mappedList[0].botId)
    }

    @Test
    fun testUseCasesOperations() = runTest {
        val fakeRepo = FakeBusinessBotsRepository()
        val observeUseCase = ObserveConnectedBotsUseCase(fakeRepo)
        val getUseCase = GetConnectedBotsUseCase(fakeRepo)
        val loadUseCase = LoadConnectedBotsUseCase(fakeRepo)
        val updateUseCase = UpdateConnectedBotUseCase(fakeRepo)
        val deleteUseCase = DeleteConnectedBotUseCase(fakeRepo)
        val findUseCase = FindConnectedBotUseCase(fakeRepo)

        assertTrue(getUseCase().isEmpty())
        assertNull(findUseCase(505L))

        val updateResult = updateUseCase(
            505L,
            BusinessBotRightsModel(manageStories = true),
            BusinessBotRecipientsModel(excludeSelected = false, contacts = true)
        )
        assertTrue(updateResult is Result.Success)
        val createdBot = (updateResult as Result.Success).data
        assertEquals(505L, createdBot.botId)
        assertTrue(createdBot.rights.manageStories)
        assertTrue(createdBot.recipients.contacts)

        assertEquals(1, getUseCase().size)
        assertEquals(createdBot, findUseCase(505L))

        val loadResult = loadUseCase(forceReload = true)
        assertTrue(loadResult is Result.Success)
        assertTrue(fakeRepo.lastLoadedForce)

        val deleteResult = deleteUseCase(505L)
        assertTrue(deleteResult is Result.Success)
        assertTrue(getUseCase().isEmpty())
        assertNull(findUseCase(505L))

        fakeRepo.shouldFail = true
        val failUpdate = updateUseCase(505L, BusinessBotRightsModel(), BusinessBotRecipientsModel())
        assertTrue(failUpdate is Result.Failure)
        val failDelete = deleteUseCase(505L)
        assertTrue(failDelete is Result.Failure)
        val failLoad = loadUseCase()
        assertTrue(failLoad is Result.Failure)
    }

    @Test
    fun testBusinessBotsViewModelFlow() = runTest {
        val fakeRepo = FakeBusinessBotsRepository()
        val viewModel = BusinessBotsViewModel(
            observeConnectedBotsUseCase = ObserveConnectedBotsUseCase(fakeRepo),
            loadConnectedBotsUseCase = LoadConnectedBotsUseCase(fakeRepo),
            updateConnectedBotUseCase = UpdateConnectedBotUseCase(fakeRepo),
            deleteConnectedBotUseCase = DeleteConnectedBotUseCase(fakeRepo)
        )

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.hasConnectedBot)
        assertNull(viewModel.uiState.value.selectedBot)
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.onEvent(
            BusinessBotsEvent.UpdateBot(
                botId = 777L,
                rights = BusinessBotRightsModel.all(),
                recipients = BusinessBotRecipientsModel(excludeSelected = true)
            )
        )
        advanceUntilIdle()

        val stateAfterUpdate = viewModel.uiState.value
        assertTrue(stateAfterUpdate.hasConnectedBot)
        assertEquals(777L, stateAfterUpdate.selectedBot?.botId)
        assertNotNull(stateAfterUpdate.actionSuccessMessage)
        assertFalse(stateAfterUpdate.isSaving)

        viewModel.onEvent(BusinessBotsEvent.ClearMessages)
        assertNull(viewModel.uiState.value.actionSuccessMessage)

        viewModel.onEvent(BusinessBotsEvent.Load(forceReload = true))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.onEvent(BusinessBotsEvent.DeleteBot(777L))
        advanceUntilIdle()

        val stateAfterDelete = viewModel.uiState.value
        assertFalse(stateAfterDelete.hasConnectedBot)
        assertNull(stateAfterDelete.selectedBot)
        assertEquals("Business bot disconnected", stateAfterDelete.actionSuccessMessage)

        fakeRepo.shouldFail = true
        viewModel.onEvent(
            BusinessBotsEvent.UpdateBot(
                botId = 888L,
                rights = BusinessBotRightsModel(),
                recipients = BusinessBotRecipientsModel()
            )
        )
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testAccountFeatureContainerIntegration() = runTest {
        val container = AccountFeatureContainer.get(0)
        val fakeRepo = FakeBusinessBotsRepository()
        container.businessBotsRepository = fakeRepo

        val vm = container.createBusinessBotsViewModel()
        assertNotNull(vm)
        assertEquals(0, container.getConnectedBotsUseCase().size)
    }
}
