package net.erezschatz.resonancedrift.ampache.model

import kotlinx.serialization.Serializable

/**
 * `song` objects (album_songs, artist_songs, genre_songs, playlist_songs, song,
 * advanced_search type=song). The server's `url` is deliberately not read: it
 * carries the token of the moment. Stream URLs are built by AmpacheUrls.stream.
 */
@Serializable
data class Song(
    @Serializable(with = LenientLongSerializer::class) val id: Long,
    val title: String = "",
    @Serializable(with = LenientIntSerializer::class) val track: Int = 0,
    /** Length in seconds. */
    @Serializable(with = LenientIntSerializer::class) val time: Int = 0,
    @Serializable(with = LenientIntSerializer::class) val year: Int = 0,
    val artist: Ref? = null,
    val albumartist: Ref? = null,
    val album: Ref? = null,
    val genre: List<Ref> = emptyList(),
    val art: String? = null,
)
