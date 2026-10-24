package net.erezschatz.resonancedrift.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The one saved queue (task 13 writes and restores it). Always the row with [key] 0. */
@Entity(tableName = "queue_state")
data class QueueStateEntity(
    @PrimaryKey val key: Int = 0,
    /** Stored as "1,2,3". */
    val songIds: List<Long>,
    val index: Int,
    val positionMs: Long,
    val shuffle: Boolean,
)
