package net.erezschatz.resonancedrift.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import net.erezschatz.resonancedrift.data.entity.PlaylistSongsEntity

/**
 * The version-counter rule from ReDrift (docs/SPEC.md → Data): every write
 * bumps `version`, and a refresh writes only if the version has not moved
 * since it started. A removal during a slow fetch therefore wins over the
 * fetch's older list.
 */
@Dao
abstract class PlaylistSongsDao {

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId")
    abstract fun observe(playlistId: String): Flow<PlaylistSongsEntity?>

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId")
    abstract suspend fun get(playlistId: String): PlaylistSongsEntity?

    @Upsert
    protected abstract suspend fun put(row: PlaylistSongsEntity)

    /** The version a refresh reads before fetching; 0 when nothing is cached yet. */
    suspend fun versionOf(playlistId: String): Int = get(playlistId)?.version ?: 0

    /**
     * Writes a fetched list unless the playlist was written since [versionAtStart]
     * was read. Returns whether it wrote.
     */
    @Transaction
    open suspend fun replaceIfUnchanged(playlistId: String, versionAtStart: Int, songIds: List<Long>): Boolean {
        val current = versionOf(playlistId)
        if (current != versionAtStart) return false
        put(PlaylistSongsEntity(playlistId, songIds, current + 1))
        return true
    }

    /**
     * Removes the song at [index] — or, if the list has shifted and [songId] is
     * no longer there, its first occurrence — and bumps the version either way,
     * so a refresh already in flight cannot put it back.
     */
    @Transaction
    open suspend fun removeSong(playlistId: String, index: Int, songId: Long) {
        val current = get(playlistId) ?: PlaylistSongsEntity(playlistId, emptyList(), 0)
        val songIds = current.songIds.toMutableList()
        val at = if (songIds.getOrNull(index) == songId) index else songIds.indexOf(songId)
        if (at >= 0) songIds.removeAt(at)
        put(PlaylistSongsEntity(playlistId, songIds, current.version + 1))
    }
}
