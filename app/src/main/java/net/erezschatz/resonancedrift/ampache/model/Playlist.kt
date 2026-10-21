package net.erezschatz.resonancedrift.ampache.model

import kotlinx.serialization.Serializable

/**
 * `playlist` objects (playlists, playlist). [id] is a String, unlike every
 * other id: Ampache lists smart playlists as "smart_<n>" alongside numeric
 * ones, and passes the same string back as `filter`. Decision D22.
 */
@Serializable
data class Playlist(
    val id: String,
    val name: String = "",
    @Serializable(with = LenientIntSerializer::class) val items: Int = 0,
    val owner: String = "",
    val type: String = "",
)
