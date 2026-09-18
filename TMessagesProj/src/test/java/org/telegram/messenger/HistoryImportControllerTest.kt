package org.telegram.messenger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HistoryImportControllerTest {

    private lateinit var controller: HistoryImportController

    @Before
    fun setUp() {
        controller = HistoryImportController.getInstance(0)
        controller.cleanup()
    }

    @Test
    fun testSingletonPerAccount() {
        val controller0 = HistoryImportController.getInstance(0)
        val controller1 = HistoryImportController.getInstance(1)
        val controller0Again = HistoryImportController.getInstance(0)

        assertNotNull(controller0)
        assertNotNull(controller1)
        assertEquals(controller0, controller0Again)
    }

    private fun createStubHistory(): SendMessagesHelper.ImportingHistory {
        val constructor = SendMessagesHelper.ImportingHistory::class.java.getDeclaredConstructor(SendMessagesHelper::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(null)
    }

    private fun createStubStickers(): SendMessagesHelper.ImportingStickers {
        val constructor = SendMessagesHelper.ImportingStickers::class.java.getDeclaredConstructor(SendMessagesHelper::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(null)
    }

    @Test
    fun testHistoryImportLifecycle() {
        val dialogId = 123456789L
        assertFalse(controller.isImportingHistory())
        assertFalse(controller.isImportingHistory(dialogId))
        assertNull(controller.getImportingHistory(dialogId))

        // Create a mock/stub ImportingHistory
        val history = createStubHistory()
        history.dialogId = dialogId
        history.totalSize = 1000L

        controller.putImportingHistory(dialogId, history)
        assertTrue(controller.isImportingHistory())
        assertTrue(controller.isImportingHistory(dialogId))
        assertEquals(history, controller.getImportingHistory(dialogId))

        // Removal
        val removed = controller.removeImportingHistory(dialogId)
        assertEquals(history, removed)
        assertFalse(controller.isImportingHistory())
        assertFalse(controller.isImportingHistory(dialogId))
        assertNull(controller.getImportingHistory(dialogId))
    }

    @Test
    fun testStickersImportLifecycle() {
        val shortName = "test_pack"
        assertFalse(controller.isImportingStickers())
        assertFalse(controller.isImportingStickers(shortName))
        assertNull(controller.getImportingStickers(shortName))

        val stickers = createStubStickers()
        stickers.shortName = shortName
        stickers.title = "Test Pack"

        controller.putImportingStickers(shortName, stickers)
        assertTrue(controller.isImportingStickers())
        assertTrue(controller.isImportingStickers(shortName))
        assertEquals(stickers, controller.getImportingStickers(shortName))

        val removed = controller.removeImportingStickers(shortName)
        assertEquals(stickers, removed)
        assertFalse(controller.isImportingStickers())
        assertFalse(controller.isImportingStickers(shortName))
        assertNull(controller.getImportingStickers(shortName))
    }

    @Test
    fun testCleanup() {
        val history = createStubHistory()
        val stickers = createStubStickers()

        controller.putImportingHistory(100L, history)
        controller.putImportingStickers("pack1", stickers)
        controller.registerHistoryFile("path1", history)
        controller.registerStickerFile("sticker_path", stickers)

        assertTrue(controller.isImportingHistory())
        assertTrue(controller.isImportingStickers())

        controller.cleanup()

        assertFalse(controller.isImportingHistory())
        assertFalse(controller.isImportingStickers())
        assertEquals(0, controller.importingHistoryFiles.size)
        assertEquals(0, controller.importingStickersFiles.size)
    }
}
