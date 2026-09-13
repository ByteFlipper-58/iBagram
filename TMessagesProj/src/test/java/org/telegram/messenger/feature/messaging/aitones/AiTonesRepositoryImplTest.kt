package org.telegram.messenger.feature.messaging.aitones

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.core.result.Result
import org.telegram.messenger.feature.messaging.aitones.data.datasource.AiTonesLocalDataSource
import org.telegram.messenger.feature.messaging.aitones.data.datasource.AiTonesRemoteDataSource
import org.telegram.messenger.feature.messaging.aitones.data.repository.AiTonesRepositoryImpl
import org.telegram.messenger.feature.messaging.aitones.domain.model.AiToneModel
import org.telegram.tgnet.tl.TL_aicompose

@OptIn(ExperimentalCoroutinesApi::class)
class AiTonesRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var localDataSource: AiTonesLocalDataSource
    private lateinit var remoteDataSource: FakeAiTonesRemoteDataSource
    private lateinit var repository: AiTonesRepositoryImpl

    private class FakeAiTonesRemoteDataSource : AiTonesRemoteDataSource(0) {
        var remoteTones: TL_aicompose.Tones = TL_aicompose.TL_tones().apply {
            this.hash = 12345L
            this.tones.add(TL_aicompose.TL_aiComposeTone().apply {
                this.id = 1L
                this.title = "Friendly"
                this.prompt = "Be polite and friendly"
            })
        }
        var unsaveSuccess: Boolean = true

        override suspend fun getTones(hash: Long): Result<TL_aicompose.Tones> {
            return Result.Success(remoteTones)
        }

        override suspend fun unsaveTone(tone: TL_aicompose.AiComposeTone): Result<Boolean> {
            return Result.Success(unsaveSuccess)
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        localDataSource = AiTonesLocalDataSource(0)
        remoteDataSource = FakeAiTonesRemoteDataSource()
        repository = AiTonesRepositoryImpl(
            currentAccount = 0,
            remoteDataSource = remoteDataSource,
            localDataSource = localDataSource,
            mainDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadTonesSuccess() = runTest {
        val result = repository.loadTones(force = true)
        assertTrue(result is Result.Success)
        val state = (result as Result.Success).data
        assertEquals(1, state.tones.size)
        assertEquals("Friendly", state.tones[0].title)
    }

    @Test
    fun testAddTone() = runTest {
        val tone = AiToneModel(
            id = 2L,
            title = "Humorous",
            prompt = "Add some witty humor"
        )
        val addRes = repository.addTone(tone)
        assertTrue(addRes is Result.Success)

        val state = repository.getTonesState()
        assertEquals(1, state.tones.size)
        assertEquals("Humorous", state.tones[0].title)
    }

    @Test
    fun testEditTone() = runTest {
        val initialTone = AiToneModel(id = 3L, title = "Formal", prompt = "Use business language")
        repository.addTone(initialTone)

        val updatedTone = AiToneModel(id = 3L, title = "Very Formal", prompt = "Strict executive tone")
        val editRes = repository.editTone(updatedTone)
        assertTrue(editRes is Result.Success)

        val state = repository.getTonesState()
        assertEquals(1, state.tones.size)
        assertEquals("Very Formal", state.tones[0].title)
        assertEquals("Strict executive tone", state.tones[0].prompt)
    }

    @Test
    fun testRemoveTone() = runTest {
        val tone = AiToneModel(id = 4L, title = "Casual", prompt = "Casual chat")
        repository.addTone(tone)
        assertEquals(1, repository.getTonesState().tones.size)

        val removeRes = repository.removeTone(tone)
        assertTrue(removeRes is Result.Success)
        assertTrue(repository.getTonesState().tones.isEmpty())
    }
}
