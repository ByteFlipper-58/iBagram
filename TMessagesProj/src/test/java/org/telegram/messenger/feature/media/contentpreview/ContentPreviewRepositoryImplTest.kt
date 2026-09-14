package org.telegram.messenger.feature.media.contentpreview

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.media.contentpreview.data.datasource.ContentPreviewLocalDataSource
import org.telegram.messenger.feature.media.contentpreview.data.datasource.ContentPreviewRemoteDataSource
import org.telegram.messenger.feature.media.contentpreview.data.repository.ContentPreviewRepositoryImpl
import org.telegram.messenger.feature.media.contentpreview.domain.model.ContentPreviewItem
import org.telegram.messenger.feature.media.contentpreview.domain.model.PreviewActionItem
import org.telegram.messenger.feature.media.contentpreview.domain.model.PreviewActionType
import org.telegram.messenger.feature.media.contentpreview.domain.model.PreviewContentType

class ContentPreviewRepositoryImplTest {

    private lateinit var localDataSource: ContentPreviewLocalDataSource
    private lateinit var remoteDataSource: ContentPreviewRemoteDataSource
    private lateinit var repository: ContentPreviewRepositoryImpl

    private val sampleItem = ContentPreviewItem(
        contentType = PreviewContentType.STICKER,
        documentId = 12345L,
        emoticon = "🔥",
        canSend = true
    )

    private val sampleActions = listOf(
        PreviewActionItem(
            action = PreviewActionType.SEND,
            title = "Send"
        ),
        PreviewActionItem(
            action = PreviewActionType.TOGGLE_FAVORITE,
            title = "Add to Favorites"
        )
    )

    @Before
    fun setUp() {
        localDataSource = ContentPreviewLocalDataSource(testMode = true)
        remoteDataSource = ContentPreviewRemoteDataSource(currentAccount = 0)
        repository = ContentPreviewRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun testInitialState() = runTest {
        val state = repository.getState()
        assertFalse(state.isVisible)
        assertFalse(state.isMenuVisible)
        assertNull(state.currentItem)
        assertEquals(0, state.availableActions.size)

        val flowState = repository.observeState().first()
        assertFalse(flowState.isVisible)
    }

    @Test
    fun testOpenPreviewAndDragProgress() = runTest {
        repository.openPreview(sampleItem, sampleActions)

        var state = repository.getState()
        assertTrue(state.isVisible)
        assertFalse(state.isMenuVisible)
        assertEquals(sampleItem, state.currentItem)
        assertEquals(2, state.availableActions.size)

        repository.updateDragProgress(dragProgress = 0.8f, isMenuVisible = true)
        state = repository.getState()
        assertEquals(0.8f, state.dragProgress, 0.001f)
        assertTrue(state.isMenuVisible)

        repository.dismissPreview()
        state = repository.getState()
        assertFalse(state.isVisible)
        assertNull(state.currentItem)
    }

    @Test
    fun testClear() = runTest {
        repository.openPreview(sampleItem, sampleActions)
        assertTrue(repository.getState().isVisible)

        repository.clear()
        assertFalse(repository.getState().isVisible)
        assertNull(repository.getState().currentItem)
    }

    @Test
    fun testRemoteDataSource() = runTest {
        val policyRes = remoteDataSource.fetchContentPreviewPolicy()
        assertTrue(policyRes.isSuccess)
    }
}
