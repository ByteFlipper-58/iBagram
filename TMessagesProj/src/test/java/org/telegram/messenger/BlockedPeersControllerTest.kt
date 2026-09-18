package org.telegram.messenger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BlockedPeersControllerTest {

    private lateinit var controller: BlockedPeersController

    @Before
    fun setUp() {
        controller = BlockedPeersController.getInstance(0)
        controller.cleanup()
    }

    @Test
    fun testSingletonPerAccount() {
        val controller0 = BlockedPeersController.getInstance(0)
        val controller1 = BlockedPeersController.getInstance(1)
        val controller0Again = BlockedPeersController.getInstance(0)

        assertNotNull(controller0)
        assertNotNull(controller1)
        assertEquals(controller0, controller0Again)
    }

    @Test
    fun testOnPeerBlockedChangedUser() {
        val userId = 12345678L
        assertFalse(controller.isBlocked(userId))

        controller.onPeerBlockedChanged(userId, true)
        assertTrue(controller.isBlocked(userId))
        assertEquals(1, controller.blockePeers.size())

        // Duplicate block should not duplicate entry
        controller.onPeerBlockedChanged(userId, true)
        assertTrue(controller.isBlocked(userId))
        assertEquals(1, controller.blockePeers.size())

        // Unblock
        controller.onPeerBlockedChanged(userId, false)
        assertFalse(controller.isBlocked(userId))
        assertEquals(0, controller.blockePeers.size())
    }

    @Test
    fun testOnPeerBlockedChangedChannel() {
        val channelId = -100987654321L
        assertFalse(controller.isBlocked(channelId))

        controller.onPeerBlockedChanged(channelId, true)
        assertTrue(controller.isBlocked(channelId))

        controller.onPeerBlockedChanged(channelId, false)
        assertFalse(controller.isBlocked(channelId))
    }

    @Test
    fun testPaginationState() {
        assertEquals(-1, controller.totalBlockedCount)
        assertFalse(controller.loadingBlockedPeers)
        assertFalse(controller.blockedEndReached)

        controller.totalBlockedCount = 42
        controller.loadingBlockedPeers = true
        controller.blockedEndReached = true

        assertEquals(42, controller.totalBlockedCount)
        assertTrue(controller.loadingBlockedPeers)
        assertTrue(controller.blockedEndReached)
    }

    @Test
    fun testCleanup() {
        controller.blockePeers.put(111L, 1)
        controller.blockePeers.put(222L, 1)
        controller.totalBlockedCount = 2
        controller.loadingBlockedPeers = true
        controller.blockedEndReached = true

        controller.cleanup()

        assertEquals(0, controller.blockePeers.size())
        assertEquals(-1, controller.totalBlockedCount)
        assertFalse(controller.loadingBlockedPeers)
        assertFalse(controller.blockedEndReached)
    }
}
