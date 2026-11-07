package net.erezschatz.resonancedrift.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first

/**
 * Lists mark the song the player is on (step 18, D42). The match is by song id,
 * not by queue, so an album reached from a shuffle queue is marked too.
 */

/** The first row holding [currentSongId], or -1 when nothing of this list is playing. */
fun playingRowIndex(songIds: List<Long>, currentSongId: Long?): Int {
    if (currentSongId == null) return -1
    return songIds.indexOf(currentSongId)
}

/**
 * The `scrollOffset` for [LazyListState.scrollToItem] that puts a row of
 * [rowHeightPx] in the middle of a viewport of [viewportHeightPx]. Negative,
 * because a positive offset scrolls the row further up, off the top; zero when
 * the row is as tall as the viewport or taller.
 */
fun centeredScrollOffset(viewportHeightPx: Int, rowHeightPx: Int): Int =
    -((viewportHeightPx - rowHeightPx) / 2).coerceAtLeast(0)

/**
 * Scrolls [listState] once so that list item [itemIndex] sits mid-screen: the
 * playing song, when a list is opened with one of its songs playing. It runs
 * when the index first becomes valid (the songs arrive after the screen does)
 * and never again on that screen: the highlight follows the player, the scroll
 * does not (D42). Nothing happens when the row is already fully in view or the
 * user has scrolled first.
 */
@Composable
fun ScrollToPlayingRowOnce(listState: LazyListState, itemIndex: Int) {
    // Saveable, so a rotation does not scroll the list back to the song.
    var done by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(itemIndex) {
        if (done || itemIndex < 0) return@LaunchedEffect
        // Wait for a real viewport, as QueuePanel does (task 17): a scroll asked
        // of a list that has not been laid out is clamped away.
        snapshotFlow { listState.layoutInfo.viewportEndOffset > listState.layoutInfo.viewportStartOffset }.first { it }
        done = true
        if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) return@LaunchedEffect
        val before = listState.layoutInfo
        val shown = before.visibleItemsInfo.firstOrNull { it.index == itemIndex }
        if (shown != null && shown.offset >= before.viewportStartOffset && shown.offset + shown.size <= before.viewportEndOffset) {
            return@LaunchedEffect
        }
        // The row's height is known only once it is laid out: bring it to the
        // top first, measure it, then centre it. Both scrolls are instant.
        listState.scrollToItem(itemIndex)
        val after = listState.layoutInfo
        val row = after.visibleItemsInfo.firstOrNull { it.index == itemIndex } ?: return@LaunchedEffect
        listState.scrollToItem(itemIndex, centeredScrollOffset(after.viewportEndOffset - after.viewportStartOffset, row.size))
    }
}
