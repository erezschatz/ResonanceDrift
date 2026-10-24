package net.erezschatz.resonancedrift.playback

import android.os.Bundle
import androidx.media3.session.SessionCommand

/**
 * The custom session commands the UI sends to the service, and the session
 * extras the service answers with. Album shuffle lives in the service; these
 * are its only doorway (docs/SPEC.md → Playback).
 */
object Commands {
    /** Args: [ARG_ALBUM_ID] for "shuffle from this album"; absent = a random album. */
    val SHUFFLE_START = SessionCommand("net.erezschatz.resonancedrift.SHUFFLE_START", Bundle.EMPTY)

    /** Keep the queue; random albums follow it (Now Playing's shuffle button). */
    val SHUFFLE_CONTINUE = SessionCommand("net.erezschatz.resonancedrift.SHUFFLE_CONTINUE", Bundle.EMPTY)

    val SHUFFLE_STOP = SessionCommand("net.erezschatz.resonancedrift.SHUFFLE_STOP", Bundle.EMPTY)
    val NEXT_ALBUM = SessionCommand("net.erezschatz.resonancedrift.NEXT_ALBUM", Bundle.EMPTY)

    val ALL = listOf(SHUFFLE_START, SHUFFLE_CONTINUE, SHUFFLE_STOP, NEXT_ALBUM)

    const val ARG_ALBUM_ID = "albumId"

    /** Session extras: [ShuffleState] name, and the reason when a start failed. */
    const val EXTRA_SHUFFLE_STATE = "shuffleState"
    const val EXTRA_SHUFFLE_MESSAGE = "shuffleMessage"

    fun startArgs(albumId: Long?): Bundle = Bundle().apply { if (albumId != null) putLong(ARG_ALBUM_ID, albumId) }

    fun extras(state: ShuffleState, message: String?): Bundle = Bundle().apply {
        putString(EXTRA_SHUFFLE_STATE, state.name)
        if (message != null) putString(EXTRA_SHUFFLE_MESSAGE, message)
    }

    fun stateOf(extras: Bundle): ShuffleState =
        extras.getString(EXTRA_SHUFFLE_STATE)?.let { name -> ShuffleState.entries.firstOrNull { it.name == name } } ?: ShuffleState.OFF
}
