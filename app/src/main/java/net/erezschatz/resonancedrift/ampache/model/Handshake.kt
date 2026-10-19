package net.erezschatz.resonancedrift.ampache.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `handshake` response. `auth` and `session_expire` are required (checked by
 * the client, not by defaults here); the library counts are optional extras.
 */
@Serializable
data class Handshake(
    val auth: String = "",
    @SerialName("session_expire") val sessionExpire: String = "",
    val api: String? = null,
    val songs: Long? = null,
    val albums: Long? = null,
)
