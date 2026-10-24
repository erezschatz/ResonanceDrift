package net.erezschatz.resonancedrift.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShuffleHistoryAndQueueTest {

    @get:Rule val rule = DbTestRule()

    @Test
    fun shuffleHistoryKeepsTheNewestFifty() = runTest {
        val history = ShuffleHistory(rule.db.shuffleHistory())

        for (albumId in 1L..60L) history.add(albumId)

        val recent = history.recentAlbumIds()
        assertEquals(ShuffleHistory.LIMIT, recent.size)
        assertEquals((60L downTo 11L).toList(), recent)
    }

    @Test
    fun reAddingAnAlbumMovesItToTheFrontWithoutDuplicating() = runTest {
        val history = ShuffleHistory(rule.db.shuffleHistory())

        history.add(1)
        history.add(2)
        history.add(1)

        assertEquals(listOf(1L, 2L), history.recentAlbumIds())
    }

    @Test
    fun queueStateRoundTripsAndOverwrites() = runTest {
        val store = QueueStateStore(rule.db.queueState())
        assertNull(store.load())

        store.save(QueueState(listOf(3, 1, 3), index = 2, positionMs = 61_500, shuffle = true))
        val latest = QueueState(listOf(9, 8), index = 1, positionMs = 1_000, shuffle = false)
        store.save(latest)

        assertEquals(latest, store.load())
        store.clear()
        assertNull(store.load())
    }
}
