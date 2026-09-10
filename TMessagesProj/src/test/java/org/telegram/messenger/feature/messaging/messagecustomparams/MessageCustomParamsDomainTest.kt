package org.telegram.messenger.feature.messaging.messagecustomparams

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.telegram.messenger.feature.messaging.messagecustomparams.data.mapper.MessageCustomParamsMapper
import org.telegram.messenger.feature.messaging.messagecustomparams.data.repository.LegacyMessageCustomParamsRepository
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.model.MessageCustomParamsModel
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.model.MessageSummaryParamsModel
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.model.MessageTranslationParamsModel
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.model.StarsErrorParamsModel
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.model.VoiceTranscriptionParamsModel
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.CheckMessageCustomParamsEmptyUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.ClearAllMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.CopyMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.GetMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.MergeMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.ObserveMessageCustomParamsStateUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.RemoveMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.SetMessageCustomParamsUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.UpdateMessageSummaryUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.UpdateMessageTranslationUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.domain.usecase.UpdateVoiceTranscriptionUseCase
import org.telegram.messenger.feature.messaging.messagecustomparams.presentation.MessageCustomParamsEvent
import org.telegram.messenger.feature.messaging.messagecustomparams.presentation.MessageCustomParamsViewModel
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class MessageCustomParamsDomainTest {

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
    fun testCheckMessageCustomParamsEmptyUseCase() {
        val checkEmpty = CheckMessageCustomParamsEmptyUseCase()

        assertTrue(checkEmpty(null))
        assertTrue(checkEmpty(MessageCustomParamsModel()))

        // Has voice transcription text -> not empty
        val withVoice = MessageCustomParamsModel(
            voiceTranscription = VoiceTranscriptionParamsModel(text = "Hello world")
        )
        assertFalse(checkEmpty(withVoice))

        // Has premium effect -> not empty
        val withEffect = MessageCustomParamsModel(premiumEffectWasPlayed = true)
        assertFalse(checkEmpty(withEffect))

        // Has translation -> not empty
        val withTrans = MessageCustomParamsModel(
            translation = MessageTranslationParamsModel(translatedText = "Bonjour")
        )
        assertFalse(checkEmpty(withTrans))
    }

    @Test
    fun testMergeMessageCustomParamsUseCase() {
        val merger = MergeMessageCustomParamsUseCase()

        val base = MessageCustomParamsModel(
            messageId = 42L,
            voiceTranscription = VoiceTranscriptionParamsModel(text = "Initial voice text", isOpen = true),
            translation = MessageTranslationParamsModel(originalLanguage = "en")
        )

        val updates = MessageCustomParamsModel(
            messageId = 42L,
            translation = MessageTranslationParamsModel(originalLanguage = "en", translatedToLanguage = "ru", translatedText = "Привет"),
            premiumEffectWasPlayed = true
        )

        val merged = merger(base, updates)

        // Preserved voice transcription
        assertNotNull(merged.voiceTranscription)
        assertEquals("Initial voice text", merged.voiceTranscription?.text)
        assertTrue(merged.voiceTranscription?.isOpen == true)

        // Updated translation
        assertNotNull(merged.translation)
        assertEquals("ru", merged.translation?.translatedToLanguage)
        assertEquals("Привет", merged.translation?.translatedText)

        // Updated effect flag
        assertTrue(merged.premiumEffectWasPlayed)
    }

    @Test
    fun testMapperLogic() {
        val message = TLRPC.TL_message().apply {
            id = 100
            voiceTranscription = "Audio text"
            voiceTranscriptionOpen = true
            voiceTranscriptionFinal = true
            voiceTranscriptionId = 9999L
            premiumEffectWasPlayed = true
            originalLanguage = "es"
            translatedToLanguage = "en"
            errorAllowedPriceStars = 50L
            errorNewPriceStars = 100L
        }

        val domain = MessageCustomParamsMapper.toDomain(message)

        assertEquals(100L, domain.messageId)
        assertNotNull(domain.voiceTranscription)
        assertEquals("Audio text", domain.voiceTranscription?.text)
        assertTrue(domain.voiceTranscription?.isOpen == true)
        assertTrue(domain.voiceTranscription?.isFinal == true)
        assertEquals(9999L, domain.voiceTranscription?.transcriptionId)
        assertTrue(domain.premiumEffectWasPlayed)
        assertEquals("es", domain.translation?.originalLanguage)
        assertEquals("en", domain.translation?.translatedToLanguage)
        assertEquals(50L, domain.starsError?.errorAllowedPriceStars)
        assertEquals(100L, domain.starsError?.errorNewPriceStars)

        // Apply back to new message
        val targetMessage = TLRPC.TL_message().apply { id = 200 }
        MessageCustomParamsMapper.applyToMessage(domain, targetMessage)

        assertEquals("Audio text", targetMessage.voiceTranscription)
        assertTrue(targetMessage.voiceTranscriptionOpen)
        assertTrue(targetMessage.voiceTranscriptionFinal)
        assertEquals(9999L, targetMessage.voiceTranscriptionId)
        assertTrue(targetMessage.premiumEffectWasPlayed)
        assertEquals("es", targetMessage.originalLanguage)
        assertEquals("en", targetMessage.translatedToLanguage)
        assertEquals(50L, targetMessage.errorAllowedPriceStars)
        assertEquals(100L, targetMessage.errorNewPriceStars)
    }

    @Test
    fun testRepositoryOperations() {
        val repository = LegacyMessageCustomParamsRepository(currentAccount = 0)

        assertEquals(0, repository.getState().cachedParamsCount)

        val params1 = MessageCustomParamsModel(
            messageId = 1L,
            voiceTranscription = VoiceTranscriptionParamsModel(text = "Voice 1")
        )
        val params2 = MessageCustomParamsModel(
            messageId = 2L,
            summary = MessageSummaryParamsModel(summaryText = "Summary 2")
        )

        repository.setParamsForMessage(1L, params1)
        repository.setParamsForMessage(2L, params2)

        assertEquals(2, repository.getState().cachedParamsCount)
        assertEquals("Voice 1", repository.getParamsForMessage(1L)?.voiceTranscription?.text)
        assertEquals("Summary 2", repository.getParamsForMessage(2L)?.summary?.summaryText)

        // Copy params from 1L to 3L
        repository.copyParams(fromMessageId = 1L, toMessageId = 3L)
        assertEquals(3, repository.getState().cachedParamsCount)
        assertEquals("Voice 1", repository.getParamsForMessage(3L)?.voiceTranscription?.text)
        assertEquals(3L, repository.getParamsForMessage(3L)?.messageId)

        // Remove params for 2L
        repository.removeParams(2L)
        assertEquals(2, repository.getState().cachedParamsCount)
        assertNull(repository.getParamsForMessage(2L))

        // Clear all
        repository.clearAll()
        assertEquals(0, repository.getState().cachedParamsCount)
        assertNull(repository.getParamsForMessage(1L))
    }

    @Test
    fun testMessageCustomParamsViewModelMviFlow() = runTest(testDispatcher) {
        val repository = LegacyMessageCustomParamsRepository(currentAccount = 0)

        val observeState = ObserveMessageCustomParamsStateUseCase(repository)
        val getParams = GetMessageCustomParamsUseCase(repository)
        val setParams = SetMessageCustomParamsUseCase(repository)
        val updateVoice = UpdateVoiceTranscriptionUseCase(repository)
        val updateTranslation = UpdateMessageTranslationUseCase(repository)
        val updateSummary = UpdateMessageSummaryUseCase(repository)
        val copyParams = CopyMessageCustomParamsUseCase(repository)
        val removeParams = RemoveMessageCustomParamsUseCase(repository)
        val clearAll = ClearAllMessageCustomParamsUseCase(repository)

        val viewModel = MessageCustomParamsViewModel(
            observeState = observeState,
            getParams = getParams,
            setParams = setParams,
            updateVoice = updateVoice,
            updateTranslation = updateTranslation,
            updateSummary = updateSummary,
            copyParams = copyParams,
            removeParams = removeParams,
            clearAll = clearAll,
            repository = repository
        )

        advanceUntilIdle()

        val initialState = viewModel.uiState.value
        assertEquals(0, initialState.state.cachedParamsCount)
        assertNull(initialState.currentParams)

        // Event: SetParams
        val model = MessageCustomParamsModel(
            messageId = 10L,
            voiceTranscription = VoiceTranscriptionParamsModel(text = "Hello", isOpen = false)
        )
        viewModel.onEvent(MessageCustomParamsEvent.SetParams(10L, model))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.state.cachedParamsCount)
        assertEquals("Hello", viewModel.uiState.value.currentParams?.voiceTranscription?.text)
        assertFalse(viewModel.uiState.value.isTranscriptionOpen)

        // Event: UpdateTranscription with isOpen = true
        viewModel.onEvent(MessageCustomParamsEvent.UpdateTranscription(10L, text = "Hello revised", isFinal = true, isOpen = true))
        advanceUntilIdle()

        assertEquals("Hello revised", viewModel.uiState.value.currentParams?.voiceTranscription?.text)
        assertTrue(viewModel.uiState.value.isTranscriptionOpen)

        // Event: UpdateTranslation
        viewModel.onEvent(MessageCustomParamsEvent.UpdateTranslation(10L, targetLanguage = "de", translatedText = "Hallo"))
        advanceUntilIdle()

        assertEquals("de", viewModel.uiState.value.currentParams?.translation?.translatedToLanguage)
        assertEquals("Hallo", viewModel.uiState.value.currentParams?.translation?.translatedText)

        // Event: CopyParams
        viewModel.onEvent(MessageCustomParamsEvent.CopyParams(10L, 20L))
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.state.cachedParamsCount)
        assertEquals(20L, viewModel.uiState.value.currentParams?.messageId)

        // Event: ClearAll
        viewModel.onEvent(MessageCustomParamsEvent.ClearAll)
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.state.cachedParamsCount)
        assertNull(viewModel.uiState.value.currentParams)
        assertEquals("All params cleared", viewModel.uiState.value.statusMessage)
    }
}
