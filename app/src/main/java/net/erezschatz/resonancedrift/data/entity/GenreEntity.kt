package net.erezschatz.resonancedrift.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import net.erezschatz.resonancedrift.ampache.model.Genre

/** The counts are what the Genres list shows beside each name. */
@Entity(tableName = "genres")
data class GenreEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val albumCount: Int,
    val artistCount: Int,
    val songCount: Int,
) {
    companion object {
        fun from(genre: Genre) = GenreEntity(genre.id, genre.name, genre.albums, genre.artists, genre.songs)
    }
}
