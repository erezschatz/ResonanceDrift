package net.erezschatz.resonancedrift.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import net.erezschatz.resonancedrift.ampache.model.Playlist

/** [id] is a String: smart playlists are "smart_<n>" (D22). */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val itemCount: Int,
    val owner: String,
    val type: String,
) {
    companion object {
        fun from(playlist: Playlist) = PlaylistEntity(playlist.id, playlist.name, playlist.items, playlist.owner, playlist.type)
    }
}
