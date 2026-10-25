package net.erezschatz.resonancedrift.playback

import net.erezschatz.resonancedrift.data.QueueState
import net.erezschatz.resonancedrift.playback.QueuePersistence.Start
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Where a restored queue starts when some of its songs could not be loaded (task 13). */
class QueuePersistenceTest {

    private val saved = QueueState(songIds = listOf(1, 2, 3, 4), index = 2, positionMs = 61_000, shuffle = false)

    @Test
    fun `everything loaded - the saved song at the saved position`() {
        assertEquals(Start(2, 61_000), QueuePersistence.startOf(saved, listOf(1, 2, 3, 4)))
    }

    @Test
    fun `a song before it missing - the index moves, the position stays`() {
        assertEquals(Start(1, 61_000), QueuePersistence.startOf(saved, listOf(1, 3, 4)))
    }

    @Test
    fun `the saved song missing - the start of the next one`() {
        assertEquals(Start(2, 0), QueuePersistence.startOf(saved, listOf(1, 2, 4)))
    }

    @Test
    fun `the saved song and everything after missing - the start of the last one`() {
        assertEquals(Start(1, 0), QueuePersistence.startOf(saved, listOf(1, 2)))
    }

    @Test
    fun `repeats in the queue are counted by position`() {
        val withRepeats = QueueState(songIds = listOf(5, 6, 5, 6), index = 3, positionMs = 1_000, shuffle = false)
        assertEquals(Start(1, 0), QueuePersistence.startOf(withRepeats, listOf(5, 5)))
        assertEquals(Start(3, 1_000), QueuePersistence.startOf(withRepeats, listOf(5, 6, 5, 6)))
    }

    @Test
    fun `nothing loaded - nothing to restore`() {
        assertNull(QueuePersistence.startOf(saved, emptyList()))
    }
}
