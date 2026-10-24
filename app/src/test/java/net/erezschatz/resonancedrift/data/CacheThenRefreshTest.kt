package net.erezschatz.resonancedrift.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.erezschatz.resonancedrift.ampache.AmpacheException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** The cache-first pattern, with a MutableStateFlow standing in for Room's Flow. */
class CacheThenRefreshTest {

    @Test
    fun `cached items come out first, then the refreshed ones`() = runTest(UnconfinedTestDispatcher()) {
        val table = MutableStateFlow(listOf("cached"))
        val fetched = CompletableDeferred<List<String>>()
        val seen = mutableListOf<Listing<String>>()

        val collecting = launch {
            LibraryRepository.cacheThenRefresh(table) { table.value = fetched.await() }.toList(seen)
        }
        assertEquals(listOf(Listing(listOf("cached"), Refresh.Running)), seen)

        fetched.complete(listOf("fresh", "rows"))
        assertEquals(Listing(listOf("fresh", "rows"), Refresh.Done), seen.last())
        collecting.cancel()
    }

    @Test
    fun `a failed refresh keeps the cached items and reports the error`() = runTest {
        val table = MutableStateFlow(listOf("cached"))
        val error = AmpacheException.Unreachable(java.io.IOException("offline"))

        val last = LibraryRepository.cacheThenRefresh(table) { throw error }
            .first { it.refresh != Refresh.Running }

        assertEquals(listOf("cached"), last.items)
        assertEquals(Refresh.Failed(error), last.refresh)
    }

    @Test
    fun `random offset leaves a full page and stays at zero for a small or unknown library`() {
        val random = Random(42)
        repeat(1000) {
            val offset = LibraryRepository.randomOffset(total = 2712, count = 100, random = random)
            assertTrue(offset in 0..2612)
        }
        assertEquals(0, LibraryRepository.randomOffset(total = 0, count = 100, random = random))
        assertEquals(0, LibraryRepository.randomOffset(total = 80, count = 100, random = random))
        assertEquals(0, LibraryRepository.randomOffset(total = 100, count = 100, random = random))
    }
}
