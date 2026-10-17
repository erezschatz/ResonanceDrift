# 03 · Ampache client

## Promise
Every piece of data the app shows comes through one client that is proven
against real responses from my server, so a server-side quirk is caught in a
unit test, not on the phone.

## Acceptance
- [ ] `AmpacheClient` implements every action in `docs/AMPACHE.md`
- [ ] `@Serializable` DTOs in `ampache/model/`, one file per type, lenient to
      unknown keys; single-object actions unwrap `[x]` vs `x` as ReDrift did
- [ ] Error envelope (`{ "error": { errorCode, errorMessage } }`) becomes a typed
      exception; `Session Expired` is distinguished
- [ ] Paged fetch helper (5000 per page until a short page) for the album pool
- [ ] `AmpacheUrls`: stream and art URLs byte-identical to ReDrift's
- [ ] Fixtures captured from **the real server** into `app/src/test/fixtures/`:
      handshake, albums (small limit), album, album_songs, artists, artist,
      artist_albums, genres, genre_albums, genre_songs, playlists,
      playlist_songs, song, advanced_search (album by title; song by title),
      an error response
- [ ] A parsing test per fixture; a URL test per URL builder
- [ ] No UI changes in this step

## Notes
Capturing fixtures: log in via the auth screen from step 02, then hit the
endpoints with the token in a browser or `curl` and save the bodies verbatim.
Strip nothing — a field we do not model today may matter tomorrow.
