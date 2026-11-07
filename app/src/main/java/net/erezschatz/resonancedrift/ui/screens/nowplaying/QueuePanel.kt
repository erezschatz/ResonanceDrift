package net.erezschatz.resonancedrift.ui.screens.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.first
import net.erezschatz.resonancedrift.ui.components.SongRow
import net.erezschatz.resonancedrift.ui.player.QueueEntry

/**
 * The queue as a bottom sheet: every item, the current one highlighted and
 * scrolled into view. Tap an item to play it; ✕ takes it out of the queue.
 * No reordering (task 11: only if cheap; it is not).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueuePanel(
    queue: List<QueueEntry>,
    currentIndex: Int,
    onJump: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val listState = rememberLazyListState()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        // Scrolled in an effect, not through initialFirstVisibleItemIndex: with
        // that, the sheet opened at the top of the queue on the phone (task 17).
        // The sheet lays its content out before it is shown, and a scroll asked
        // of a list without a viewport is clamped away, so wait for a real one.
        // Keyed on the index, so the list also follows auto-advance and picks
        // while the panel is open; on the size too, so a queue that grows under
        // the sheet (shuffle appending an album) keeps the current song in view.
        LaunchedEffect(currentIndex, queue.size) {
            if (currentIndex !in queue.indices) return@LaunchedEffect
            snapshotFlow { listState.layoutInfo.viewportEndOffset > listState.layoutInfo.viewportStartOffset }.first { it }
            listState.scrollToItem(queueScrollTarget(currentIndex))
        }
        LazyColumn(state = listState) {
            itemsIndexed(queue, key = { index, entry -> "$index-${entry.mediaId}" }) { index, entry ->
                val current = index == currentIndex
                SongRow(
                    number = index + 1,
                    title = entry.title,
                    detail = entry.artist,
                    durationSeconds = 0,
                    onClick = { onJump(index) },
                    modifier = if (current) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier,
                    trailing = {
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove from queue")
                        }
                    },
                )
            }
        }
    }
}

/** The first visible row when the panel shows [currentIndex]: two rows of context above it. */
fun queueScrollTarget(currentIndex: Int): Int = (currentIndex - 2).coerceAtLeast(0)
