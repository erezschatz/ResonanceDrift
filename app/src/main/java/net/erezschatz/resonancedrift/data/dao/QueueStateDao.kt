package net.erezschatz.resonancedrift.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import net.erezschatz.resonancedrift.data.entity.QueueStateEntity

@Dao
interface QueueStateDao {

    @Upsert
    suspend fun put(state: QueueStateEntity)

    @Query("SELECT * FROM queue_state WHERE `key` = 0")
    suspend fun get(): QueueStateEntity?

    @Query("DELETE FROM queue_state")
    suspend fun clear()
}
