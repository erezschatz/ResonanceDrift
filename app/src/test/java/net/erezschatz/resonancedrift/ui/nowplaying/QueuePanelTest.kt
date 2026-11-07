package net.erezschatz.resonancedrift.ui.nowplaying

import net.erezschatz.resonancedrift.ui.screens.nowplaying.queueScrollTarget
import org.junit.Assert.assertEquals
import org.junit.Test

class QueuePanelTest {

    @Test
    fun `the current song sits two rows below the top, never above it`() {
        assertEquals(0, queueScrollTarget(0))
        assertEquals(0, queueScrollTarget(1))
        assertEquals(0, queueScrollTarget(2))
        assertEquals(1, queueScrollTarget(3))
        assertEquals(13, queueScrollTarget(15))
    }
}
