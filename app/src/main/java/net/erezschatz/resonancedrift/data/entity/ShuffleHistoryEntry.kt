package net.erezschatz.resonancedrift.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One album that album shuffle played. [seq] orders the history (newest = highest);
 * the unique [albumId] means re-adding an album moves it to the front instead
 * of listing it twice.
 */
@Entity(tableName = "shuffle_history", indices = [Index("albumId", unique = true)])
data class ShuffleHistoryEntry(
    @PrimaryKey(autoGenerate = true) val seq: Long = 0,
    val albumId: Long,
)
