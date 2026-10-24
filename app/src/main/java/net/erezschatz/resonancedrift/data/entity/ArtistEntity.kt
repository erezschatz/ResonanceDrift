package net.erezschatz.resonancedrift.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import net.erezschatz.resonancedrift.ampache.model.Artist

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val albumCount: Int,
    val songCount: Int,
) {
    companion object {
        fun from(artist: Artist) = ArtistEntity(artist.id, artist.name, artist.albumcount, artist.songcount)
    }
}
