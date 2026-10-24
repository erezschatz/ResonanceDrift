package net.erezschatz.resonancedrift.data

import net.erezschatz.resonancedrift.data.dao.QueueStateDao
import net.erezschatz.resonancedrift.data.entity.QueueStateEntity

/** What the service saves and restores — paused — across launches (task 13, D8). */
data class QueueState(
    val songIds: List<Long>,
    val index: Int,
    val positionMs: Long,
    val shuffle: Boolean,
)

/** The persisted queue: one row, overwritten on every save. */
class QueueStateStore(private val dao: QueueStateDao) {

    suspend fun save(state: QueueState) =
        dao.put(QueueStateEntity(songIds = state.songIds, index = state.index, positionMs = state.positionMs, shuffle = state.shuffle))

    suspend fun load(): QueueState? {
        val row = dao.get() ?: return null
        return QueueState(row.songIds, row.index, row.positionMs, row.shuffle)
    }

    suspend fun clear() = dao.clear()
}
