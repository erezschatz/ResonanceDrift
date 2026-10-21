package net.erezschatz.resonancedrift.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.ZoneOffset

class PlaybackLogTest {

    @get:Rule
    val folder = TemporaryFolder()

    private var now = 1_000L
    private fun newLog(capacity: Int = 5) =
        PlaybackLog(folder.root, capacity = capacity, clock = { now++ }, zone = ZoneOffset.UTC)

    @Test
    fun `ring buffer keeps only the newest entries`() {
        val log = newLog(capacity = 5)
        log.startRun("start")
        (1..10).forEach { log.log("e$it") }
        assertEquals(listOf("e6", "e7", "e8", "e9", "e10"), log.entries().map { it.text })
    }

    @Test
    fun `starting a run makes the last run the previous one`() {
        val first = newLog()
        first.startRun("run 1")
        first.log("a")
        first.log("b")

        // A new process: a fresh PlaybackLog over the same directory.
        val second = newLog()
        second.startRun("run 2")
        second.log("c")

        assertEquals(listOf("run 1", "a", "b"), second.previousRun().map { it.text })
        assertEquals(listOf("run 2", "c"), second.entries().map { it.text })
    }

    @Test
    fun `the file stays bounded and still holds the newest entries`() {
        val log = newLog(capacity = 5)
        log.startRun("start")
        (1..23).forEach { log.log("e$it") }
        val lines = folder.root.resolve("current.log").readLines()
        assertTrue("at most twice the capacity, was ${lines.size}", lines.size <= 10)
        assertTrue(lines.last().endsWith("\te23"))

        newLog(capacity = 5).startRun("next")
        assertEquals(listOf("e19", "e20", "e21", "e22", "e23"), newLog(capacity = 5).previousRun().map { it.text })
    }

    @Test
    fun `only one previous run is kept`() {
        newLog().startRun("run 1")
        newLog().startRun("run 2")
        val third = newLog()
        third.startRun("run 3")
        assertEquals(listOf("run 2"), third.previousRun().map { it.text })
    }

    @Test
    fun `tokens never reach the log`() {
        val log = newLog()
        log.startRun("start")
        log.log("Unable to connect to https://h/play/index.php?ssid=abc123&type=song&oid=1")
        log.log("GET https://h/server/json.server.php?action=ping&auth=def456")
        val text = log.entries().joinToString { it.text }
        assertFalse(text.contains("abc123"))
        assertFalse(text.contains("def456"))
        assertTrue(text.contains("ssid=…&type=song"))
    }

    @Test
    fun `report has the header, this run and the previous run with times`() {
        newLog().apply {
            startRun("old run")
        }
        val log = newLog()
        log.startRun("new run")
        log.log("state READY")
        val report = log.report(listOf("ResonanceDrift diagnostics", "1.5 (6)"))
        assertTrue(report.startsWith("ResonanceDrift diagnostics\n1.5 (6)\n"))
        assertTrue(report.contains("== This run (2 events) ==\n01-01 00:00:01.001  new run\n"))
        assertTrue(report.contains("== Previous run (1 events) ==\n01-01 00:00:01.000  old run\n"))
    }

    @Test
    fun `multi-line text stays one entry`() {
        val log = newLog()
        log.startRun("start")
        log.log("line one\nline two")
        newLog().startRun("next")
        assertEquals("line one line two", newLog().previousRun().last().text)
    }
}
