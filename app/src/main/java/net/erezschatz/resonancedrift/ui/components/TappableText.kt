package net.erezschatz.resonancedrift.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import net.erezschatz.resonancedrift.data.entity.GenreTag

/**
 * Text that navigates when tapped but looks like plain text: no underline, no
 * link colour, no ripple — only a dip in alpha while pressed (SPEC → UI).
 * With [onClick] null it is just text.
 */
@Composable
fun TappableText(
    text: String,
    style: TextStyle,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    textAlign: TextAlign? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val tappable = if (onClick != null) {
        Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
    } else {
        Modifier
    }
    Text(
        text = text,
        style = style,
        color = color,
        textAlign = textAlign,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.then(tappable).alpha(if (pressed) PRESSED_ALPHA else 1f),
    )
}

private const val PRESSED_ALPHA = 0.5f

/** Now Playing's genre line: the first two genres joined with " · " (ReDrift TODO 2a); "" when none. */
fun genreLine(genres: List<GenreTag>): String = genres.take(2).joinToString(" · ") { it.name }
