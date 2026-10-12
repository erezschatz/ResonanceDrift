package net.erezschatz.resonancedrift.ampache

/**
 * Ampache disambiguates same-named albums by prefixing the album name with
 * "Artist - ". Showing that in the UI reads as a bug, so strip the prefix when
 * it matches the known artist, or — when no artist is known — take whatever
 * follows the first " - ". Ported verbatim in behaviour from ReDrift's
 * AudioPlayer.updateMediaSession().
 */
object AlbumNames {

    private const val SEPARATOR = " - "

    fun display(albumName: String, artistName: String?): String {
        if (!albumName.contains(SEPARATOR)) return albumName

        if (!artistName.isNullOrEmpty()) {
            val prefix = artistName + SEPARATOR
            return if (albumName.startsWith(prefix)) albumName.removePrefix(prefix) else albumName
        }

        return albumName.substringAfter(SEPARATOR)
    }
}
