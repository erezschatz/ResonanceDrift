package net.erezschatz.resonancedrift.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import net.erezschatz.resonancedrift.data.entity.ShuffleHistoryEntry

@Dao
abstract class ShuffleHistoryDao {

    /** REPLACE: an album already in the history loses its old row and gets a new, newest seq. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insert(entry: ShuffleHistoryEntry)

    @Query("DELETE FROM shuffle_history WHERE seq NOT IN (SELECT seq FROM shuffle_history ORDER BY seq DESC LIMIT :keep)")
    protected abstract suspend fun trimTo(keep: Int)

    @Transaction
    open suspend fun addAndTrim(albumId: Long, keep: Int) {
        insert(ShuffleHistoryEntry(albumId = albumId))
        trimTo(keep)
    }

    @Query("SELECT albumId FROM shuffle_history ORDER BY seq DESC")
    abstract suspend fun albumIdsNewestFirst(): List<Long>
}
