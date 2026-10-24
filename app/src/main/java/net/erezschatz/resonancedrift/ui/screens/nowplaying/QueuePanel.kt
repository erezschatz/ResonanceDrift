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
import androidx.compose.ui.Modifier
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
    // Start with the current item near the top; two rows of context above it.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (currentIndex - 2).coerceAtLeast(0))
    ModalBottomSheet(onDismissRequest = onDismiss) {
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
