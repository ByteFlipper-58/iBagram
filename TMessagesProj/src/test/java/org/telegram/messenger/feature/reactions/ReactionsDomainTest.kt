package org.telegram.messenger.feature.reactions

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
import org.telegram.messenger.feature.reactions.data.mapper.ReactionMapper
import org.telegram.messenger.feature.reactions.domain.model.MessageReactionCountModel
import org.telegram.messenger.feature.reactions.domain.model.MessageReactionsStateModel
import org.telegram.messenger.feature.reactions.domain.model.ReactionItemModel
import org.telegram.messenger.feature.reactions.domain.model.ReactionsSettingsModel
import org.telegram.messenger.feature.reactions.domain.repository.ReactionsRepository
import org.telegram.messenger.feature.reactions.domain.usecase.ClearReactionsUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.GetAvailableReactionsUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.GetDoubleTapReactionUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.GetReactionsSettingsUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.GetRecentReactionsUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.LoadAvailableReactionsUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.ObserveAvailableReactionsUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.ObserveRecentReactionsUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.SendReactionUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.SendVoteUseCase
import org.telegram.messenger.feature.reactions.domain.usecase.SetDoubleTapReactionUseCase
import org.telegram.messenger.feature.reactions.presentation.ReactionsEvent
import org.telegram.messenger.feature.reactions.presentation.ReactionsViewModel
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class ReactionsDomainTest {

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
    fun testDomainModels() {
        val reaction = ReactionItemModel(
            reaction = "👍",
            title = "Thumbs Up",
            isCustom = false,
            documentId = 0L,
            isInactive = false,
            isPremium = false,
            isPaid = false,
            order = 1
        )
        assertEquals("👍", reaction.reaction)
        assertEquals("Thumbs Up", reaction.title)
        assertFalse(reaction.isCustom)
        assertFalse(reaction.isPremium)
        assertEquals(1, reaction.order)

        val customReaction = ReactionItemModel(
            reaction = "custom_star",
            title = "Star",
            isCustom = true,
            documentId = 123456789L,
            isInactive = false,
            isPremium = true,
            isPaid = false
        )
        assertTrue(customReaction.isCustom)
        assertTrue(customReaction.isPremium)
        assertEquals(123456789L, customReaction.documentId)

        val paidReaction = ReactionItemModel(
            reaction = "⭐️",
            title = "Star",
            isPaid = true
        )
        assertTrue(paidReaction.isPaid)

        val countModel = MessageReactionCountModel(
            reaction = reaction,
            count = 42,
            isChosen = true,
            chosenOrder = 1
        )
        assertEquals(42, countModel.count)
        assertTrue(countModel.isChosen)
        assertEquals(1, countModel.chosenOrder)

        val stateModel = MessageReactionsStateModel(
            dialogId = 999L,
            messageId = 100,
            canSeeList = true,
            reactions = listOf(countModel)
        )
        assertEquals(999L, stateModel.dialogId)
        assertEquals(100, stateModel.messageId)
        assertTrue(stateModel.canSeeList)
        assertEquals(1, stateModel.reactions.size)

        val settingsModel = ReactionsSettingsModel(
            doubleTapReaction = "❤️",
            availableReactions = listOf(reaction),
            recentReactions = listOf(reaction),
            topReactions = listOf(reaction)
        )
        assertEquals("❤️", settingsModel.doubleTapReaction)
        assertEquals(1, settingsModel.availableReactions.size)
        assertEquals(1, settingsModel.recentReactions.size)
    }

    @Test
    fun testReactionMapper() {
        val tlAvailable = TLRPC.TL_availableReaction().apply {
            reaction = "🔥"
            title = "Fire"
            inactive = false
            premium = true
            positionInList = 5
        }

        val mappedAvailable = ReactionMapper.mapAvailableReaction(tlAvailable)
        assertEquals("🔥", mappedAvailable.reaction)
        assertEquals("Fire", mappedAvailable.title)
        assertFalse(mappedAvailable.isInactive)
        assertTrue(mappedAvailable.isPremium)
        assertEquals(5, mappedAvailable.order)

        // TLRPC.TL_reactionEmoji
        val tlEmoji = TLRPC.TL_reactionEmoji().apply {
            emoticon = "🎉"
        }
        val mappedEmoji = ReactionMapper.mapReaction(tlEmoji)
        assertEquals("🎉", mappedEmoji.reaction)
        assertFalse(mappedEmoji.isCustom)

        // TLRPC.TL_reactionCustomEmoji
        val tlCustom = TLRPC.TL_reactionCustomEmoji().apply {
            document_id = 987654321L
        }
        val mappedCustom = ReactionMapper.mapReaction(tlCustom)
        assertTrue(mappedCustom.isCustom)
        assertEquals(987654321L, mappedCustom.documentId)

        // TLRPC.TL_reactionPaid
        val tlPaid = TLRPC.TL_reactionPaid()
        val mappedPaid = ReactionMapper.mapReaction(tlPaid)
        assertTrue(mappedPaid.isPaid)

        // toTLReaction conversions
        val tlConvertedEmoji = ReactionMapper.toTLReaction(mappedEmoji)
        assertTrue(tlConvertedEmoji is TLRPC.TL_reactionEmoji)
        assertEquals("🎉", (tlConvertedEmoji as TLRPC.TL_reactionEmoji).emoticon)

        val tlConvertedCustom = ReactionMapper.toTLReaction(mappedCustom)
        assertTrue(tlConvertedCustom is TLRPC.TL_reactionCustomEmoji)
        assertEquals(987654321L, (tlConvertedCustom as TLRPC.TL_reactionCustomEmoji).document_id)

        val tlConvertedPaid = ReactionMapper.toTLReaction(mappedPaid)
        assertTrue(tlConvertedPaid is TLRPC.TL_reactionPaid)

        // ReactionCount mapping
        val tlCount = TLRPC.TL_reactionCount().apply {
            this.reaction = tlEmoji
            count = 15
            chosen = true
            chosen_order = 2
        }
        val mappedCount = ReactionMapper.mapReactionCount(tlCount)
        assertEquals("🎉", mappedCount.reaction.reaction)
        assertEquals(15, mappedCount.count)
        assertTrue(mappedCount.isChosen)
        assertEquals(2, mappedCount.chosenOrder)
    }

    @Test
    fun testUseCasesWithFakeRepository() = runBlocking {
        val fakeRepo = FakeReactionsRepository()

        val getAvailableUseCase = GetAvailableReactionsUseCase(fakeRepo)
        val loadAvailableUseCase = LoadAvailableReactionsUseCase(fakeRepo)
        val getRecentUseCase = GetRecentReactionsUseCase(fakeRepo)
        val getSettingsUseCase = GetReactionsSettingsUseCase(fakeRepo)
        val getDoubleTapUseCase = GetDoubleTapReactionUseCase(fakeRepo)
        val setDoubleTapUseCase = SetDoubleTapReactionUseCase(fakeRepo)
        val sendReactionUseCase = SendReactionUseCase(fakeRepo)
        val clearReactionsUseCase = ClearReactionsUseCase(fakeRepo)
        val sendVoteUseCase = SendVoteUseCase(fakeRepo)

        // Available Reactions
        val availableResult = getAvailableUseCase()
        assertTrue(availableResult is Result.Success)
        assertEquals(3, (availableResult as Result.Success).data.size)

        val reloadedResult = loadAvailableUseCase(force = true)
        assertTrue(reloadedResult is Result.Success)

        // Recent Reactions
        val recentResult = getRecentUseCase()
        assertTrue(recentResult is Result.Success)
        assertEquals(1, (recentResult as Result.Success).data.size)

        // Settings & Double Tap
        val doubleTapResult = getDoubleTapUseCase()
        assertTrue(doubleTapResult is Result.Success)
        assertEquals("👍", (doubleTapResult as Result.Success).data)

        val setDoubleTapResult = setDoubleTapUseCase("❤️")
        assertTrue(setDoubleTapResult is Result.Success)
        assertEquals("❤️", (getDoubleTapUseCase() as Result.Success).data)

        val settingsResult = getSettingsUseCase()
        assertTrue(settingsResult is Result.Success)
        assertEquals("❤️", (settingsResult as Result.Success).data.doubleTapReaction)

        // Send Reaction
        val reactionToSend = ReactionItemModel(reaction = "🔥", title = "Fire")
        val sendResult = sendReactionUseCase.single(1001L, 200, reactionToSend)
        assertTrue(sendResult is Result.Success)

        // Clear Reactions
        val clearResult = clearReactionsUseCase(1001L, 200)
        assertTrue(clearResult is Result.Success)

        // Send Vote
        val voteResult = sendVoteUseCase(1001L, 300, 5555L, listOf(byteArrayOf(0)))
        assertTrue(voteResult is Result.Success)
    }

    @Test
    fun testReactionsViewModelFlowAndEvents() = runBlocking {
        val fakeRepo = FakeReactionsRepository()

        val viewModel = ReactionsViewModel(
            observeAvailableReactionsUseCase = ObserveAvailableReactionsUseCase(fakeRepo),
            getAvailableReactionsUseCase = GetAvailableReactionsUseCase(fakeRepo),
            loadAvailableReactionsUseCase = LoadAvailableReactionsUseCase(fakeRepo),
            observeRecentReactionsUseCase = ObserveRecentReactionsUseCase(fakeRepo),
            getRecentReactionsUseCase = GetRecentReactionsUseCase(fakeRepo),
            getReactionsSettingsUseCase = GetReactionsSettingsUseCase(fakeRepo),
            getDoubleTapReactionUseCase = GetDoubleTapReactionUseCase(fakeRepo),
            setDoubleTapReactionUseCase = SetDoubleTapReactionUseCase(fakeRepo),
            sendReactionUseCase = SendReactionUseCase(fakeRepo),
            clearReactionsUseCase = ClearReactionsUseCase(fakeRepo),
            sendVoteUseCase = SendVoteUseCase(fakeRepo)
        )

        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(3, state.availableReactions.size)
        assertEquals(1, state.recentReactions.size)
        assertEquals("👍", state.doubleTapReaction)

        // Set Double Tap Reaction
        viewModel.onEvent(ReactionsEvent.SetDoubleTapReaction("🔥"))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals("🔥", state.doubleTapReaction)
        assertEquals("Quick reaction updated", state.actionSuccessMessage)

        // Send Reaction
        val newReaction = ReactionItemModel(reaction = "🎉", title = "Party")
        viewModel.onEvent(ReactionsEvent.SendReaction(100L, 50, newReaction))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals("Reaction sent", state.actionSuccessMessage)
        assertTrue(state.recentReactions.any { it.reaction == "🎉" })

        // Send Multiple Reactions
        val multiReactions = listOf(
            ReactionItemModel(reaction = "👍", title = "Thumbs Up"),
            ReactionItemModel(reaction = "❤️", title = "Heart")
        )
        viewModel.onEvent(ReactionsEvent.SendMultipleReactions(100L, 50, multiReactions))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals("Reactions sent", state.actionSuccessMessage)

        // Clear Reactions
        viewModel.onEvent(ReactionsEvent.ClearReactions(100L, 50))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals("Reactions cleared", state.actionSuccessMessage)

        // Send Vote
        viewModel.onEvent(ReactionsEvent.SendVote(100L, 60, 9999L, listOf(byteArrayOf(1))))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals("Vote recorded", state.actionSuccessMessage)

        // Clear messages
        viewModel.onEvent(ReactionsEvent.ClearMessages)
        state = viewModel.uiState.value
        assertNull(state.actionSuccessMessage)
        assertNull(state.errorMessage)
    }

    private class FakeReactionsRepository : ReactionsRepository {

        private var doubleTap: String? = "👍"
        private val availableList = mutableListOf(
            ReactionItemModel(reaction = "👍", title = "Like", order = 0),
            ReactionItemModel(reaction = "❤️", title = "Heart", order = 1),
            ReactionItemModel(reaction = "🔥", title = "Fire", order = 2)
        )
        private val recentList = mutableListOf(
            ReactionItemModel(reaction = "👍", title = "Like", order = 0)
        )

        private val availableFlow = MutableStateFlow<List<ReactionItemModel>>(availableList)
        private val recentFlow = MutableStateFlow<List<ReactionItemModel>>(recentList)

        override fun observeAvailableReactions(): Flow<List<ReactionItemModel>> = availableFlow.asStateFlow()

        override suspend fun getAvailableReactions(): Result<List<ReactionItemModel>> = Result.Success(availableList)

        override suspend fun loadAvailableReactions(force: Boolean): Result<List<ReactionItemModel>> {
            availableFlow.value = availableList
            return Result.Success(availableList)
        }

        override fun observeRecentReactions(): Flow<List<ReactionItemModel>> = recentFlow.asStateFlow()

        override suspend fun getRecentReactions(): Result<List<ReactionItemModel>> = Result.Success(recentList)

        override suspend fun getReactionsSettings(): Result<ReactionsSettingsModel> {
            return Result.Success(
                ReactionsSettingsModel(
                    doubleTapReaction = doubleTap,
                    availableReactions = availableList,
                    recentReactions = recentList,
                    topReactions = availableList
                )
            )
        }

        override suspend fun getDoubleTapReaction(): Result<String?> = Result.Success(doubleTap)

        override suspend fun setDoubleTapReaction(reaction: String): Result<Unit> {
            doubleTap = reaction
            return Result.Success(Unit)
        }

        override suspend fun sendReaction(
            dialogId: Long,
            messageId: Int,
            reactions: List<ReactionItemModel>,
            isBig: Boolean,
            addToRecent: Boolean
        ): Result<Unit> {
            if (addToRecent) {
                for (r in reactions) {
                    if (!recentList.any { it.reaction == r.reaction }) {
                        recentList.add(0, r)
                    }
                }
                recentFlow.value = recentList.toList()
            }
            return Result.Success(Unit)
        }

        override suspend fun clearReactions(dialogId: Long, messageId: Int): Result<Unit> {
            return Result.Success(Unit)
        }

        override suspend fun sendVote(
            dialogId: Long,
            messageId: Int,
            pollId: Long,
            options: List<ByteArray>
        ): Result<Unit> {
            return Result.Success(Unit)
        }
    }
}
