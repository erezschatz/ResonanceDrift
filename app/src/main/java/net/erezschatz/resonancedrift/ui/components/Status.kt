package net.erezschatz.resonancedrift.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.erezschatz.resonancedrift.data.Refresh

/**
 * The one line under a list's header that says why the list is empty or
 * possibly stale: a spinner while the first load runs, the error when a load
 * failed, [emptyText] when the server has nothing. Nothing when items show and
 * all is well — a background refresh of cached items is silent.
 */
@Composable
fun ListStatus(refresh: Refresh, isEmpty: Boolean, emptyText: String) {
    when {
        refresh is Refresh.Failed -> StatusText(
            if (isEmpty) refresh.error.userMessage else "Showing saved results. ${refresh.error.userMessage}",
        )
        isEmpty && refresh == Refresh.Running -> Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
            CircularProgressIndicator()
        }
        isEmpty -> StatusText(emptyText)
    }
}

@Composable
fun StatusText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
    )
}
