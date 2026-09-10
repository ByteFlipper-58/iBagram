package org.telegram.messenger.feature.storycustomparams

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
import org.telegram.messenger.feature.storycustomparams.data.mapper.StoryCustomParamsMapper
import org.telegram.messenger.feature.storycustomparams.data.repository.LegacyStoryCustomParamsRepository
import org.telegram.messenger.feature.storycustomparams.domain.model.StoryCustomParamsModel
import org.telegram.messenger.feature.storycustomparams.domain.model.StoryTranslationParamsModel
import org.telegram.messenger.feature.storycustomparams.domain.usecase.CheckStoryCustomParamsEmptyUseCase
import org.telegram.messenger.feature.storycustomparams.domain.usecase.ClearAllStoryCustomParamsUseCase
import org.telegram.messenger.feature.storycustomparams.domain.usecase.ComputeStoryCustomParamsFlagsUseCase
import org.telegram.messenger.feature.storycustomparams.domain.usecase.CopyStoryCustomParamsUseCase
import org.telegram.messenger.feature.storycustomparams.domain.usecase.GetStoryCustomParamsStateUseCase
import org.telegram.messenger.feature.storycustomparams.domain.usecase.GetStoryCustomParamsUseCase
import org.telegram.messenger.feature.storycustomparams.domain.usecase.ObserveStoryCustomParamsStateUseCase
import org.telegram.messenger.feature.storycustomparams.domain.usecase.RemoveStoryCustomParamsUseCase
import org.telegram.messenger.feature.storycustomparams.domain.usecase.SaveStoryCustomParamsUseCase
import org.telegram.messenger.feature.storycustomparams.domain.usecase.UpdateStoryTranslationUseCase
import org.telegram.messenger.feature.storycustomparams.presentation.StoryCustomParamsEvent
import org.telegram.messenger.feature.storycustomparams.presentation.StoryCustomParamsViewModel
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_stories

@OptIn(ExperimentalCoroutinesApi::class)
class StoryCustomParamsDomainTest {

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
    fun testCheckStoryCustomParamsEmptyUseCase() {
        val checkEmpty = CheckStoryCustomParamsEmptyUseCase()

        assertTrue(checkEmpty(null as StoryCustomParamsModel?))
        assertTrue(checkEmpty(null as StoryTranslationParamsModel?))

        val emptyTranslation = StoryTranslationParamsModel()
        assertTrue(checkEmpty(emptyTranslation))

        val emptyParams = StoryCustomParamsModel(storyId = 1, dialogId = 100L)
        assertTrue(checkEmpty(emptyParams))

        // Non-empty cases
        assertFalse(checkEmpty(StoryTranslationParamsModel(isTranslated = true)))
        assertFalse(checkEmpty(StoryTranslationParamsModel(detectedLanguage = "en")))
        assertFalse(checkEmpty(StoryTranslationParamsModel(translatedText = "Bonjour")))
        assertFalse(checkEmpty(StoryTranslationParamsModel(translatedLanguage = "fr")))

        val nonEmptyParams = StoryCustomParamsModel(
            storyId = 2,
            dialogId = 200L,
            translation = StoryTranslationParamsModel(isTranslated = true, translatedText = "Hello")
        )
        assertFalse(checkEmpty(nonEmptyParams))
    }

    @Test
    fun testComputeStoryCustomParamsFlagsUseCase() {
        val computeFlags = ComputeStoryCustomParamsFlagsUseCase()

        assertEquals(0, computeFlags(StoryTranslationParamsModel()))
        assertEquals(1, computeFlags(StoryTranslationParamsModel(isTranslated = true)))
        assertEquals(2, computeFlags(StoryTranslationParamsModel(detectedLanguage = "es")))
        assertEquals(4, computeFlags(StoryTranslationParamsModel(translatedText = "Hola")))
        assertEquals(8, computeFlags(StoryTranslationParamsModel(translatedLanguage = "de")))

        val allFlags = StoryTranslationParamsModel(
            isTranslated = true,
            detectedLanguage = "ru",
            translatedText = "Привет",
            translatedLanguage = "en"
        )
        assertEquals(15, computeFlags(allFlags))
    }

    @Test
    fun testStoryCustomParamsMapper() {
        val storyItem = TL_stories.TL_storyItem()
        storyItem.id = 77
        storyItem.translated = true
        storyItem.detectedLng = "it"
        storyItem.translatedLng = "ru"
        val textWithEntities = TLRPC.TL_textWithEntities()
        textWithEntities.text = "Ciao mondo"
        storyItem.translatedText = textWithEntities

        val domain = StoryCustomParamsMapper.mapToDomain(dialogId = 999L, storyItem = storyItem)

        assertEquals(77, domain.storyId)
        assertEquals(999L, domain.dialogId)
        assertTrue(domain.translation.isTranslated)
        assertEquals("it", domain.translation.detectedLanguage)
        assertEquals("ru", domain.translation.translatedLanguage)
        assertEquals("Ciao mondo", domain.translation.translatedText)
        assertEquals(15, domain.flags)

        // Apply back to another story item
        val targetItem = TL_stories.TL_storyItem()
        StoryCustomParamsMapper.applyToStoryItem(domain, targetItem)

        assertTrue(targetItem.translated)
        assertEquals("it", targetItem.detectedLng)
        assertEquals("ru", targetItem.translatedLng)
        assertEquals("Ciao mondo", targetItem.translatedText?.text)
    }

    @Test
    fun testRepositoryOperations() {
        val repository = LegacyStoryCustomParamsRepository(0)
        val dialogId = 555L
        val storyId = 10

        assertNull(repository.getParams(dialogId, storyId))

        // Save
        val params = StoryCustomParamsModel(
            storyId = storyId,
            dialogId = dialogId,
            translation = StoryTranslationParamsModel(
                isTranslated = true,
                translatedText = "Automated story translation",
                translatedLanguage = "en"
            )
        )
        repository.saveParams(params)

        val retrieved = repository.getParams(dialogId, storyId)
        assertNotNull(retrieved)
        assertEquals(storyId, retrieved?.storyId)
        assertEquals(dialogId, retrieved?.dialogId)
        assertTrue(retrieved?.translation?.isTranslated == true)
        assertEquals("Automated story translation", retrieved?.translation?.translatedText)

        // Update translation
        repository.updateTranslation(
            dialogId = dialogId,
            storyId = storyId,
            isTranslated = true,
            detectedLang = "de",
            translatedText = "Guten Morgen",
            targetLang = "en"
        )
        val updated = repository.getParams(dialogId, storyId)
        assertEquals("de", updated?.translation?.detectedLanguage)
        assertEquals("Guten Morgen", updated?.translation?.translatedText)

        // Copy to another story
        val newStoryId = 20
        repository.copyParams(dialogId, storyId, dialogId, newStoryId)
        val copied = repository.getParams(dialogId, newStoryId)
        assertNotNull(copied)
        assertEquals(newStoryId, copied?.storyId)
        assertEquals("Guten Morgen", copied?.translation?.translatedText)

        // Remove
        repository.removeParams(dialogId, storyId)
        assertNull(repository.getParams(dialogId, storyId))
        assertNotNull(repository.getParams(dialogId, newStoryId))

        // Clear all
        repository.clearAll()
        assertTrue(repository.getState().paramsByStoryKey.isEmpty())
    }

    @Test
    fun testStoryCustomParamsViewModelMviFlow() = runTest(testDispatcher) {
        val repository = LegacyStoryCustomParamsRepository(0)
        val viewModel = StoryCustomParamsViewModel(
            observeStateUseCase = ObserveStoryCustomParamsStateUseCase(repository),
            getParamsUseCase = GetStoryCustomParamsUseCase(repository),
            saveParamsUseCase = SaveStoryCustomParamsUseCase(repository),
            updateTranslationUseCase = UpdateStoryTranslationUseCase(repository),
            copyParamsUseCase = CopyStoryCustomParamsUseCase(repository),
            removeParamsUseCase = RemoveStoryCustomParamsUseCase(repository),
            clearAllUseCase = ClearAllStoryCustomParamsUseCase(repository),
            checkEmptyUseCase = CheckStoryCustomParamsEmptyUseCase()
        )

        val dialogId = 111L
        val storyId = 33

        // Select story
        viewModel.onEvent(StoryCustomParamsEvent.SelectStory(dialogId, storyId))
        testScheduler.advanceUntilIdle()

        assertEquals(dialogId, viewModel.uiState.value.selectedDialogId)
        assertEquals(storyId, viewModel.uiState.value.selectedStoryId)
        assertNull(viewModel.uiState.value.selectedParams)
        assertFalse(viewModel.uiState.value.isTranslationVisible)

        // Update translation
        viewModel.onEvent(
            StoryCustomParamsEvent.UpdateTranslation(
                dialogId = dialogId,
                storyId = storyId,
                isTranslated = true,
                detectedLang = "es",
                translatedText = "Buenas noches",
                targetLang = "en"
            )
        )
        testScheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.selectedParams)
        assertEquals("Buenas noches", viewModel.uiState.value.selectedParams?.translation?.translatedText)

        // Toggle visibility
        viewModel.onEvent(StoryCustomParamsEvent.ToggleTranslationVisibility)
        assertTrue(viewModel.uiState.value.isTranslationVisible)

        // Delete params
        viewModel.onEvent(StoryCustomParamsEvent.DeleteParams(dialogId, storyId))
        testScheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedParams)
        assertFalse(viewModel.uiState.value.isTranslationVisible)

        // Dismiss error
        viewModel.onEvent(StoryCustomParamsEvent.DismissError)
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
