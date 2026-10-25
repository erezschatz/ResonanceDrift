package net.erezschatz.resonancedrift.playback

import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * What the player did, in words: a ring buffer of the last [capacity] events
 * of this run, mirrored to a file so that after a crash or a kill the next
 * run can still report the previous one. One run = one PlaybackService
 * lifetime; [startRun] rotates `current.log` to `previous.log`.
 *
 * Called from the main thread and from ExoPlayer's loading thread, hence
 * `@Synchronized`. Writes are one short line appended per event.
 */
class PlaybackLog(
    private val dir: File,
    private val capacity: Int = DEFAULT_CAPACITY,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    data class Entry(val timeMillis: Long, val text: String)

    private val entries = ArrayDeque<Entry>()
    private var linesInFile = 0

    private val currentFile get() = File(dir, "current.log")
    private val previousFile get() = File(dir, "previous.log")

    /** Service start: the last run becomes "previous", this one starts empty. */
    @Synchronized
    fun startRun(firstLine: String) {
        dir.mkdirs()
        if (currentFile.exists()) {
            previousFile.delete()
            currentFile.renameTo(previousFile)
        }
        entries.clear()
        linesInFile = 0
        log(firstLine)
    }

    @Synchronized
    fun log(text: String) {
        val entry = Entry(clock(), scrub(text).replace('\n', ' '))
        entries.addLast(entry)
        while (entries.size > capacity) entries.removeFirst()
        persist(entry)
    }

    @Synchronized
    fun entries(): List<Entry> = entries.toList()

    /** The previous run as it was written to disk, oldest first. */
    @Synchronized
    fun previousRun(): List<Entry> =
        try {
            if (previousFile.exists()) previousFile.readLines().mapNotNull { parse(it) }.takeLast(capacity) else emptyList()
        } catch (e: IOException) {
            emptyList()
        }

    /** The text the diagnostics button copies: [header] lines, then this run, then the previous one. */
    @Synchronized
    fun report(header: List<String>): String {
        val out = StringBuilder()
        header.forEach { out.append(it).append('\n') }
        out.append("\n== This run (").append(entries.size).append(" events) ==\n")
        entries.forEach { out.append(format(it)).append('\n') }
        val previous = previousRun()
        out.append("\n== Previous run (").append(previous.size).append(" events) ==\n")
        previous.forEach { out.append(format(it)).append('\n') }
        return out.toString()
    }

    /**
     * Appends; when the file holds twice the capacity, rewrites it with the
     * ring buffer, so the file stays bounded without rewriting the whole buffer per event.
     */
    private fun persist(entry: Entry) {
        try {
            if (linesInFile >= capacity * 2) {
                currentFile.writeText(entries.joinToString("") { "${it.timeMillis}\t${it.text}\n" })
                linesInFile = entries.size
            } else {
                currentFile.appendText("${entry.timeMillis}\t${entry.text}\n")
                linesInFile++
            }
        } catch (e: IOException) {
            // The in-memory log still works; losing the file only loses the next run's "previous".
        }
    }

    private fun parse(line: String): Entry? {
        val tab = line.indexOf('\t')
        if (tab <= 0) return null
        val time = line.substring(0, tab).toLongOrNull() ?: return null
        return Entry(time, line.substring(tab + 1))
    }

    private fun format(entry: Entry): String =
        TIME_FORMAT.format(Instant.ofEpochMilli(entry.timeMillis).atZone(zone)) + "  " + entry.text

    companion object {
        // About ten hours of listening (~140 events an hour); 300 lost the start of a two-hour test (task 12).
        const val DEFAULT_CAPACITY = 1500

        private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS")

        // The report is pasted into chats and issues; session tokens must not be.
        private val TOKEN_PARAM = Regex("""\b(ssid|auth)=[^&\s]+""")

        fun scrub(text: String): String = TOKEN_PARAM.replace(text) { "${it.groupValues[1]}=…" }
    }
}
