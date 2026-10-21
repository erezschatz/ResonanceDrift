# 03 · Ampache client

## Promise
Every piece of data the app shows comes through one client that is proven
against real responses from my server, so a server-side quirk is caught in a
unit test, not on the phone.

## Acceptance
- [x] `AmpacheClient` implements every action in `docs/AMPACHE.md`
- [x] `@Serializable` DTOs in `ampache/model/`, one file per type, lenient to
      unknown keys; single-object actions unwrap `[x]` vs `x` as ReDrift did
- [x] Error envelope (`{ "error": { errorCode, errorMessage } }`) becomes a typed
      exception; `Session Expired` is distinguished
- [x] Paged fetch helper (5000 per page until a short page) for the album pool
- [x] `AmpacheUrls`: stream and art URLs byte-identical to ReDrift's
- [x] Fixtures captured from **the real server** into `app/src/test/fixtures/`:
      handshake, albums (small limit), album, album_songs, artists, artist,
      artist_albums, genres, genre_albums, genre_songs, playlists,
      playlist_songs, song, advanced_search (album by title; song by title),
      an error response
- [x] A parsing test per fixture; a URL test per URL builder
- [x] No UI changes in this step

## Notes
Capturing fixtures: log in via the auth screen from step 02, then hit the
endpoints with the token in a browser or `curl` and save the bodies verbatim.
Strip nothing — a field we do not model today may matter tomorrow.

`app/src/test/fixtures/capture.sh` does all of it: asks for server, username
and password, hashes like the app, saves each body verbatim, and ends the
session with `goodbye` so the token inside the fixtures is dead when committed.
Long lists (`genre_songs`, `playlist_songs`) are captured with `limit=20`.
Search fixtures query the first album's name and its first song's title.

`FixtureParsingTest` fails with "Missing fixture … run capture.sh" if a
fixture is absent. Run the script, then `./gradlew test`.

Captured 2026-10-21; what they showed is recorded in `docs/AMPACHE.md` →
*What the fixtures showed*. No phone check: this step changes nothing on screen.
