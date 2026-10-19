package net.erezschatz.resonancedrift.ampache.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `ping` response. With a live token it carries a fresh `session_expire`;
 * with a dead one Ampache still answers 200 but leaves it out — the client
 * treats that absence as an expired session.
 */
@Serializable
data class Ping(
    @SerialName("session_expire") val sessionExpire: String? = null,
)
