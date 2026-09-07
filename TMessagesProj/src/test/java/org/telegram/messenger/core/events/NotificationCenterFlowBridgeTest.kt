package org.telegram.messenger.core.events

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationCenterFlowBridgeTest {

    @Test
    fun testNotificationEventDataIntegrity() {
        val args1: Array<Any> = arrayOf("chat_update", 123L)
        val event1 = NotificationEvent(10, 0, args1)
        val event2 = NotificationEvent(10, 0, arrayOf<Any>("chat_update", 123L))
        val event3 = NotificationEvent(11, 0, args1)
        val eventDifferentAccount = NotificationEvent(10, 1, args1)

        assertEquals(event1, event2)
        assertEquals(event1.hashCode(), event2.hashCode())
        assertNotEquals(event1, event3)
        assertNotEquals(event1, eventDifferentAccount)

        assertEquals(10, event1.id)
        assertEquals(0, event1.account)
        assertEquals(2, event1.args.size)
        assertEquals("chat_update", event1.args[0])
        assertEquals(123L, event1.args[1])
    }

    @Test
    fun testNotificationEventEqualityEdgeCases() {
        val event = NotificationEvent(1, 0, arrayOf<Any>("a"))
        assertTrue(event.equals(event))
        assertFalse(event.equals(null))
        assertFalse(event.equals("not an event"))
    }
}
