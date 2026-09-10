package org.telegram.messenger.feature.messaging.aitones

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
import org.telegram.messenger.core.result.AppError
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.aitones.data.mapper.AiToneMapper
import org.telegram.messenger.feature.messaging.aitones.domain.model.AiToneModel
import org.telegram.messenger.feature.messaging.aitones.domain.model.AiTonesStateModel
import org.telegram.messenger.feature.messaging.aitones.domain.repository.AiTonesRepository
import org.telegram.messenger.feature.messaging.aitones.domain.usecase.AddAiToneUseCase
import org.telegram.messenger.feature.messaging.aitones.domain.usecase.EditAiToneUseCase
import org.telegram.messenger.feature.messaging.aitones.domain.usecase.GetAiTonesStateUseCase
import org.telegram.messenger.feature.messaging.aitones.domain.usecase.LoadAiTonesUseCase
import org.telegram.messenger.feature.messaging.aitones.domain.usecase.ObserveAiTonesUseCase
import org.telegram.messenger.feature.messaging.aitones.domain.usecase.RemoveAiToneUseCase
import org.telegram.messenger.feature.messaging.aitones.domain.usecase.UnsaveAiToneUseCase
import org.telegram.messenger.feature.messaging.aitones.presentation.AiTonesEvent
import org.telegram.messenger.feature.messaging.aitones.presentation.AiTonesUiState
import org.telegram.messenger.feature.messaging.aitones.presentation.AiTonesViewModel
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_aicompose

@OptIn(ExperimentalCoroutinesApi::class)
class AiTonesDomainTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeAiTonesRepository : AiTonesRepository {
        val tones = mutableListOf<AiToneModel>()
        var isLoading = false
        var shouldFail = false
        var hash = 12345L

        private val stateFlow = MutableStateFlow(getStateInternal())

        private fun getStateInternal(): AiTonesStateModel {
            return AiTonesStateModel(
                tones = tones.toList(),
                savedCount = tones.count { !it.isDefault },
                isLoading = isLoading,
                hash = hash
            )
        }

        fun emit() {
            stateFlow.value = getStateInternal()
        }

        fun setTones(newTones: List<AiToneModel>) {
            tones.clear()
            tones.addAll(newTones)
            emit()
        }

        override fun observeTones(): Flow<AiTonesStateModel> = stateFlow.asStateFlow()

        override fun getTonesState(): AiTonesStateModel = getStateInternal()

        override suspend fun loadTones(force: Boolean): Result<AiTonesStateModel> {
            if (shouldFail) {
                return Result.Failure(AppError.Network("Network error", 500))
            }
            emit()
            return Result.Success(getStateInternal())
        }

        override suspend fun addTone(tone: AiToneModel): Result<Unit> {
            if (shouldFail) return Result.Failure(AppError.Generic("Failed to add"))
            val idx = tones.indexOfFirst { it.id == tone.id }
            if (idx >= 0) {
                tones[idx] = tone
            } else {
                tones.add(0, tone)
            }
            emit()
            return Result.Success(Unit)
        }

        override suspend fun removeTone(tone: AiToneModel): Result<Unit> {
            if (shouldFail) return Result.Failure(AppError.Generic("Failed to remove"))
            tones.removeAll { it.id == tone.id || it.title == tone.title }
            emit()
            return Result.Success(Unit)
        }

        override suspend fun unsaveTone(tone: AiToneModel): Result<Unit> {
            return removeTone(tone)
        }

        override suspend fun editTone(tone: AiToneModel): Result<Unit> {
            if (shouldFail) return Result.Failure(AppError.Generic("Failed to edit"))
            val idx = tones.indexOfFirst { it.id == tone.id }
            return if (idx >= 0) {
                tones[idx] = tone
                emit()
                Result.Success(Unit)
            } else {
                Result.Failure(AppError.Generic("Tone not found"))
            }
        }
    }

    @Test
    fun testDomainModels() {
        val tone = AiToneModel(
            id = 101L,
            title = "Formal",
            emojiDocumentId = 555L,
            slug = "formal-style",
            prompt = "Make it sound formal and polite",
            isCreator = true,
            installsCount = 42,
            isDefault = false,
            defaultToneKey = null,
            exampleFromText = "hi what's up",
            exampleToText = "Dear Sir or Madam, I hope this message finds you well."
        )

        assertEquals(101L, tone.id)
        assertEquals("Formal", tone.title)
        assertEquals(555L, tone.emojiDocumentId)
        assertEquals("formal-style", tone.slug)
        assertEquals("Make it sound formal and polite", tone.prompt)
        assertTrue(tone.isCreator)
        assertEquals(42, tone.installsCount)
        assertFalse(tone.isDefault)
        assertNull(tone.defaultToneKey)
        assertEquals("hi what's up", tone.exampleFromText)
        assertEquals("Dear Sir or Madam, I hope this message finds you well.", tone.exampleToText)

        val state = AiTonesStateModel(
            tones = listOf(tone),
            savedCount = 1,
            isLoading = false,
            hash = 999L
        )

        assertEquals(1, state.tones.size)
        assertEquals(1, state.savedCount)
        assertFalse(state.isLoading)
        assertEquals(999L, state.hash)
    }

    @Test
    fun testMapperWithCustomAndDefaultTones() {
        val customTl = TL_aicompose.TL_aiComposeTone().apply {
            id = 1L
            title = "Friendly"
            emoji_id = 777L
            slug = "friendly"
            prompt = "Be friendly"
            creator = true
            installs_count = 100
            example_english = TL_aicompose.aiComposeToneExample().apply {
                from = TLRPC.TL_textWithEntities().apply { text = "no" }
                to = TLRPC.TL_textWithEntities().apply { text = "I am terribly sorry, but unfortunately no." }
            }
        }

        val defaultTl = TL_aicompose.TL_aiComposeToneDefault().apply {
            title = "Humorous"
            tone = "humorous"
            emoji_id = 888L
        }

        val mappedCustom = AiToneMapper.mapTone(customTl)
        assertNotNull(mappedCustom)
        assertEquals(1L, mappedCustom?.id)
        assertEquals("Friendly", mappedCustom?.title)
        assertEquals(777L, mappedCustom?.emojiDocumentId)
        assertEquals("friendly", mappedCustom?.slug)
        assertEquals("Be friendly", mappedCustom?.prompt)
        assertTrue(mappedCustom?.isCreator == true)
        assertEquals(100, mappedCustom?.installsCount)
        assertFalse(mappedCustom?.isDefault == true)
        assertEquals("no", mappedCustom?.exampleFromText)
        assertEquals("I am terribly sorry, but unfortunately no.", mappedCustom?.exampleToText)

        val mappedDefault = AiToneMapper.mapTone(defaultTl)
        assertNotNull(mappedDefault)
        assertNull(mappedDefault?.id)
        assertEquals("Humorous", mappedDefault?.title)
        assertEquals(888L, mappedDefault?.emojiDocumentId)
        assertTrue(mappedDefault?.isDefault == true)
        assertEquals("humorous", mappedDefault?.defaultToneKey)

        val list = AiToneMapper.mapTonesList(listOf(customTl, defaultTl))
        assertEquals(2, list.size)
    }

    @Test
    fun testUseCasesOperations() = runTest {
        val repo = FakeAiTonesRepository()
        val observeUseCase = ObserveAiTonesUseCase(repo)
        val getStateUseCase = GetAiTonesStateUseCase(repo)
        val loadUseCase = LoadAiTonesUseCase(repo)
        val addUseCase = AddAiToneUseCase(repo)
        val editUseCase = EditAiToneUseCase(repo)
        val removeUseCase = RemoveAiToneUseCase(repo)
        val unsaveUseCase = UnsaveAiToneUseCase(repo)

        val initial = getStateUseCase()
        assertEquals(0, initial.tones.size)

        val tone1 = AiToneModel(id = 1L, title = "Tone 1")
        val tone2 = AiToneModel(id = 2L, title = "Tone 2")

        addUseCase(tone1)
        addUseCase(tone2)

        val updated = getStateUseCase()
        assertEquals(2, updated.tones.size)
        assertEquals("Tone 2", updated.tones[0].title)

        val editedTone = tone1.copy(title = "Tone 1 Edited")
        val editResult = editUseCase(editedTone)
        assertTrue(editResult is Result.Success)

        val afterEdit = getStateUseCase()
        val found = afterEdit.tones.firstOrNull { it.id == 1L }
        assertEquals("Tone 1 Edited", found?.title)

        val unsaveResult = unsaveUseCase(editedTone)
        assertTrue(unsaveResult is Result.Success)
        assertEquals(1, getStateUseCase().tones.size)

        val removeResult = removeUseCase(tone2)
        assertTrue(removeResult is Result.Success)
        assertEquals(0, getStateUseCase().tones.size)

        val observed = observeUseCase().first()
        assertEquals(0, observed.tones.size)
    }

    @Test
    fun testViewModelLifecycleAndMvi() = runTest {
        val repo = FakeAiTonesRepository()
        repo.setTones(listOf(AiToneModel(id = 10L, title = "Original Tone", isDefault = false)))

        val viewModel = AiTonesViewModel(
            observeAiTonesUseCase = ObserveAiTonesUseCase(repo),
            getAiTonesStateUseCase = GetAiTonesStateUseCase(repo),
            loadAiTonesUseCase = LoadAiTonesUseCase(repo),
            addAiToneUseCase = AddAiToneUseCase(repo),
            removeAiToneUseCase = RemoveAiToneUseCase(repo),
            unsaveAiToneUseCase = UnsaveAiToneUseCase(repo),
            editAiToneUseCase = EditAiToneUseCase(repo)
        )

        advanceUntilIdle()
        val state1 = viewModel.uiState.value
        assertTrue(state1 is AiTonesUiState.Success)
        assertEquals(1, (state1 as AiTonesUiState.Success).state.tones.size)

        val selected = state1.state.tones[0]
        viewModel.onEvent(AiTonesEvent.SelectTone(selected))
        val state2 = viewModel.uiState.value as AiTonesUiState.Success
        assertEquals(selected, state2.selectedTone)

        val newTone = AiToneModel(id = 20L, title = "New Tone", isDefault = false)
        viewModel.onEvent(AiTonesEvent.AddTone(newTone))
        advanceUntilIdle()

        val state3 = viewModel.uiState.value as AiTonesUiState.Success
        assertEquals(2, state3.state.tones.size)
        assertFalse(state3.isSaving)

        val editedTone = newTone.copy(title = "New Tone Updated")
        viewModel.onEvent(AiTonesEvent.EditTone(editedTone))
        advanceUntilIdle()

        val state4 = viewModel.uiState.value as AiTonesUiState.Success
        val updatedInState = state4.state.tones.firstOrNull { it.id == 20L }
        assertEquals("New Tone Updated", updatedInState?.title)

        viewModel.onEvent(AiTonesEvent.RemoveTone(editedTone))
        advanceUntilIdle()

        val state5 = viewModel.uiState.value as AiTonesUiState.Success
        assertEquals(1, state5.state.tones.size)

        repo.shouldFail = true
        viewModel.onEvent(AiTonesEvent.AddTone(AiToneModel(id = 30L, title = "Failing Tone")))
        advanceUntilIdle()

        val state6 = viewModel.uiState.value as AiTonesUiState.Success
        assertNotNull(state6.errorMessage)

        viewModel.onEvent(AiTonesEvent.ClearError)
        val state7 = viewModel.uiState.value as AiTonesUiState.Success
        assertNull(state7.errorMessage)
    }
}
