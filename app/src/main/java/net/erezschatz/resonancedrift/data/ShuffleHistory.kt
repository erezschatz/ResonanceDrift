package net.erezschatz.resonancedrift.data

import net.erezschatz.resonancedrift.data.dao.ShuffleHistoryDao

/**
 * The albums that album shuffle played most recently, so it does not pick them
 * again (task 12). Fifty, as SPEC says; ReDrift kept 200.
 */
class ShuffleHistory(private val dao: ShuffleHistoryDao) {

    /** Records [albumId] as the newest and drops whatever falls past the limit. */
    suspend fun add(albumId: Long) = dao.addAndTrim(albumId, LIMIT)

    suspend fun recentAlbumIds(): List<Long> = dao.albumIdsNewestFirst()

    companion object {
        const val LIMIT = 50
    }
}
