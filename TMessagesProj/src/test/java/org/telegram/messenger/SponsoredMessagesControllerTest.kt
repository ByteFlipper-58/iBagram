package org.telegram.messenger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SponsoredMessagesControllerTest {

    private lateinit var controller: SponsoredMessagesController

    @Before
    fun setUp() {
        controller = SponsoredMessagesController.getInstance(0)
        controller.cleanup()
    }

    @Test
    fun testSingletonPerAccount() {
        val controller0 = SponsoredMessagesController.getInstance(0)
        val controller1 = SponsoredMessagesController.getInstance(1)
        val controller0Again = SponsoredMessagesController.getInstance(0)

        assertNotNull(controller0)
        assertNotNull(controller1)
        assertEquals(controller0, controller0Again)
    }

    @Test
    fun testDefaultSettings() {
        assertEquals(30, controller.channelRestrictSponsoredLevelMin)
        assertFalse(controller.sponsoredLinksInappAllow)

        controller.channelRestrictSponsoredLevelMin = 15
        controller.sponsoredLinksInappAllow = true

        assertEquals(15, controller.channelRestrictSponsoredLevelMin)
        assertTrue(controller.sponsoredLinksInappAllow)
    }

    @Test
    fun testCacheRetrievalAndTtl() {
        val dialogId = -100123456789L
        val info = MessagesController.SponsoredMessagesInfo()
        info.loadTime = System.currentTimeMillis()
        info.loading = false
        controller.sponsoredMessages.put(dialogId, info)

        val retrieved = controller.getSponsoredMessages(dialogId)
        assertNotNull(retrieved)
        assertEquals(info, retrieved)
    }

    @Test
    fun testCacheRetrievalWhileLoading() {
        val dialogId = -100987654321L
        val info = MessagesController.SponsoredMessagesInfo()
        info.loading = true
        controller.sponsoredMessages.put(dialogId, info)

        val retrieved = controller.getSponsoredMessages(dialogId)
        assertNotNull(retrieved)
        assertTrue(retrieved!!.loading)
    }

    @Test
    fun testCleanup() {
        val dialogId = -100111222333L
        val info = MessagesController.SponsoredMessagesInfo()
        controller.sponsoredMessages.put(dialogId, info)
        assertEquals(1, controller.sponsoredMessages.size())

        controller.cleanup()
        assertEquals(0, controller.sponsoredMessages.size())
    }

    @Test
    fun testMarkSponsoredAsRead() {
        // markSponsoredAsRead is currently a no-op / stub in upstream Telegram
        controller.markSponsoredAsRead(-100111L, null)
    }
}
