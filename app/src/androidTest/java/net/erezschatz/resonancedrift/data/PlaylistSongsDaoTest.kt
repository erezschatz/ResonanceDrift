package net.erezschatz.resonancedrift.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The version counter: a removal made while a refresh was fetching survives that refresh. */
@RunWith(AndroidJUnit4::class)
class PlaylistSongsDaoTest {

    @get:Rule val rule = DbTestRule()
    private val dao get() = rule.db.playlistSongs()

    @Test
    fun aRefreshWritesWhenNothingChangedMeanwhile() = runTest {
        val start = dao.versionOf("12")
        assertEquals(0, start)

        assertTrue(dao.replaceIfUnchanged("12", start, listOf(1, 2, 3)))

        val row = dao.get("12")
        assertEquals(listOf(1L, 2L, 3L), row?.songIds)
        assertEquals(1, row?.version)
    }

    @Test
    fun aRemovalDuringARefreshIsNotUndone() = runTest {
        dao.replaceIfUnchanged("12", 0, listOf(1, 2, 3))

        val refreshStart = dao.versionOf("12")
        // ...the refresh is fetching; meanwhile the user removes song 2...
        dao.removeSong("12", index = 1, songId = 2)
        // ...and the fetch returns the list as it was before the removal.
        val wrote = dao.replaceIfUnchanged("12", refreshStart, listOf(1, 2, 3))

        assertFalse(wrote)
        assertEquals(listOf(1L, 3L), dao.get("12")?.songIds)
    }

    @Test
    fun theNextRefreshAfterARemovalWritesAgain() = runTest {
        dao.replaceIfUnchanged("12", 0, listOf(1, 2, 3))
        dao.removeSong("12", index = 1, songId = 2)

        assertTrue(dao.replaceIfUnchanged("12", dao.versionOf("12"), listOf(1, 3, 4)))
        assertEquals(listOf(1L, 3L, 4L), dao.get("12")?.songIds)
    }

    @Test
    fun removalByStaleIndexFallsBackToTheFirstOccurrenceOfTheSong() = runTest {
        dao.replaceIfUnchanged("12", 0, listOf(5, 6, 7, 6))

        dao.removeSong("12", index = 0, songId = 6)

        assertEquals(listOf(5L, 7L, 6L), dao.get("12")?.songIds)
    }

    @Test
    fun removalOfASongNotCachedStillBumpsTheVersion() = runTest {
        dao.replaceIfUnchanged("12", 0, listOf(1, 2))
        val before = dao.versionOf("12")

        dao.removeSong("12", index = 5, songId = 99)

        assertEquals(listOf(1L, 2L), dao.get("12")?.songIds)
        assertEquals(before + 1, dao.versionOf("12"))
    }
}
