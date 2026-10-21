package net.erezschatz.resonancedrift.ampache.model

import kotlinx.serialization.Serializable

/**
 * The `{ "id", "name" }` pair Ampache nests inside other objects: a song's
 * artist and album, an album's artist, the genre list on either.
 */
@Serializable
data class Ref(
    @Serializable(with = LenientLongSerializer::class) val id: Long,
    val name: String = "",
)
