package org.telegram.messenger.feature.factcheck

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.factcheck.data.mapper.FactCheckMapper
import org.telegram.messenger.feature.factcheck.domain.model.FactCheckEntityModel
import org.telegram.messenger.feature.factcheck.domain.model.FactCheckLimitsModel
import org.telegram.messenger.feature.factcheck.domain.model.FactCheckModel
import org.telegram.messenger.feature.factcheck.domain.repository.FactCheckRepository
import org.telegram.messenger.feature.factcheck.domain.usecase.ApplyFactCheckUseCase
import org.telegram.messenger.feature.factcheck.domain.usecase.DeleteFactCheckUseCase
import org.telegram.messenger.feature.factcheck.domain.usecase.GetFactCheckLimitUseCase
import org.telegram.messenger.feature.factcheck.domain.usecase.GetFactCheckUseCase
import org.telegram.messenger.feature.factcheck.domain.usecase.LoadFactCheckUseCase
import org.telegram.messenger.feature.factcheck.domain.usecase.ObserveFactCheckLoadedUseCase
import org.telegram.messenger.feature.factcheck.presentation.FactCheckEvent
import org.telegram.messenger.feature.factcheck.presentation.FactCheckViewModel
import org.telegram.tgnet.TLRPC

@OptIn(ExperimentalCoroutinesApi::class)
class FactCheckDomainTest {

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
    fun testFactCheckModelAndLimits() {
        val model = FactCheckModel(
            hash = 123456789L,
            dialogId = -100123456L,
            messageId = 42,
            text = "This statement has been verified as misleading.",
            entities = listOf(
                FactCheckEntityModel(0, 4, "bold", null),
                FactCheckEntityModel(5, 9, "text_url", "https://example.com")
            ),
            country = "US",
            needCheck = false
        )

        assertEquals(123456789L, model.hash)
        assertEquals(-100123456L, model.dialogId)
        assertEquals(42, model.messageId)
        assertEquals("This statement has been verified as misleading.", model.text)
        assertEquals(2, model.entities?.size)
        assertEquals("US", model.country)
        assertFalse(model.needCheck)

        val limits = FactCheckLimitsModel(maxLength = 1024)
        assertEquals(1024, limits.maxLength)
    }

    @Test
    fun testFactCheckMapper() {
        val tlFactCheck = TLRPC.TL_factCheck().apply {
            hash = 99887766L
            need_check = false
            country = "DE"
            text = TLRPC.TL_textWithEntities().apply {
                text = "Fact check message text"
                entities.add(TLRPC.TL_messageEntityBold().apply {
                    offset = 0
                    length = 4
                })
                entities.add(TLRPC.TL_messageEntityTextUrl().apply {
                    offset = 5
                    length = 5
                    url = "https://factcheck.org"
                })
            }
        }

        val domainModel = FactCheckMapper.toDomain(tlFactCheck, -100555L, 77)
        assertEquals(99887766L, domainModel.hash)
        assertEquals(-100555L, domainModel.dialogId)
        assertEquals(77, domainModel.messageId)
        assertEquals("Fact check message text", domainModel.text)
        assertEquals("DE", domainModel.country)
        assertFalse(domainModel.needCheck)
        assertEquals(2, domainModel.entities?.size)

        val boldEntity = domainModel.entities?.get(0)
        assertEquals("bold", boldEntity?.type)
        assertEquals(0, boldEntity?.offset)
        assertEquals(4, boldEntity?.length)

        val urlEntity = domainModel.entities?.get(1)
        assertEquals("text_url", urlEntity?.type)
        assertEquals("https://factcheck.org", urlEntity?.url)

        val tlTextObj = FactCheckMapper.toTlTextWithEntities(
            text = "Reverse mapped text",
            entities = listOf(
                FactCheckEntityModel(0, 7, "bold", null),
                FactCheckEntityModel(8, 6, "italic", null),
                FactCheckEntityModel(15, 4, "text_url", "https://t.me")
            )
        )
        assertEquals("Reverse mapped text", tlTextObj.text)
        assertEquals(3, tlTextObj.entities.size)
        assertTrue(tlTextObj.entities[0] is TLRPC.TL_messageEntityBold)
        assertTrue(tlTextObj.entities[1] is TLRPC.TL_messageEntityItalic)
        assertTrue(tlTextObj.entities[2] is TLRPC.TL_messageEntityTextUrl)
        assertEquals("https://t.me", (tlTextObj.entities[2] as TLRPC.TL_messageEntityTextUrl).url)
    }

    @Test
    fun testFactCheckUseCases() = runTest(testDispatcher) {
        val fakeRepo = FakeFactCheckRepository()

        val observeFactCheckLoadedUseCase = ObserveFactCheckLoadedUseCase(fakeRepo)
        val getFactCheckUseCase = GetFactCheckUseCase(fakeRepo)
        val loadFactCheckUseCase = LoadFactCheckUseCase(fakeRepo)
        val applyFactCheckUseCase = ApplyFactCheckUseCase(fakeRepo)
        val deleteFactCheckUseCase = DeleteFactCheckUseCase(fakeRepo)
        val getFactCheckLimitUseCase = GetFactCheckLimitUseCase(fakeRepo)

        val limit = getFactCheckLimitUseCase()
        assertEquals(1024, limit)

        val cached = getFactCheckUseCase(-100L, 1, 12345L)
        assertNotNull(cached)
        assertEquals("Existing fact check", cached?.text)

        val loaded = loadFactCheckUseCase(-100L, 1)
        assertTrue(loaded is Result.Success)
        assertEquals("Existing fact check", (loaded as Result.Success).data?.text)

        val applyResult = applyFactCheckUseCase(-100L, 1, "New fact check text")
        assertTrue(applyResult is Result.Success)

        val deleteResult = deleteFactCheckUseCase(-100L, 1)
        assertTrue(deleteResult is Result.Success)

        val observation = observeFactCheckLoadedUseCase().first()
        assertEquals(Unit, observation)
    }

    @Test
    fun testFactCheckViewModel() = runTest(testDispatcher) {
        val fakeRepo = FakeFactCheckRepository()
        val viewModel = FactCheckViewModel(
            observeFactCheckLoadedUseCase = ObserveFactCheckLoadedUseCase(fakeRepo),
            getFactCheckUseCase = GetFactCheckUseCase(fakeRepo),
            loadFactCheckUseCase = LoadFactCheckUseCase(fakeRepo),
            applyFactCheckUseCase = ApplyFactCheckUseCase(fakeRepo),
            deleteFactCheckUseCase = DeleteFactCheckUseCase(fakeRepo),
            getFactCheckLimitUseCase = GetFactCheckLimitUseCase(fakeRepo)
        )
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(1024, state.characterLimit)

        viewModel.onEvent(FactCheckEvent.LoadFactCheck(-100L, 1, 12345L))
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals(-100L, state.dialogId)
        assertEquals(1, state.messageId)
        assertEquals("Existing fact check", state.currentInputText)
        assertTrue(state.hasExistingFactCheck)
        assertFalse(state.isLoading)

        viewModel.onEvent(FactCheckEvent.UpdateInputText("Updated text"))
        state = viewModel.uiState.value
        assertEquals("Updated text", state.currentInputText)
        assertEquals(1024 - "Updated text".length, state.remainingCharacters)
        assertFalse(state.isOverLimit)

        viewModel.onEvent(FactCheckEvent.ApplyFactCheck)
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals("Fact check saved", state.actionSuccessMessage)
        assertFalse(state.isSubmitting)

        viewModel.onEvent(FactCheckEvent.ClearMessages)
        state = viewModel.uiState.value
        assertNull(state.actionSuccessMessage)

        viewModel.onEvent(FactCheckEvent.DeleteFactCheck)
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals("Fact check deleted", state.actionSuccessMessage)
        assertNull(state.factCheck)
        assertEquals("", state.currentInputText)
    }

    private class FakeFactCheckRepository : FactCheckRepository {
        val loadedFlow = MutableSharedFlow<Unit>(replay = 1).apply {
            tryEmit(Unit)
        }

        var sample = FactCheckModel(
            hash = 12345L,
            dialogId = -100L,
            messageId = 1,
            text = "Existing fact check",
            entities = emptyList(),
            country = "US",
            needCheck = false
        )

        override fun observeFactCheckLoaded(): Flow<Unit> = loadedFlow

        override suspend fun getFactCheck(dialogId: Long, messageId: Int, hash: Long): FactCheckModel? = sample

        override suspend fun loadFactCheck(dialogId: Long, messageId: Int): Result<FactCheckModel?> =
            Result.Success(sample)

        override suspend fun applyFactCheck(
            dialogId: Long,
            messageId: Int,
            text: String,
            entities: List<FactCheckEntityModel>?
        ): Result<Unit> {
            sample = sample.copy(text = text, entities = entities ?: emptyList())
            return Result.Success(Unit)
        }

        override suspend fun deleteFactCheck(dialogId: Long, messageId: Int): Result<Unit> {
            sample = sample.copy(text = "", entities = emptyList())
            return Result.Success(Unit)
        }

        override suspend fun getFactCheckLimit(): Int = 1024
    }
}
