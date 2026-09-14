package org.telegram.messenger.feature.system.pinchtozoom

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.telegram.messenger.feature.system.pinchtozoom.data.datasource.PinchToZoomLocalDataSource
import org.telegram.messenger.feature.system.pinchtozoom.data.datasource.PinchToZoomRemoteDataSource
import org.telegram.messenger.feature.system.pinchtozoom.data.repository.PinchToZoomRepositoryImpl
import org.telegram.messenger.feature.system.pinchtozoom.domain.model.PinchImageDimensions
import org.telegram.messenger.feature.system.pinchtozoom.domain.model.PinchTouchPoint

class PinchToZoomRepositoryImplTest {

    private lateinit var localDataSource: PinchToZoomLocalDataSource
    private lateinit var remoteDataSource: PinchToZoomRemoteDataSource
    private lateinit var repository: PinchToZoomRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = PinchToZoomLocalDataSource()
        remoteDataSource = PinchToZoomRemoteDataSource()
        repository = PinchToZoomRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun testInitialState() {
        val state = repository.getState()
        assertFalse(state.isInOverlayMode)
        assertFalse(state.isInTouchMode)
        assertEquals(1f, state.scale, 0.001f)
    }

    @Test
    fun testZoomLifecycle() {
        repository.startZoom(scale = 1.2f)
        var state = repository.getState()
        assertTrue(state.isInOverlayMode)
        assertTrue(state.isInTouchMode)
        assertEquals(1.2f, state.scale, 0.001f)

        repository.updateZoom(scale = 2.0f, translationX = 50f, translationY = -30f)
        state = repository.getState()
        assertEquals(2.0f, state.scale, 0.001f)
        assertEquals(50f, state.translationX, 0.001f)
        assertEquals(-30f, state.translationY, 0.001f)

        repository.finishZoom(progress = 0f)
        state = repository.getState()
        assertFalse(state.isInOverlayMode)
        assertFalse(state.isInTouchMode)

        repository.reset()
        assertEquals(1f, repository.getState().scale, 0.001f)
    }

    @Test
    fun testMathCalculations() {
        val scale = repository.calculateScale(currentDistance = 200f, startDistance = 100f)
        assertEquals(2.0f, scale, 0.001f)

        val translation = repository.calculateTranslation(
            startCenterX = 100f,
            startCenterY = 100f,
            currentCenterX = 150f,
            currentCenterY = 150f,
            scale = 2.0f
        )
        assertNotNull(translation)

        val bounds = repository.calculateImageBounds(
            dimensions = PinchImageDimensions(
                imageX = 0f,
                imageY = 0f,
                imageWidth = 500f,
                imageHeight = 500f,
                fullImageWidth = 1000f,
                fullImageHeight = 1000f
            ),
            scale = 1.5f
        )
        assertNotNull(bounds)

        val decision = repository.evaluatePinchGesture(
            startDistance = 100f,
            startCenterX = 200f,
            startCenterY = 200f,
            point1 = PinchTouchPoint(150f, 200f),
            point2 = PinchTouchPoint(250f, 200f),
            isInOverlay = false
        )
        assertNotNull(decision)
    }

    @Test
    fun testRemoteDataSource() = runBlocking {
        val result = remoteDataSource.syncPinchToZoomConfig()
        assertTrue(result.isSuccess)
    }
}
