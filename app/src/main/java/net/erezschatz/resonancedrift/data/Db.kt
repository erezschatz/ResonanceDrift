package net.erezschatz.resonancedrift.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import net.erezschatz.resonancedrift.data.dao.LibraryDao
import net.erezschatz.resonancedrift.data.dao.PlaylistSongsDao
import net.erezschatz.resonancedrift.data.dao.QueueStateDao
import net.erezschatz.resonancedrift.data.dao.ShuffleHistoryDao
import net.erezschatz.resonancedrift.data.entity.AlbumEntity
import net.erezschatz.resonancedrift.data.entity.ArtistEntity
import net.erezschatz.resonancedrift.data.entity.GenreEntity
import net.erezschatz.resonancedrift.data.entity.GenreTag
import net.erezschatz.resonancedrift.data.entity.PlaylistEntity
import net.erezschatz.resonancedrift.data.entity.PlaylistSongsEntity
import net.erezschatz.resonancedrift.data.entity.QueueStateEntity
import net.erezschatz.resonancedrift.data.entity.ShuffleHistoryEntry
import net.erezschatz.resonancedrift.data.entity.SongEntity

/**
 * The one Room database (built in AppGraph). Schema JSON per version is
 * exported to app/schemas/. A version bump needs a Migration here: no
 * destructive fallback, because queue_state and shuffle_history are not
 * re-fetchable from the server.
 */
@Database(
    version = 1,
    entities = [
        AlbumEntity::class,
        ArtistEntity::class,
        GenreEntity::class,
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongsEntity::class,
        ShuffleHistoryEntry::class,
        QueueStateEntity::class,
    ],
)
@TypeConverters(Converters::class)
abstract class Db : RoomDatabase() {
    abstract fun library(): LibraryDao
    abstract fun playlistSongs(): PlaylistSongsDao
    abstract fun shuffleHistory(): ShuffleHistoryDao
    abstract fun queueState(): QueueStateDao

    companion object {
        const val FILE_NAME = "library.db"
    }
}

/** The two non-column types: id lists as "1,2,3", genre lists as JSON. */
class Converters {

    @TypeConverter
    fun idsToText(ids: List<Long>): String = ids.joinToString(",")

    @TypeConverter
    fun textToIds(text: String): List<Long> =
        if (text.isEmpty()) emptyList() else text.split(",").map { it.toLong() }

    @TypeConverter
    fun genresToText(genres: List<GenreTag>): String = Json.encodeToString(GENRES, genres)

    @TypeConverter
    fun textToGenres(text: String): List<GenreTag> = Json.decodeFromString(GENRES, text)

    private companion object {
        // Typed explicitly: KSP (Room) runs before the serialization plugin makes GenreTag.serializer().
        val GENRES: KSerializer<List<GenreTag>> = ListSerializer(GenreTag.serializer())
    }
}
