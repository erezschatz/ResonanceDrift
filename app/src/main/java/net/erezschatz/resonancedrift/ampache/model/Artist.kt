package net.erezschatz.resonancedrift.ampache.model

import kotlinx.serialization.Serializable

/** `artist` objects (artists, artist). */
@Serializable
data class Artist(
    @Serializable(with = LenientLongSerializer::class) val id: Long,
    val name: String = "",
    @Serializable(with = LenientIntSerializer::class) val albumcount: Int = 0,
    @Serializable(with = LenientIntSerializer::class) val songcount: Int = 0,
)
