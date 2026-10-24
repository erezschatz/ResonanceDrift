package net.erezschatz.resonancedrift.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import net.erezschatz.resonancedrift.ampache.model.Ref
import net.erezschatz.resonancedrift.ampache.model.Song

/**
 * One song, wherever it was seen (an album, a playlist, a genre). [toSong]
 * turns it back into the server shape, so playback builds MediaItems the same
 * way from cached and fresh songs.
 */
@Entity(tableName = "songs", indices = [Index("albumId")])
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val track: Int,
    val durationSeconds: Int,
    val year: Int,
    val artistId: Long?,
    val artistName: String,
    val albumArtistId: Long?,
    val albumArtistName: String,
    val albumId: Long?,
    val albumName: String,
    /** Stored as JSON (Db's converters); only the Now Playing genre line reads it. */
    val genres: List<GenreTag>,
) {
    fun toSong() = Song(
        id = id,
        title = title,
        track = track,
        time = durationSeconds,
        year = year,
        artist = artistId?.let { Ref(it, artistName) },
        albumartist = albumArtistId?.let { Ref(it, albumArtistName) },
        album = albumId?.let { Ref(it, albumName) },
        genre = genres.map { Ref(it.id, it.name) },
    )

    companion object {
        fun from(song: Song) = SongEntity(
            id = song.id,
            title = song.title,
            track = song.track,
            durationSeconds = song.time,
            year = song.year,
            artistId = song.artist?.id,
            artistName = song.artist?.name.orEmpty(),
            albumArtistId = song.albumartist?.id,
            albumArtistName = song.albumartist?.name.orEmpty(),
            albumId = song.album?.id,
            albumName = song.album?.name.orEmpty(),
            genres = song.genre.map { GenreTag(it.id, it.name) },
        )
    }
}

@Serializable
data class GenreTag(val id: Long, val name: String)
