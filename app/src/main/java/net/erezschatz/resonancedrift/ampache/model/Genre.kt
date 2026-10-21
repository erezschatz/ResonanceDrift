package net.erezschatz.resonancedrift.ampache.model

import kotlinx.serialization.Serializable

/** `genre` objects (genres, genre). The counts are what the Genres list shows. */
@Serializable
data class Genre(
    @Serializable(with = LenientLongSerializer::class) val id: Long,
    val name: String = "",
    @Serializable(with = LenientIntSerializer::class) val albums: Int = 0,
    @Serializable(with = LenientIntSerializer::class) val artists: Int = 0,
    @Serializable(with = LenientIntSerializer::class) val songs: Int = 0,
)
