package net.erezschatz.resonancedrift.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A playlist's song order: one row per playlist, as ReDrift kept it. The songs
 * themselves are in `songs`. [version] goes up on every write; a refresh only
 * writes if the version is still the one it read before fetching, so a removal
 * made while a slow fetch was in flight is not undone (PlaylistSongsDao).
 */
@Entity(tableName = "playlist_songs")
data class PlaylistSongsEntity(
    @PrimaryKey val playlistId: String,
    /** Stored as "1,2,3"; a song may appear more than once. */
    val songIds: List<Long>,
    val version: Int,
)
