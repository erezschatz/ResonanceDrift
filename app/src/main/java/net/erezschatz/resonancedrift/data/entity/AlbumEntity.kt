package net.erezschatz.resonancedrift.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import net.erezschatz.resonancedrift.ampache.model.Album

/** One album as the list and detail screens show it. */
@Entity(tableName = "albums", indices = [Index("artistId")])
data class AlbumEntity(
    @PrimaryKey val id: Long,
    /** As the server sends it; may be "Artist - Album" — display through AlbumNames.display(). */
    val name: String,
    val artistId: Long?,
    val artistName: String,
    val year: Int,
    val songCount: Int,
    val durationSeconds: Int,
) {
    companion object {
        fun from(album: Album) = AlbumEntity(
            id = album.id,
            name = album.name,
            artistId = album.artist?.id,
            artistName = album.artist?.name.orEmpty(),
            year = album.year,
            songCount = album.songcount,
            durationSeconds = album.time,
        )
    }
}
