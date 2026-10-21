package net.erezschatz.resonancedrift.ampache.model

import kotlinx.serialization.Serializable

/**
 * `album` objects (albums, album, artist_albums, genre_albums, advanced_search
 * type=album). [name] may be "Artist - Album" — use AlbumNames.display().
 */
@Serializable
data class Album(
    @Serializable(with = LenientLongSerializer::class) val id: Long,
    val name: String = "",
    val artist: Ref? = null,
    @Serializable(with = LenientIntSerializer::class) val year: Int = 0,
    @Serializable(with = LenientIntSerializer::class) val songcount: Int = 0,
    /** Total length in seconds. */
    @Serializable(with = LenientIntSerializer::class) val time: Int = 0,
    /** Server-built art URL; unused (we build our own, AmpacheUrls.art) but kept for diagnostics. */
    val art: String? = null,
    val genre: List<Ref> = emptyList(),
)
